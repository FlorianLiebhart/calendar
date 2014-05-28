package datasource.user

import scala.slick.driver.PostgresDriver.simple._
import scala.slick.lifted.Shape


trait UserDataAccessComponent {

  
  trait UserDataAccessModule {

    /*
     * Types
     */

    /** User data transfer object representation */
    type User <: AbstractUser

    /** User database table representation */
    type UserTable <: AbstractUserTable

    /*
     * Shapes
     */
 
    
    implicit val userShape: Shape[_, UserTable, User, UserTable]

    /*
     * Database tables
     */

    
    trait AbstractUserTable extends Table[User] {

      def id       : Column[Int]
      def name     : Column[String]
      def password : Column[String]
    }

    /** Data definition language */
    def userDdl = users.ddl
 
    /*
     * Queries
     */

    
    val users: TableQuery[UserTable]

    
    def usersById(id: Int): Query[UserTable, User] = users.filter(_.id === id)

    
    def usersByName(name: String): Query[UserTable, User] = users.filter(_.name === name)
  }
}


trait UserDataAccessComponentImpl extends UserDataAccessComponent {

  trait UserDataAccessModuleImpl extends UserDataAccessModule {

    /*
     * Types
     */

    type User = datasource.user.User

    type UserTable = UserTableImpl

    /*
     * Shapes
     */

    implicit val userShape: Shape[_, UserTable, User, UserTable] = implicitly[Shape[_, UserTable, User, UserTable]]
    
    /*
     * Database tables
     */

    
    class UserTableImpl(tag: Tag) extends Table[User](tag, "app_user") with AbstractUserTable {

      def id       = column[Int   ]("id"      , O.PrimaryKey, O.AutoInc)
      def name     = column[String]("name"    , O.NotNull              )
      def password = column[String]("password", O.NotNull              )

      def uniqueName = index("unique_name", name, unique = true)

      def *        = (id, name, password) <> (User.tupled, User.unapply)
    }

    /*
     * Queries
     */

    val users = TableQuery[UserTable]
  }
}