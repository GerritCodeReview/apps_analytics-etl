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

package com.gerritforge.analytics.support.api

import com.typesafe.scalalogging.LazyLogging
import com.urswolfer.gerrit.client.rest.{GerritAuthData, GerritRestApi, GerritRestApiFactory}

object GerritApiAuth extends LazyLogging {

  def getGerritApi(gerritBaseUrl: String, maybeUsername: Option[String], maybePassword: Option[String]): GerritRestApi = {

    lazy val gerritRestApiFactory: GerritRestApiFactory = new GerritRestApiFactory()

    logger.info(s"Connecting to gerrit API url = ${gerritBaseUrl}")

    val authData: GerritAuthData.Basic = (
      for {
        username <- maybeUsername
        password <- maybePassword
      } yield new GerritAuthData.Basic(gerritBaseUrl, username, password)
      ).getOrElse(new GerritAuthData.Basic(gerritBaseUrl))

    gerritRestApiFactory.create(authData)
  }
}