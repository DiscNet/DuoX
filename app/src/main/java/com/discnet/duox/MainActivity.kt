package com.discnet.duox

import android.app.Activity
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.graphics.*
import kotlin.math.*

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContentView(Arena(this))
    }
}

private data class Slime(
    var x: Float, var y: Float, val color: Int,
    var vx: Float = 0f, var vy: Float = 0f, var face: Int = 1,
    var grounded: Boolean = false, var shieldUntil: Float = 0f,
    var shotAt: Float = -10f, var dashAt: Float = -10f, var shieldAt: Float = -10f
)
private data class Bullet(var x: Float, var y: Float, val vx: Float, val owner: Int, var life: Float = 1.7f)
private data class Spark(var x: Float, var y: Float, var vx: Float, var vy: Float, var life: Float, val color: Int)
private data class Platform(val left: Float, val top: Float, val right: Float, val bottom: Float)
private data class Control(val owner: Int, val action: Int, val cx: Float, val cy: Float, val radius: Float, val label: String)
private const val LEFT = 0
private const val RIGHT = 1
private const val JUMP = 2
private const val FIRE = 3
private const val DASH = 4
private const val SHIELD = 5

private class Arena(activity: Activity) : View(activity) {
    private val p = Paint(3)
    private val bg = Paint(3)
    private val platformColor = Color.rgb(62, 75, 110)
    private val blue = Color.rgb(58, 203, 255)
    private val pink = Color.rgb(255, 100, 171)
    private val players = arrayOf(Slime(290f, 300f, blue), Slime(710f, 300f, pink, face = -1))
    private val bullets = mutableListOf<Bullet>()
    private val sparks = mutableListOf<Spark>()
    private val platforms = listOf(
        Platform(90f, 354f, 340f, 373f), Platform(660f, 354f, 910f, 373f),
        Platform(396f, 290f, 604f, 308f), Platform(205f, 228f, 360f, 246f),
        Platform(640f, 228f, 795f, 246f), Platform(0f, 475f, 1000f, 500f)
    )
    private val controls = buildList {
        for (i in 0..1) {
            fun x(value: Float) = if (i == 0) value else 1000f - value
            add(Control(i, LEFT, x(52f), 438f, 33f, "◀"))
            add(Control(i, RIGHT, x(129f), 438f, 33f, "▶"))
            add(Control(i, JUMP, x(213f), 438f, 31f, "↑"))
            add(Control(i, FIRE, x(293f), 438f, 30f, "●"))
            add(Control(i, DASH, x(372f), 438f, 29f, "»"))
            add(Control(i, SHIELD, x(452f), 438f, 29f, "◇"))
        }
    }
    private val touchToControl = mutableMapOf<Int, Control>()
    private val previousPressed = mutableSetOf<Pair<Int, Int>>()
    private val score = intArrayOf(0, 0)
    private var time = 0f
    private var lastTick = 0L
    private var roundPause = 0f
    private var gameOver = false
    private var message = "PREPAREM-SE!"
    private var messageUntil = 1.6f
    private val rnd = java.util.Random()

    override fun onTouchEvent(e: MotionEvent): Boolean {
        val w = width.toFloat().coerceAtLeast(1f)
        val h = height.toFloat().coerceAtLeast(1f)
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                val idx = e.actionIndex
                val id = e.getPointerId(idx)
                val x = e.getX(idx) * 1000f / w
                val y = e.getY(idx) * 560f / h
                if (gameOver && y < 220f) {
                    score.fill(0); gameOver = false; resetRound(); return true
                }
                controls.filter { hypot(x - it.cx, y - it.cy) < it.radius + 16f }
                    .minByOrNull { hypot(x - it.cx, y - it.cy) }?.let { touchToControl[id] = it }
            }
            MotionEvent.ACTION_MOVE -> {
                for (i in 0 until e.pointerCount) {
                    val id = e.getPointerId(i)
                    val initial = touchToControl[id] ?: continue
                    val x = e.getX(i) * 1000f / w
                    val y = e.getY(i) * 560f / h
                    if (hypot(x - initial.cx, y - initial.cy) > initial.radius + 45f) {
                        val other = controls.filter { it.owner == initial.owner && hypot(x - it.cx, y - it.cy) < it.radius + 10f }
                            .minByOrNull { hypot(x - it.cx, y - it.cy) }
                        if (other != null) touchToControl[id] = other else touchToControl.remove(id)
                    }
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> touchToControl.remove(e.getPointerId(e.actionIndex))
            MotionEvent.ACTION_CANCEL -> touchToControl.clear()
        }
        return true
    }

    private fun resetRound() {
        val a = players[0]; val b = players[1]
        a.x = 280f; a.y = 320f; a.vx = 0f; a.vy = 0f; a.face = 1; a.grounded = false
        b.x = 720f; b.y = 320f; b.vx = 0f; b.vy = 0f; b.face = -1; b.grounded = false
        for (s in players) { s.shieldUntil = 0f; s.shotAt = -10f; s.dashAt = -10f; s.shieldAt = -10f }
        bullets.clear(); sparks.clear(); roundPause = 0.7f; message = "LUTEM!"; messageUntil = time + 1.2f
        touchToControl.clear(); previousPressed.clear()
    }

    private fun burst(x: Float, y: Float, color: Int, count: Int = 18) {
        repeat(count) {
            val a = rnd.nextFloat() * Math.PI.toFloat() * 2f
            val speed = 55f + rnd.nextFloat() * 220f
            sparks.add(Spark(x, y, cos(a)*speed, sin(a)*speed, .35f + rnd.nextFloat()*.5f, color))
        }
    }

    private fun eliminate(loser: Int) {
        if (roundPause > 0f || gameOver) return
        burst(players[loser].x, players[loser].y, players[loser].color, 32)
        val winner = 1 - loser
        score[winner]++
        roundPause = 2.0f
        message = "PLAYER ${winner+1} MARCOU!"
        messageUntil = time + 2f
        if (score[winner] >= 5) { gameOver = true; message = "PLAYER ${winner+1} VENCEU!" }
    }

    private fun simulate(dt: Float) {
        time += dt
        for (s in sparks) { s.x += s.vx*dt; s.y += s.vy*dt; s.vy += 330f*dt; s.life -= dt }
        sparks.removeAll { it.life <= 0f }
        if (gameOver) return
        if (roundPause > 0f) {
            roundPause -= dt
            if (roundPause <= 0f) resetRound()
            return
        }
        val pressed = touchToControl.values.map { it.owner to it.action }.toSet()
        for ((i,s) in players.withIndex()) {
            val left = i to LEFT in pressed
            val right = i to RIGHT in pressed
            val horizontal = (if (right) 1 else 0) - (if (left) 1 else 0)
            if (horizontal != 0) s.face = horizontal
            s.vx += (horizontal * 1150f - s.vx * if (horizontal == 0) 8f else 4.2f) * dt
            s.vx = s.vx.coerceIn(-270f, 270f)
            fun newPress(a: Int) = (i to a in pressed) && (i to a !in previousPressed)
            if (newPress(JUMP) && s.grounded) { s.vy = -470f; s.grounded = false; burst(s.x,s.y+23f,s.color,8) }
            if (newPress(FIRE) && time-s.shotAt >= .55f) {
                bullets.add(Bullet(s.x+s.face*32f,s.y-5f,s.face*560f,i))
                s.shotAt = time
            }
            if (newPress(DASH) && time-s.dashAt >= 2.3f) {
                s.vx = s.face * 630f; s.vy = -95f; s.dashAt = time; burst(s.x,s.y,s.color,12)
            }
            if (newPress(SHIELD) && time-s.shieldAt >= 5f) {
                s.shieldAt = time; s.shieldUntil = time+1.3f
            }
            val oldBottom = s.y + 24f
            s.vy += 980f*dt
            s.vy = s.vy.coerceAtMost(650f)
            s.x = (s.x + s.vx*dt).coerceIn(25f,975f)
            s.y += s.vy*dt
            s.grounded = false
            for (platform in platforms) {
                if (s.vy >= 0f && oldBottom <= platform.top+7f && s.y+24f >= platform.top &&
                    s.x+20f >= platform.left && s.x-20f <= platform.right) {
                    s.y = platform.top-24f; s.vy = 0f; s.grounded = true
                }
            }
            if (s.y > 530f) { eliminate(i); break }
        }
        previousPressed.clear(); previousPressed.addAll(pressed)
        for (b in bullets) {
            b.x += b.vx*dt; b.life -= dt
            val enemy = players[1-b.owner]
            if (hypot(b.x-enemy.x, (b.y-enemy.y)*1.1f) < 28f) {
                b.life = 0f
                if (time < enemy.shieldUntil) burst(b.x,b.y,enemy.color,10)
                else { eliminate(1-b.owner); break }
            }
            if (platforms.any { b.x in it.left..it.right && b.y in it.top..it.bottom }) {
                burst(b.x,b.y,players[b.owner].color,5); b.life = 0f
            }
        }
        bullets.removeAll { it.life <= 0f || it.x < -30f || it.x > 1030f }
    }

    private fun fill(c: Canvas, color: Int, l: Float,t: Float,r: Float,b: Float, rad: Float = 0f) {
        p.color=color; p.style=Paint.Style.FILL; c.drawRoundRect(l,t,r,b,rad,rad,p)
    }
    private fun text(c: Canvas, value: String, x: Float, y: Float, size: Float, color: Int = Color.WHITE) {
        p.color=color; p.textAlign=Paint.Align.CENTER; p.typeface=Typeface.create("sans-serif-black",Typeface.BOLD)
        p.textSize=size; p.style=Paint.Style.FILL; c.drawText(value,x,y,p)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val now = System.nanoTime()
        if(lastTick == 0L) lastTick = now
        val dt = ((now-lastTick)/1_000_000_000f).coerceIn(0f,.033f)
        lastTick=now
        simulate(dt)
        canvas.save()
        canvas.scale(width/1000f,height/560f)
        bg.shader = LinearGradient(0f,0f,0f,560f,Color.rgb(16,22,43),Color.rgb(33,34,65),Shader.TileMode.CLAMP)
        canvas.drawRect(0f,0f,1000f,560f,bg); bg.shader=null
        p.color=Color.argb(20,170,200,255)
        for (x in 0..20) canvas.drawLine(x*50f,45f,x*50f,505f,p)
        for (y in 1..10) canvas.drawLine(0f,y*50f,1000f,y*50f,p)
        for (it in platforms) {
            fill(canvas,Color.rgb(29,38,62),it.left,it.top+6f,it.right,it.bottom+6f,10f)
            fill(canvas,platformColor,it.left,it.top,it.right,it.bottom,9f)
            fill(canvas,Color.rgb(120,132,184),it.left+5f,it.top,it.right-5f,it.top+5f,4f)
        }
        for (b in bullets) {
            p.color=players[b.owner].color
            p.setShadowLayer(15f,0f,0f,p.color)
            canvas.drawCircle(b.x,b.y,9f,p); p.clearShadowLayer()
            p.color=Color.WHITE; canvas.drawCircle(b.x-2f,b.y-2f,3f,p)
        }
        for(s in players) {
            val squash = if (s.grounded) 1.08f else .94f
            p.color = Color.argb(80,0,0,0)
            canvas.drawOval(s.x-29f, s.y+21f,s.x+29f,s.y+29f,p)
            canvas.save(); canvas.translate(s.x,s.y); canvas.scale(squash,1f/squash)
            p.color=s.color; p.setShadowLayer(19f,0f,4f,s.color)
            canvas.drawOval(-26f,-24f,26f,25f,p); p.clearShadowLayer()
            p.color=Color.argb(75,255,255,255); canvas.drawOval(-18f,-19f,6f,-9f,p)
            p.color=Color.rgb(22,31,53)
            canvas.drawOval(-13f,-5f,-5f,7f,p); canvas.drawOval(6f,-5f,14f,7f,p)
            p.color=Color.WHITE
            canvas.drawCircle(-10f,-2f,2.5f,p); canvas.drawCircle(9f,-2f,2.5f,p)
            canvas.restore()
            if (time < s.shieldUntil) {
                p.style=Paint.Style.STROKE; p.strokeWidth=4f; p.color=Color.argb(210,168,245,255)
                canvas.drawCircle(s.x,s.y,37f,p); p.style=Paint.Style.FILL
            }
        }
        for (s in sparks) {
            p.color=s.color; p.alpha=(255f*(s.life/.8f).coerceIn(0f,1f)).toInt()
            canvas.drawCircle(s.x,s.y,3.5f,p)
        }
        p.alpha=255
        fill(canvas,Color.argb(185,12,17,34),320f,8f,680f,61f,23f)
        text(canvas,"DUOX",500f,31f,22f)
        text(canvas,"${score[0]}     :     ${score[1]}",500f,53f,21f)
        text(canvas,"P1",70f,37f,24f,blue)
        text(canvas,"P2",930f,37f,24f,pink)
        for (control in controls) {
            val s=players[control.owner]
            val available=when(control.action) {
                FIRE -> (time-s.shotAt)/.55f
                DASH -> (time-s.dashAt)/2.3f
                SHIELD -> (time-s.shieldAt)/5f
                else -> 1f
            }.coerceIn(0f,1f)
            val active=touchToControl.values.any { it===control }
            p.color=if(active) Color.argb(185,105,150,185) else Color.argb(185,29,37,60)
            canvas.drawCircle(control.cx,control.cy,control.radius,p)
            p.style=Paint.Style.STROKE; p.strokeWidth=3f
            p.color=if(control.owner==0) blue else pink
            canvas.drawArc(control.cx-control.radius,control.cy-control.radius,control.cx+control.radius,control.cy+control.radius,-90f,360f*available,false,p)
            p.style=Paint.Style.FILL
            text(canvas,control.label,control.cx,control.cy+9f,26f,if(available>=1f)Color.WHITE else Color.LTGRAY)
        }
        if (time < messageUntil || gameOver) {
            fill(canvas,Color.argb(210,15,20,39),299f,95f,701f,163f,20f)
            text(canvas,message,500f,138f,25f)
        }
        if (gameOver) text(canvas,"TOQUE AQUI PARA REINICIAR",500f,188f,17f)
        text(canvas,"MOVER   PULAR   TIRO   DASH   ESCUDO",250f,520f,12f,Color.LTGRAY)
        text(canvas,"MOVER   PULAR   TIRO   DASH   ESCUDO",750f,520f,12f,Color.LTGRAY)
        canvas.restore()
        postInvalidateDelayed(16L)
    }
}
