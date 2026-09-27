package com.example.calibretv.data.opds

import java.net.HttpURLConnection
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

/**
 * Permissive SSL Helper for home-hosted Calibre-Web instances, DuckDNS,
 * reverse proxies (Caddy, Nginx) and custom local certificates.
 * Prevents SSLHandshakeException and CertificateException on Android TV.
 */
object SslHelper {

    private val trustAllCerts = arrayOf<TrustManager>(
        object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<X509Certificate>?, authType: String?) {}
            override fun checkServerTrusted(chain: Array<X509Certificate>?, authType: String?) {}
            override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
        }
    )

    private val permissiveSslContext: SSLContext by lazy {
        SSLContext.getInstance("TLS").apply {
            init(null, trustAllCerts, SecureRandom())
        }
    }

    private val permissiveHostnameVerifier = HostnameVerifier { _, _ -> true }

    fun configureHttps(connection: HttpURLConnection) {
        if (connection is HttpsURLConnection) {
            try {
                connection.sslSocketFactory = permissiveSslContext.socketFactory
                connection.hostnameVerifier = permissiveHostnameVerifier
            } catch (_: Exception) {
                // Ignore fallback to system defaults
            }
        }
    }
}
