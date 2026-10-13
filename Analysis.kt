package com.example.orderflow

data class Candle(val o: Double, val h: Double, val l: Double, val c: Double, val v: Double) {
    /** Approximate signed tick volume: whole candle volume goes to its direction. */
    val delta: Double get() = if (c >= o) v else -v
}

data class Bucket(val price: Double, val longPct: Double, val shortPct: Double)

object Analysis {
    private fun ema(x: List<Double>, n: Int): List<Double> {
        val k = 2.0 / (n + 1)
        var p = x[0]
        return x.map { v -> p = v * k + p * (1 - k); p }
    }

    private fun sma(x: List<Double>, n: Int): Double? =
        if (x.size < n) null else x.takeLast(n).average()

    private fun rsi(x: List<Double>, n: Int = 14): Double {
        var g = 0.0
        var l = 0.0
        val a = 1.0 / n
        for (i in 1 until x.size) {
            val d = x[i] - x[i - 1]
            g = (1 - a) * g + a * maxOf(d, 0.0)
            l = (1 - a) * l + a * maxOf(-d, 0.0)
        }
        return if (l == 0.0) 100.0 else 100 - 100 / (1 + g / l)
    }

    fun run(c: List<Candle>): Pair<String, List<String>> {
        val close = c.map { it.c }
        val last = close.last()
        var score = 0
        val notes = mutableListOf<String>()

        sma(close, 50)?.let {
            if (last > it) { score++; notes.add("Price above 50-period average") }
            else { score--; notes.add("Price below 50-period average") }
        }
        val s50 = sma(close, 50)
        val s200 = sma(close, 200)
        if (s50 != null && s200 != null) {
            if (s50 > s200) { score++; notes.add("50 above 200 (uptrend)") }
            else { score--; notes.add("50 below 200 (downtrend)") }
        }
        val macd = ema(close, 12).zip(ema(close, 26)) { a, b -> a - b }
        val sig = ema(macd, 9)
        if (macd.last() > sig.last()) { score++; notes.add("MACD above signal (bullish momentum)") }
        else { score--; notes.add("MACD below signal (bearish momentum)") }

        val r = rsi(close)
        when {
            r > 70 -> { score--; notes.add("RSI %.0f: overbought".format(r)) }
            r < 30 -> { score++; notes.add("RSI %.0f: oversold".format(r)) }
            else -> notes.add("RSI %.0f: neutral".format(r))
        }
        val bias = when {
            score >= 2 -> "BULLISH"
            score <= -2 -> "BEARISH"
            else -> "NEUTRAL"
        }
        return Pair(bias, notes)
    }
}
