package controllers

import play.api.mvc._
import play.api.mvc.Security._

trait Restricted {

  case class AuthenticatedUser(id: Int, name: String)

  def getUser(request: RequestHeader) =
  	for {
  		id   <- request.session.get("userid").map(_.toInt)
  		name <- request.session.get("username")
  	}
  	yield AuthenticatedUser(id, name)

  object Authenticated extends AuthenticatedBuilder(getUser(_))
}