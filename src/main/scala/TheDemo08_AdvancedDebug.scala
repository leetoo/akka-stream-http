import akka.actor.ActorSystem
import akka.event.Logging
import akka.stream.{OperationAttributes, ActorFlowMaterializer}
import akka.stream.scaladsl._


import akka.stream.stage._


object TheDemo08_AdvancedDebug extends App {
  implicit val sys = ActorSystem("TheDemo")
  implicit val mat = ActorFlowMaterializer()

  class LoggingStage[T] extends PushStage[T, T] {
    private val log = Logging(sys, "loggingName")

    override def onPush(elem: T, ctx: Context[T]): SyncDirective = {
      log.debug("Element flowing through: {}", elem)
      ctx.push(elem)
    }

    override def onUpstreamFailure(cause: Throwable,
                                   ctx: Context[T]): TerminationDirective = {
      log.error(cause, "Upstream failed.")
      super.onUpstreamFailure(cause, ctx)
    }

    override def onUpstreamFinish(ctx: Context[T]): TerminationDirective = {
      log.debug("Upstream finished")
      super.onUpstreamFinish(ctx)
    }
  }


  val source = Source(List(1, "alpha", 3))

  /**
   * a difficult but easily customizable way for logging as of 1.0-M1
   * see: [[http://doc.akka.io/docs/akka-stream-and-http-experimental/1.0-M2/scala/stream-cookbook.html]]
   ***/

  val advDebug = source.transform(() => new LoggingStage)
  advDebug.runWith(Sink.ignore)

  Thread.sleep(100)


  /**
   * another easier alternative way for logging as of 1.0-RC3
   * see: [[http://doc.akka.io/docs/akka-stream-and-http-experimental/1.0-RC3/scala/stream-cookbook.html]]
   ***/

  val analyse: Any => Unit = println

  val advDebug2 = source.log("before-map")
    .withAttributes(OperationAttributes.logLevels(onElement = Logging.DebugLevel))
    .map(analyse)

  advDebug2.runWith(Sink.ignore)


  Thread.sleep(100)
  sys.shutdown()
}
