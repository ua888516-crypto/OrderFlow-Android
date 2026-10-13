package com.example.orderflow

import android.app.Activity
import android.os.Bundle
import android.text.InputType
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.*

class MainActivity : Activity() {
    private val instruments = listOf("EUR_USD", "GBP_USD", "USD_JPY", "AUD_USD", "USD_CAD",
        "USD_CHF", "NZD_USD", "EUR_GBP", "XAU_USD")
    private val grans = listOf("M5", "M15", "H1", "H4", "D")

    private lateinit var token: EditText
    private lateinit var live: CheckBox
    private lateinit var inst: Spinner
    private lateinit var gran: Spinner
    private lateinit var status: TextView
    private lateinit var chart: ChartView
    private lateinit var book: BookView

    private fun dp(x: Int) = (x * resources.displayMetrics.density).toInt()

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        val prefs = getSharedPreferences("cfg", MODE_PRIVATE)
        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(40), dp(14), dp(20))
        }
        col.addView(TextView(this).apply { text = "Order Flow (OANDA)"; textSize = 22f })

        token = EditText(this).apply {
            hint = "OANDA API token"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            setText(prefs.getString("token", ""))
        }
        live = CheckBox(this).apply {
            text = "Live account (default: practice)"
            isChecked = prefs.getBoolean("live", false)
        }
        inst = Spinner(this).apply { adapter = ArrayAdapter(context, android.R.layout.simple_spinner_dropdown_item, instruments) }
        gran = Spinner(this).apply {
            adapter = ArrayAdapter(context, android.R.layout.simple_spinner_dropdown_item, grans)
            setSelection(2)
        }
        val go = Button(this).apply { text = "Refresh"; setOnClickListener { refresh(prefs) } }
        status = TextView(this).apply { textSize = 14f; setPadding(0, dp(8), 0, dp(8)) }
        chart = ChartView(this)
        book = BookView(this)

        col.addView(token); col.addView(live); col.addView(inst); col.addView(gran); col.addView(go)
        col.addView(status)
        col.addView(TextView(this).apply { text = "Price + approx. delta (tick volume)" })
        col.addView(chart, LinearLayout.LayoutParams(MATCH_PARENT, dp(300)))
        col.addView(TextView(this).apply { text = "Position book: shorts (red) | longs (green)"; setPadding(0, dp(12), 0, 0) })
        col.addView(book, LinearLayout.LayoutParams(MATCH_PARENT, dp(420)))
        col.addView(TextView(this).apply {
            text = "Analysis only. Not financial advice. Delta is approximated from candle direction; " +
                "OANDA does not publish true traded volume."
            textSize = 12f
        }, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
        setContentView(ScrollView(this).apply { addView(col) })
    }

    private fun refresh(prefs: android.content.SharedPreferences) {
        val t = token.text.toString().trim()
        if (t.isEmpty()) { status.text = "Enter your OANDA API token first."; return }
        prefs.edit().putString("token", t).putBoolean("live", live.isChecked).apply()
        val base = if (live.isChecked) Oanda.LIVE else Oanda.PRACTICE
        val i = instruments[inst.selectedItemPosition]
        val g = grans[gran.selectedItemPosition]
        status.text = "Loading..."
        Thread {
            try {
                val c = Oanda.candles(base, t, i, g)
                val (bias, notes) = Analysis.run(c)
                val d20 = c.takeLast(20).sumOf { it.delta }
                var bk: List<Bucket> = emptyList()
                var px = c.last().c
                var err = ""
                try {
                    val r = Oanda.positionBook(base, t, i)
                    bk = r.first; px = r.second
                } catch (e: Exception) { err = "\nPosition book unavailable: ${e.message}" }
                val near = bk.filter { kotlin.math.abs(it.price - px) / px < 0.005 }
                val longs = near.sumOf { it.longPct }
                val shorts = near.sumOf { it.shortPct }
                val msg = "$i  ${"%.5f".format(c.last().c)}  |  $bias\n" +
                    notes.joinToString("\n") { "- $it" } +
                    "\nDelta (last 20 candles): ${"%.0f".format(d20)} " + (if (d20 >= 0) "(buying pressure)" else "(selling pressure)") +
                    (if (near.isNotEmpty()) "\nNearby positions (±0.5%): longs ${"%.2f".format(longs)}% vs shorts ${"%.2f".format(shorts)}%" else "") + err
                runOnUiThread {
                    status.text = msg
                    chart.candles = c
                    if (bk.isNotEmpty()) book.update(bk, px)
                }
            } catch (e: Exception) {
                runOnUiThread { status.text = "Error: ${e.message}" }
            }
        }.start()
    }
}
