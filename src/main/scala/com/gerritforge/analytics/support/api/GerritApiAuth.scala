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

  def getGerritApi(gerritBaseUrl: String, maybeGerritApiUsername: Option[String], maybeGerritApiPassword: Option[String]): GerritRestApi = {

    lazy val gerritRestApiFactory: GerritRestApiFactory = new GerritRestApiFactory()

    logger.info(s"Connecting to gerrit API url = ${gerritBaseUrl}")

    val authData: GerritAuthData.Basic = if (maybeGerritApiUsername.isDefined && maybeGerritApiPassword.isDefined) {
      new GerritAuthData.Basic(gerritBaseUrl, maybeGerritApiUsername.get, maybeGerritApiPassword.get)
    } else new GerritAuthData.Basic(gerritBaseUrl)

    gerritRestApiFactory.create(authData)
  }
}
