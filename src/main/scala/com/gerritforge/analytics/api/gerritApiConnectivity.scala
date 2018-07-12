// Copyright (C) 2018 GerritForge Ltd
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

package com.gerritforge.analytics.api

import java.net.URL

import com.typesafe.scalalogging.LazyLogging
import org.apache.commons.codec.binary.Base64

import scala.io.{BufferedSource, Codec, Source}


sealed trait HttpBasicAuthentication {

  val BASIC = "Basic"
  val AUTHORIZATION = "Authorization"

  def encodeCredentials(username: String, password: String): String = {
    new String(Base64.encodeBase64String((username + ":" + password).getBytes))
  }

  def getHeader(username: String, password: String): String =
    BASIC + " " + encodeCredentials(username, password)
}

class GerritConnectivity(maybeUsername: Option[String], maybePassword: Option[String]) extends HttpBasicAuthentication with Serializable with LazyLogging {
  private def createBasicSecuredConnection(url: String, username: String, password: String): BufferedSource = {
    try {
      val unsecureURL = new URL(url)
      val endPointPath = unsecureURL.getFile
      val basicAuthURL = unsecureURL.toString.replace(endPointPath, s"/a$endPointPath")

      logger.info(s"Connecting to API $basicAuthURL with basic auth")

      val connection = new URL(basicAuthURL).openConnection
      connection.setRequestProperty(AUTHORIZATION, getHeader(username, password))
      Source.fromInputStream(connection.getInputStream, Codec.UTF8.name)
    }
    catch {
      case e: Exception => throw new Exception(s"Unable to connect to $url. $e")
    }
  }

  private def createNonSecuredConnection(url: String): BufferedSource = {
    logger.info(s"Connecting to API $url")
    Source.fromURL(url, Codec.UTF8.name)
  }

  def getContentFromApi(url: String): BufferedSource = {
    (
      for {
        username <- maybeUsername
        password <- maybePassword
      } yield (createBasicSecuredConnection(url, username, password))
      ).getOrElse(createNonSecuredConnection(url))
  }
}
