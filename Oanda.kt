package com.example.orderflow

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object Oanda {
    const val PRACTICE = "https://api-fxpractice.oanda.com"
    const val LIVE = "https://api-fxtrade.oanda.com"

    private fun get(base: String, path: String, token: String): JSONObject {
        val c = URL(base + path).openConnection() as HttpURLConnection
        c.setRequestProperty("Authorization", "Bearer $token")
        c.connectTimeout = 10000
        c.readTimeout = 15000
        val code = c.responseCode
        val body = (if (code in 200..299) c.inputStream else c.errorStream)
            ?.bufferedReader()?.use { it.readText() } ?: ""
        if (code !in 200..299) throw Exception("HTTP $code ${body.take(120)}")
        return JSONObject(body)
    }

    fun candles(base: String, token: String, inst: String, gran: String): List<Candle> {
        val arr = get(base, "/v3/instruments/$inst/candles?granularity=$gran&count=250&price=M", token)
            .getJSONArray("candles")
        return (0 until arr.length()).map {
            val o = arr.getJSONObject(it)
            val m = o.getJSONObject("mid")
            Candle(m.getString("o").toDouble(), m.getString("h").toDouble(),
                m.getString("l").toDouble(), m.getString("c").toDouble(),
                o.getDouble("volume"))
        }
    }

    /** Returns (buckets sorted by price, snapshot price). */
    fun positionBook(base: String, token: String, inst: String): Pair<List<Bucket>, Double> {
        val b = get(base, "/v3/instruments/$inst/positionBook", token).getJSONObject("positionBook")
        val arr = b.getJSONArray("buckets")
        val list = (0 until arr.length()).map {
            val o = arr.getJSONObject(it)
            Bucket(o.getString("price").toDouble(),
                o.getString("longCountPercent").toDouble(),
                o.getString("shortCountPercent").toDouble())
        }.sortedBy { it.price }
        return Pair(list, b.getString("price").toDouble())
    }
}
