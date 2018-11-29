package com.gerritforge.analytics.auditlog.broadcast
import com.gerritforge.analytics.common.api.GerritConnectivity
import com.gerritforge.analytics.support.ops.GerritSourceOps._
import com.typesafe.scalalogging.LazyLogging
import org.json4s.native.JsonMethods._
import org.json4s.{DefaultFormats, _}

import scala.annotation.tailrec
import scala.util.{Failure, Success, Try}

case class GerritAccount(_account_id: Int, username: Option[String], email: Option[String], name: Option[String]) {
  val getIdentifier: String =
    name.getOrElse(
      email.getOrElse(
        username.getOrElse(s"${_account_id}")
      )
    )
}

case class GerritAccounts(private val accounts: Map[Int, String]) {
  def getIdentifier(accountId: Int): String = accounts.getOrElse(accountId, s"$accountId")
}

object GerritAccounts extends LazyLogging {

  private val empty = GerritAccounts(Map.empty[Int, String])

  implicit val formats: DefaultFormats.type = DefaultFormats

  def loadAccounts(gerritConnectivity: GerritConnectivity, gerritUrl: String): Try[GerritAccounts] = {

    logger.debug(s"Loading gerrit accounts...")

    val baseUrl = s"""$gerritUrl/accounts/?q=name:""&o=details"""

    @tailrec
    def loopThroughPages(more: Boolean, triedAcc: Try[GerritAccounts] = Success(empty)): Try[GerritAccounts] = {
        if (!more)
          triedAcc
        else {
          val acc = triedAcc.get

          val url              = baseUrl + s"&start=${ acc.accounts.size}"
          val accountsJsonPage = gerritConnectivity.getContentFromApi(url).dropGerritPrefix.mkString

          logger.debug(s"Getting gerrit accounts - start: ${acc.accounts.size}")

          val pageInfo = Try(parse(accountsJsonPage)).map { jsListAccounts =>
            val more = (jsListAccounts \ "_more_accounts").extractOrElse(default = false)

            val thisPageAccounts = jsListAccounts
              .extract[List[GerritAccount]]
              .map( ga => ga._account_id -> ga.getIdentifier)
              .toMap

            (more, acc.copy(accounts = acc.accounts ++ thisPageAccounts))
          }

          pageInfo match {
            case Success((newMore, newGerritAccounts)) => loopThroughPages(newMore, Success(newGerritAccounts))
            case Failure(exception) => loopThroughPages(more=false, Failure(exception))
          }
        }
    }
    loopThroughPages(more=true)
  }
}
