package scalable.service

import java.util.concurrent.CountDownLatch

import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.Directives._
import scalable.transfer.Transfer
import akka.http.scaladsl.server.Directives.{complete, get, pathPrefix, post}
import akka.stream.ActorMaterializer

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.concurrent.duration.FiniteDuration

private [service]
class Server(receptionist: ActorRef, port: Int)(implicit system: ActorSystem) {

  def transfer(ref: ActorRef, src: String, dst: String, amt: Long, timeout: FiniteDuration): Future[Transfer.Result] = {
    import akka.pattern.ask
    val transfer = Receptionist.MoneyTransfer(src, dst, amt)
    ref.ask(Receptionist.Request(transfer))(timeout).mapTo[Transfer.Result]
  }

  def start(): Unit = {
    implicit val materializer = ActorMaterializer()
    implicit val executionContext = system.dispatcher

    val latch = new CountDownLatch(1)

    val route: Route =
      get {
        pathPrefix("balance" / LongNumber) { id =>
          complete(s"not supported\n")
        }
      } ~
      post {
        pathPrefix("transfer" / Segment / Segment / LongNumber) {
          (src, dst, amt) => {
            val futureResult = transfer(receptionist, src, dst, amt, 5 seconds).map({
              case Transfer.Result(id, _, _, _, None) =>
                s"Transfer ${id} completed\n"
              case Transfer.Result(id, _, _, _, Some(e)) =>
                s"Transfer ${id} failed: ${e}\n"
            })

            complete(futureResult)
          }
        }
      } ~
      post {
        path("shutdown") {
          latch.countDown()
          complete("shutdown\n")
        }
      }

    val bindingFuture = Http().bindAndHandle(route, "localhost", port)

    latch.await() // block until server is shut-down

    bindingFuture
      .flatMap(_.unbind())
      .onComplete(_ => system.terminate())
  }

}

object Server {
  def apply(port: Int, system: ActorSystem, receptionist: ActorRef) = new Server(receptionist, port)(system)
}
