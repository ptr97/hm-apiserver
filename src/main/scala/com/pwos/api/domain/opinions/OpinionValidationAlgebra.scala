package com.pwos.api.domain.opinions

import cats.data.EitherT
import com.pwos.api.domain.HelloMountainsError._


trait OpinionValidationAlgebra[F[_]] {

  def exists(opinionId: Long): EitherT[F, OpinionNotFoundError.type, Unit]

  def validateOwnership(userId: Long, opinionId: Long): EitherT[F, OpinionOwnershipError.type, Unit]

}
