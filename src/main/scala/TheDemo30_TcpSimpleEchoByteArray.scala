import akka.actor.ActorSystem
import akka.pattern.after
import akka.stream.scaladsl._
import akka.stream.{ActorFlowMaterializer, OperationAttributes}
import akka.util.ByteString

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

object TheDemo30_TcpSimpleEchoByteArray extends App {
  implicit val sys=ActorSystem("TheDemo")
  implicit val mat=ActorFlowMaterializer()
  //implicit val timeout = Timeout(3.seconds)
  import sys.dispatcher


  val server = Tcp().bind("localhost",8888).to(Sink.foreach {
    conn =>
      conn.flow.join(Flow[ByteString]).run()
  }).run()
  val myaddr = Await.result(server, 1.second)
  val client = Tcp().outgoingConnection(myaddr.localAddress)
  Source(0 to 10).map(x => ByteString(s"hello $x")).via(client)
    .map(_.decodeString("UTF-8")).runForeach(println)
}
