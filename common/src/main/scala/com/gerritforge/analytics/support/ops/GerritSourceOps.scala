package com.gerritforge.analytics.support.ops
import scala.io.Source

object GerritSourceOps {

  implicit class PimpedSource(source: Source) {
    def dropGerritPrefix: Iterator[Char] = {
      val GERRIT_PREFIX = ")]}'\n"
      source.drop(GERRIT_PREFIX.length)
    }
  }

}
