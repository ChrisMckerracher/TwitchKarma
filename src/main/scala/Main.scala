import java.util.UUID

import akka.actor.ActorSystem
import entities.Vote
import models.VoteModel
import org.joda.time.DateTime
import spray.routing.SimpleRoutingApp

import scala.concurrent.Await
import scala.concurrent.duration._

import connectors.Connector._

import scala.util.{Failure, Success, Try}

import scala.concurrent.ExecutionContext.Implicits.global
/**
  * Created on 2016-01-28.
  * @author Sina Ghaffari
  */
object Main extends App with SimpleRoutingApp {
  implicit val system = ActorSystem("my-system")
  val voteModel = new VoteModel
  createTables

  val route =
    path("upvote") {
      get {
        parameters('user1, 'user2) { (user1, user2) =>
          complete {
            Try(voteModel.insertVote(Vote(UUID.randomUUID, user1, user2, DateTime.now, 1))) match {
              case Success(s) => s"$user1 upvoted $user2"
              case Failure(f) => "Failure"
            }
          }
        }
      }
    } ~
    path("downvote") {
      get {
        parameters('user1, 'user2) { (user1, user2) =>
          complete {
            Try(voteModel.insertVote(Vote(UUID.randomUUID, user1, user2, DateTime.now, -1))) match {
              case Success(s) => s"$user1 downvoted $user2"
              case Failure(f) => "Failure"
            }
          }
        }
      }
    } ~
    path("karma") {
      get {
        parameter('user) { user =>
          onComplete(voteModel.findUserKarma(user).map(_.sum)) {
            case Success(s) => complete("" + s)
            case Failure(f) => complete("Failure")
          }
        }
      }
    }

  startServer(interface = "localhost", port = 8080) {
    route
  }
  def createTables = {
    Await.ready(voteModel.create.ifNotExists().future(), 5.seconds)
  }
}
