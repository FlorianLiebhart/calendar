package format

import com.github.nscala_time.time.Imports._

import datasource.calendar._
import datasource.user._
import datasource.proposal._

import org.joda.time.format.PeriodFormat
import org.joda.time.PeriodType

import play.api.libs.json.{util => _, _}

import service.protocol._

import util.JsonConversion._


object DebugWrites {

  /*
   * Base types
   */
  implicit object dateTimeFormat extends Writes[DateTime] {

    def writes(o: DateTime): JsValue = o.toDateTime(DateTimeZone.forOffsetHours(0)).toString().toJson
  }

  implicit object localTimeFormat extends Writes[LocalTime] {

    def writes(o: LocalTime): JsValue = o.toString().toJson
  }

  implicit object durationFormat extends Writes[Duration] {

    def writes(o: Duration): JsValue = PeriodFormat.getDefault.print(o.toPeriod(PeriodType.time)).toJson
  }

  implicit object dateTimeZoneFormat extends Writes[DateTimeZone] {

    def writes(o: DateTimeZone): JsValue = o.toString().toJson
  }

  implicit val addAppointmentWrites = Json.writes[AddAppointment]
  implicit val findFreeTimeSlotsWrites = Json.writes[FindFreeTimeSlots]

  implicit val appointmentWrites = Json.writes[Appointment]
  implicit val timeSlotWrites = Json.writes[TimeSlot]
}