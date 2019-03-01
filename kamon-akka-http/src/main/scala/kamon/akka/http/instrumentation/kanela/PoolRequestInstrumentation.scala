/*
 * =========================================================================================
 * Copyright © 2013-2018 the kamon project <http://kamon.io/>
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

package akka.http.impl.engine.client

import akka.http.impl.engine.client.PoolInterfaceActor.PoolRequest
import akka.http.scaladsl.model.HttpHeader
import akka.http.scaladsl.model.headers.RawHeader
import kamon.Kamon
import kamon.akka.http.AkkaHttp
import kamon.context.HttpPropagation.HeaderWriter
import kamon.instrumentation.Mixin.HasContext

import scala.collection.mutable.ListBuffer

object PoolRequestInstrumentation {
    def attachContextTo(poolRequest: PoolRequest): AnyRef = {
      val contextHeaders = AkkaHttp.encodeContext(poolRequest.asInstanceOf[HasContext].context)
      val requestWithContext = poolRequest.request.withHeaders(poolRequest.request.headers ++ contextHeaders)
      poolRequest.copy(request = requestWithContext)
    }
}
