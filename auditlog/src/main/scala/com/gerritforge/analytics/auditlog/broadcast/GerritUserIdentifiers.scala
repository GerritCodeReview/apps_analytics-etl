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

package com.gerritforge.analytics.auditlog.broadcast

import com.gerritforge.analytics.common.api.GerritConnectivity
import com.gerritforge.analytics.support.ops.GerritSourceOps._
import com.typesafe.scalalogging.LazyLogging
import org.json4s.native.JsonMethods._
import org.json4s.{DefaultFormats, _}

import scala.annotation.tailrec
import scala.util.{Failure, Success, Try}
import org.json4s.FieldSerializer._

case class GerritUserIdentifiers(private val accounts: Map[GerritAccountId, GerritUserIdentifier]) {
  def getIdentifier(accountId: GerritAccountId): GerritUserIdentifier =
    accounts.getOrElse(accountId, s"$accountId")
}

object GerritUserIdentifiers extends LazyLogging {

  private case class GerritAccount(
      accountId: GerritAccountId,
      username: Option[String],
      email: Option[String],
      name: Option[String]
  ) {
    val getIdentifier: GerritUserIdentifier =
      name.getOrElse(
        email.getOrElse(
          username.getOrElse(s"$accountId")
        )
      )
  }

  val empty = GerritUserIdentifiers(Map.empty[GerritAccountId, GerritUserIdentifier])

  private val gerritAccountSerializer = FieldSerializer[GerritAccount](
    deserializer = renameFrom(name = "_account_id", newName = "accountId")
  )

  implicit val formats: Formats = DefaultFormats + gerritAccountSerializer

  def loadAccounts(
      gerritConnectivity: GerritConnectivity,
      gerritUrl: String
  ): Try[GerritUserIdentifiers] = {

    logger.debug(s"Loading gerrit accounts...")

    val baseUrl = s"""$gerritUrl/accounts/?q=name:""&o=details"""

    @tailrec
    def loopThroughPages(
        more: Boolean,
        triedAcc: Try[GerritUserIdentifiers] = Success(empty)
    ): Try[GerritUserIdentifiers] = {
      if (!more)
        triedAcc
      else {
        val acc = triedAcc.get

        val url              = baseUrl + s"&start=${acc.accounts.size}"
        val accountsJsonPage = gerritConnectivity.getContentFromApi(url).dropGerritPrefix.mkString

        logger.debug(s"Getting gerrit accounts - start: ${acc.accounts.size}")

        val pageInfo = Try(parse(accountsJsonPage)).map { jsListAccounts =>
          val more = (jsListAccounts \ "_more_accounts").extractOrElse(default = false)

          val thisPageAccounts = jsListAccounts
            .extract[List[GerritAccount]]
            .map(ga => ga.accountId -> ga.getIdentifier)
            .toMap

          (more, acc.copy(accounts = acc.accounts ++ thisPageAccounts))
        }

        pageInfo match {
          case Success((newMore, newGerritAccounts)) =>
            loopThroughPages(newMore, Success(newGerritAccounts))
          case Failure(exception) => loopThroughPages(more = false, Failure(exception))
        }
      }
    }
    loopThroughPages(more = true)
  }
}
