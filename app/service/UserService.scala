package service

import akka.actor._

import datasource.user._

import scala.slick.driver.PostgresDriver.simple._

import service.protocol._


object UserService {

  def props(db: Database): Props = Props(classOf[UserService], db)
}


class UserService(db: Database)
  extends Actor with
          ActorLogging with
          UserDataAccessComponentImpl with
          ExceptionHandling {

  protected object userDataAccess extends UserDataAccessModuleImpl

  import userDataAccess._

  def getUserById(msg: GetUserById) = db.withSession { implicit session =>
    sender ! usersById(msg.id).firstOption.fold[Any](NoSuchUserError(s"User with id $msg.id does not exist!"))(UserById(_))
  }

  def getUserByName(msg: GetUserByName) = db.withSession { implicit session =>
    sender ! usersByName(msg.name).firstOption.fold[Any](NoSuchUserError(s"User with name $msg.name does not exist!"))(UserByName(_))
  }

  def getAllUsers(msg: GetAllUsers) = db.withSession { implicit session =>
    sender ! AllUsers(
      users
        .buildColl[Seq]
        .map(u => UserWithoutPassword(u.id, u.name))
    )
  }

  def addUser(msg: AddUser) = db.withSession { implicit session =>
    sender ! UserAdded((users returning users.map(_.id)) += User(-1, msg.name, msg.password)) 
  }

  def receive = handled {
    case msg: GetUserById   => getUserById(msg)
    case msg: GetUserByName => getUserByName(msg)
    case msg: GetAllUsers   => getAllUsers(msg)
    case msg: AddUser       => addUser(msg)
  }
}