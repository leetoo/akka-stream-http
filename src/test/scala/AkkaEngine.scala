import akka.actor.ActorSystem
import akka.stream.ActorFlowMaterializer
import org.scalatest.{Suite, BeforeAndAfterAll}

//
trait AkkaEngine extends BeforeAndAfterAll {
  this: Suite =>

  // must be specified
  def akkaName: String
  implicit val system = ActorSystem(akkaName)
  implicit val materializer = ActorFlowMaterializer()

  //override def beforeAll = {}
  override def afterAll = {
    Thread.sleep(1000)
    system.shutdown()
  }

  def waitABit(seconds: Int = 1) = {
    Thread.sleep(seconds * 1000)
  }


}
