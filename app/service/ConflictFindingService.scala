package service

import akka.actor._

import com.github.nscala_time.time.Imports._

import datasource.calendar._

import scala.slick.driver.PostgresDriver.simple._

import service.protocol._


object ConflictFindingService {

  def props: Props = Props(classOf[ConflictFindingService])
}


class ConflictFindingService extends Actor with ActorLogging {

  def findConflicts(conflicts: Seq[Appointment]) = {
    var sorted = conflicts.sortBy(_.start)
    var result: List[(Appointment, Appointment)] = Nil
    while (!sorted.isEmpty) {
      val first = sorted.head
      sorted = sorted.tail
      var innerSorted = sorted
      while (!innerSorted.isEmpty && innerSorted.head.start < first.end) {
        result = (first, innerSorted.head) :: result
        innerSorted = innerSorted.drop(1)
      }
    }
    sender ! Conflicts(result.reverse)
  }

  def receive =  {
    case FindConflicts(conflicts) => findConflicts(conflicts)
  }
}