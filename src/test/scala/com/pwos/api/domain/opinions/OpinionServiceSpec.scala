package com.pwos.api.domain.opinions

import cats.Id
import com.pwos.api.domain.authentication.PasswordService
import com.pwos.api.domain.places.Place
import com.pwos.api.domain.places.PlaceValidationInterpreter
import com.pwos.api.domain.users.User
import com.pwos.api.domain.users.UserRole
import com.pwos.api.infrastructure.dao.memory.MemoryOpinionDAOInterpreter
import com.pwos.api.infrastructure.dao.memory.MemoryPlaceDAOInterpreter
import com.pwos.api.infrastructure.dao.memory.MemoryReportDAOInterpreter
import com.pwos.api.infrastructure.dao.memory.MemoryUserDAOInterpreter
import org.scalatest.FunSpec
import org.scalatest.Matchers


class OpinionServiceSpec extends FunSpec with Matchers {

  case class OpinionServiceSpecResources(
    opinionDAO: MemoryOpinionDAOInterpreter,
    reportDAO: MemoryReportDAOInterpreter,
    userDAO: MemoryUserDAOInterpreter,
    placeDAO: MemoryPlaceDAOInterpreter,
    opinionValidation: OpinionValidationInterpreter[Id],
    placeValidation: PlaceValidationInterpreter[Id],
    opinionService: OpinionService[Id]
  )

  object OpinionServiceSpecResources {
    def apply(): OpinionServiceSpecResources = {

      val memoryOpinionDAO = MemoryOpinionDAOInterpreter()
      val memoryReportDAO = MemoryReportDAOInterpreter()
      val memoryUserDAO = MemoryUserDAOInterpreter()
      val memoryPlaceDAO = MemoryPlaceDAOInterpreter()
      val opinionValidation: OpinionValidationInterpreter[Id] = OpinionValidationInterpreter(memoryOpinionDAO)
      val placeValidation: PlaceValidationInterpreter[Id] = PlaceValidationInterpreter(memoryPlaceDAO)
      val opinionService: OpinionService[Id] = OpinionService(memoryOpinionDAO, memoryReportDAO, memoryUserDAO, opinionValidation, placeValidation)

      new OpinionServiceSpecResources(
        memoryOpinionDAO,
        memoryReportDAO,
        memoryUserDAO,
        memoryPlaceDAO,
        opinionValidation,
        placeValidation,
        opinionService
      )
    }
  }

  private val userStephenPasswordPlain = "Password123"
  private val userStephenPasswordHashed = PasswordService.hash(userStephenPasswordPlain)
  private val userStephen: User = User("stephCurry", "steph@gsw.com", userStephenPasswordHashed, UserRole.User)

  private val userKlay: User = User("klayThompson", "klay@gsw.com", "SecretPass123", UserRole.User)
  private val userKevin: User = User("kevinDurant", "kevin@gsw.com", "SecretPass123", UserRole.User)
  private val admin: User = User("steveKerr", "steve@gsw.com", "Secret123", UserRole.Admin)


  private val place: Place = Place("AGH", 50.067106, 19.913587, 203)
  private val secondPlace: Place = Place("Zakynthos", 37.7825062, 20.8950319, 13)
  private val thirdPlace: Place = Place("Meladives", -0.617644, 73.093730, 7)
  private val fourthPlace: Place = Place("Los Angeles", 34.0536909, -118.2427666, 88)


  describe("Getting list of opinions for place") {

  }


}
