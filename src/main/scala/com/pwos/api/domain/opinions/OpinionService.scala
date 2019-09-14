package com.pwos.api.domain.opinions

import cats.Monad
import cats.data.EitherT
import com.pwos.api.domain.HelloMountainsError._
import com.pwos.api.domain.PagingRequest
import com.pwos.api.domain.QueryParameters
import com.pwos.api.domain.opinions.OpinionModels._
import com.pwos.api.domain.users.UserInfo


class OpinionService[F[_] : Monad](opinionDAO: OpinionDAOAlgebra[F], opinionValidation: OpinionValidationAlgebra[F]) {

  def list(placeId: Option[Long], queryParameters: QueryParameters, pagingRequest: PagingRequest): F[List[Opinion]] = {
    ???
  }

  def addOpinion(userInfo: UserInfo, placeId: Long, createOpinionModel: CreateOpinionModel): EitherT[F, OpinionValidationError, Opinion] = {
    ???
  }

  def getOpinion(opinionId: Long): EitherT[F, OpinionNotFoundError.type, Opinion] = {
    ???
  }

  def deleteOpinion(userInfo: UserInfo, opinionId: Long): EitherT[F, OpinionValidationError, Boolean] = {
    ???
  }

  def updateOpinion(userInfo: UserInfo, opinionId: Long, updateOpinionModel: UpdateOpinionModel): EitherT[F, OpinionValidationError, Opinion] = {
    ???
  }

  def reportOpinion(userInfo: UserInfo, opinionId: Long, reportOpinionModel: ReportOpinionModel): EitherT[F, OpinionNotFoundError.type, Boolean] = {
    ???
  }

  def updateOpinionStatus(userInfo: UserInfo, opinionId: Long, updateOpinionStatusModel: UpdateOpinionStatusModel): EitherT[F, OpinionNotFoundError.type, Boolean] = {
    ???
  }
}

object OpinionService {
  def apply[F[_] : Monad](opinionDAO: OpinionDAOAlgebra[F], opinionValidation: OpinionValidationAlgebra[F]): OpinionService[F] =
    new OpinionService(opinionDAO, opinionValidation)
}
