package service

import akka.actor._

import datasource.proposal._
import datasource.calendar._
import datasource.user._

import com.github.nscala_time.time.Imports._

import org.joda.time.format.ISODateTimeFormat

import scala.slick.driver.PostgresDriver.simple._

import service.protocol._

import util.Color


object AdministrationService {

  def props(db: Database): Props = Props(classOf[AdministrationService], db)
}


class AdministrationService(db: Database)
  extends Actor with
          ActorLogging with
          UserDataAccessComponentImpl with
          CalendarDataAccessComponentImpl with
          ProposalDataAccessComponentImpl with
          ExceptionHandling {

  protected object userDataAccess extends UserDataAccessModuleImpl
  protected object calendarDataAccess extends CalendarDataAccessModuleImpl
  protected object proposalDataAccess extends ProposalDataAccessModuleImpl

  import userDataAccess._
  import calendarDataAccess._
  import proposalDataAccess._

  def parse(str: String) = ISODateTimeFormat.dateTimeNoMillis.parseDateTime(str)

  def addAppointment(title: String, start: DateTime, end: DateTime, tagIds: Seq[Int])(implicit session: Session) = {
    val appointmentId = (appointments returning appointments.map(_.id)) += Appointment(-1, title, start, end)
    appointmentBelongsToTag ++= tagIds.map((appointmentId, _))
    appointmentId
  }

  def createSchema = {
    val ddl = userDdl ++ calendarDdl ++ proposalDdl
    db.withTransaction { implicit session =>
      ddl.create
    }

    log.debug("Schema created\n" + ddl.createStatements.mkString("\n"))
    sender ! SchemaCreated
  }

  def dropSchema = {
    val ddl = userDdl ++ calendarDdl ++ proposalDdl
    db.withTransaction { implicit session =>
      ddl.drop
    }

    // println("Schema dropped: " + ddl.dropStatements.mkString("; "))
    log.debug("Schema dropped\n" + ddl.dropStatements.mkString("\n"))
    sender ! SchemaDropped
  }

  def createSampleData = {
    db.withTransaction { implicit session =>

      // Clear tables
      appointments.delete
      tags.delete
      users.delete

      // Insert sample data

      val test      = (users returning users.map(_.id)) += User(-1, "test", "test")
      val simon     = (users returning users.map(_.id)) += User(-1, "Simon", "test")
      val florian   = (users returning users.map(_.id)) += User(-1, "Florian", "test")
      val christoph = (users returning users.map(_.id)) += User(-1, "Christoph", "test")

      val tagIds = (tags returning tags.map(_.id)) ++= Seq(
        Tag(-1, "default", 0, Color.colors(0), test),
        Tag(-1, "family" , 1, Color.colors(1), test),
        Tag(-1, "work"   , 2, Color.colors(2), test),

        Tag(-1, "default", 0, Color.colors(0), simon),
        Tag(-1, "family" , 1, Color.colors(1), simon),
        Tag(-1, "work"   , 2, Color.colors(2), simon),

        Tag(-1, "default", 0, Color.colors(0), florian),
        Tag(-1, "family" , 1, Color.colors(1), florian),
        Tag(-1, "work"   , 2, Color.colors(2), florian),

        Tag(-1, "default", 0, Color.colors(0), christoph),
        Tag(-1, "family" , 1, Color.colors(1), christoph),
        Tag(-1, "work"   , 2, Color.colors(2), christoph)
      )

      val defaultTagId = tagIds(0)
      val familyTagId  = tagIds(1)
      val workTagId    = tagIds(2)

      val appointmentIds = Seq(
        // tag default
        addAppointment("Tim & Caroline visiting", parse("2014-01-17T12:00:00Z"), parse("2014-01-19T12:00:00Z"), Seq(defaultTagId)),
        // tag family
        // first week
        // second week
        addAppointment("Anna piano lesson", parse("2014-01-08T15:00:00Z"), parse("2014-01-08T16:00:00Z"), Seq(defaultTagId, familyTagId)),
        addAppointment("Tim soccer"       , parse("2014-01-09T15:00:00Z"), parse("2014-01-09T17:00:00Z"), Seq(defaultTagId, familyTagId)),
        addAppointment("Anna theatre     ", parse("2014-01-10T20:00:00Z"), parse("2014-01-10T22:00:00Z"), Seq(defaultTagId, familyTagId)),
        // third week
        addAppointment("Anna piano lesson", parse("2014-01-15T15:00:00Z"), parse("2014-01-15T16:00:00Z"), Seq(defaultTagId, familyTagId)),
        addAppointment("Tim soccer"       , parse("2014-01-16T15:00:00Z"), parse("2014-01-16T17:00:00Z"), Seq(defaultTagId, familyTagId)),
        addAppointment("Carl badminton"   , parse("2014-01-16T16:30:00Z"), parse("2014-01-16T19:00:00Z"), Seq(defaultTagId, familyTagId)),
        // fourth week
        addAppointment("Anna piano lesson", parse("2014-01-22T15:00:00Z"), parse("2014-01-22T16:00:00Z"), Seq(defaultTagId, familyTagId)),
        addAppointment("Tim soccer"       , parse("2014-01-23T15:00:00Z"), parse("2014-01-23T17:00:00Z"), Seq(defaultTagId, familyTagId)),
        addAppointment("Release RC1A"     , parse("2014-01-23T09:00:00Z"), parse("2014-01-23T18:00:00Z"), Seq(defaultTagId, workTagId)),
        // tag work
        addAppointment("Milestone Meeting", parse("2014-01-08T10:00:00Z"), parse("2014-01-08T12:00:00Z"), Seq(defaultTagId, workTagId)),

        // Create 3 appointments for Simon
        addAppointment("Simon appointment #1", parse("2014-01-06T10:00:00Z"), parse("2014-01-06T15:00:00Z"), Seq(tagIds(3))),
        addAppointment("Simon appointment #2", parse("2014-01-07T10:00:00Z"), parse("2014-01-07T15:00:00Z"), Seq(tagIds(3))),
        addAppointment("Simon appointment #3", parse("2014-01-10T13:00:00Z"), parse("2014-01-10T14:00:00Z"), Seq(tagIds(3))),

        // Create 3 appointments for Florian
        addAppointment("Florian appointment #1", parse("2014-01-13T10:00:00Z"), parse("2014-01-13T15:00:00Z"), Seq(tagIds(6))),
        addAppointment("Florian appointment #2", parse("2014-01-14T10:00:00Z"), parse("2014-01-14T15:00:00Z"), Seq(tagIds(6))),
        addAppointment("Florian appointment #3", parse("2014-01-17T13:00:00Z"), parse("2014-01-17T14:00:00Z"), Seq(tagIds(6))),

        // Create 3 appointments for Christoph
        addAppointment("Christoph appointment #1", parse("2014-01-20T10:00:00Z"), parse("2014-01-20T15:00:00Z"), Seq(tagIds(9))),
        addAppointment("Christoph appointment #2", parse("2014-01-21T10:00:00Z"), parse("2014-01-21T15:00:00Z"), Seq(tagIds(9))),
        addAppointment("Christoph appointment #3", parse("2014-01-24T13:00:00Z"), parse("2014-01-24T14:00:00Z"), Seq(tagIds(9)))
      )

      // add proposal with 3 times, test is the creator, other default user are participants.
      val participants = Seq(test, simon, florian, christoph)
      val proposalId = (proposals returning proposals.map(_.id)) += Proposal(-1, "Final presentation", Color.colors(4), test)

      val proposalTime1 = (proposalTimes returning proposalTimes.map(_.id)) += ProposalTime(-1, parse("2014-01-13T15:00:00Z"), parse("2014-01-13T17:00:00Z"), proposalId)
      proposalTimeVotes ++= participants.map(ProposalTimeVote(proposalTime1, _, Vote.NotVoted))
      proposalTimeVotes
        .filter(_.proposalTimeId === proposalTime1).filter(_.userId === simon)
        .map(v => (v.vote)).update((Vote.Accepted))
      proposalTimeVotes
        .filter(_.proposalTimeId === proposalTime1).filter(_.userId === test)
        .map(v => (v.vote)).update((Vote.Accepted))

      val proposalTime2 = (proposalTimes returning proposalTimes.map(_.id)) += ProposalTime(-1, parse("2014-01-14T15:00:00Z"), parse("2014-01-14T17:00:00Z"), proposalId)
      proposalTimeVotes ++= participants.map(ProposalTimeVote(proposalTime2, _, Vote.NotVoted))
      proposalTimeVotes
        .filter(_.proposalTimeId === proposalTime2).filter(_.userId === florian)
        .map(v => (v.vote)).update((Vote.Refused))
      proposalTimeVotes
        .filter(_.proposalTimeId === proposalTime2).filter(_.userId === test)
        .map(v => (v.vote)).update((Vote.Accepted))

      val proposalTime3 = (proposalTimes returning proposalTimes.map(_.id)) += ProposalTime(-1, parse("2014-01-15T15:00:00Z"), parse("2014-01-15T17:00:00Z"), proposalId)
      proposalTimeVotes ++= participants.map(ProposalTimeVote(proposalTime3, _, Vote.NotVoted))
      proposalTimeVotes
        .filter(_.proposalTimeId === proposalTime3).filter(_.userId === christoph)
        .map(v => (v.vote)).update((Vote.Uncertain))
      proposalTimeVotes
        .filter(_.proposalTimeId === proposalTime3).filter(_.userId === test)
        .map(v => (v.vote)).update((Vote.Accepted))
    }

    sender ! SampleDataCreated
  }

  def receive = handled {
    case CreateSchema     => createSchema
    case DropSchema       => dropSchema
    case CreateSampleData => createSampleData
  }
}