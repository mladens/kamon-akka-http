package kamon.akka

import akka.actor.ActorSystem
import akka.http.javadsl.server.RejectionHandler
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives
import akka.stream.ActorMaterializer
import kamon.akka.http.KamonDirectives

object ExampleServer extends App with Directives with KamonDirectives {
  implicit val system = ActorSystem("example-server")
  implicit val materializer = ActorMaterializer()

  val route = trace {
    handleRejections(RejectionHandler.defaultHandler.asScala) {
      get {
        path("test") {
          complete("ok")
        }
      }
    }
  }

  Http().bindAndHandle(route, interface = "0.0.0.0", port = 9949)


}
