import java.net.InetSocketAddress

import akka.actor.ActorDSL._
import akka.actor.ActorSystem
import akka.stream.{OperationAttributes, ActorFlowMaterializer}
import akka.stream.scaladsl._
import akka.util.{ByteString, Timeout}
import akka.pattern.after
import scala.concurrent.duration._


import scala.concurrent.{Await, Future}

object TheDemo extends App {
  implicit val sys=ActorSystem("TheDemo")
  implicit val mat=ActorFlowMaterializer()
  //implicit val timeout = Timeout(3.seconds)
  import sys.dispatcher

  //Source(List(1,2,3)).runForeach(println)
  val numbers=Source(List(1,2,3))
  val strings=Source(List("a","b","c"))

  val composite = Source(){ implicit b =>
    import FlowGraph.Implicits._
    val zip=b.add(Zip[Int,String]())
    numbers ~> zip.in0
    strings ~> zip.in1
    zip.out
  }

  //composite.runForeach(println)
  val fast = Source(()=> Iterator from 0)
  // this is taking much cpu
  //fast.runForeach(println)
  //
  // this is doing an active wait which is discouraged
  // fast.map(x => {Thread.sleep(1000); x}).runForeach(println)

  // separating the delay and implemented as a future to avoid
  // thread blocking
  def delay(x:Int):Future[Int] =
    after(1.second, sys.scheduler)(Future.successful(x))

  // 10 means 10 at a time
/*
  fast.mapAsync(10)(delay)
    .runForeach(println)
*/
  // this is not working as roland tells us in the video
  val single = Flow[Int].withAttributes(OperationAttributes.inputBuffer(1,1))
  /*fast.mapAsync(4)(delay)
    .via(single)
    .runForeach(println)*/


  val server = Tcp().bind("localhost",8888).to(Sink.foreach {
    conn =>
      conn.flow.join(Flow[ByteString]).run()
  }).run()
  val myaddr = Await.result(server, 1.second)
  val client = Tcp().outgoingConnection(myaddr.localAddress)
  Source(0 to 10).map(x => ByteString(s"hello $x")).via(client)
    .map(_.decodeString("UTF-8")).runForeach(println)
}
