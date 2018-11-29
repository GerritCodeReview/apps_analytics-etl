package com.gerritforge.analytics.auditlog.broadcast
import com.gerritforge.analytics.common.api.GerritConnectivity
import com.gerritforge.analytics.support.ops.GerritSourceOps._
import com.typesafe.scalalogging.LazyLogging
import org.json4s.native.JsonMethods._
import org.json4s.{DefaultFormats, _}

import scala.util.{Success, Try}

case class GerritAccount(_account_id: Int, username: Option[String], email: Option[String], name: Option[String])

case class GerritAccounts(private val accounts: Map[Int, String]) {
  def getIdentifier(accountId: Int): String = accounts.getOrElse(accountId, s"$accountId")
}

case object GerritAccounts extends LazyLogging {

  private val EMPTY = GerritAccounts(Map.empty[Int, String])

  implicit val formats: DefaultFormats.type = DefaultFormats

  def loadAccounts(gerritConnectivity: GerritConnectivity, gerritUrl: String): Try[GerritAccounts] = {

    logger.debug(s"Loading gerrit accounts...")

    val baseUrl = s"""$gerritUrl/accounts/?q=name:""&o=details"""

    def loopThroughPages(acc: GerritAccounts = EMPTY): Try[GerritAccounts] = {
        val url = baseUrl + s"&start=${acc.accounts.size}"
        val accountsJsonPage = gerritConnectivity.getContentFromApi(url).dropGerritPrefix.mkString

        logger.debug(s"Getting gerrit accounts - start: ${acc.accounts.size}")

        Try(parse(accountsJsonPage)).flatMap { jsListAccounts =>
          val more = (jsListAccounts \ "_more_accounts").extractOrElse(default = false)

          val thisPageAccounts = jsListAccounts.extract[List[GerritAccount]].map { a =>
            a._account_id -> a.name.getOrElse(a.email.getOrElse(a.username.getOrElse(s"${a._account_id}")))
          }.toMap

          val newAcc = acc.copy(accounts=acc.accounts ++ thisPageAccounts)

          if (more)
            loopThroughPages(newAcc)
          else
            Success(newAcc)
        }
    }

    loopThroughPages()
  }
}
