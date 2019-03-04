/*
 * =========================================================================================
 * Copyright © 2013-2016 the kamon project <http://kamon.io/>
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 * =========================================================================================
 */

package kamon.akka.http

import akka.actor.ReflectiveDynamicAccess
import akka.http.scaladsl.model.{HttpHeader, HttpRequest, HttpResponse}
import akka.http.scaladsl.model.headers.{Host, RawHeader}
import com.typesafe.config.Config
import kamon.Configuration.OnReconfigureHook
import kamon.Kamon
import kamon.context.Context
import kamon.context.HttpPropagation.{HeaderReader, HeaderWriter}
import kamon.instrumentation.Mixin.HasContext

import scala.collection.immutable.TreeMap
import scala.collection.mutable.ListBuffer


object AkkaHttp {

  @volatile private var nameGenerator: OperationNameGenerator = nameGeneratorFromConfig(Kamon.config())

  def serverOperationName(request: HttpRequest): String =
    nameGenerator.serverOperationName(request)

  def clientOperationName(request: HttpRequest): String =
    nameGenerator.clientOperationName(request)

  def serverNotFoundOperationName(response: HttpResponse): Option[String] =
    nameGenerator.serverNotFoundOperationName(response)

  trait OperationNameGenerator {
    def serverOperationName(request: HttpRequest): String
    def clientOperationName(request: HttpRequest): String

    def serverNotFoundOperationName(request: HttpResponse): Option[String] = Some(defaultNotFoundOperationName)
  }


  @volatile private var defaultNotFoundOperationName = defaultNotFoundOperationNameFromConfig(Kamon.config)

  private def defaultOperationNameGenerator(): OperationNameGenerator = new OperationNameGenerator {

    def clientOperationName(request: HttpRequest): String = {
      val uriAddress = request.uri.authority.host.address
      if (uriAddress.isEmpty) hostFromHeaders(request).getOrElse("unknown-host") else uriAddress
    }

    def serverOperationName(request: HttpRequest): String =
      request.uri.path.toString()

    private def hostFromHeaders(request: HttpRequest): Option[String] =
      request.header[Host].map(_.host.toString())
  }

  private def defaultNotFoundOperationNameFromConfig(config: Config): String = {
    config.getString("kamon.akka-http.not-found-operation-name")
  }

  private def nameGeneratorFromConfig(config: Config): OperationNameGenerator = {
    val nameGeneratorFQN = config.getString("kamon.akka-http.name-generator")
    if(nameGeneratorFQN == "default") defaultOperationNameGenerator() else {
      new ReflectiveDynamicAccess(getClass.getClassLoader)
        .createInstanceFor[OperationNameGenerator](nameGeneratorFQN, Nil).get // let's bubble up any problems.
    }
  }

  @volatile var addHttpStatusCodeAsMetricTag: Boolean = addHttpStatusCodeAsMetricTagFromConfig(Kamon.config())

  private def addHttpStatusCodeAsMetricTagFromConfig(config: Config): Boolean =
    config.getBoolean("kamon.akka-http.add-http-status-code-as-metric-tag")

  Kamon.onReconfigure(new OnReconfigureHook {
    override def onReconfigure(newConfig: Config): Unit = {
      nameGenerator = nameGeneratorFromConfig(newConfig)
      addHttpStatusCodeAsMetricTag = addHttpStatusCodeAsMetricTagFromConfig(newConfig)
      defaultNotFoundOperationName = defaultNotFoundOperationNameFromConfig(newConfig)
    }
  })


  def encodeContext(context: Context): List[HttpHeader] = {
    val contextHeaders = ListBuffer.empty[HttpHeader]
    val headerWriter = new HeaderWriter {
      override def write(header: String, value: String): Unit = contextHeaders.append(RawHeader(header, value))
    }

    Kamon.defaultHttpPropagation().write(context, headerWriter)
    contextHeaders.toList
  }

  def decodeContext(headers: Seq[HttpHeader]): Context = {
    val headerReader: HeaderReader = new HeaderReader {
      private val headersKeyValueMap =
        new TreeMap[String, String]()(Ordering.comparatorToOrdering(String.CASE_INSENSITIVE_ORDER)) ++
          headers.map(h => h.name -> h.value())

      override def read(header: String): Option[String] = headersKeyValueMap.get(header)
      override def readAll(): Map[String, String] = headersKeyValueMap
    }
    Kamon.defaultHttpPropagation().read(headerReader)
  }
}
