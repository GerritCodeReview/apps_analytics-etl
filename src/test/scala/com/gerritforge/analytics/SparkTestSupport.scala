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

package com.gerritforge.analytics

import org.apache.commons.lang.RandomStringUtils
import org.apache.spark.{SparkConf, SparkContext}

trait SparkTestSupport {

  def withSparkContext(test: SparkContext => Unit): Unit = {
    val sc = new SparkContext("local[4]", RandomStringUtils.randomAlphabetic(10))
    try {
      test(sc)
    } finally {
      sc.stop()
      // To avoid Akka rebinding to the same port, since it doesn't unbind immediately on shutdown
      System.clearProperty("spark.master.port")
    }
  }
}