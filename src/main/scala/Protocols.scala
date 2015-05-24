import java.nio.ByteOrder

import akka.stream._
import akka.stream.scaladsl._
import akka.stream.stage._
import akka.util.ByteString

object Protocols {


  // we have two types of messages which are essentially Int
  trait Message
  case class Ping(id: Int) extends Message
  case class Pong(id: Int) extends Message

  def bytes2hex(bytes: Array[Byte], sep: Option[String] = None): String = {
    sep match {
      case None => bytes.map("%02x".format(_)).mkString
      case _ => bytes.map("%02x".format(_)).mkString(sep.get)
    }
    // bytes.foreach(println)
  }

  // serialization is just a prefix 1 for ping and 2 for pong and the int
  def toBytes(msg: Message): ByteString = {
    implicit val order = ByteOrder.LITTLE_ENDIAN
    msg match {
      case Ping(id) => ByteString.newBuilder.putByte(1).putInt(id).result()
      case Pong(id) => ByteString.newBuilder.putByte(2).putInt(id).result()
    }
  }

  // deserialization reads a byte and if 1 is ping 2 pong
  def fromBytes(bytes: ByteString): Message = {
    implicit val order = ByteOrder.LITTLE_ENDIAN
    val it = bytes.iterator
    it.getByte match {
      case 1     => Ping(it.getInt)
      case 2     => Pong(it.getInt)
      case other => throw new RuntimeException(s"parse error: expected 1|2 got $other")
    }
  }

  // framing is a bidirectional processor
  val framing = BidiFlow() { b =>
    implicit val order = ByteOrder.LITTLE_ENDIAN

    // the frame has a header with length of the message
    // the actual length is stored in 4 bytes
    def addLengthHeader(bytes: ByteString) = {
      val len = bytes.length
      ByteString.newBuilder.putInt(len).append(bytes).result()
    }

    // the theory is that we "frame" each message with a Int header with total length
    class FrameParser extends PushPullStage[ByteString, ByteString] {
      // this holds the received but not yet parsed bytes
      // stash is normally less equal than 4
      var stash = ByteString.empty
      // this holds the current message length or -1 if at a boundary
      var needed = -1

      // on push accumulates data and check if we can emit something
      override def onPush(bytes: ByteString, ctx: Context[ByteString]) = {

        println(s"Stashing a block ${bytes2hex(bytes.toArray,Some("."))}")
        stash ++= bytes
        run(ctx)
      }
      // on pull just check if we have something in the stash ready
      // if not we must propagate the pull
      override def onPull(ctx: Context[ByteString]) = run(ctx)

      // if upstream is finished check if we have stash to be consumed
      override def onUpstreamFinish(ctx: Context[ByteString]) = {
        // no stash meaning we are really finished
        if (stash.isEmpty) ctx.finish()
        else ctx.absorbTermination() // we still have bytes to emit
      }

      // run decides if we can emit or not
      private def run(ctx: Context[ByteString]): SyncDirective =
        // we started at a boundary or we just emitted everything
        if (needed == -1) {
          // are we at a boundary? then figure out next length
          // if we have less than 4 we don't know how long is the message and must pull
          if (stash.length < 4) pullOrFinish(ctx)
          else {
            // we have at least 4 bytes, so get how many bytes we need to finish the message
            needed = stash.iterator.getInt
            // remove message length we just decoded
            stash = stash.drop(4)
            run(ctx) // cycle back to possibly already emit the next chunk
          }
        } else if (stash.length < needed) {
          // we are in the middle of a message, need more bytes
          pullOrFinish(ctx)
        } else {
          // we have enough to emit at least one message, so do it
          val emit = stash.take(needed)
          stash = stash.drop(needed)
          needed = -1
          // hurrah! we have the complete message
          // message is something like 0109000000 which is 1 byte for the type plus 4 bytes for the Int
          println(s"Emitting a single message ${bytes2hex(emit.toArray,Some("."))}")
          ctx.push(emit)
        }

      /*
       * After having called absorbTermination() we cannot pull any more, so if we need
       * more data we will just have to give up.
       */
      private def pullOrFinish(ctx: Context[ByteString]) =
        if (ctx.isFinishing) ctx.finish()
        else ctx.pull()
    }

    // OutBound messages => each ByteString added is mapped prefixing with its length
    val outbound = b.add(Flow[ByteString].map(addLengthHeader))
    // inbound messages are transformed so that they are truly framed and without the message length
    val inbound = b.add(Flow[ByteString].transform(() => new FrameParser))
    BidiShape(outbound, inbound)
  } // this is the framing BidiFlow

  // this other bidiflow is used to mapconcat, i.e. to have a flat stream of ByteString
  // cut in simple pieces instead of multiple ByteStrings on the same stream
  val chopUp = BidiFlow() { b =>
    val f = Flow[ByteString].mapConcat(_.map(ByteString(_)))
    BidiShape(b.add(f), b.add(f))
  }

  // this is doing the reverse, concatenating multiple ByteStreams together having chunks of 1000 bytes
  // except for the last which might be smaller
  val accumulate = BidiFlow() { b =>
    val f = Flow[ByteString].grouped(1000).map(_.fold(ByteString.empty)(_ ++ _))
    BidiShape(b.add(f), b.add(f))
  }
}
