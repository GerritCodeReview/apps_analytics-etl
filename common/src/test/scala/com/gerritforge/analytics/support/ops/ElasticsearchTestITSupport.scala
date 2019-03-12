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
import com.dimafeng.testcontainers.{ForAllTestContainer, GenericContainer}
import org.scalatest.{BeforeAndAfterAll, Suite}
import org.testcontainers.containers.wait.strategy.HostPortWaitStrategy

trait ElasticsearchTestITSupport extends ForAllTestContainer with BeforeAndAfterAll { this: Suite =>

  lazy val elasticsearchHTTPIntPort: Int = 9200
  lazy val elasticsearchTCPIntPort: Int  = 9300

  lazy val esHostHTTPExtPortMapping: Int = container.mappedPort(elasticsearchHTTPIntPort)
  lazy val esHostTCPExtPortMapping: Int  = container.mappedPort(elasticsearchTCPIntPort)

  override lazy val container = GenericContainer(
    "elasticsearch:5.6.15-alpine",
    waitStrategy = new HostPortWaitStrategy,
    env = Map("discovery.type" -> "single-node"),
    exposedPorts = Seq(elasticsearchHTTPIntPort, elasticsearchTCPIntPort)
  )
}
