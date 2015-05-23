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
  override def afterAll() = {

    system.shutdown()
  }

  def waitABit(seconds: Double = 0.1) = {
    Thread.sleep((seconds * 1000).toInt)
  }


}
