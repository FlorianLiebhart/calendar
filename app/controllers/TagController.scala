package controllers

import akka.pattern.ask

import access.Restricted

import datasource.calendar._

import play.api._
import play.api.mvc._
import play.api.libs.json.Json.toJson

import service._
import service.protocol._

import formatters._

object TagController
  extends Controller with
          Restricted with
          ExecutionEnvironment with
          ResponseTransformation {

  def show(id: Int) = Action.async {
  	(Services.calendarService ? GetTagById(id)).mapTo[Response].map(_.toJsonResult)
  }

  def list() = Authenticated.async { implicit request =>
  	(Services.calendarService ? GetTagsFromUser(request.user.id)).mapTo[Response].map(_.toJsonResult)
  }

  def add() = Action {
  	Status(501)("")
  }

  def update(id: Int) = Action {
  	Status(501)("")
  }

  def delete(id: Int) = Action {
  	Status(501)("")
  }
  
}