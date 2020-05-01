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
import java.time.{Instant, LocalDateTime, ZoneOffset}

import org.scalatest.{FlatSpec, Matchers}

class IndexNameGeneratorSpec extends FlatSpec with Matchers {

  "Index name generator" should "return an index name based on current time" in {
    val instantUTC: Instant =
      LocalDateTime
        .of(2019, 1, 1, 12, 0, 0, 0)
        .atOffset(ZoneOffset.UTC)
        .toInstant

    val indexName = "index_name"

    val timeBasedIndexName: String = IndexNameGenerator.timeBasedIndexName(indexName, instantUTC)

    val expectedIndexName = s"${indexName}_2019-01-01_${instantUTC.toEpochMilli}"
    timeBasedIndexName shouldEqual (expectedIndexName)
  }
}
