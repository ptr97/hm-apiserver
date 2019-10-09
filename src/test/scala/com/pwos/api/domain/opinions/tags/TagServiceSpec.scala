package com.pwos.api.domain.opinions.tags

import cats.Id
import cats.implicits._
import com.pwos.api.domain.HelloMountainsError.TagNotFoundError
import com.pwos.api.domain.HelloMountainsError.TagPrivilegeError
import com.pwos.api.domain.opinions.tags.TagModels.UpdateTagModel
import com.pwos.api.domain.users.User
import com.pwos.api.domain.users.UserInfo
import com.pwos.api.domain.users.UserRole
import com.pwos.api.infrastructure.dao.memory.MemoryTagDAOInterpreter
import com.pwos.api.infrastructure.dao.memory.MemoryUserDAOInterpreter
import org.scalatest.FunSpec
import org.scalatest.Matchers


class TagServiceSpec extends FunSpec with Matchers {

  private def getTestResources: (MemoryTagDAOInterpreter, TagService[Id]) = {
    val memoryUserDAO = MemoryUserDAOInterpreter()

    val adminFromDb: Id[User] = memoryUserDAO.create(admin)
    adminUserInfo = UserInfo.forUser(adminFromDb)

    val userFromDb: Id[User] = memoryUserDAO.create(userStephen)
    userStephInfo = UserInfo.forUser(userFromDb)

    val memoryTagDAO = MemoryTagDAOInterpreter()
    val tagValidation = TagValidationInterpreter(memoryTagDAO)
    val tagService: TagService[Id] = TagService(memoryTagDAO, tagValidation)
    (memoryTagDAO, tagService)
  }

  private val admin: User = User("admin", "admin@hm.com", "HashedPassword123", UserRole.Admin)
  private var adminUserInfo: UserInfo = _

  private val userStephen: User = User("stephCurry", "steph@gsw.com", "HashedPassword123", UserRole.User)
  private var userStephInfo: UserInfo = _

  private val tagOne: Tag = Tag("Tag Number 1", TagCategory.EQUIPMENT)
  private val tagTwo: Tag = Tag("Tag Number 2", TagCategory.SUBSOIL)
  private val tagThree: Tag = Tag("Tag Number 3", TagCategory.THREATS)


  describe("Creating a Tag") {
    it("should create tag") {
      val (tagDAO, tagService) = getTestResources
      val result = tagService.create(adminUserInfo, tagOne).value

      val tagFromDb = tagDAO.findByName(tagOne.name).get

      result shouldBe Right(tagOne.copy(id = tagFromDb.id))
    }

    it("should return TagPrivilegeError when non admin tries to create Tag") {
      val (tagDAO, tagService) = getTestResources

      val result = tagService.create(userStephInfo, tagOne).value
      result shouldBe Left(TagPrivilegeError)

      val tagFromDb = tagDAO.findByName(tagOne.name)
      tagFromDb shouldBe None
    }
  }

  describe("Updating a Tag") {
    it("should update tag values") {
      val (tagDAO, tagService) = getTestResources
      val tagFromDb = tagDAO.create(tagOne)
      val updateTagModel = UpdateTagModel(maybeName = "Different Name".some)

      val result = tagService.updateTag(adminUserInfo, tagFromDb.id.get, updateTagModel).value
      result shouldBe Right(true)

      val updatedTagFromDb = tagDAO.get(tagFromDb.id.get)
      updatedTagFromDb shouldBe tagFromDb.copy(name = updateTagModel.maybeName.get).some
    }

    it("should return TagNotFoundError when tag doesn't exist") {
      val (tagDAO, tagService) = getTestResources
      val updateTagModel = UpdateTagModel(maybeName = "Different Name".some)

      val result = tagService.updateTag(adminUserInfo, tagDAO.getLastId + 1, updateTagModel).value
      result shouldBe Left(TagNotFoundError)
    }

    it("should return TagPrivilegeError when non admin tries to update Tag") {
      val (tagDAO, tagService) = getTestResources
      val tagFromDb = tagDAO.create(tagOne)
      val updateTagModel = UpdateTagModel(maybeName = "Different Name".some)

      val result = tagService.updateTag(userStephInfo, tagFromDb.id.get, updateTagModel).value
      result shouldBe Left(TagPrivilegeError)

      val updatedTagFromDb = tagDAO.get(tagFromDb.id.get)
      updatedTagFromDb shouldBe tagFromDb.some
    }
  }

  describe("Getting list of all Tags") {
    it("should return list of active tags") {
      val (tagDAO, tagService) = getTestResources
      val tag1FromDb = tagDAO.create(tagOne)
      val tag2FromDb = tagDAO.create(tagTwo)
      val tag3FromDb = tagDAO.create(tagThree.copy(enabled = false))

      val result = tagService.listAllTags(adminUserInfo, active = true).value.toOption.get
      result shouldBe List(tag2FromDb, tag1FromDb)
    }

    it("should return list of non active tags") {
      val (tagDAO, tagService) = getTestResources
      val tag1FromDb = tagDAO.create(tagOne)
      val tag2FromDb = tagDAO.create(tagTwo.copy(enabled = false))
      val tag3FromDb = tagDAO.create(tagThree.copy(enabled = false))

      val result = tagService.listAllTags(adminUserInfo, active = false).value.toOption.get
      result shouldBe List(tag3FromDb, tag2FromDb)
    }

    it("should return empty list when there is no tags at all") {
      val (_, tagService) = getTestResources

      val result = tagService.listAllTags(adminUserInfo, active = true).value.toOption.get
      result shouldBe List.empty
    }

    it("should return empty list when there is no active tags") {
      val (tagDAO, tagService) = getTestResources
      val tag1FromDb = tagDAO.create(tagOne.copy(enabled = false))
      val tag2FromDb = tagDAO.create(tagTwo.copy(enabled = false))
      val tag3FromDb = tagDAO.create(tagThree.copy(enabled = false))
      val result = tagService.listAllTags(adminUserInfo, active = true).value.toOption.get
      result shouldBe List.empty
    }

    it("should return empty list when there is no inactive tags") {
      val (tagDAO, tagService) = getTestResources
      val tag1FromDb = tagDAO.create(tagOne)
      val tag2FromDb = tagDAO.create(tagTwo)
      val tag3FromDb = tagDAO.create(tagThree)
      val result = tagService.listAllTags(adminUserInfo, active = false).value.toOption.get
      result shouldBe List.empty
    }

    it("should return TagPrivilegeError when non Admin tries to get list of inactive tags") {
      val (tagDAO, tagService) = getTestResources
      val tag1FromDb = tagDAO.create(tagOne)
      val tag2FromDb = tagDAO.create(tagTwo.copy(enabled = false))
      val tag3FromDb = tagDAO.create(tagThree.copy(enabled = false))

      val result = tagService.listAllTags(userStephInfo, active = false).value
      result shouldBe Left(TagPrivilegeError)
    }
  }

  describe("Getting list of only active Tags") {
    it("should return list of active tags") {
      val (tagDAO, tagService) = getTestResources
      val tag1FromDb = tagDAO.create(tagOne)
      val tag2FromDb = tagDAO.create(tagTwo)
      val tag3FromDb = tagDAO.create(tagThree.copy(enabled = false))

      val result = tagService.listActiveTags()
      result shouldBe List(tag2FromDb, tag1FromDb)
    }

    it("should return empty list when there is no active tags") {
      val (tagDAO, tagService) = getTestResources
      val tag1FromDb = tagDAO.create(tagOne.copy(enabled = false))
      val tag2FromDb = tagDAO.create(tagTwo.copy(enabled = false))
      val tag3FromDb = tagDAO.create(tagThree.copy(enabled = false))

      val result = tagService.listActiveTags()
      result shouldBe List.empty
    }
  }

}
