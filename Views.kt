package com.example.orderflow

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.view.View
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

private const val GREEN = 0xFF2E9E5B.toInt()
private const val RED = 0xFFD64545.toInt()

/** Candles on top, approximate delta bars underneath. */
class ChartView(ctx: Context) : View(ctx) {
    var candles: List<Candle> = emptyList()
        set(v) { field = v; invalidate() }
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)

    override fun onDraw(cv: Canvas) {
        val d = candles.takeLast(60)
        if (d.isEmpty()) return
        val w = width.toFloat()
        val h = height.toFloat()
        val top = h * 0.68f
        val hi = d.maxOf { it.h }
        val lo = d.minOf { it.l }
        val rng = (hi - lo).coerceAtLeast(1e-9)
        fun y(v: Double) = ((hi - v) / rng * (top - 8f)).toFloat() + 4f
        val step = w / d.size
        val bw = step * 0.3f
        val maxV = d.maxOf { abs(it.delta) }.coerceAtLeast(1.0)
        val mid = top + (h - top) / 2
        d.forEachIndexed { i, c ->
            val x = i * step + step / 2
            p.color = if (c.c >= c.o) GREEN else RED
            p.strokeWidth = 2f
            cv.drawLine(x, y(c.h), x, y(c.l), p)
            cv.drawRect(x - bw, min(y(c.o), y(c.c)), x + bw, max(y(c.o), y(c.c)) + 1f, p)
            val bh = (c.delta / maxV).toFloat() * ((h - top) / 2 - 4f)
            cv.drawRect(x - bw, min(mid, mid - bh), x + bw, max(mid, mid - bh), p)
        }
        p.color = 0xFF888888.toInt()
        p.strokeWidth = 1f
        cv.drawLine(0f, mid, w, mid, p)
    }
}

/** Position book: shorts (red, left) vs longs (green, right) per price bucket. */
class BookView(ctx: Context) : View(ctx) {
    private var rows: List<Bucket> = emptyList()
    private var price = 0.0
    private val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 24f }

    fun update(buckets: List<Bucket>, px: Double) {
        val idx = buckets.indices.minByOrNull { abs(buckets[it].price - px) } ?: 0
        rows = buckets.subList(max(0, idx - 20), min(buckets.size, idx + 21)).sortedByDescending { it.price }
        price = px
        invalidate()
    }

    override fun onDraw(cv: Canvas) {
        if (rows.isEmpty()) return
        val rh = height.toFloat() / rows.size
        val cx = width / 2f
        val mx = rows.maxOf { max(it.longPct, it.shortPct) }.coerceAtLeast(1e-9)
        val nearest = rows.minByOrNull { abs(it.price - price) }
        rows.forEachIndexed { i, b ->
            val t = i * rh
            p.color = GREEN
            cv.drawRect(cx, t + 1, cx + (b.longPct / mx).toFloat() * (cx - 8), t + rh - 1, p)
            p.color = RED
            cv.drawRect(cx - (b.shortPct / mx).toFloat() * (cx - 8), t + 1, cx, t + rh - 1, p)
            if (b === nearest) {
                p.color = 0xFF1F5FBF.toInt()
                p.strokeWidth = 3f
                cv.drawLine(0f, t + rh / 2, width.toFloat(), t + rh / 2, p)
            }
            if (i % 5 == 0) {
                p.color = 0xFF222222.toInt()
                cv.drawText("%.5f".format(b.price), 6f, t + rh - 2, p)
            }
        }
    }
}
