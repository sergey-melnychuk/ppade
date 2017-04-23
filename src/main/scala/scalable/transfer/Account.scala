package scalable.transfer

import akka.actor.{Actor, ActorLogging, Props}

class Account(nr: String, startBalance: Long, limit: Long) extends Actor with ActorLogging {
  import Account._

  var balance = startBalance
  var sequence: Long = 0
  val threshold = -limit

  override def receive = {
    case GetBalance =>
      log.info("balance: {}", balance)
      sender ! Balance(nr, sequence, balance)

    case op @ Deposit(amount) =>
      balance += amount
      sequence += 1
      log.info("seq: {}, deposit: {}, balance: {}", sequence, amount, balance)
      sender ! Result(nr, op, sequence, balance, None)

    case op @ Withdraw(amount) if balance - amount >= threshold =>
      balance -= amount
      sequence += 1
      log.info("seq: {}, withdraw: {}, balance: {}", sequence, amount, balance)
      sender ! Result(nr, op, sequence, balance, None)

    case op @ Withdraw(amount) =>
      val msg = s"limit check failed withdrawing ${amount} from ${balance}"
      sender ! Result(nr, op, sequence, balance, Some(msg))
  }
}

object Account {
  def props(nr: String, balance: Long = 0L, limit: Long = 0L): Props = Props(classOf[Account], nr, balance, limit)

  case object GetBalance
  case class Balance(accountNr: String, lastOp: Long, amount: Long)

  sealed trait Op {
    val amount: Long
  }
  case class Withdraw(amount: Long) extends Op
  case class Deposit(amount: Long) extends Op

  case class Result(accountNr: String, op: Op, seqNo: Long, amount: Long, error: Option[String])
}
