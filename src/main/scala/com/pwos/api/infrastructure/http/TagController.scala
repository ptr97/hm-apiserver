package com.pwos.api.infrastructure.http

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import com.pwos.api.domain.HelloMountainsError._
import com.pwos.api.domain.opinions.tags.TagModels.UpdateTagModel
import com.pwos.api.domain.opinions.tags.TagService
import com.pwos.api.domain.opinions.tags.{Tag => HmTag}
import com.pwos.api.domain.users.UserRole
import com.pwos.api.infrastructure.http.JsonImplicits._
import com.pwos.api.infrastructure.http.authentication.SecuredAccess
import com.pwos.api.infrastructure.http.versions._
import com.pwos.api.infrastructure.implicits._
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
import io.circe.generic.auto._
import slick.dbio.DBIO
import slick.jdbc.MySQLProfile.api._

import scala.concurrent.ExecutionContext


class TagController(tagService: TagService[DBIO])(implicit ec: ExecutionContext, database: Database) extends SecuredAccess {

  val tagRoutes: Route = listActiveTags ~ listAllTags ~ create ~ updateTagStatus

  import TagController._


  def listActiveTags: Route = path(v1 / TAGS) {
    authorizedGet(UserRole.User) { _ =>
      complete {
        tagService.listActiveTags().unsafeRun map (HttpOps.ok(_))
      }
    }
  }

  def listAllTags: Route = path(v1 / TAGS / "all") {
    authorizedGet(UserRole.Admin) { userInfo =>
      parameter('active.as[Boolean]) { active =>
        complete {
          tagService.listAllTags(userInfo, active).value.unsafeRun map {
            case Right(tags) => HttpOps.ok(tags)
            case Left(tagsPrivilegeError) => HttpOps.forbidden(tagsPrivilegeError)
          }
        }
      }
    }
  }

  def create: Route = path(v1 / TAGS) {
    authorizedPost(UserRole.Admin) { userInfo =>
      entity(as[HmTag]) { tag =>
        complete {
          tagService.create(userInfo, tag).value.unsafeRun map {
            case Right(tag) => HttpOps.created(tag)
            case Left(tagError) =>
              tagError match {
                case tagError: TagAlreadyExistsError => HttpOps.badRequest(tagError)
                case tagError: TagPrivilegeError.type => HttpOps.forbidden(tagError)
                case _ => HttpOps.internalServerError()
              }
          }
        }
      }
    }
  }

  def updateTagStatus: Route = path(v1 / TAGS / LongNumber) { tagId =>
    authorizedPut(UserRole.User) { userInfo =>
      entity(as[UpdateTagModel]) { updateTagModel =>
        complete {
          tagService.updateTag(userInfo, tagId, updateTagModel).value.unsafeRun map {
            case Right(tag) => HttpOps.ok(tag)
            case Left(tagError) =>
              tagError match {
                case tagError: TagNotFoundError.type => HttpOps.badRequest(tagError)
                case tagError: TagPrivilegeError.type => HttpOps.forbidden(tagError)
                case _ => HttpOps.internalServerError()
              }
          }
        }
      }
    }
  }

}

object TagController {
  val TAGS = "tags"

  def apply(tagService: TagService[DBIO])(implicit ec: ExecutionContext, database: Database): TagController =
    new TagController(tagService)
}
