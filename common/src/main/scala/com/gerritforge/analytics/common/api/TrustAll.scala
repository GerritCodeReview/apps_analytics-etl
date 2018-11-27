package com.gerritforge.analytics.common.api

import java.security.cert.X509Certificate

import javax.net.ssl._

object TrustAll extends X509TrustManager {
  override val getAcceptedIssuers: Array[X509Certificate] = Array.empty[X509Certificate]
  override def checkClientTrusted(x509Certificates: Array[X509Certificate], s: String): Unit = ()
  override def checkServerTrusted(x509Certificates: Array[X509Certificate], s: String): Unit = ()
}

object VerifiesAllHostNames extends HostnameVerifier {
  override def verify(s: String, sslSession: SSLSession) = true
}