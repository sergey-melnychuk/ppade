package transfer

import scala.language.postfixOps
import akka.actor.{Actor, ActorLogging, ActorNotFound, ActorRef, Props, ReceiveTimeout}
import akka.pattern.pipe

import scala.concurrent.duration._
import scala.concurrent.duration.FiniteDuration

class Transfer(id: Long, srcNr: String, dstNr: String, amount: Long, replyTo: ActorRef) extends Actor with ActorLogging {
  import Transfer._

  // required for Future{.map, .recover} as implicit ExecutionContext
  import context.dispatcher

  def started: Receive = {
    case StartTransfer =>
      lookup(s"/user/account/${srcNr}", "src", self)
      lookup(s"/user/account/${dstNr}", "dst", self)
      context become waiting(Set("src", "dst"), Map.empty[String, ActorRef], amount)
  }

  def lookup(path: String, key: String, recipient: ActorRef, timeout: FiniteDuration = 1 second): Unit = {
    val f = context.actorSelection(path)
      .resolveOne(timeout)
      .map(ref => Found(key, ref))
      .recover({
        case ActorNotFound(selection) => NotFound(key, selection.pathString)
      })
    pipe(f).pipeTo(recipient)
  }

  def waiting(q: Set[String], acc: Map[String, ActorRef], amount: Long): Receive = {
    case Found(key, ref) if q(key) =>
      val data = acc + (key -> ref)
      val pending = q - key
      if (pending.isEmpty) {
        context become withdraw(data("src"), data("dst"), amount)
        self ! StartWithdraw
      } else {
        context become waiting(pending, data, amount)
      }
    case NotFound(key, selection) if q(key) =>
      val msg = s"Can't find actor '${key}' by selection '${selection}'"
      fail(msg)
  }

  def withdraw(src: ActorRef, dst: ActorRef, amount: Long): Receive = {
    case StartWithdraw =>
      src ! Account.Withdraw(amount)
      context.become({
        case Account.Result(_, _, _, _, None) =>
          context become deposit(src, dst, amount)
          self ! StartDeposit
        case Account.Result(_, _, _, _, Some(e)) =>
          val msg = s"Withdraw failed id=${id} src=${srcNr}: $e"
          fail(msg)
        case ReceiveTimeout =>
          fail("Withdraw timeout")
      }, discardOld = true)
      context.setReceiveTimeout(5 seconds)
  }

  def deposit(src: ActorRef, dst: ActorRef, amount: Long): Receive = {
    case StartDeposit =>
      dst ! Account.Deposit(amount)
      context.become({
        case Account.Result(_, _, _, _, None) =>
          success()
        case Account.Result(_, _, _, _, Some(e)) =>
          val msg = s"Deposit failed id=${id} dst=${dstNr}: $e"
          log.error(msg)
          context.become(deposit(src, dst, amount), discardOld = true) // infinite retry
          context.system.scheduler.scheduleOnce(5 seconds, self, StartDeposit)
        case ReceiveTimeout =>
          val msg = "Deposit timeout"
          log.error(msg)
          context.become(deposit(src, dst, amount), discardOld = true) // infinite retry
          context.system.scheduler.scheduleOnce(5 seconds, self, StartDeposit)
      }, discardOld = true)
      context.setReceiveTimeout(5 seconds)
  }

  def success(): Unit = {
    log.debug("Transfer id={} src={} dst={} amt={} succeeded", id, srcNr, dstNr, amount)
    replyTo ! Result(id, srcNr, dstNr, amount, None)
    context stop self
  }

  def fail(msg: String): Unit = {
    log.error(msg)
    replyTo ! Result(id, srcNr, dstNr, amount, Some(msg))
    context stop self
  }

  def receive = started
}

object Transfer {
  def props(id: Long, srcNr: String, dstNr: String, amount: Long, replyTo: ActorRef): Props =
    Props(classOf[Transfer], id, srcNr, dstNr, amount, replyTo)

  case object StartTransfer

  private[transfer] case object StartWithdraw
  private[transfer] case object StartDeposit

  private[transfer] case class Found(key: String, ref: ActorRef)
  private[transfer] case class NotFound(key: String, path: String)

  private[transfer] case class Withdraw(src: ActorRef, dst: ActorRef, amount: Long)
  private[transfer] case class Deposit(src: ActorRef, dst: ActorRef, amount: Long)

  case class Result(id: Long, srdNr: String, dstNr: String, amount: Long, error: Option[String])
}
