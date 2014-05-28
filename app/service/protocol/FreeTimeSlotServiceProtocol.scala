package service.protocol

import datasource.calendar._
import datasource.user._

import com.github.nscala_time.time.Imports._


case class TimeSlot(start: DateTime, end: DateTime)

/*
 * Requests
 */


case class FindFreeTimeSlots(userIds: Seq[Int], duration: Duration, from: LocalDate, to: LocalDate, startTime: Option[LocalTime], endTime: Option[LocalTime], timeZone: DateTimeZone) extends Request

/*
 * Responses
 */


case class FreeTimeSlots(slots: Seq[TimeSlot]) extends Success