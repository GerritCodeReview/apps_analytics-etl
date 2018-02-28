package com.gerritforge.analytics.support.api

import java.io.InputStream

import com.google.gerrit.extensions.common.ProjectInfo
import com.urswolfer.gerrit.client.rest.RestClient

import scala.collection.JavaConverters._


class GerritServiceApi(gerritBaseUrl: String, userName: Option[String], password: Option[String]) extends Serializable {

  @transient
  lazy val api = GerritApiAuth.getGerritApi(gerritBaseUrl, userName, password)

  def getContentFrom(url: String): InputStream = api.restClient().requestRest(url, null, RestClient.HttpVerb.GET).getEntity.getContent

  def listProjectsWithPrefix(prefix: Option[String]): Iterator[ProjectInfo] = api.projects()
    .list()
    .withPrefix(prefix.getOrElse(""))
    .get()
    .asScala
    .iterator

}