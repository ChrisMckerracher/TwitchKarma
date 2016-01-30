package models

import java.util.UUID

import com.websudos.phantom.CassandraTable
import com.websudos.phantom.dsl._

import connectors.Connector._
import entities.User

import scala.concurrent.Future

/**
  * Created on 2016-01-29.
  * @author Sina Ghaffari
  */
// case class User(name: String, karma: Int, credits: Double, enabled: Boolean)
class UserModel extends CassandraTable[UserModel, User] {
  override val tableName = "users"

  object name extends StringColumn(this) with PartitionKey[String]
  object karma extends IntColumn(this)
  object credits extends DoubleColumn(this)
  object enabled extends BooleanColumn(this)

  override def fromRow(r: Row): User = User(name(r), karma(r), credits(r), enabled(r))

  def insertUser(user: User): Future[ResultSet] = {
    insert
      .value(_.name, user.name)
      .value(_.karma, user.karma)
      .value(_.credits, user.credits)
      .value(_.enabled, user.enabled)
      .future
  }

  def opt(user: String, enabled: Boolean): Future[ResultSet] = {
    insert
      .value(_.name, user)
      .value(_.enabled, enabled)
      .future
  }

//  def vote(user: String, value: Int): Future[ResultSet] = {
//
//  }
}
