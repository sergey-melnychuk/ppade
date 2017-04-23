package transfer

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestKit}
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

class AccountSpec extends TestKit(ActorSystem("AccountSpec"))
  with ImplicitSender
  with WordSpecLike
  with Matchers
  with BeforeAndAfterAll {

  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }

  "An Account actor" must {
    "respond with current balance" in {
      val account = system.actorOf(Account.props("0"), "0")
      account ! Account.GetBalance
      expectMsg(Account.Balance("0", 0, 0L))
    }

    "respond with deposit result" in {
      val account = system.actorOf(Account.props("1"), "1")
      val op = Account.Deposit(1000)
      account ! op
      expectMsg(Account.Result("1", op, 1L, 1000, None))
    }

    "respond with withdraw result" in {
      val account = system.actorOf(Account.props("2", 1000), "2")
      val op = Account.Withdraw(1000)
      account ! op
      expectMsg(Account.Result("2", op, 1L, 0, None))
    }

    "respond with error withdraw result" in {
      val account = system.actorOf(Account.props("3"), "3")
      val op = Account.Withdraw(1000)
      account ! op
      expectMsg(Account.Result("3", op, 0, 0, Some("limit check failed withdrawing 1000 from 0")))
    }
  }
}
