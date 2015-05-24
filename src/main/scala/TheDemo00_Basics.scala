import akka.actor.ActorSystem
import akka.stream.ActorFlowMaterializer
import akka.stream.scaladsl._

object TheDemo00_Basics extends App {
  implicit val sys=ActorSystem("TheDemo")
  implicit val mat=ActorFlowMaterializer()


  val source=Source(List(1,"alpha",3))

  val sink=Sink.foreach(println)
  source.to(sink).run()
  Thread.sleep(100)

  // simplest debug
  val debugged=source.map(t => { println(s"source is providing '$t'"); t})
  debugged.runForeach(t => Unit)
  Thread.sleep(100)
  sys.shutdown()
}
