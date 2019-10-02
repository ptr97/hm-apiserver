package com.pwos.api.domain.opinions

import cats.Monad
import cats.data.EitherT
import cats.implicits._
import com.pwos.api.PaginatedResult
import com.pwos.api.domain.HelloMountainsError.OpinionValidationError
import com.pwos.api.domain.HelloMountainsError._
import com.pwos.api.domain.PagingRequest
import com.pwos.api.domain.QueryParameters
import com.pwos.api.domain.opinions.OpinionModels._
import com.pwos.api.domain.opinions.reports.Report
import com.pwos.api.domain.opinions.reports.ReportDAOAlgebra
import com.pwos.api.domain.opinions.reports.ReportView
import com.pwos.api.domain.places.PlaceValidationAlgebra
import com.pwos.api.domain.users.User
import com.pwos.api.domain.users.UserDAOAlgebra
import com.pwos.api.domain.users.UserInfo
import com.pwos.api.domain.users.UserRole
import com.pwos.api.domain.users.UserRole.UserRole
import org.joda.time.DateTime


class OpinionService[F[_] : Monad](
  opinionDAO: OpinionDAOAlgebra[F],
  reportDAO: ReportDAOAlgebra[F],
  userDAO: UserDAOAlgebra[F],
  opinionValidation: OpinionValidationAlgebra[F],
  placeValidation: PlaceValidationAlgebra[F]) {

  def list(maybePlaceId: Option[Long], queryParameters: QueryParameters, pagingRequest: PagingRequest): F[PaginatedResult[Opinion]] = {
    maybePlaceId map { placeId =>
      opinionDAO.listForPlace(placeId, pagingRequest)
    } getOrElse {
      opinionDAO.listAll(queryParameters, pagingRequest)
    }
  }

  def addOpinion(userInfo: UserInfo, placeId: Long, createOpinionModel: CreateOpinionModel): EitherT[F, PlaceNotFoundError.type, Opinion] = {
    for {
      _ <- placeValidation.exists(placeId)
      opinion = Opinion.fromCreateOpinionModel(placeId, userInfo.id, createOpinionModel)
      newOpinion <- EitherT.liftF(opinionDAO.create(opinion))
    } yield newOpinion
  }

  def getOpinion(opinionId: Long): EitherT[F, OpinionNotFoundError.type, Opinion] = {
    EitherT.fromOptionF(opinionDAO.get(opinionId), OpinionNotFoundError)
  }

  private def validateOwnershipIfNonAdmin(userRole: UserRole): (Long, Long) => EitherT[F, OpinionOwnershipError.type, Unit] = {
    (userId, opinionId) => {
      if (userRole == UserRole.Admin) {
        EitherT.rightT(())
      } else {
        opinionValidation.validateOwnership(userId, opinionId)
      }
    }
  }

  def deleteOpinion(userInfo: UserInfo, opinionId: Long): EitherT[F, OpinionValidationError, Boolean] = {
    for {
      _ <- opinionValidation.exists(opinionId)
      _ <- validateOwnershipIfNonAdmin(userInfo.role)(userInfo.id, opinionId)
      deleteResult <- EitherT.liftF(opinionDAO.markDeleted(opinionId))
    } yield deleteResult
  }

  def updateOpinion(userInfo: UserInfo, opinionId: Long, updateOpinionModel: UpdateOpinionModel): EitherT[F, OpinionValidationError, Opinion] = {
    type OpinionUpdate = Opinion => Option[Opinion]

    val updateLastModified: OpinionUpdate = opinion => Option(opinion.copy(lastModified = DateTime.now))
    val updateBody: OpinionUpdate = opinion => updateOpinionModel.body.map(body => opinion.copy(body = Option(body)))
    val updateTags: OpinionUpdate = opinion => updateOpinionModel.tags.map(tags => opinion.copy(tags = tags))
    val updateReferenceDate: OpinionUpdate = opinion => updateOpinionModel.referenceDate.map(referenceDate => opinion.copy(referenceDate = referenceDate))

    val updates: List[OpinionUpdate] = List(updateLastModified, updateBody, updateTags, updateReferenceDate)

    val updateOpinionData: Opinion => Opinion = oldOpinion => {
      updates.foldLeft(oldOpinion) { (opinion, updateFun) =>
        updateFun(opinion).getOrElse(opinion)
      }
    }

    for {
      _ <- opinionValidation.exists(opinionId)
      _ <- validateOwnershipIfNonAdmin(userInfo.role)(userInfo.id, opinionId)
      opinionToUpdate <- getOpinion(opinionId)
      updatedOpinion = updateOpinionData(opinionToUpdate)
      updateResult <- EitherT.fromOptionF(opinionDAO.update(updatedOpinion), OpinionNotFoundError: OpinionValidationError)
    } yield updateResult
  }

  def reportOpinion(userInfo: UserInfo, opinionId: Long, reportOpinionModel: ReportOpinionModel): EitherT[F, OpinionNotFoundError.type, Boolean] = {
    for {
      _ <- opinionValidation.exists(opinionId)
      opinion <- getOpinion(opinionId)
      report = Report.fromReportOpinionModel(userInfo.id, opinion.id.get, reportOpinionModel)
      _ <- EitherT.liftF(reportDAO.create(report))
    } yield true
  }

  def updateOpinionStatus(userInfo: UserInfo, opinionId: Long, updateOpinionStatusModel: UpdateOpinionStatusModel): EitherT[F, OpinionNotFoundError.type, Opinion] = {
    for {
      _ <- opinionValidation.exists(opinionId)
      opinion <- getOpinion(opinionId)
      updatedOpinion <- EitherT.fromOptionF(opinionDAO.update(opinion.copy(deleted = updateOpinionStatusModel.blocked)), OpinionNotFoundError)
    } yield updatedOpinion
  }

  def updateOpinionLikes(userInfo: UserInfo, opinionId: Long, updateOpinionLikesModel: UpdateOpinionLikesModel): EitherT[F, OpinionValidationError, Opinion] = {

    def likeOrUnlikeOpinion(opinion: Opinion): Either[OpinionValidationError, Opinion] = {
      val newLikesOrError: Either[OpinionValidationError, List[String]] = if (updateOpinionLikesModel.like) {
        Either.fromOption(addLike(opinion.likes), OpinionAlreadyLikedError: OpinionValidationError)
      } else {
        Either.fromOption(removeLike(opinion.likes), OpinionWasNotLikedError: OpinionValidationError)
      }

      newLikesOrError map { likes =>
        opinion.copy(likes = likes)
      }
    }

    def addLike(likes: List[String]): Option[List[String]] = {
      if (likes.contains(userInfo.userName)) {
        None
      } else {
        Some(userInfo.userName :: likes)
      }
    }

    def removeLike(likes: List[String]): Option[List[String]] = {
      if (likes.contains(userInfo.userName)) {
        Some(likes.filterNot(_ == userInfo.userName))
      } else {
        None
      }
    }

    for {
      _ <- opinionValidation.exists(opinionId)
      opinion <- getOpinion(opinionId)
      updatedOpinion <- EitherT.fromEither(likeOrUnlikeOpinion(opinion)) : EitherT[F, OpinionValidationError, Opinion]
      updateOpinionResult <- EitherT.fromOptionF(opinionDAO.update(updatedOpinion), OpinionNotFoundError: OpinionValidationError)
    } yield updateOpinionResult
  }

  def reports(opinionId: Long, queryParameters: QueryParameters, pagingRequest: PagingRequest): EitherT[F, OpinionNotFoundError.type, PaginatedResult[ReportView]] = {

    def collectReportAuthors(reports: List[Report]): F[Map[Long, User]] = {
      val authorsIds: List[Long] = reports.map(_.authorId)
      val authorsF: F[List[User]] = userDAO.get(authorsIds)

      authorsF.map { authors: List[User] =>
        reports.foldLeft(Map.empty[Long, User]) { (acc, report) =>
          val author: User = authors.find(_.id == Option(report.authorId)).get
          acc + (report.id.get -> author)
        }
      }
    }

    def buildReportViews(reports: List[Report], authorsMapping: Map[Long, User]): List[ReportView] = {
      reports.map { report: Report =>
        val author: User = authorsMapping(report.id.get)
        ReportView.fromReport(author.id.get, author.userName, report)
      }
    }

    for {
      _ <- opinionValidation.exists(opinionId)
      opinion <- getOpinion(opinionId)
      reportsPaginated <- EitherT.liftF(reportDAO.list(opinion.id.get))
      reportAuthors <- EitherT.liftF(collectReportAuthors(reportsPaginated.items))
      reportViews = buildReportViews(reportsPaginated.items, reportAuthors)
    } yield reportsPaginated.copy(items = reportViews)
  }

}

object OpinionService {
  def apply[F[_] : Monad](
    opinionDAO: OpinionDAOAlgebra[F],
    reportDAO: ReportDAOAlgebra[F],
    userDAO: UserDAOAlgebra[F],
    opinionValidation: OpinionValidationAlgebra[F],
    placeValidation: PlaceValidationAlgebra[F]): OpinionService[F] =
    new OpinionService(opinionDAO, reportDAO, userDAO, opinionValidation, placeValidation)
}
