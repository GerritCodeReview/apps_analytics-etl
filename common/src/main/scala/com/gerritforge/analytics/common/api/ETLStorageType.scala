package com.gerritforge.analytics.common.api

sealed trait ETLStorageType
case class ElasticSearchST(aliasName: String, documentType: String) extends ETLStorageType
case class PSQLST(aliasName: String, documentType: String)          extends ETLStorageType
