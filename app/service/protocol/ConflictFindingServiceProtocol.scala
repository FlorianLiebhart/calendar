package service.protocol

import datasource.calendar._

/*
 * Requests
 */


case class FindConflicts(appointments: Seq[Appointment]) extends Request

/*
 * Reponses
 */


case class Conflicts(conflicts: Seq[(Appointment, Appointment)]) extends Success {

	def value = conflicts
}