package service

import akka.actor._

import datasource.user._
import datasource.calendar._
import datasource.proposal._

import com.github.nscala_time.time.Imports._

import scala.slick.driver.PostgresDriver.simple.{Tag =>_, _}

import service.protocol._

import util._


object ProposalService {

  def props(db: Database): Props = Props(classOf[ProposalService], db)
}


class ProposalService(db: Database)
  extends Actor with 
          ActorLogging with
          UserDataAccessComponentImpl with
          CalendarDataAccessComponentImpl with
          ProposalDataAccessComponentImpl with
          ExceptionHandling {

  
  protected object userDataAccess extends UserDataAccessModuleImpl

  
  protected object calendarDataAccess extends CalendarDataAccessModuleImpl

  protected object proposalDataAccess extends ProposalDataAccessModuleImpl

  import calendarDataAccess._
  import proposalDataAccess._
  import userDataAccess._

  /*
   * Proposal
   */

  def addProposal(msg: AddProposal) = db.withSession { implicit session =>
    sender ! ProposalAdded((proposals returning proposals.map(_.id)) += Proposal(-1, msg.title, msg.color, msg.creatorId))
  }

  def addProposalWithTimes(msg: AddProposalWithTimes) = db.withTransaction { implicit session =>
    val proposalId = (proposals returning proposals.map(_.id)) += Proposal(-1, msg.title, msg.color, msg.creatorId)
    msg.times.foreach { time =>
      val timeId = (proposalTimes returning proposalTimes.map(_.id)) += ProposalTime(-1, time._1, time._2, proposalId)
      proposalTimeVotes ++= msg.participants.map(ProposalTimeVote(timeId, _, Vote.NotVoted))
    }
    sender ! ProposalAdded(proposalId)
  }

  def addProposalTime(msg: AddProposalTime) = 
    sender ! ProposalTimeAdded(db.withTransaction { implicit session =>
      val proposalTimeId = (proposalTimes returning proposalTimes.map(_.id)) += ProposalTime(-1, msg.start, msg.end, msg.proposalId)
      proposalTimeVotes ++= msg.participants.map(ProposalTimeVote(proposalTimeId, _, Vote.NotVoted))
      proposalTimeId
  })

  def addProposalTimeVote(msg: AddProposalTimeVote) = db.withSession { implicit session =>
    proposalTimeVotes
      .filter(_.proposalTimeId === msg.proposalTimeId)
      .filter(_.userId === msg.userId)
      .map(v => v.vote)
      .update((msg.vote))
    sender ! ProposalTimeVoteAdded
  }

  // returns all proposals of user sorted by proposal id
  def getProposalsForUser(msg: GetProposalsForUser) = db.withSession { implicit session =>

    sender ! ProposalsForUser(
      proposalsForUserWithCreatorAndParticipant(msg.userId)
        .buildColl[Seq]
        .groupBy { case (p, cr, _) => (p, cr) }
        .mapValues(_.map(_._3))
        .toSeq
        .sortBy(_._1._1.id)
        .map { case ((p, cr), pars) => ProposalWithCreatorAndParticipants(p, cr, pars) }
    )
  }
  // returns proposal time suggestions sorted by proposal id
  def getProposalTimesFromProposal(msg: GetProposalTimesFromProposal) = db.withSession { implicit session =>
    sender ! ProposalTimesFromProposal(
      proposalTimesWithVotesFromProposal(msg.proposalId)
        .buildColl[Seq]
        .groupBy(_._1)
        .mapValues(_.map { case (_, proposalTimeVote, user) => VoteWithUser(proposalTimeVote.vote, user) })
        .toSeq
        .sortBy(_._1.id)
        .map(ProposalTimeWithVotes.tupled)
    )
  }

  def removeProposal(msg: RemoveProposal) = db.withTransaction { implicit session =>
    proposals.filter(_.id === msg.id).delete
    sender ! ProposalRemoved
  }

  def finishVote(msg: FinishVote) = db.withTransaction { implicit session =>
    proposalById(msg.proposalId).firstOption match {
      case Some(proposal) if proposal.creatorId == msg.creator => {
        msg.winningTimeIds.foreach({ winningTimeId =>
          // error handling?
          val proposalTime = proposalTimes.filter(_.id === winningTimeId).firstOption.get
          proposalTimeVotes.filter(_.proposalTimeId === proposalTime.id).buildColl[Seq].foreach({ vote =>
            // how to handle tags?
            val tag = tags.filter(_.userId === vote.userId).buildColl[Seq].sortBy(_.id).head
            val appointmentId = (appointments returning appointments.map(_.id)) += Appointment(-1, proposal.title, proposalTime.start, proposalTime.end)
            appointmentBelongsToTag += ((appointmentId, tag.id))
            proposalTimeVotes.filter(_.proposalTimeId === proposalTime.id).delete
          })
          proposalTimes.filter(_.id === winningTimeId).delete
        })
        proposalById(msg.proposalId).delete
        sender ! VoteFinished
      }
      case Some(_) => sender ! PermissionDeniedError("Proposal does not belong to specified user!")
      case _       => sender ! NoSuchProposalError(s"Proposal with id $msg.id does not exist!")
    }
  }

  def receive = handled {
    case msg: AddProposal                  => addProposal(msg)
    case msg: AddProposalWithTimes         => addProposalWithTimes(msg)
    case msg: AddProposalTime              => addProposalTime(msg)
    case msg: AddProposalTimeVote          => addProposalTimeVote(msg)
    case msg: GetProposalsForUser          => getProposalsForUser(msg)
    case msg: GetProposalTimesFromProposal => getProposalTimesFromProposal(msg)
    case msg: RemoveProposal               => removeProposal(msg)
    case msg: FinishVote                   => finishVote(msg)
  }
}