import akka.actor.ActorSystem
import akka.stream.{OverflowStrategy, ActorFlowMaterializer}
import akka.stream.scaladsl._
import akka.stream.stage._
import akka.util.ByteString

import scala.annotation.tailrec
import scala.concurrent.Future
import scala.util.Success


object TheDemo11_ReduceByKey extends App {

  import scala.concurrent.ExecutionContext.Implicits.global

  implicit val sys = ActorSystem("TheDemo")
  implicit val mat = ActorFlowMaterializer()

  private val sentences: List[String] = List(
    "this is a sentence"
    , "this is another sentence"
    , "random piece of sentence"
    , "how is the weather"
  )

  // extract all the words
  var words = Source(sentences.flatMap(_.split(" ")))


  // this is creating a stream for all different words and for each a substream with all the repetitions
  val wordStreams: Source[(String, Source[String, Unit]), Unit] = words.groupBy(identity)

  // just try to check what the head is made by two repetitions of word "this"
  val testing = false
  if (testing) {
    wordStreams.runWith(Sink.head).onComplete {
      case Success((word, stream)) => println(s"Stream for $word"); stream.runForeach(println)
    }
    Thread.sleep(100)
  }

  // add counting logic to the streams
  val countedWords: Source[Future[(String, Int)], Unit] = wordStreams.map {
    case (word, wordStream) =>
      wordStream.runFold((word, 0)) {
        case ((w, count), _) => (w, count + 1)
      }
  }

  val counts: Source[(String, Int), Unit] =
    countedWords
      .buffer(100 /*Max words to be counted */, OverflowStrategy.fail)
      .mapAsync(4)(identity)


  counts.runForeach(println)




  Thread.sleep(1000)
  sys.shutdown()
}
