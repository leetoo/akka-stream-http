import akka.actor.ActorSystem

import akka.stream.ActorFlowMaterializer
import akka.stream.scaladsl._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Success

object TheDemo02_StrictCollection extends App {
  implicit val sys=ActorSystem("TheDemo")
  implicit val mat=ActorFlowMaterializer()

  val source=Source(1 to 100)

  // generate a stream of streams split with 10 elements in each substream
  // and then fetch just the first (or others)
  val collection=source.grouped(10).runWith(Sink.head)

  collection.onComplete{
    case Success(x) => println(s"Future completed with $x")
  }

  Thread.sleep(100)
  sys.shutdown()
}
