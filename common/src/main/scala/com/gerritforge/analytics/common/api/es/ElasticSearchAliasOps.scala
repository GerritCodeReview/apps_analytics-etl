// Copyright (C) 2019 GerritForge Ltd
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

package com.gerritforge.analytics.common.api.es
import com.sksamuel.elastic4s.Index
import com.sksamuel.elastic4s.alias.{AddAliasActionRequest, RemoveAliasAction}
import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.http.index.admin.AliasActionResponse
import com.sksamuel.elastic4s.http.{ElasticClient, Response}
import com.typesafe.scalalogging.Logger

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait ElasticSearchAliasOps {

  val esClient: ElasticClient

  private val logger = Logger(classOf[ElasticSearchAliasOps])

  def getIndicesFromAlias(aliasName: String): Future[Iterable[Index]] = {
    logger.info(s"Getting indices from $aliasName")

    esClient
      .execute(
        getAliases(aliasName, Seq.empty[String])
      )
      .map(_.result.mappings.keys)

  }

  def moveAliasToNewIndex(aliasName: String,
                          newIndexName: String): Future[Response[AliasActionResponse]] = {
    val oldIndices: Future[Iterable[Index]] = getIndicesFromAlias(aliasName)

    oldIndices.flatMap { indices =>
      val removeAliasActions: Iterable[RemoveAliasAction] = indices.map { idxName =>
        removeAlias(aliasName) on s"${idxName.name}"
      }
      val addAliasAction: AddAliasActionRequest = addAlias(aliasName) on newIndexName

      logger.info(
        s"Replacing old indices (${indices.mkString(",")}) with $newIndexName from alias $aliasName")

      esClient.execute {
        aliases(
          removeAliasActions ++ List(addAliasAction)
        )
      }
    }
  }
}
