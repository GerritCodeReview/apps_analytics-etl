package com.gerritforge.analytics.gitcommits.plugin

import com.google.gerrit.server.config.GerritServerConfig
import com.google.inject.Inject
import com.typesafe.scalalogging.LazyLogging
import org.eclipse.jgit.lib.Config

class GerritConfigSupport @Inject()(@GerritServerConfig val cfg: Config) extends LazyLogging {

  def getListenUrl(): Option[String] = {
    val listenUrl = cfg.getString("httpd", null, "listenUrl")
    val portRegex = "(proxy-)?https?://[^:]+:([0-9]+)/(.*)".r
    listenUrl match {
      case portRegex(_, port, path) =>
        val url = s"http://127.0.0.1:$port/$path"
        if(url.endsWith("/")) {
          Some(url.dropRight(1))
        } else {
          Some(url)
        }
      case _ => {
        logger.warn(s"Unable to extract local Gerrit URL from $listenUrl")
        None
      }
    }
  }
}
