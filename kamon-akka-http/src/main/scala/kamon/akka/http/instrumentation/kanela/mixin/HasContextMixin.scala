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

package kamon.akka.http.instrumentation.kanela.mixin

import kamon.Kamon
import kamon.context.Context
import kamon.instrumentation.Mixin.HasContext
import kanela.agent.api.instrumentation.mixin.Initializer

class HasContextMixin extends HasContext {
  var context: Context = _

  @Initializer
  def _initializer(): Unit = this.context =
    Kamon.currentContext()
}