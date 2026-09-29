package com.spiderclock.wallpaper

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Shader
import android.graphics.Typeface
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import java.util.Calendar
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

class SpiderWallpaperService : WallpaperService() {

    override fun onCreateEngine(): Engine = SpiderEngine()

    private inner class SpiderEngine : Engine() {

        private val handler = Handler(Looper.getMainLooper())
        private val calendar: Calendar = Calendar.getInstance()

        private var visible = false
        private var surfaceReady = false
        private var width = 0
        private var height = 0

        // Everything below is allocated once, never per frame.
        private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val webPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(55, 235, 205, 160)
            style = Paint.Style.STROKE
        }
        private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(220, 235, 220, 190)
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create("sans-serif", Typeface.NORMAL)
        }
        private val handPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
        }
        private val secondPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
        }
        private val spiderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.FILL
        }
        private val legPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
        }
        private val path = Path()

        private val frameRunnable = object : Runnable {
            override fun run() {
                if (!visible) return
                drawFrame()
                handler.postDelayed(this, FRAME_DELAY_MS)
            }
        }

        override fun onCreate(surfaceHolder: SurfaceHolder) {
            super.onCreate(surfaceHolder)
            setTouchEventsEnabled(false)
        }

        override fun onSurfaceCreated(holder: SurfaceHolder) {
            super.onSurfaceCreated(holder)
            surfaceReady = true
        }

        override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, w: Int, h: Int) {
            super.onSurfaceChanged(holder, format, w, h)
            width = w
            height = h
            buildShaders()
            drawFrame()
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            surfaceReady = false
            handler.removeCallbacks(frameRunnable)
            super.onSurfaceDestroyed(holder)
        }

        override fun onVisibilityChanged(isVisible: Boolean) {
            visible = isVisible
            handler.removeCallbacks(frameRunnable)
            if (isVisible) handler.post(frameRunnable)
        }

        override fun onDestroy() {
            visible = false
            handler.removeCallbacks(frameRunnable)
            super.onDestroy()
        }

        /** Shaders depend only on the surface size, so they are rebuilt only when it changes. */
        private fun buildShaders() {
            if (width <= 0 || height <= 0) return
            val w = width.toFloat()
            val h = height.toFloat()
            val m = min(w, h)
            bgPaint.shader = RadialGradient(
                w / 2f, h / 2f, max(m * 0.9f, 1f),
                intArrayOf(Color.rgb(145, 70, 5), Color.rgb(55, 24, 2), Color.rgb(8, 4, 2)),
                floatArrayOf(0f, 0.62f, 1f),
                Shader.TileMode.CLAMP
            )
            val spiderSize = m * SCALE * 0.24f
            // Glow is centred on (0,0) and drawn in the spider's translated coordinate space.
            glowPaint.shader = RadialGradient(
                0f, 0f, max(spiderSize * 1.7f, 1f),
                Color.argb(70, 255, 255, 255), Color.TRANSPARENT,
                Shader.TileMode.CLAMP
            )
        }

        private fun drawFrame() {
            if (!surfaceReady || width <= 0 || height <= 0) return
            val holder = surfaceHolder
            var canvas: Canvas? = null
            try {
                canvas = holder.lockCanvas()
                if (canvas != null) render(canvas)
            } catch (e: Exception) {
                // Surface can disappear between the check and the lock; skip this frame.
            } finally {
                if (canvas != null) {
                    try {
                        holder.unlockCanvasAndPost(canvas)
                    } catch (e: Exception) {
                        // Ignore: surface already released.
                    }
                }
            }
        }

        private fun render(c: Canvas) {
            val w = width.toFloat()
            val h = height.toFloat()
            val cx = w / 2f
            val cy = h / 2f
            val m = min(w, h)
            val radius = m * SCALE
            val t = SystemClock.uptimeMillis() / 1000f

            c.drawRect(0f, 0f, w, h, bgPaint)

            // Web: rings + spokes + hanging thread.
            webPaint.strokeWidth = max(1f, m * 0.0025f)
            for (i in 1..4) c.drawCircle(cx, cy, radius * i / 4f, webPaint)
            for (i in 0 until 12) {
                val a = Math.toRadians(i * 30.0)
                c.drawLine(
                    cx, cy,
                    (cx + radius * cos(a)).toFloat(), (cy + radius * sin(a)).toFloat(),
                    webPaint
                )
            }
            c.drawLine(cx, 0f, cx, cy, webPaint)

            // Numerals just outside the outer ring.
            textPaint.textSize = radius * 0.18f
            val textOffset = (textPaint.descent() + textPaint.ascent()) / 2f
            for (i in 1..12) {
                val a = Math.toRadians(i * 30.0 - 90.0)
                val x = (cx + radius * 1.12 * cos(a)).toFloat()
                val y = (cy + radius * 1.12 * sin(a)).toFloat() - textOffset
                c.drawText(i.toString(), x, y, textPaint)
            }

            // Real current time (device time zone), sub-second precision for a smooth sweep.
            calendar.timeInMillis = System.currentTimeMillis()
            val hour = calendar.get(Calendar.HOUR)
            val minute = calendar.get(Calendar.MINUTE)
            val second = calendar.get(Calendar.SECOND)
            val ms = calendar.get(Calendar.MILLISECOND)
            val hourAngle = (hour + minute / 60f) * 30f - 90f
            val minuteAngle = (minute + second / 60f) * 6f - 90f
            val secondAngle = (second + ms / 1000f) * 6f - 90f

            drawHand(c, cx, cy, hourAngle, radius * 0.55f, max(4f, radius * 0.025f), 0.20, handPaint)
            drawHand(c, cx, cy, minuteAngle, radius * 0.82f, max(2.5f, radius * 0.014f), -0.16, handPaint)
            drawHand(c, cx, cy, secondAngle, radius * 0.88f, max(1.5f, radius * 0.007f), 0.0, secondPaint)

            drawSpider(c, cx, cy, radius * 0.24f, t)
        }

        private fun drawHand(
            c: Canvas, cx: Float, cy: Float, deg: Float,
            len: Float, strokeWidth: Float, bend: Double, paint: Paint
        ) {
            paint.strokeWidth = strokeWidth
            val a = Math.toRadians(deg.toDouble())
            val mx = (cx + len * 0.5 * cos(a + bend)).toFloat()
            val my = (cy + len * 0.5 * sin(a + bend)).toFloat()
            val ex = (cx + len * cos(a)).toFloat()
            val ey = (cy + len * sin(a)).toFloat()
            path.reset()
            path.moveTo(cx, cy)
            path.quadTo(mx, my, ex, ey)
            c.drawPath(path, paint)
        }

        private fun drawSpider(c: Canvas, cx: Float, cy: Float, s: Float, t: Float) {
            c.save()
            // Gentle breathing motion.
            c.translate(cx, cy + sin(t * 1.6f) * s * 0.04f)

            glowPaint.alpha = (200 + 55 * sin(t * 1.2f)).toInt().coerceIn(0, 255)
            c.drawCircle(0f, 0f, s * 1.5f, glowPaint)

            c.drawOval(-s * 0.42f, -s * 0.15f, s * 0.42f, s * 0.72f, spiderPaint)
            c.drawCircle(0f, -s * 0.28f, s * 0.30f, spiderPaint)

            legPaint.strokeWidth = max(2f, s * 0.07f)
            for (side in intArrayOf(-1, 1)) {
                for (i in 0..3) {
                    val wiggle = 0.07 * sin(t * 2.2 + i * 0.9 + (if (side == 1) 0.0 else PI / 2))
                    val a = Math.toRadians((-55 + i * 35).toDouble()) + wiggle
                    val x1 = (side * s * 0.65 * cos(a)).toFloat()
                    val y1 = (s * 0.65 * sin(a)).toFloat()
                    val x2 = (side * s * 1.35 * cos(a + 0.18)).toFloat()
                    val y2 = (s * 1.15 * sin(a + 0.18)).toFloat()
                    path.reset()
                    path.moveTo(0f, 0f)
                    path.quadTo(x1, y1, x2, y2)
                    c.drawPath(path, legPaint)
                }
            }
            c.restore()
        }
    }

    private companion object {
        const val FRAME_DELAY_MS = 33L   // ~30 fps while visible, 0 work while hidden
        const val SCALE = 0.36f          // clock radius as a fraction of the shorter screen side
    }
}
