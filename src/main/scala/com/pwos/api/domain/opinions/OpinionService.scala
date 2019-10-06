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

  def list(userInfo: UserInfo, maybePlaceId: Option[Long], queryParameters: QueryParameters, pagingRequest: PagingRequest): F[PaginatedResult[OpinionView]] = {
    maybePlaceId map { placeId =>
      opinionDAO.listForPlace(placeId, pagingRequest)
    } getOrElse {
      opinionDAO.listAll(queryParameters, pagingRequest)
    } map { result =>
      result mapResult { case (opinion, tagsNames, likesIds) =>
        OpinionView(opinion, tagsNames, OpinionLikes.fromListOfIds(userInfo.id, likesIds))
      }
    }
  }

  def addOpinion(userInfo: UserInfo, placeId: Long, createOpinionModel: CreateOpinionModel): EitherT[F, PlaceNotFoundError.type, Boolean] = {
    for {
      _ <- placeValidation.exists(placeId)
      opinion = Opinion.fromCreateOpinionModel(placeId, userInfo.id, createOpinionModel)
      newOpinion <- EitherT.liftF(opinionDAO.create(opinion))
      addTagsResult <- EitherT.liftF(opinionDAO.addTags(newOpinion.id.get, createOpinionModel.tagsIds))
    } yield addTagsResult
  }

  def getOpinionView(userInfo: UserInfo, opinionId: Long): EitherT[F, OpinionNotFoundError.type, OpinionView] = {
    EitherT.fromOptionF(opinionDAO.get(opinionId), OpinionNotFoundError) map { case (opinion, tagsNames, likesIds) =>
      OpinionView(opinion, tagsNames, OpinionLikes.fromListOfIds(userInfo.id, likesIds))
    }
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

  def updateOpinion(userInfo: UserInfo, opinionId: Long, updateOpinionModel: UpdateOpinionModel): EitherT[F, OpinionValidationError, Boolean] = {
    type OpinionUpdate = Opinion => Option[Opinion]

    val updateLastModified: OpinionUpdate = opinion => Option(opinion.copy(lastModified = DateTime.now))
    val updateBody: OpinionUpdate = opinion => updateOpinionModel.body.map(body => opinion.copy(body = Option(body)))
    val updateReferenceDate: OpinionUpdate = opinion => updateOpinionModel.referenceDate.map(referenceDate => opinion.copy(referenceDate = referenceDate))

    val updates: List[OpinionUpdate] = List(updateLastModified, updateBody, updateReferenceDate)

    val updateOpinionData: Opinion => Opinion = oldOpinion => {
      updates.foldLeft(oldOpinion) { (opinion, updateFun) =>
        updateFun(opinion).getOrElse(opinion)
      }
    }

    def updateTags: F[Boolean] = {
      updateOpinionModel.tagsIds map { tagsIds =>
        opinionDAO.removeTags(opinionId) >> opinionDAO.addTags(opinionId, tagsIds)
      } getOrElse {
        Monad[F].pure(true)
      }
    }

    for {
      _ <- opinionValidation.exists(opinionId)
      _ <- validateOwnershipIfNonAdmin(userInfo.role)(userInfo.id, opinionId)
      opinionToUpdate <- getOpinionView(userInfo, opinionId).map(_.opinion)
      updatedOpinion = updateOpinionData(opinionToUpdate)
      updateOpinionResult <- EitherT.liftF(opinionDAO.update(updatedOpinion))
      updateTagsResult <- EitherT.liftF(updateTags)
    } yield updateOpinionResult && updateTagsResult
  }

  def reportOpinion(userInfo: UserInfo, opinionId: Long, reportOpinionModel: ReportOpinionModel): EitherT[F, OpinionNotFoundError.type, Boolean] = {
    for {
      _ <- opinionValidation.exists(opinionId)
      opinion <- getOpinionView(userInfo, opinionId).map(_.opinion)
      report = Report.fromReportOpinionModel(userInfo.id, opinion.id.get, reportOpinionModel)
      _ <- EitherT.liftF(reportDAO.create(report))
    } yield true
  }

  def updateOpinionStatus(userInfo: UserInfo, opinionId: Long, updateOpinionStatusModel: UpdateOpinionStatusModel): EitherT[F, OpinionNotFoundError.type, Boolean] = {
    for {
      _ <- opinionValidation.exists(opinionId)
      opinion <- getOpinionView(userInfo, opinionId).map(_.opinion)
      updateResult <- EitherT.liftF(opinionDAO.update(opinion.copy(deleted = updateOpinionStatusModel.blocked)))
    } yield updateResult
  }

  def updateOpinionLikes(userInfo: UserInfo, opinionId: Long, updateOpinionLikesModel: UpdateOpinionLikesModel): EitherT[F, OpinionValidationError, Boolean] = {

    def likeOrUnlikeOpinion(opinionLikes: OpinionLikes): EitherT[F, OpinionValidationError, Boolean] = {
      if (updateOpinionLikesModel.like) {
        if (opinionLikes.likedByYou) {
          EitherT.leftT[F, Boolean](OpinionAlreadyLikedError : OpinionValidationError)
        } else {
          EitherT.liftF(opinionDAO.addLike(opinionId, userInfo.id))
        }
      } else {
        if (opinionLikes.likedByYou) {
          EitherT.liftF(opinionDAO.removeLike(opinionId, userInfo.id))
        } else {
          EitherT.leftT[F, Boolean](OpinionWasNotLikedError : OpinionValidationError)
        }
      }
    }

    for {
      _ <- opinionValidation.exists(opinionId)
      opinionLikes <- getOpinionView(userInfo, opinionId).map(_.likes)
      updatedOpinionOpinionLikesResult <- likeOrUnlikeOpinion(opinionLikes)
    } yield updatedOpinionOpinionLikesResult
  }

  def reports(userInfo: UserInfo, opinionId: Long): EitherT[F, OpinionNotFoundError.type, List[ReportView]] = {

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
      opinion <- getOpinionView(userInfo, opinionId).map(_.opinion)
      reports <- EitherT.liftF(reportDAO.list(opinion.id.get))
      reportAuthors <- EitherT.liftF(collectReportAuthors(reports))
      reportViews = buildReportViews(reports, reportAuthors)
    } yield reportViews
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
