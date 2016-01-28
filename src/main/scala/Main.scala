import akka.actor.ActorSystem
import spray.routing.SimpleRoutingApp

/**
  * Created on 2016-01-28.
  * @author Sina Ghaffari
  */
object Main extends App with SimpleRoutingApp {
  implicit val system = ActorSystem("my-system")
  val route =
    path("upvote") {
      get {
        parameters('user1, 'user2) { (user1, user2) =>
          complete {

          }
        }
      }
    } ~
    path("downvote") {
      get {
        parameters('user1, 'user2) { (user1, user2) =>
          complete {

          }
        }
      }
    } ~
    path("karma") {
      get {
        parameters('user) { user =>
          complete {

          }
        }
      }
    }

  startServer(interface = "localhost", port = 8080) {
    route
  }
}
