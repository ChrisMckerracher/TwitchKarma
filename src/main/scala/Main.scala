import java.util.UUID
import javax.xml.ws.spi.http.HttpContext

import akka.actor.ActorSystem
import entities.Vote
import models.VoteModel
import org.joda.time.DateTime
import spray.http.{HttpResponse, HttpRequest}
import spray.routing.{RequestContext, SimpleRoutingApp}
import spray.http._
import spray.client.pipelining._

import scala.concurrent.{Future, Await}
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
  val pipeline: HttpRequest => Future[HttpResponse] = sendReceive
  val voteModel = new VoteModel
  createTables

  val route =
    path("upvote") {
      get {
        clientIP { ip =>
          println(s"$ip sent request.")
          parameters('user1, 'user2) { (_user1, _user2) => ctx: RequestContext =>
            println(s"Request: $ctx")
            val user1 = _user1.toString.toLowerCase
            val user2 = _user2.toString.toLowerCase
            isUserInvalid(user1)
            .fallbackTo(isUserInvalid(user2))
            .fallbackTo(isNotNightbot(ip))
            .fallbackTo(isSelfVote(user1, user2))
            .fallbackTo(cantVote(user1, user2))
            .fallbackTo(
              Try(voteModel.insertVote(Vote(UUID.randomUUID, user1, user2, DateTime.now, 1))) match {
                case Success(s) => getKarma(user2).map( karma => s"$user1 upvoted $user2. $karma")
                case Failure(f) => Future("Failure")
              }).map(message => ctx.complete(message))
          }
        }
      }
    } ~
    path("downvote") {
      get {
        clientIP { ip =>
          println(ip.toString)
          parameters('user1.as[String], 'user2.as[String]) { (_user1, _user2) => ctx: RequestContext =>
            val user1 = _user1.toString.toLowerCase
            val user2 = _user2.toString.toLowerCase

            pipeline(Get(s"https://api.twitch.tv/kraken/channels/$user1")).map { user1Response =>
              pipeline(Get(s"https://api.twitch.tv/kraken/channels/$user2")).map { user2Response =>
                if (user1Response.status.isSuccess && user2Response.status.isSuccess) {
                  if (ip.toOption.get.getHostAddress.startsWith("23.92.68") || ip.toOption.get.getHostAddress.startsWith("107.155.125")) {
                    if (user1.toString.toLowerCase == user2.toString.toLowerCase) {
                      ctx.complete("You cannot vote on yourself.")
                    } else {
                      voteModel.findSentVotes(user1).map { sentVotes =>
                        val last10 = sentVotes.take(10)
                        println("last10: " + last10)
                        val voteMap = last10.map(_.user2).distinct.map(v => v -> last10.filter(_.user2 == v).map(_.time)).toMap
                        println("voteMap: " + voteMap)
                        var credits = 10.0
                        if (voteMap.nonEmpty)
                          credits += (DateTime.now.getMillis - last10.head.time.getMillis).millisecond.toMinutes

                        voteMap.foreach { usersVotedOn =>
                          println("usersVotedOn: " + usersVotedOn)
                          credits -= ((1 - Math.pow(1.25, usersVotedOn._2.length)) / (1 - 1.25))
                        }
                        println("credits: " + credits)
                        var allocation = credits
                        if (voteMap.contains(user2))
                          allocation -= Math.pow(1.25, voteMap(user2).length + 1)
                        if (allocation <= 0) {
                          ctx.complete("You have voted too many times recently. Please wait and try again.")
                        } else {
                          Try(voteModel.insertVote(Vote(UUID.randomUUID, user1.toString.toLowerCase, user2.toString.toLowerCase, DateTime.now, -1))) match {
                            case Success(s) => ctx.complete(s"$user1 downvoted $user2. ${getKarma(user2)}")
                            case Failure(f) => ctx.complete(s"Failed to downvote user.")
                          }
                        }
                      }
                    }
                  } else {
                    ctx.complete(s"Nice try, you can't cheat the system.")
                  }
                } else {
                  ctx.complete(s"One of the users entered does not exist.")
                }
              }
            }
          }
        }
      }
    } ~
    path("karma") {
      get {
        clientIP { ip =>
          parameter('user) { _user => ctx: RequestContext =>
            val user = _user.toLowerCase
            pipeline(Get(s"https://api.twitch.tv/kraken/channels/$user")).map { userResponse =>
              if (userResponse.status.isSuccess) {
                ctx.complete(getKarma(user))
              } else {
                ctx.complete(s"That user does not exist.")
              }
            }
          }
        }
      }
    }

  startServer(interface = "159.203.16.72", port = 8080) {
    route
  }
  def isUserInvalid(user: String): Future[String] = {
    pipeline(Get(s"https://api.twitch.tv/kraken/channels/$user")).map { response =>
      if (response.status.isFailure) {
        s"$user does not exist."
      } else {
        throw new Exception("User exists!")
      }
    }
  }

  def isNotNightbot(ip: RemoteAddress): Future[String] = Future {
    if (!ip.toOption.get.getHostAddress.startsWith("23.92.68") && !ip.toOption.get.getHostAddress.startsWith("107.155.125")) {
      s"Nice try, you can't cheat the system!"
    } else {
      throw new Exception("Requestor is Nightbot")
    }
  }

  def isSelfVote(user1: String, user2: String): Future[String] = Future {
    if (user1 == user2) {
      s"You cannot vote on yourself."
    } else {
      throw new Exception("User is voting on someone else.")
    }
  }

  def cantVote(voter: String, votee: String): Future[String] = Future {
//    val maxCredits = 10
//    val creditsPerHour = 1
//    val duplicateMultiplier = 2
    throw new Exception("User is can vote.")
  }


  def getKarma(user: String): Future[String] = {
    voteModel.findUserKarma(user).map { voteList =>
      if (voteList.isEmpty) {
        s"$user is not in the database yet."
      } else {
        s"$user has ${voteList.sum} karma."
      }
    }
  }
  def createTables = {
    Await.ready(voteModel.create.ifNotExists().future(), 5.seconds)
  }
}
