package scalable.transfer

import akka.actor.{Actor, ActorLogging, Props}

/**
  * Supervisor for all accounts.
  * Here the supervision strategy must be defined (if any).
  */
class Accountant extends Actor with ActorLogging {
  import scalable.transfer.Accountant._

  def receive = {
    case Create(nr, balance, limit) =>
      context.actorOf(Account.props(nr, balance, limit), nr)
      log.info(s"Acccount actor created nr=${nr} balance=${balance} limit=${limit}")
  }
}

object Accountant {
  def props: Props = Props[Accountant]

  case class Create(nr: String, balance: Long, limit: Long)
}
