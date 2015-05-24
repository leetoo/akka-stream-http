import akka.actor.ActorSystem
import akka.event.Logging
import akka.stream.scaladsl._
import akka.stream.stage._
import akka.stream.{ActorFlowMaterializer, OperationAttributes}
import akka.util.{ByteString, CompactByteString}

import scala.annotation.tailrec


object TheDemo10_ParseLines extends App {
  implicit val sys = ActorSystem("TheDemo")
  implicit val mat = ActorFlowMaterializer()

  // useful function if we need to dump hexadecimal for debug
  def bytes2hex(bytes: Array[Byte], sep: Option[String] = None): String = {
    sep match {
      case None => bytes.map("%02x".format(_)).mkString
      case _ => bytes.map("%02x".format(_)).mkString(sep.get)
    }
    // bytes.foreach(println)
  }

  /**
   * Taken from [[http://doc.akka.io/docs/akka-stream-and-http-experimental/1.0-RC3/scala/stream-cookbook.html]]
   * It is a complete handler with states (actually just one) which accumulates in a vector all the parsed lines and emits
   * taking care of handling an internal buffer
   *
   * @param separator
   * @param maximumLineBytes
   * @return
   */
  def parseLines(separator: String, maximumLineBytes: Int) =
    new StatefulStage[ByteString, String] {
      private val separatorBytes = ByteString(separator)
      private val firstSeparatorByte = separatorBytes.head

      private var buffer = ByteString.empty
      private var nextPossibleMatch = 0

      def initial = new State {
        override def onPush(chunk: ByteString, ctx: Context[String]): SyncDirective = {
          buffer ++= chunk
          if (buffer.size > maximumLineBytes)
            ctx.fail(new IllegalStateException(s"Read ${buffer.size} bytes " +
              s"which is more than $maximumLineBytes without seeing a line terminator"))
          else emit(doParse(Vector.empty).iterator, ctx)
        }

        @tailrec
        private def doParse(parsedLinesSoFar: Vector[String]): Vector[String] = {
          val possibleMatchPos = buffer.indexOf(firstSeparatorByte, from = nextPossibleMatch)
          if (possibleMatchPos == -1) {
            // No matching character, we need to accumulate more bytes into the buffer
            nextPossibleMatch = buffer.size
            parsedLinesSoFar
          } else if (possibleMatchPos + separatorBytes.size > buffer.size) {
            // We have found a possible match (we found the first character of the terminator
            // sequence) but we don't have yet enough bytes. We remember the position to
            // retry from next time.
            nextPossibleMatch = possibleMatchPos
            parsedLinesSoFar
          } else {
            if (buffer.slice(possibleMatchPos, possibleMatchPos + separatorBytes.size)
              == separatorBytes) {
              // Found a match
              val parsedLine = buffer.slice(0, possibleMatchPos).utf8String
              buffer = buffer.drop(possibleMatchPos + separatorBytes.size)
              nextPossibleMatch -= possibleMatchPos + separatorBytes.size
              doParse(parsedLinesSoFar :+ parsedLine)
            } else {
              nextPossibleMatch += 1
              doParse(parsedLinesSoFar)
            }
          }

        }
      }
    }

  val originalString="line1\nline2\nline3\nline4"

  val source = Source.single(ByteString(originalString))

  source.transform( () => parseLines("\n",80)).runForeach(println)





  Thread.sleep(100)
  sys.shutdown()
}
