package com.gerritforge.analytics.support.api

import java.io.InputStream

import com.google.gerrit.extensions.common.ProjectInfo
import com.urswolfer.gerrit.client.rest.{GerritRestApi, RestClient}

import scala.collection.JavaConverters._

class GerritServiceApi(api: GerritRestApi) {

  def getContentFrom(url: String): InputStream = api.restClient().requestRest(url, null, RestClient.HttpVerb.GET).getEntity.getContent

  def listProjectsWithPrefix(prefix: Option[String]): Iterator[ProjectInfo] = api.projects()
    .list()
    .withPrefix(prefix.getOrElse(""))
    .get()
    .asScala
    .iterator

}
