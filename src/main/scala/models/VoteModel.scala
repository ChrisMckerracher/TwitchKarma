package models

import java.util.UUID

import com.websudos.phantom.CassandraTable
import com.websudos.phantom.dsl._

import connectors.Connector._
import entities.Vote

import scala.concurrent.Future

/**
  * Created on 2016-01-28.
  * @author Sina Ghaffari
  */
// case class Vote(id: UUID, user1: String, user2: String, time: DateTime, vote: Int)
class VoteModel extends CassandraTable[VoteModel, Vote] {
  override val tableName = "votes"

  object id extends UUIDColumn(this) with PartitionKey[UUID]
  object user1 extends StringColumn(this) with Index[String]
  object user2 extends StringColumn(this) with Index[String]
  object time extends DateTimeColumn(this) with ClusteringOrder[DateTime]
  object vote extends IntColumn(this) with ClusteringOrder[Int]

  override def fromRow(r: Row): Vote = Vote(id(r), user1(r), user2(r), time(r), vote(r))

  def insertVote(vote: Vote): Future[ResultSet] = {
    insert
      .value(_.id, vote.id)
      .value(_.user1, vote.user1)
      .value(_.user2, vote.user2)
      .value(_.time, vote.time)
      .value(_.vote, vote.vote)
      .future
  }

  def findSentVotes(user: String) = {
    select
      .where(_.user1 eqs user)
      .fetch
  }
  def findRecievedVotes(user: String) = {
    select
      .where(_.user2 eqs user)
      .fetch
  }
  def findUserKarma(user: String) = {
    select(_.vote)
      .where(_.user2 eqs user)
      .fetch
  }
}
