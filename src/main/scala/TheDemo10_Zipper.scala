import akka.actor.ActorSystem
import akka.pattern.after
import akka.stream.scaladsl._
import akka.stream.{ActorFlowMaterializer, OperationAttributes}

import scala.concurrent.Future
import scala.concurrent.duration._

object TheDemo10_Zipper extends App {
  implicit val sys=ActorSystem("TheDemo")
  implicit val mat=ActorFlowMaterializer()


  val numbers=Source(List(1,2,3))
  val strings=Source(List("a","b","c"))

  val composite = Source(){ implicit b =>
    import FlowGraph.Implicits._
    val zip=b.add(Zip[Int,String]())
    numbers ~> zip.in0
    strings ~> zip.in1
    zip.out
  }

    composite.runForeach(println)

}
