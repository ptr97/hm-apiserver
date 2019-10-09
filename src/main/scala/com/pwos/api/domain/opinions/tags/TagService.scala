package com.pwos.api.domain.opinions.tags

import cats.Monad
import cats.data.EitherT
import com.pwos.api.domain.HelloMountainsError._
import com.pwos.api.domain.opinions.tags.TagModels.UpdateTagModel
import com.pwos.api.domain.users.UserInfo


class TagService[F[_] : Monad](tagDAO: TagDAOAlgebra[F], tagValidation: TagValidationAlgebra[F]) {

  def listActiveTags(): F[List[Tag]] = {
    tagDAO.list(active = true)
  }

  def listAllTags(userInfo: UserInfo, active: Boolean): EitherT[F, TagPrivilegeError.type, List[Tag]] = {
    for {
      _ <- EitherT(Monad[F].pure(tagValidation.validateAdminAccess(userInfo)))
      tags <- EitherT.liftF(tagDAO.list(active))
    } yield tags
  }

  def create(userInfo: UserInfo, tag: Tag): EitherT[F, TagValidationError, Tag] = {
    for {
      _ <- EitherT(Monad[F].pure(tagValidation.validateAdminAccess(userInfo)))
      _ <- tagValidation.doesNotExists(tag)
      tag <- EitherT.liftF(tagDAO.create(tag))
    } yield tag
  }

  def updateTag(userInfo: UserInfo, tagId: Long, updateTagModel: UpdateTagModel): EitherT[F, TagValidationError, Boolean] = {
    type TagUpdate = Tag => Option[Tag]

    val updateName: TagUpdate = tag => updateTagModel.maybeName.map(name => tag.copy(name = name))
    val updateCategory: TagUpdate = tag => updateTagModel.maybeTagCategory.map(category => tag.copy(tagCategory = category))
    val updateStatus: TagUpdate = tag => updateTagModel.maybeEnabled.map(enabled => tag.copy(enabled = enabled))

    val updates: List[TagUpdate] = List(updateName, updateCategory, updateStatus)

    val updateTagData: Tag => Tag = oldTag => {
      updates.foldLeft(oldTag)((tag, update) => update(tag).getOrElse(tag))
    }

    for {
      _ <- EitherT(Monad[F].pure(tagValidation.validateAdminAccess(userInfo)))
      _ <- tagValidation.exists(tagId)
      tag <- EitherT.fromOptionF(tagDAO.get(tagId), TagNotFoundError)
      updatedTag = updateTagData(tag)
      updateResult <- EitherT.liftF(tagDAO.update(updatedTag))
    } yield updateResult
  }

}

object TagService {
  def apply[F[_] : Monad](tagDAO: TagDAOAlgebra[F], tagValidation: TagValidationAlgebra[F]): TagService[F] =
    new TagService(tagDAO, tagValidation)
}
