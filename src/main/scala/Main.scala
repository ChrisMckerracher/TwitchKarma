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
          println(ip.toString)
          parameters('user1, 'user2) { (_user1, _user2) => ctx: RequestContext =>
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
                          Try(voteModel.insertVote(Vote(UUID.randomUUID, user1, user2, DateTime.now, 1))) match {
                            case Success(s) => ctx.complete(s"$user1 upvoted $user2")
                            case Failure(f) => ctx.complete("Failure")
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
                            case Success(s) => ctx.complete(s"$user1 downvoted $user2")
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
                if (ip.toOption.get.getHostAddress.startsWith("23.92.68") || ip.toOption.get.getHostAddress.startsWith("107.155.125")) {
                  voteModel.findUserKarma(user).map { voteList =>
                    if (voteList.isEmpty) {
                      ctx.complete(s"$user is not in the database yet.")
                    } else {
                      ctx.complete(s"$user has ${voteList.sum} karma.")
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

  startServer(interface = "159.203.16.72", port = 8080) {
    route
  }
  def createTables = {
    Await.ready(voteModel.create.ifNotExists().future(), 5.seconds)
  }
}
