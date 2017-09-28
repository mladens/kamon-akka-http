package kamon.akka.http

import akka.http.scaladsl.model.{HttpHeader, HttpRequest, StatusCode, StatusCodes}
import akka.http.scaladsl.server.{Directive, RouteResult}
import kamon.Kamon
import kamon.context.{Context, TextMap}
import kamon.trace.Span
import kamon.util.CallingThreadExecutionContext

import scala.util.{Failure, Success}

trait KamonDirectives {

  def trace: Directive[Unit] = traceDirective
  def context: Directive[Unit] = contextDirective

  private val traceDirective: Directive[Unit] = Directive[Unit] {
    innerRoute => requestContext => {
      val incomingContext = decodeContext(requestContext.request)
      val serverSpan = Kamon.buildSpan(requestContext.request.uri.path.toString)
        .asChildOf(incomingContext.get(Span.ContextKey))
        .withMetricTag("span.kind", "server")
        .withTag("http.method", requestContext.request.method.value)
        .withTag("http.url", requestContext.request.uri.toString())
        .start()

      Kamon.withContext(incomingContext.withKey(Span.ContextKey, serverSpan)) {
        val innerRouteResult = innerRoute()(requestContext)
        innerRouteResult.onComplete(res => {
          res match {
            case Success(routeResult) => routeResult match {
              case RouteResult.Complete(httpResponse) =>
                if (httpResponse.status.isFailure())
                  serverSpan.addMetricTag("error", true.toString)

              case RouteResult.Rejected(_) =>
                serverSpan.setOperationName("not-found")
                serverSpan.addMetricTag("error", true.toString)
            }

            case Failure(throwable) =>
              serverSpan
                .addMetricTag("error", true.toString)
                .addMetricTag("error.object", throwable.getMessage)
          }

          println("RESPONSE " + res)

          serverSpan.finish()
        })(CallingThreadExecutionContext)
        innerRouteResult
      }
    }
  }

  private val contextDirective: Directive[Unit] = Directive[Unit] {
    innerRoute => requestContext => {
      val incomingContext = decodeContext(requestContext.request)

      Kamon.withContext(incomingContext) {
        innerRoute()(requestContext)
      }
    }
  }

  private def decodeContext(request: HttpRequest): Context = {
    val headersTextMap = readOnlyTextMapFromHeaders(request.headers)
    Kamon.contextCodec().HttpHeaders.decode(headersTextMap)
  }

  private def readOnlyTextMapFromHeaders(headers: Seq[HttpHeader]): TextMap = new TextMap {
    private val headersMap = headers.map { h => h.name -> h.value }.toMap

    override def put(key: String, value: String): Unit = {}
    override def get(key: String): Option[String] = headersMap.get(key)
    override def values: Iterator[(String, String)] = headersMap.iterator
  }
}
