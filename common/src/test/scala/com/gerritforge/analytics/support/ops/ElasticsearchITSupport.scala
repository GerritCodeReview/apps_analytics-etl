// Copyright (C) 2019 GerritForge Ltd
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.gerritforge.analytics.support.ops
import com.dimafeng.testcontainers.{FixedHostPortGenericContainer, ForAllTestContainer}
import org.scalatest.{BeforeAndAfterAll, Suite}
import org.testcontainers.containers.wait.strategy.Wait

trait ElasticsearchITSupport extends ForAllTestContainer with BeforeAndAfterAll { this: Suite =>

  override lazy val container: FixedHostPortGenericContainer = FixedHostPortGenericContainer(
    "elasticsearch:5.6.15-alpine",
    waitStrategy = Wait.forHttp("/"),
    exposedHostPort = 9200,
    exposedContainerPort = 9200,
    env = Map("discovery.type" -> "single-node")
  )

}
