import akka.actor.ActorSystem
import akka.stream.{BidiShape, ActorFlowMaterializer}
import akka.stream.scaladsl._
import akka.util.ByteString

import scala.concurrent.Await
import scala.concurrent.duration._
import Protocols._

object TheDemo61_TcpFramedExample extends App {
  implicit val sys=ActorSystem("TheDemo")
  implicit val mat=ActorFlowMaterializer()
  //implicit val timeout = Timeout(3.seconds)


  // this codec converts message => ByteString and back from ByteString to Message
  // for instance Ping(2) becomes 01.02.00.00.00 and back
  val codec = BidiFlow() { implicit b =>
    val top = b.add(Flow[Message].map(toBytes))
    val bottom = b.add(Flow[ByteString].map(fromBytes))

    BidiShape(top,bottom)
  }

  // have this codec to be on the top of a framing so Messages => ByteString => Framed messages
  // Ping(2) becomes 05.00.00.00 01.02.00.00.00
  val protocol = codec atop framing

  // server is using REVERSED protocol which is converting from byte
  // for example from 05.00.00.00 02.02.00.00.00 to Pong(2)
  val server = Tcp().bind("localhost",0).to(Sink.foreach {
    conn =>
      // conn flows through the protocol reversed to a simple Flow which closes the loop
      // and converts Ping(x) in Pong(x)
      conn.flow.join(protocol.reversed).join(Flow[Message].collect {
        case Ping(id) => Pong(id):Message // This :Message IS needed otherwise scala doesn't understand see the video from ScalaDays
      }).run() // run the sink
  }).run() // and run the bind
  val myaddr = Await.result(server, 1.second)

  val client = Tcp().outgoingConnection(myaddr.localAddress)

  // client protocol starts with a Message, converts to proper chunks and then loops
  // over the tcp interpreting back the protocol
  val stack = protocol join client



  Source(0 to 10).map(Ping).via(stack).runForeach(println)
}
