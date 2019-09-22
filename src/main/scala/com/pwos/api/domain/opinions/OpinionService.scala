package com.pwos.api.domain.opinions

import cats.Monad
import cats.data.EitherT
import com.pwos.api.PaginatedResult
import com.pwos.api.domain.HelloMountainsError._
import com.pwos.api.domain.PagingRequest
import com.pwos.api.domain.QueryParameters
import com.pwos.api.domain.opinions.OpinionModels._
import com.pwos.api.domain.opinions.reports.Report
import com.pwos.api.domain.places.PlaceValidationAlgebra
import com.pwos.api.domain.users.UserInfo
import com.pwos.api.domain.users.UserRole
import com.pwos.api.domain.users.UserRole.UserRole
import org.joda.time.DateTime


class OpinionService[F[_] : Monad](opinionDAO: OpinionDAOAlgebra[F], opinionValidation: OpinionValidationAlgebra[F], placeValidation: PlaceValidationAlgebra[F]) {

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
      opinion = Opinion(
        uuid = Opinion.generateUUID,
        placeId = placeId,
        authorId = userInfo.id,
        body = createOpinionModel.body,
        tags = createOpinionModel.tags,
        referenceDate = createOpinionModel.referenceDate.getOrElse(DateTime.now()))
      newOpinion <- EitherT.liftF(opinionDAO.create(opinion))
    } yield newOpinion
  }

  def getOpinion(opinionUUID: String): EitherT[F, OpinionNotFoundError.type, Opinion] = {
    EitherT.fromOptionF(opinionDAO.get(opinionUUID), OpinionNotFoundError)
  }

  private def validateOwnershipIfNonAdmin(userRole: UserRole): (Long, String) => EitherT[F, OpinionOwnershipError.type, Unit] = {
    (userId, opinionUUID) => {
      if (userRole == UserRole.Admin) {
        EitherT.rightT(())
      } else {
        opinionValidation.validateOwnership(userId, opinionUUID)
      }
    }
  }

  def deleteOpinion(userInfo: UserInfo, opinionUUID: String): EitherT[F, OpinionValidationError, Boolean] = {
    for {
      _ <- opinionValidation.exists(opinionUUID)
      _ <- validateOwnershipIfNonAdmin(userInfo.role)(userInfo.id, opinionUUID)
      deleteResult <- EitherT.liftF(opinionDAO.markDeleted(opinionUUID))
    } yield deleteResult
  }

  def updateOpinion(userInfo: UserInfo, opinionUUID: String, updateOpinionModel: UpdateOpinionModel): EitherT[F, OpinionValidationError, Opinion] = {
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
      _ <- opinionValidation.exists(opinionUUID)
      _ <- validateOwnershipIfNonAdmin(userInfo.role)(userInfo.id, opinionUUID)
      opinionToUpdate <- getOpinion(opinionUUID)
      updatedOpinion = updateOpinionData(opinionToUpdate)
      updateResult <- EitherT.fromOptionF(opinionDAO.update(updatedOpinion), OpinionNotFoundError : OpinionValidationError)
    } yield updateResult
  }

  def reportOpinion(userInfo: UserInfo, opinionUUID: String, reportOpinionModel: ReportOpinionModel): EitherT[F, OpinionNotFoundError.type, Boolean] = {
    for {
      _ <- opinionValidation.exists(opinionUUID)
      opinion <- getOpinion(opinionUUID)
      report = Report.fromReportOpinionModel(userInfo.id, opinion.id.get, reportOpinionModel)
//      reportResult <- EitherT.fromOptionT(reportDAO.reportOpinion(report))
    } yield ???
  }

  def updateOpinionStatus(userInfo: UserInfo, opinionUUID: String, updateOpinionStatusModel: UpdateOpinionStatusModel): EitherT[F, OpinionNotFoundError.type, Opinion] = {
    for {
      _ <- opinionValidation.exists(opinionUUID)
      opinion <- getOpinion(opinionUUID)
      updatedOpinion <- EitherT.fromOptionF(opinionDAO.update(opinion.copy(deleted = updateOpinionStatusModel.blocked)), OpinionNotFoundError)
    } yield updatedOpinion
  }

}

object OpinionService {
  def apply[F[_] : Monad](opinionDAO: OpinionDAOAlgebra[F], opinionValidation: OpinionValidationAlgebra[F], placeValidation: PlaceValidationAlgebra[F]): OpinionService[F] =
    new OpinionService(opinionDAO, opinionValidation, placeValidation)
}
