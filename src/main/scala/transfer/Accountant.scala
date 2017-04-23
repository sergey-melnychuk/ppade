package transfer

import akka.actor.{Actor, ActorLogging, Props}
import transfer.Accountant.Create

/**
  * Supervisor for all accounts.
  * Here the supervision strategy must be defined (if any).
  */
class Accountant extends Actor with ActorLogging {
  def receive = {
    case Create(nr, balance, limit) =>
      context.actorOf(Account.props(nr, balance, limit), nr)
  }
}

object Accountant {
  def props: Props = Props[Accountant]

  case class Create(nr: String, balance: Long, limit: Long)
}
