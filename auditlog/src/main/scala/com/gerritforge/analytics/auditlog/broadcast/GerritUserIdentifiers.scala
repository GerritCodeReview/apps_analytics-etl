package com.gerritforge.analytics.auditlog.broadcast
import com.gerritforge.analytics.common.api.GerritConnectivity
import com.gerritforge.analytics.support.ops.GerritSourceOps._
import com.typesafe.scalalogging.LazyLogging
import org.json4s.native.JsonMethods._
import org.json4s.{DefaultFormats, _}

import scala.annotation.tailrec
import scala.util.{Failure, Success, Try}
import org.json4s.FieldSerializer._


case class GerritUserIdentifiers(private val accounts: Map[GerriAccountId, GerritUserIdentifier]) {
  def getIdentifier(accountId: GerriAccountId): GerritUserIdentifier = accounts.getOrElse(accountId, s"$accountId")
}

object GerritUserIdentifiers extends LazyLogging {

  private case class GerritAccount(accountId: GerriAccountId, username: Option[String], email: Option[String], name: Option[String]) {
    val getIdentifier: GerritUserIdentifier =
      name.getOrElse(
        email.getOrElse(
          username.getOrElse(s"$accountId")
        )
      )
  }

  private val empty = GerritUserIdentifiers(Map.empty[GerriAccountId, GerritUserIdentifier])

  private val gerritAccountSerializer = FieldSerializer[GerritAccount](
    deserializer=renameFrom(name="_account_id",newName="accountId")
  )

  implicit val formats: Formats = DefaultFormats + gerritAccountSerializer

  def loadAccounts(gerritConnectivity: GerritConnectivity, gerritUrl: String): Try[GerritUserIdentifiers] = {

    logger.debug(s"Loading gerrit accounts...")

    val baseUrl = s"""$gerritUrl/accounts/?q=name:""&o=details"""

    @tailrec
    def loopThroughPages(more: Boolean, triedAcc: Try[GerritUserIdentifiers] = Success(empty)): Try[GerritUserIdentifiers] = {
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
              .map( ga => ga.accountId -> ga.getIdentifier)
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
