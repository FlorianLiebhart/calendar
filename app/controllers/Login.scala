package controllers

import play.api.mvc._
import play.api.data.Form
import play.api.data.Forms._

import scala.concurrent._
import akka.pattern.ask

import datasource.user._
import service._
import service.protocol._


case class LoginData(name: String, password: String)

object Login
  extends Controller with
          ExecutionEnvironment with
          ResponseHandling {

  
  def error(message: String) = Redirect(routes.Login.index).flashing("error" -> message)

  
  def login(user: User) = Redirect(routes.Calendar.calendar).withSession("username" -> user.name, "userid" -> user.id.toString)

  
  def authenticate(loginData: LoginData): Future[SimpleResult] = {

    val userByNameRequest = (Services.userService ? GetUserByName(loginData.name)).expecting[UserByName]

    userByNameRequest
      .map {
        case UserByName(user) if user.password == loginData.password => login(user)
        case UserByName(_)                                           => error("User name or password incorrect!")
      }
      .recover {
        case NoSuchUserError(_) => error("User name or password incorrect!")
        case DatabaseError(_)   => error("Internal server error")
    }
  }

  
  val form = Form(
    mapping(
      "username"   -> nonEmptyText,
      "password"   -> nonEmptyText
    )(LoginData.apply)(LoginData.unapply)
  )

  
  def index = Action { implicit request =>
    if (!request.session.get("username").isEmpty && !request.session.get("userid").isEmpty) {
      Redirect(routes.Calendar.calendar)
    } else {
      Ok(views.html.login(form))
    }
  }

  
  def login = Action.async { implicit request =>
    form.bindFromRequest.fold(
      form => future { error(form.errors.head.key + " is required") },
      authenticate _
    )
  }

  
  def logout = Action{ implicit request => Redirect(routes.Login.index).withNewSession }
}