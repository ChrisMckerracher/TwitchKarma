import java.util.UUID
import javax.xml.ws.spi.http.HttpContext

import akka.actor.ActorSystem
import entities.Vote
import models.VoteModel
import org.joda.time.DateTime
import spray.routing.{RequestContext, SimpleRoutingApp}

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
        parameters('user1, 'user2) { (user1, user2) => ctx =>
          println(ctx.toString)
          if (user1.toString.toLowerCase == user2.toString.toLowerCase) {
            ctx.complete("You cannot vote on yourself.")
          } else {
            Try(voteModel.insertVote(Vote(UUID.randomUUID, user1, user2, DateTime.now, 1))) match {
              case Success(s) => ctx.complete(s"$user1 upvoted $user2")
              case Failure(f) => ctx.complete("Failure")
            }
          }
        }
      }
    } ~
    path("downvote") {
      get {
        parameters('user1, 'user2) { (user1, user2) => ctx =>
          println(ctx.toString)
          if (user1.toString.toLowerCase == user2.toString.toLowerCase) {
            ctx.complete("You cannot vote on yourself.")
          } else {
            Try(voteModel.insertVote(Vote(UUID.randomUUID, user1.toString.toLowerCase, user2.toString.toLowerCase, DateTime.now, -1))) match {
              case Success(s) => ctx.complete(s"$user1 downvoted $user2")
              case Failure(f) => ctx.complete("Failure")
            }
          }
        }
      }
    } ~
    path("karma") {
      get {
        parameter('user) { user => ctx =>
          println(ctx.toString)
          voteModel.findUserKarma(user).map { voteList =>
            if (voteList.isEmpty) {
              ctx.complete("User is not in the database yet.")
            } else {
              ctx.complete(voteList.sum)
            }
          }
        }
      }
    }

  startServer(interface = "159.203.16.72", port = 8080) {
    route
  }
  def createTables = {
    Await.ready(voteModel.create.ifNotExists().future(), 5.seconds)
  }
}
