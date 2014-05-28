package datasource.calendar

import datasource.user._

import com.github.nscala_time.time.Imports._

import scala.slick.driver.PostgresDriver.simple.{Tag => _, _}
import scala.slick.model.ForeignKeyAction

import util._
import util.CustomColumnTypes._


trait CalendarDataAccessComponent {

  self: UserDataAccessComponent =>

  /** User data access module accessor */
  protected val userDataAccess: UserDataAccessModule

  import userDataAccess._

  trait CalendarDataAccessModule {

    /*
     * Types
     */

    type Appointment <: AbstractAppointment
    type AppointmentTable <: AbstractAppointmentTable
    type Tag <: AbstractTag
    type TagTable <: AbstractTagTable
    type AppointmentBelongsToTagTable <: AbstractAppointmentBelongsToTagTable

    /*
     * Shapes
     */

    implicit val appointmentShape: Shape[_, AppointmentTable, Appointment, AppointmentTable]
    implicit val tagShape: Shape[_, TagTable, Tag, TagTable]

    /*
     * Database tables
     */

    trait AbstractAppointmentTable extends Table[Appointment] {

      def id    : Column[Int]
      def title : Column[String]
      def start : Column[DateTime]
      def end   : Column[DateTime]

      def tags : Query[TagTable, Tag]
    }

    trait AbstractTagTable extends Table[Tag] {

      def id       : Column[Int]
      def name     : Column[String]
      def priority : Column[Int]
      def color    : Column[Color]
      def userId   : Column[Int]

      def user : Query[UserTable, User]
    }

    trait AbstractAppointmentBelongsToTagTable extends Table[(Int, Int)] {

      def appointmentId : Column[Int]
      def tagId         : Column[Int]

      // def pk            : Constraint
      def appointment : Query[AppointmentTable, Appointment]
      def tag         : Query[TagTable, Tag]
    }

    /** Data definition language */
    def calendarDdl = appointments.ddl ++ tags.ddl ++ appointmentBelongsToTag.ddl

    /*
     * Queries
     */

    /** Appointments */
    val appointments: TableQuery[AppointmentTable]
    val appointmentBelongsToTag: TableQuery[AppointmentBelongsToTagTable]

    def appointmentsById(id: Column[Int]): Query[AppointmentTable, Appointment] =
      appointments.filter(_.id === id)

    def appointmentsByIdWithUserId(id: Int): Query[(AppointmentTable, Column[Int]), (Appointment, Int)] =
      for {
        abtt <- appointmentBelongsToTag
        t    <- abtt.tag
        a    <- abtt.appointment
        if a.id === id
      }
      yield (a, t.userId)

    def appointmentsWithTag(tagId: Column[Int]): Query[AppointmentTable, Appointment] =
      for {
        abtt <- appointmentBelongsToTag
        a    <- abtt.appointment
        if abtt.tagId === tagId
      }
      yield a

    def appointmentsFromUser(userId: Column[Int]): Query[AppointmentTable, Appointment] =
      appointments.filter(a =>
        appointmentBelongsToTag.filter(abtt =>
          abtt.appointmentId === a.id && tags.filter(t =>
            t.id === abtt.tagId && t.userId === userId
          ).exists
        ).exists
      )

    
    def appointmentsFromUsers(userIds: Seq[Int]): Query[AppointmentTable, Appointment] =
      appointments.filter(a =>
        appointmentBelongsToTag.filter(abtt =>
          abtt.appointmentId === a.id && tags.filter(t =>
            t.id === abtt.tagId && t.userId.inSet(userIds)
          ).exists
        ).exists
      )

    
    def appointmentsFromUsers(userIds: Seq[Int], from: Column[Option[DateTime]], to: Column[Option[DateTime]]): Query[AppointmentTable, Appointment] = 
      appointmentsFromUsers(userIds).filter(a => !((from.isNotNull && a.end < from) || (to.isNotNull && a.start > to)))

    /** From a user, receives all appointments including its tags, between a given time, where both, from and to are inclusive */
    def appointmentFromUserWithTag(userId: Int, from: Column[Option[DateTime]], to: Column[Option[DateTime]]): Query[(AppointmentTable, TagTable), (Appointment, Tag)] =
      for {
        abtt <- appointmentBelongsToTag
        t    <- abtt.tag
        a    <- abtt.appointment
        if t.userId === userId && !((from.isNotNull && a.end < from) || (to.isNotNull && a.start > to))
      }
      yield (a, t)


    /** Tags */
    val tags: TableQuery[TagTable]

    def tagsById(id: Column[Int]): Query[TagTable, Tag] = tags.filter(_.id === id)

    def tagsFromUser(userId: Column[Int]): Query[TagTable, Tag] = tags.filter(_.userId === userId)

    def tagsFromAppointment(appointmentId: Column[Int]): Query[TagTable, Tag] =
      for{
        abtt <- appointmentBelongsToTag
        t    <- abtt.tag
        if abtt.appointmentId === appointmentId
      }
      yield t

    def tagsFromUserSortedByName(userId: Column[Int]) = tagsFromUser(userId).sortBy(_.name)
  }
}


trait CalendarDataAccessComponentImpl extends CalendarDataAccessComponent {

  self: UserDataAccessComponent =>

  import userDataAccess._

  trait CalendarDataAccessModuleImpl extends CalendarDataAccessModule {

    /*
     * Types
     */

    type Appointment = datasource.calendar.Appointment
    type AppointmentTable = AppointmentTableImpl
    type Tag = datasource.calendar.Tag
    type TagTable = TagTableImpl
    type AppointmentBelongsToTagTable = AppointmentBelongsToTagTableImpl

    /*
     * Shapes
     */

    implicit val appointmentShape: Shape[_, AppointmentTable, Appointment, AppointmentTable] = implicitly[Shape[_, AppointmentTable, Appointment, AppointmentTable]]
    implicit val tagShape: Shape[_, TagTable, Tag, TagTable] = implicitly[Shape[_, TagTable, Tag, TagTable]]

    /*
     * Database tables
     */

    class AppointmentTableImpl(tag: scala.slick.lifted.Tag) extends Table[Appointment](tag, "appointment") with AbstractAppointmentTable {

      def id    = column[Int     ]("id"        , O.PrimaryKey, O.AutoInc)
      def title = column[String  ]("title"     , O.NotNull              )
      def start = column[DateTime]("start_date", O.NotNull              )
      def end   = column[DateTime]("end_date"  , O.NotNull              )

      def tags        = for(abtt <- appointmentBelongsToTag; t <- abtt.tag if abtt.appointmentId === id) yield t

      def *           = (id, title, start, end) <> (Appointment.tupled, Appointment.unapply)
    }

    class TagTableImpl(tag: scala.slick.lifted.Tag) extends Table[Tag](tag, "tag") with AbstractTagTable {

      def id       = column[Int   ]("id"      , O.PrimaryKey, O.AutoInc)
      def name     = column[String]("name"    , O.NotNull              )
      def priority = column[Int   ]("priority", O.NotNull              )
      def color    = column[Color ]("color"   , O.NotNull              )
      def userId   = column[Int   ]("user_id" , O.NotNull              )

      def user     = foreignKey("user_fk", userId, users)(_.id, onDelete = ForeignKeyAction.Cascade)

      def *        = (id, name, priority, color, userId) <> (Tag.tupled, Tag.unapply)
    }

    class AppointmentBelongsToTagTableImpl(tag: scala.slick.lifted.Tag) extends Table[(Int, Int)](tag, "appointment_belongsto_tag") with AbstractAppointmentBelongsToTagTable {

      def appointmentId = column[Int]("appointment_id")
      def tagId         = column[Int]("tag_id"        )

      // def pk            = primaryKey("pk_appointment_belongsto_tag", (appointmentId, tagId))
      def appointment   = foreignKey("appointment_fk", appointmentId, appointments)(_.id, onDelete = ForeignKeyAction.Cascade)
      def tag           = foreignKey("tag_fk", tagId, tags)(_.id, onDelete = ForeignKeyAction.Cascade)

      def *             = (appointmentId, tagId)
    }
    
    /*
     * Queries
     */

    val appointments = TableQuery[AppointmentTable]
    val tags = TableQuery[TagTable]
    val appointmentBelongsToTag = TableQuery[AppointmentBelongsToTagTable]
  }
}