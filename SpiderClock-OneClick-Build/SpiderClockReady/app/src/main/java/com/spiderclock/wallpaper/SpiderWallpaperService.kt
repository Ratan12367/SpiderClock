package com.spiderclock.wallpaper

import android.content.SharedPreferences
import android.graphics.*
import android.os.Handler
import android.os.Looper
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import androidx.preference.PreferenceManager
import java.util.Calendar
import kotlin.math.*

class SpiderWallpaperService : WallpaperService() {
    override fun onCreateEngine() = SpiderEngine()

    inner class SpiderEngine : Engine(), SharedPreferences.OnSharedPreferenceChangeListener {
        private val handler = Handler(Looper.getMainLooper())
        private var visible = false
        private var scale = 1f
        private var showSeconds = true
        private var glow = true
        private var speed = 1f
        private var rotation = 0f
        private val prefs by lazy { PreferenceManager.getDefaultSharedPreferences(applicationContext) }
        private val webPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(55, 235, 205, 160); style = Paint.Style.STROKE }
        private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(220, 235, 220, 190); textAlign = Paint.Align.CENTER; typeface = Typeface.create("sans-serif", Typeface.NORMAL) }
        private val handPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND }
        private val secondPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; style = Paint.Style.STROKE; strokeWidth = 2.5f; strokeCap = Paint.Cap.ROUND }
        private val spiderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; style = Paint.Style.FILL }
        private val legPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND }
        private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val frame = object : Runnable { override fun run() { if (visible) { drawFrame(); handler.postDelayed(this, 33) } } }

        override fun onCreate(holder: SurfaceHolder) { super.onCreate(holder); prefs.registerOnSharedPreferenceChangeListener(this); updatePrefs() }
        override fun onDestroy() { handler.removeCallbacks(frame); prefs.unregisterOnSharedPreferenceChangeListener(this); super.onDestroy() }
        override fun onVisibilityChanged(v: Boolean) { visible = v; handler.removeCallbacks(frame); if (v) handler.post(frame) }
        override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) { super.onSurfaceChanged(holder, format, width, height); drawFrame() }
        override fun onSurfaceDestroyed(holder: SurfaceHolder) { visible = false; handler.removeCallbacks(frame); super.onSurfaceDestroyed(holder) }
        override fun onSharedPreferenceChanged(p: SharedPreferences?, key: String?) { updatePrefs() }

        private fun updatePrefs() { scale = prefs.getInt("spider_scale", 100) / 100f; showSeconds = prefs.getBoolean("show_seconds", true); glow = prefs.getBoolean("enable_glow", true); speed = prefs.getInt("gear_speed", 10) / 10f }

        private fun drawFrame() {
            val c = runCatching { surfaceHolder.lockCanvas() }.getOrNull() ?: return
            try { render(c) } finally { runCatching { surfaceHolder.unlockCanvasAndPost(c) } }
        }

        private fun render(c: Canvas) {
            val w = c.width.toFloat(); val h = c.height.toFloat(); val cx = w/2f; val cy = h/2f
            val radius = min(w,h) * .36f * scale
            bgPaint.shader = RadialGradient(cx,cy,min(w,h)*.9f,intArrayOf(Color.rgb(145,70,5),Color.rgb(55,24,2),Color.rgb(8,4,2)),floatArrayOf(0f,.62f,1f),Shader.TileMode.CLAMP)
            c.drawRect(0f,0f,w,h,bgPaint)
            rotation = (rotation + .3f * speed) % 360f
            webPaint.strokeWidth = max(1f, min(w,h) * .0025f)
            for (i in 1..4) c.drawCircle(cx,cy,radius*i/4f,webPaint)
            for (i in 0 until 12) { val a=Math.toRadians(i*30.0); c.drawLine(cx,cy,(cx+radius*cos(a)).toFloat(),(cy+radius*sin(a)).toFloat(),webPaint) }
            textPaint.textSize = radius*.18f
            for (i in 1..12) { val a=Math.toRadians(i*30.0-90); val x=(cx+radius*cos(a)).toFloat(); val y=(cy+radius*sin(a)-(textPaint.descent()+textPaint.ascent())/2); c.drawText(i.toString(),x,y,textPaint) }
            val cal=Calendar.getInstance(); val hour=cal.get(Calendar.HOUR); val minute=cal.get(Calendar.MINUTE); val second=cal.get(Calendar.SECOND); val ms=cal.get(Calendar.MILLISECOND)
            val ha=(hour+minute/60f)*30f-90f; val ma=(minute+second/60f)*6f-90f; val sa=(second+ms/1000f)*6f-90f
            c.drawLine(cx,0f,cx,cy,webPaint)
            drawHand(c,cx,cy,ha,radius*.55f, max(4f,radius*.025f), .20)
            drawHand(c,cx,cy,ma,radius*.82f, max(2.5f,radius*.014f), -.16)
            if(showSeconds) drawHand(c,cx,cy,sa,radius*.88f,max(1.5f,radius*.007f),0.0,secondPaint)
            drawSpider(c,cx,cy,radius*.24f)
        }

        private fun drawHand(c:Canvas,cx:Float,cy:Float,deg:Float,len:Float,width:Float,bend:Double,paint:Paint=handPaint) { paint.strokeWidth=width; val a=Math.toRadians(deg.toDouble()); val p=Path(); p.moveTo(cx,cy); val mx=(cx+len*.5*cos(a+bend)).toFloat(); val my=(cy+len*.5*sin(a+bend)).toFloat(); val ex=(cx+len*cos(a)).toFloat(); val ey=(cy+len*sin(a)).toFloat(); p.quadTo(mx,my,ex,ey); c.drawPath(p,paint) }
        private fun drawSpider(c:Canvas,cx:Float,cy:Float,s:Float) {
            if(glow){ val gp=Paint(Paint.ANTI_ALIAS_FLAG); gp.shader=RadialGradient(cx,cy,s*1.7f,Color.argb(70,255,255,255),Color.TRANSPARENT,Shader.TileMode.CLAMP); c.drawCircle(cx,cy,s*1.5f,gp) }
            c.drawOval(cx-s*.42f,cy-s*.15f,cx+s*.42f,cy+s*.72f,spiderPaint); c.drawCircle(cx,cy-s*.28f,s*.30f,spiderPaint)
            legPaint.strokeWidth=max(2f,s*.07f)
            for(side in intArrayOf(-1,1)) for(i in 0..3){ val a=Math.toRadians((-55+i*35).toDouble()); val x1=cx+side*s*.65f*cos(a); val y1=cy+s*.65f*sin(a); val x2=cx+side*s*1.35f*cos(a+.18); val y2=cy+s*1.15f*sin(a+.18); val p=Path(); p.moveTo(cx,cy); p.quadTo(x1.toFloat(),y1.toFloat(),x2.toFloat(),y2.toFloat()); c.drawPath(p,legPaint) }
        }
    }
}
