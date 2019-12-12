package com.gerritforge.analytics.common.api

import com.gerritforge.analytics.infrastructure.{
  ElasticSearchPimpedWriter,
  EnrichedAliasActionResponse
}
import org.apache.spark.sql.DataFrame

trait Storage[S] {
  def storeDf(a: S, df: DataFrame): EnrichedAliasActionResponse
}

object Storage {

  def storeDf[S](s: S, df: DataFrame)(implicit storage: Storage[S]): EnrichedAliasActionResponse =
    storage.storeDf(s, df: DataFrame)

  implicit val esCanStore: Storage[ElasticSearchST] = new Storage[ElasticSearchST] {

    def storeDf(s: ElasticSearchST, df: DataFrame): EnrichedAliasActionResponse = {
      val esWriter = new ElasticSearchPimpedWriter(df)
      esWriter.saveToEsWithAliasSwap(s.aliasName, s.documentType)
    }
  }
}
