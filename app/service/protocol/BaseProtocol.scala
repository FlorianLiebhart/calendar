package service.protocol

import scala.reflect.ClassTag

/** Base trait for all requests sent to service actors
  */
trait Request

/** Base trait for all requests sent to service actors
  */
trait Response {

  
  def fold[S <: Success, A](onError: Error => A, onSuccess: S => A)(implicit ct: ClassTag[S]): A = this match {
    case error : Error => onError(error)
    case success: S    => onSuccess(success)
  }

  
  def toEither[S <: Success](implicit ct: ClassTag[S]): Either[Error, S] = this match {
    case error: Error => Left(error)
    case success: S   => Right(success)
  }

  
  def get[S <: Success](implicit ct: ClassTag[S]): S = this match {
    case error: Error => throw error
    case success: S   => success
  }
}

/** Base trait for all success messages sent by service actors
  */
trait Success extends Response

/** Base class for all error messages sent by service actors
  */
class Error(message: String) extends Exception(message) with Response

/** Enables pattern matching on instances of [[service.Error]]
  */
object Error {

  def unapply(response: Response) = response match {
    case error: Error => Some(error.getMessage)
    case _            => None
  }
}