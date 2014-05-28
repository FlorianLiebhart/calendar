package service

import akka.actor._
import akka.pattern.ask
import akka.routing._

import datasource.calendar._

import play.api.db._
import play.api.Logger
import play.api.Play.current

import scala.concurrent.{util => _, _}
import scala.slick.driver.PostgresDriver.simple._
import scala.slick.driver.PostgresDriver.SchemaDescription

import service.protocol._

/** Global service accessor
  */
object Services extends ExecutionEnvironment with ResponseHandling {

  /* Database connection pool */
  val db = Database.forDataSource(DB.getDataSource())

  /** Service for user related operations */
  val userService = system.actorOf(UserService.props(db).withRouter(FromConfig()), "user-service")

  /** Service for basic calendar and tag related operations */
  val calendarService = system.actorOf(CalendarService.props(db).withRouter(FromConfig()), "calendar-service")

  /** Service for basic calendar and tag related operations */
  val proposalService = system.actorOf(ProposalService.props(db).withRouter(FromConfig()), "proposal-service")

  /** Service for basic calendar and tag related operations */
  val conflictFindingService = system.actorOf(ConflictFindingService.props.withRouter(FromConfig()), "conflict-finding-service")

  /** Service for basic calendar and tag related operations */
  val freeTimeSlotService = system.actorOf(FreeTimeSlotService.props(db).withRouter(FromConfig()), "free-time-slot-service")

  /** Service for inserting sample data into the database */
  val administrationService = system.actorOf(AdministrationService.props(db), "administration-service")
}