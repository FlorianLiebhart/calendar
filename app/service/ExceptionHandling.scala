package service

import akka.actor._

import java.sql.SQLException

import scala.slick.SlickException

import service.protocol._


trait ExceptionHandling {

  self: Actor =>

  /* Handles exceptions that might occur in the partial function `pf` */
  class Handled(pf: PartialFunction[Any, Unit]) extends PartialFunction[Any, Unit] {

    def apply(v: Any): Unit =
      try { pf(v) }
      catch {
        case e: SlickException => sender ! DatabaseError(e.getMessage)
        case e: SQLException   => sender ! DatabaseError(e.getMessage)
      }

    def isDefinedAt(v: Any) = pf.isDefinedAt(v)
  }

  def handled(pf: PartialFunction[Any, Unit]) = new Handled(pf)
}