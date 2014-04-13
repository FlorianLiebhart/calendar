package controllers

import play.api.libs.json.{util => _, _}
import play.api.libs.json.Reads._
import play.api.mvc._
import play.api.mvc.Results._

import scala.concurrent._

/**
 *
 * @author Simon Kaltenbacher
 * @author Florian Liebhart
 */
trait RequestBodyReader {

  def readBody[A](block: A => Future[SimpleResult])(implicit rs: Reads[A], request: Request[JsValue]) =
    request.body.validate[A] match {
      case s: JsSuccess[A] => block(s.get)
      case e: JsError      => Future.successful(BadRequest(e.errors.map(errorTuple => errorTuple._1 + ": " +  errorTuple._2.mkString(", ")).mkString("\n")))
    }
}