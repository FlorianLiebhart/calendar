package controllers

import akka.pattern.AskTimeoutException

import format.ResponseFormat._

import play.api.libs.json.{util => _, _}
import play.api.mvc._
import play.api.mvc.Results._
import play.api.Logger

import scala.concurrent.{util => _, _}

import service._
import service.protocol._

import util.JsonConversion._
import java.util.concurrent.TimeoutException

/**
 *
 *
 * 
 */
trait ResponseSerialization {

  self: ExecutionEnvironment =>

  def toJsonResult[W](block: => Future[W])(implicit ws: Writes[W]): Future[SimpleResult] =
    block
      .map(result => Ok(result.toJson))
      .recover {
        case NoSuchUserError(message)        => NotFound(message.toJson)
        case NoSuchTagError(message)         => NotFound(message.toJson)
        case NoSuchAppointmentError(message) => NotFound(message.toJson)
        case PermissionDeniedError(message)  => Forbidden(message.toJson)
        case DatabaseError(message)          => Logger.error(message)
                                                InternalServerError(message.toJson)
        case e: AskTimeoutException          => Logger.error(e.getStackTraceString)
                                                InternalServerError(e.getMessage)
        case e: Exception                    => Logger.error(e.getStackTraceString)
                                                InternalServerError(e.getMessage)
      }
}