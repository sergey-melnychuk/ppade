package transfer

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestKit}
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

class TransferSpec extends TestKit(ActorSystem("TransferSpec"))
  with ImplicitSender
  with WordSpecLike
  with Matchers
  with BeforeAndAfterAll {

  override def beforeAll {
    val accountant = system.actorOf(Accountant.props, "account")
    accountant ! Accountant.Create("001", 1000, 0)
    accountant ! Accountant.Create("002",    0, 0)
  }

  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }

  "A Transfer actor" must {
    "execute transfer for valid accounts" in {
      system.actorSelection("/user/account/001").tell(Account.GetBalance, self)
      expectMsg(Account.Balance("001", 0, 1000))

      system.actorSelection("/user/account/002").tell(Account.GetBalance, self)
      expectMsg(Account.Balance("002", 0, 0))

      val transfer = system.actorOf(Transfer.props(1L, "001", "002", 1000, self))
      transfer ! Transfer.StartTransfer
      expectMsg(Transfer.Result(1L, "001", "002", 1000, None))

      system.actorSelection("/user/account/001").tell(Account.GetBalance, self)
      expectMsg(Account.Balance("001", 1, 0))

      system.actorSelection("/user/account/002").tell(Account.GetBalance, self)
      expectMsg(Account.Balance("002", 1, 1000))
    }

    "report error for invalid src account" in {
      val transfer = system.actorOf(Transfer.props(1L, "001111", "002", 1000, self))
      transfer ! Transfer.StartTransfer
      expectMsg(Transfer.Result(1L, "001111", "002", 1000, Some("Can't find actor 'src' by selection '/user/account/001111'")))
    }

    "report error for invalid dst account" in {
      val transfer = system.actorOf(Transfer.props(1L, "001", "002222", 1000, self))
      transfer ! Transfer.StartTransfer
      expectMsg(Transfer.Result(1L, "001", "002222", 1000, Some("Can't find actor 'dst' by selection '/user/account/002222'")))
    }

    "report error for src account limit exhaustion" in {
      val transfer = system.actorOf(Transfer.props(1L, "001", "002", 1000, self))
      transfer ! Transfer.StartTransfer
      expectMsg(Transfer.Result(1L, "001", "002", 1000, Some("Withdraw failed id=1 src=001: limit check failed withdrawing 1000 from 0")))
    }

  }
}