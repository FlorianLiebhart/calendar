package service

import akka.actor._

import com.github.nscala_time.time.Imports._

import datasource.calendar._
import datasource.user._

import format.DebugWrites._

import org.joda.time.Days

import scala.slick.driver.PostgresDriver.simple._

import service.protocol._

import util.CustomColumnTypes._
import util.JodaTimeExtensions._
import util.JsonConversion._


object FreeTimeSlotService {

  def props(db: Database): Props = Props(classOf[FreeTimeSlotService], db)
}


class FreeTimeSlotService(db: Database)
  extends Actor with
          ActorLogging with
          UserDataAccessComponentImpl with
          CalendarDataAccessComponentImpl {

  
  protected object userDataAccess extends UserDataAccessModuleImpl

  
  protected object calendarDataAccess extends CalendarDataAccessModuleImpl

  import calendarDataAccess._

  
  private type Acc = (Seq[TimeSlot], DateTime)

  
  def datesBetween(from: DateTime, to: DateTime) =
    (0 to Days.daysBetween(from, to).getDays).map(from + _.days)

  def receive = {
    case fftss @ FindFreeTimeSlots(userIds, duration, from, to, startTime, endTime, timeZone) =>

      log.debug(s"Received free time slot request with ${fftss.toJson}")

      val fromDateTime = startTime.map(from.toDateTime(_, timeZone)).getOrElse(from.toDateTimeAtStartOfDay(timeZone))
      val toDateTime   = endTime.map(to.toDateTime(_, timeZone)).getOrElse(to.toDateTimeAtStartOfDay(timeZone).withTimeAtEndOfDay)

      val appointments = db.withSession { implicit session =>
        appointmentsFromUsers(userIds, Some(fromDateTime), Some(toDateTime)).buildColl[Seq]
      }

      val constraints: Seq[Appointment] = datesBetween(fromDateTime, toDateTime).flatMap { date => 
        Seq(
          startTime.map(startTime => Appointment(-1, "", date.withTimeAtStartOfDay, date.withMillisOfDay(startTime.getMillisOfDay))),
          endTime.map(endTime => Appointment(-1, "", date.withMillisOfDay(endTime.getMillisOfDay), date.withTimeAtEndOfDay))
        ).flatten
      }

      val sorted = (appointments ++ constraints :+ Appointment(-1, "", toDateTime, toDateTime)).sortBy(_.start)

      val freeTimeSlots =
        sorted
          .foldLeft[Acc]((Seq(), fromDateTime)) {
            case ((slots, lastEnd), appointment) =>
              if(appointment.start - lastEnd >= duration)
                (slots :+ TimeSlot(lastEnd, appointment.start), appointment.end)
              else
                (slots, appointment.end)
          }._1

      log.debug(s"Free time slots ${freeTimeSlots.toJson}")

      sender ! FreeTimeSlots(freeTimeSlots)
  }
}