import java.security.{Security, MessageDigest}

import akka.actor.ActorSystem
import akka.stream.ActorFlowMaterializer
import akka.stream.scaladsl._
import akka.util.ByteString

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Success


object TheDemo05_ByteStringDigest extends App {
  implicit val sys=ActorSystem("TheDemo")
  implicit val mat=ActorFlowMaterializer()


  /**
   * see [[http://doc.akka.io/docs/akka-stream-and-http-experimental/1.0-RC3/scala/stream-cookbook.html]]
   */

  import akka.stream.stage._
  def digestCalculator(algorithm: String) = new PushPullStage[ByteString, ByteString] {
    val digest = MessageDigest.getInstance(algorithm)

    override def onPush(chunk: ByteString, ctx: Context[ByteString]): SyncDirective = {
      digest.update(chunk.toArray)
      ctx.pull()
    }

    override def onPull(ctx: Context[ByteString]): SyncDirective = {
      if (ctx.isFinishing) ctx.pushAndFinish(ByteString(digest.digest()))
      else ctx.pull()
    }

    override def onUpstreamFinish(ctx: Context[ByteString]): TerminationDirective = {
      // If the stream is finished, we need to emit the last element in the onPull block.
      // It is not allowed to directly emit elements from a termination block
      // (onUpstreamFinish or onUpstreamFailure)
      ctx.absorbTermination()
    }
  }
  def bytes2hex(bytes: Array[Byte], sep: Option[String] = None): String = {
    sep match {
      case None => bytes.map("%02x".format(_)).mkString
      case _ => bytes.map("%02x".format(_)).mkString(sep.get)
    }
    // bytes.foreach(println)
  }

  // have a source of byteStrings
  val source=Source(List("line1","line2","line3")).map(ByteString(_))

  source.transform( () => digestCalculator("SHA-256")).runForeach(ba => println(s"Digest is '${bytes2hex(ba.toArray)}'"))

  Thread.sleep(100)
  sys.shutdown()
}
