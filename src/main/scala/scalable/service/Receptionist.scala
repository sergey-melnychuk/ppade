package scalable.service

import akka.actor.{Actor, ActorLogging, Props}
import scalable.transfer.Transfer

class Receptionist extends Actor with ActorLogging {
  import Receptionist._
  var sequence = 0L
  def receive = {
    case Request(MoneyTransfer(srcNr, dstNr, amount)) =>
      sequence += 1
      val replyTo = sender()
      val transferActor = context.actorOf(Transfer.props(sequence, srcNr, dstNr, amount, replyTo), sequence.toString)
      transferActor ! Transfer.StartTransfer
  }
}

object Receptionist {
  def props = Props[Receptionist]

  sealed trait Op
  case class MoneyTransfer(srcNr: String, dstNr: String, amount: Long) extends Op

  case class Request(op: Op)
}
