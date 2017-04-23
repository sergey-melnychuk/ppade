package scalable

import akka.actor.ActorSystem

import scala.util.Random
import scalable.service.{Receptionist, Server}
import scalable.transfer.Accountant

/**
  * Created by sergey on 4/23/17.
  */
object Main {
  val random = new Random(42)

  implicit val system = ActorSystem("transfers")
  val accountant = system.actorOf(Accountant.props, "account")
  val receptionist = system.actorOf(Receptionist.props, "receptionist")

  (1 to 10).foreach(i => {
    val nr = f"${i}%05d"
    val balance = random.nextInt(10) * 1000
    accountant ! Accountant.Create(nr, balance, 0L)
  })

  def main(args: Array[String]): Unit = {
    val server = Server(8080, system, receptionist)
    server.start()
  }
}
