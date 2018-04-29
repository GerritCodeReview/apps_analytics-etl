// Copyright (C) 2017 GerritForge Ltd
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


package com.gerritforge.analytics.engine.events

import com.gerritforge.analytics.engine.ChangesReportGenerator
import com.gerritforge.analytics.model.{ChangesWithModificationStats, New}
import com.gerritforge.analytics.support.SparkTestSupport
import org.apache.spark.sql.Dataset
import org.scalatest.{Matchers, WordSpec}

class ChangesReportGeneratorSpec extends WordSpec with Matchers with SparkTestSupport {

  "A Risky Change Generator" when {

    "given 3 Open or Draft Changes with some stats" should {
      val riskyChangeOne = ChangesWithModificationStats(
        "changeId-1",
        1000,
        New.stringValue,
        200,
        600,
        300,
        800,
        1
      )

      val riskyChangeTwo = ChangesWithModificationStats(
        "changeId-2",
        1003,
        New.stringValue,
        20,
        40,
        3,
        60,
        10
      )

      val riskyChangeThree = ChangesWithModificationStats(
        "changeId-3",
        1002,
        New.stringValue,
        5,
        7,
        3,
        12,
        10
      )


      "rank the risk based on number of comments and change size size" in {
        import spark.implicits._
        val listOfRiskyChanges: Dataset[ChangesWithModificationStats] = List(riskyChangeOne, riskyChangeTwo, riskyChangeThree).toDS()

        val expectedResult = List((riskyChangeOne.change_id, 1), (riskyChangeTwo.change_id, 2), (riskyChangeThree.change_id, 3))

        val result: List[(String, Int)] = ChangesReportGenerator
          .rankChangeModificationStats(listOfRiskyChanges)
          .map(riskyChange => (riskyChange.change_id, riskyChange.risk_rank)).collect.toList

        result should contain only (expectedResult: _*)
      }
    }
  }

}
