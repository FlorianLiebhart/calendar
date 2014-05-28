package service

import scala.concurrent._
import scala.reflect.ClassTag

import service.protocol._


trait ResponseHandling {

  self: ExecutionEnvironment =>

  implicit def reponseToResponseHandler(response: Future[Any]) = new ResponseHandler(response)

  class ResponseHandler(response: Future[Any]) {

    def expecting[S <: Success](implicit ct: ClassTag[S]): Future[S] = response.mapTo[Response].map(_.get[S])
  }
}