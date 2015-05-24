import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.StatusCodes
import akka.pattern._
import akka.stream.scaladsl.{Sink, Flow}
import akka.util.ByteString

import scala.concurrent.Future

//import akka.http._
import akka.http.scaladsl.server.Directives._
//import akka.http.scaladsl.server._
import akka.http.scaladsl.server.directives.FileAndResourceDirectives.DirectoryRenderer
import akka.stream.ActorFlowMaterializer
import scala.concurrent.duration._
import akka.pattern.after
import scala.concurrent.ExecutionContext.Implicits.global

object TheDemo70_Http extends App {
  implicit val sys=ActorSystem("TheDemo")
  implicit val mat=ActorFlowMaterializer()
  //implicit val timeout = Timeout(3.seconds)
  def delay(x:ByteString)  =
    after(1.second, sys.scheduler)(Future.successful(x))


  val f = Flow[ByteString].
    mapAsync(4)(delay)

  val route = pathPrefix("demo"){
    getFromBrowseableDirectories("d:\\apps")
  } ~
  path("upload"){
    extractRequest{
      req =>
        req.entity.dataBytes.via(f).to(Sink.ignore).run()
        complete(StatusCodes.OK)
    }
  }

  Http().bind("localhost",8080)
    .runForeach(conn => conn.flow.join(route).run()) // remember the inner run() !!!
}
