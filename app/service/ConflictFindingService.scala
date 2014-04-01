package service

import akka.actor._

import datasource.calendar._

import scala.slick.driver.PostgresDriver.simple._

import service.protocol._

/**
  *
  * @author Christoph Goettschkes
  */
object ConflictFindingService {

  def props: Props = Props(classOf[ConflictFindingService])
}

/**
  *
  * @author Christoph Goettschkes
  */
class ConflictFindingService extends Actor with ActorLogging {

  def findConflicts(conflicts: Seq[Appointment]) = {
      var sorted = conflicts.sortBy(a => a.start)
      var result: List[(Appointment, Appointment)] = Nil
      while (!sorted.isEmpty) {
        val first = sorted.head
        sorted = sorted.tail
        var innerSorted = sorted
        while (!innerSorted.isEmpty && innerSorted.head.start.lt(first.end)) {
          result = (first, innerSorted.head) :: result
          innerSorted = innerSorted.drop(1)
        }
      }
      sender ! Conflicts(result.reverse)
    }

  def receive =  {
    case FindConflict(conflicts) => findConflicts(conflicts)
  }
}