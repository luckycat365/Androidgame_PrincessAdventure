package com.example.princessadventure

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

class PrincessGameView(context: Context) : SurfaceView(context), Runnable, SurfaceHolder.Callback {
    private val assets = GameAssets(context)
    private val levelSegmentCount = 10
    private val levelSegmentWidth = 3400f
    private val worldWidth = levelSegmentWidth * levelSegmentCount
    private val worldHeight = 720f
    private val playfieldLift = worldHeight * 0.10f
    private val princessSpriteFootCorrection = 22f
    private val princessSpriteHeight = 174f
    private val princessSpriteHalfWidth = princessSpriteHeight * 448f / 256f / 2f
    private val levelTimeLimit = 180f
    private val castleCollisionWidth = 205f
    private val castleCollisionHeight = 230f
    private val castleVisualScale = 2f
    private val castleTransparentBottomPadding = 19f
    private val castlePlatformTop = liftedY(650f)
    private val castleCenterX = worldWidth - 225f
    private val castleVisualBottomPadding = castleCollisionHeight *
        castleVisualScale *
        castleTransparentBottomPadding / assets.castle.height
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        typeface = android.graphics.Typeface.create(android.graphics.Typeface.SANS_SERIF, android.graphics.Typeface.BOLD)
    }
    private val smallTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(99, 40, 88)
        textAlign = Paint.Align.CENTER
        typeface = android.graphics.Typeface.create(android.graphics.Typeface.SANS_SERIF, android.graphics.Typeface.BOLD)
    }
    private val pointerPositions = mutableMapOf<Int, Pair<Float, Float>>()
    private val projectiles = mutableListOf<Projectile>()
    private val platforms: List<Platform>
    private val enemies: MutableList<TeacupEnemy>
    private val endActionButton = RectF()
    private val castleRect = RectF(
        castleCenterX - castleCollisionWidth / 2f,
        castlePlatformTop - castleCollisionHeight,
        castleCenterX + castleCollisionWidth / 2f,
        castlePlatformTop,
    )
    private val castleVisualRect = RectF(
        castleCenterX - (castleCollisionWidth * castleVisualScale) / 2f,
        castlePlatformTop + castleVisualBottomPadding - castleCollisionHeight * castleVisualScale,
        castleCenterX + (castleCollisionWidth * castleVisualScale) / 2f,
        castlePlatformTop + castleVisualBottomPadding,
    )
    private var thread: Thread? = null
    @Volatile private var running = false
    private var lastFrameNanos = 0L
    private var elapsed = 0f
    private var cameraX = 0f
    private var levelEnemyScore = 0
    private var totalScore = 0
    private var princessX = 120f
    private var princessY = liftedY(548f)
    private var velocityY = 0f
    private var facing = 1
    private var health = 2
    private var jumpCount = 0
    private var onGround = false
    private var attackTimer = 0f
    private var attackCooldown = 0f
    private var hurtTimer = 0f
    private var invulnerableTimer = 0f
    private var moveLeft = false
    private var moveRight = false
    private var gameState = GameState.Starting
    private var finalTimingScore = 0
    private var finalLevelScore = 0
    private val leftControl = Control("left", assets.leftButton)
    private val rightControl = Control("right", assets.rightButton)
    private val jumpControl = Control("jump", assets.jumpButton)
    private val attackControl = Control("attack", assets.attackButton)

    init {
        holder.addCallback(this)
        isFocusable = true
        platforms = createPlatforms()
        enemies = createEnemies()
    }

    fun resume() {
        assets.resumeMusic()
        if (holder.surface.isValid && !running) startLoop()
    }

    fun pause() {
        running = false
        assets.pauseMusic()
        try {
            thread?.join(500)
        } catch (_: InterruptedException) {
            Thread.currentThread().interrupt()
        }
        thread = null
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        startLoop()
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) = Unit

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        pause()
    }

    override fun run() {
        lastFrameNanos = System.nanoTime()
        while (running) {
            val now = System.nanoTime()
            val dt = min((now - lastFrameNanos) / 1_000_000_000f, 0.033f)
            lastFrameNanos = now
            update(dt)
            drawFrame()
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                val index = event.actionIndex
                pointerPositions[event.getPointerId(index)] = event.getX(index) to event.getY(index)
                handleTap(event.getX(index), event.getY(index))
            }
            MotionEvent.ACTION_MOVE -> {
                for (i in 0 until event.pointerCount) {
                    pointerPositions[event.getPointerId(i)] = event.getX(i) to event.getY(i)
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP, MotionEvent.ACTION_CANCEL -> {
                val index = event.actionIndex
                pointerPositions.remove(event.getPointerId(index))
            }
        }
        updateHeldControls()
        return true
    }

    private fun startLoop() {
        if (running) return
        running = true
        thread = Thread(this, "PrincessGameLoop").also { it.start() }
    }

    private fun update(dt: Float) {
        if (gameState != GameState.Playing) return

        elapsed += dt
        attackCooldown = max(0f, attackCooldown - dt)
        attackTimer = max(0f, attackTimer - dt)
        hurtTimer = max(0f, hurtTimer - dt)
        invulnerableTimer = max(0f, invulnerableTimer - dt)

        val speed = 310f
        val horizontal = when {
            moveLeft && !moveRight -> -1
            moveRight && !moveLeft -> 1
            else -> 0
        }
        if (horizontal != 0) facing = horizontal
        princessX += horizontal * speed * dt
        princessX = princessX.coerceIn(55f, worldWidth - 55f)

        val previousY = princessY
        velocityY += 1450f * dt
        princessY += velocityY * dt
        resolvePlatforms(previousY)

        updateProjectiles(dt)
        updateEnemies(dt)
        updateCamera()

        if (playerBody().intersect(castleRect)) {
            finishLevel()
        }

        if (princessY > worldHeight + 180f) {
            hurtPrincess()
            princessX = max(80f, cameraX + 90f)
            princessY = liftedY(250f)
            velocityY = 0f
        }
    }

    private fun resolvePlatforms(previousY: Float) {
        onGround = false
        val body = playerBody()
        for (platform in platforms) {
            val top = platform.rect.top
            val wasAbove = previousY <= top + 8f
            val overlapsX = body.right > platform.rect.left + 12f && body.left < platform.rect.right - 12f
            val crossesTop = princessY >= top && velocityY >= 0f
            if (wasAbove && overlapsX && crossesTop) {
                princessY = top
                velocityY = 0f
                onGround = true
                jumpCount = 0
                return
            }
        }
    }

    private fun updateProjectiles(dt: Float) {
        val iterator = projectiles.iterator()
        while (iterator.hasNext()) {
            val projectile = iterator.next()
            projectile.x += projectile.vx * dt
            if (projectile.x < cameraX - 100f || projectile.x > cameraX + viewportWidth() + 180f) {
                iterator.remove()
                continue
            }
            val projectileRect = projectile.rect()
            for (enemy in enemies) {
                if (!enemy.destroyed && projectileRect.intersect(enemy.body())) {
                    enemy.health -= 1
                    enemy.hitTimer = 0.25f
                    iterator.remove()
                    if (enemy.health <= 0) {
                        enemy.destroyed = true
                        enemy.destroyedTimer = 0.65f
                        levelEnemyScore += 1
                        assets.playSound("teacupCrash")
                    }
                    break
                }
            }
        }
    }

    private fun updateEnemies(dt: Float) {
        val player = playerBody()
        val iterator = enemies.iterator()
        while (iterator.hasNext()) {
            val enemy = iterator.next()
            if (enemy.destroyed) {
                enemy.destroyedTimer -= dt
                if (enemy.destroyedTimer <= 0f) {
                    iterator.remove()
                }
                continue
            }
            val platform = platforms[enemy.platformIndex].rect
            enemy.x += enemy.direction * 115f * dt
            val minX = platform.left + 52f
            val maxX = platform.right - 52f
            if (enemy.x < minX || enemy.x > maxX) {
                enemy.x = enemy.x.coerceIn(minX, maxX)
                enemy.direction *= -1
            }
            enemy.hitTimer = max(0f, enemy.hitTimer - dt)
            if (invulnerableTimer <= 0f && player.intersect(enemy.body())) {
                hurtPrincess()
            }
        }
    }

    private fun updateCamera() {
        val viewport = viewportWidth()
        cameraX = (princessX - viewport * 0.38f).coerceIn(0f, max(0f, worldWidth - viewport))
    }

    private fun jump() {
        if (gameState != GameState.Playing) return
        if (onGround) {
            velocityY = -680f
            onGround = false
            jumpCount = 1
        } else if (jumpCount == 1) {
            velocityY = -610f
            jumpCount = 2
            assets.playSound("doubleJump")
        }
    }

    private fun attack() {
        if (gameState != GameState.Playing || attackCooldown > 0f) return
        attackCooldown = 0.34f
        attackTimer = 0.28f
        projectiles += Projectile(princessX + facing * 62f, princessY - 92f, facing * 790f)
        assets.playSound("starAttack")
    }

    private fun hurtPrincess() {
        health -= 1
        hurtTimer = 0.55f
        invulnerableTimer = 1.25f
        velocityY = -330f
        princessX -= facing * 55f
        assets.playSound("princessHurt")
        if (health <= 0) {
            finalTimingScore = 0
            finalLevelScore = levelEnemyScore
            totalScore += finalLevelScore
            gameState = GameState.Lost
        }
    }

    private fun finishLevel() {
        finalTimingScore = timingScore()
        finalLevelScore = levelEnemyScore + finalTimingScore
        totalScore += finalLevelScore
        gameState = GameState.Won
    }

    private fun resetGame() {
        princessX = 120f
        princessY = liftedY(548f)
        velocityY = 0f
        facing = 1
        health = 2
        elapsed = 0f
        levelEnemyScore = 0
        finalTimingScore = 0
        finalLevelScore = 0
        cameraX = 0f
        attackTimer = 0f
        hurtTimer = 0f
        invulnerableTimer = 0f
        projectiles.clear()
        enemies.clear()
        enemies += createEnemies()
        gameState = GameState.Playing
    }

    private fun drawFrame() {
        val canvas = holder.lockCanvas() ?: return
        try {
            if (gameState == GameState.Starting) {
                drawStartingView(canvas)
            } else {
                canvas.drawColor(Color.rgb(185, 229, 255))
                drawWorld(canvas)
                drawHud(canvas)
                if (gameState != GameState.Playing) drawEndOverlay(canvas)
            }
        } finally {
            holder.unlockCanvasAndPost(canvas)
        }
    }

    private fun drawStartingView(canvas: Canvas) {
        canvas.drawColor(Color.rgb(197, 228, 250))
        val viewWidth = width.toFloat()
        val viewHeight = height.toFloat()
        val bitmap = assets.startingView
        val imageAspect = bitmap.width.toFloat() / bitmap.height.toFloat()
        val viewAspect = viewWidth / viewHeight
        val dest = if (imageAspect > viewAspect) {
            val drawHeight = viewWidth / imageAspect
            val top = (viewHeight - drawHeight) / 2f
            RectF(0f, top, viewWidth, top + drawHeight)
        } else {
            val drawWidth = viewHeight * imageAspect
            val left = (viewWidth - drawWidth) / 2f
            RectF(left, 0f, left + drawWidth, viewHeight)
        }
        canvas.drawBitmap(bitmap, null, dest, paint)
    }

    private fun drawWorld(canvas: Canvas) {
        val scale = worldScale()
        canvas.save()
        canvas.scale(scale, scale)
        canvas.translate(-cameraX, 0f)
        canvas.drawBitmap(assets.background, null, RectF(cameraX, 0f, cameraX + viewportWidth(), worldHeight), paint)

        for (platform in platforms) {
            canvas.drawBitmap(platform.bitmap, null, platform.rect, paint)
        }
        canvas.drawBitmap(assets.castle, null, castleVisualRect, paint)

        val time = elapsed
        for (enemy in enemies) {
            if (enemy.destroyed && enemy.destroyedTimer <= 0f) continue
            val bitmap = when {
                enemy.destroyed -> assets.teacupDestroyed
                enemy.hitTimer > 0f -> assets.teacupHit
                else -> assets.teacupWalk.frame(time)
            }
            val dest = RectF(enemy.x - 54f, enemy.y - 88f, enemy.x + 54f, enemy.y + 20f)
            canvas.save()
            if (enemy.direction > 0) {
                canvas.scale(-1f, 1f, enemy.x, enemy.y - 35f)
            }
            canvas.drawBitmap(bitmap, null, dest, paint)
            canvas.restore()
        }

        for (projectile in projectiles) {
            canvas.drawBitmap(assets.starProjectile, null, projectile.drawRect(), paint)
        }

        val princessBitmap = princessFrame()
        val spriteBottom = princessY + 4f + princessSpriteFootCorrection
        val dest = RectF(
            princessX - princessSpriteHalfWidth,
            spriteBottom - princessSpriteHeight,
            princessX + princessSpriteHalfWidth,
            spriteBottom,
        )
        canvas.save()
        if (facing < 0) {
            canvas.scale(-1f, 1f, princessX, spriteBottom - 84f)
        }
        if (invulnerableTimer <= 0f || ((elapsed * 12).toInt() % 2 == 0)) {
            canvas.drawBitmap(princessBitmap, null, dest, paint)
        }
        canvas.restore()
        canvas.restore()
    }

    private fun drawHud(canvas: Canvas) {
        val width = width.toFloat()
        val height = height.toFloat()
        textPaint.textSize = height * 0.038f
        smallTextPaint.textSize = height * 0.03f

        paint.color = Color.argb(190, 255, 238, 247)
        canvas.drawRoundRect(RectF(width * 0.03f, height * 0.035f, width * 0.22f, height * 0.12f), 24f, 24f, paint)
        for (index in 0 until health.coerceAtLeast(0)) {
            val left = width * 0.05f + index * height * 0.07f
            canvas.drawBitmap(assets.heart, null, RectF(left, height * 0.045f, left + height * 0.055f, height * 0.10f), paint)
        }

        drawHudPill(canvas, RectF(width * 0.39f, height * 0.035f, width * 0.61f, height * 0.12f), "Score: $levelEnemyScore")
        drawHudPill(canvas, RectF(width * 0.76f, height * 0.035f, width * 0.96f, height * 0.12f), "Time: ${elapsed.toInt()}s")

        layoutControls()
        drawControl(canvas, leftControl)
        drawControl(canvas, rightControl)
        drawControl(canvas, jumpControl)
        drawControl(canvas, attackControl)
    }

    private fun drawHudPill(canvas: Canvas, rect: RectF, label: String) {
        paint.color = Color.argb(200, 255, 230, 245)
        canvas.drawRoundRect(rect, 28f, 28f, paint)
        paint.color = Color.argb(130, 255, 111, 177)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 3f
        canvas.drawRoundRect(rect, 28f, 28f, paint)
        paint.style = Paint.Style.FILL
        textPaint.color = Color.rgb(106, 43, 101)
        canvas.drawText(label, rect.centerX(), rect.centerY() - (textPaint.ascent() + textPaint.descent()) / 2f, textPaint)
        textPaint.color = Color.WHITE
    }

    private fun drawControl(canvas: Canvas, control: Control) {
        val idleAlpha = if (control.name == "left" || control.name == "right") 95 else 175
        val pressedAlpha = if (control.name == "left" || control.name == "right") 155 else 235
        paint.alpha = if (control.pressed) pressedAlpha else idleAlpha
        canvas.drawBitmap(control.bitmap, null, control.rect, paint)
        paint.alpha = 255
    }

    private fun drawEndOverlay(canvas: Canvas) {
        val width = width.toFloat()
        val height = height.toFloat()
        paint.color = Color.argb(180, 54, 20, 64)
        canvas.drawRect(0f, 0f, width, height, paint)

        val panel = RectF(width * 0.24f, height * 0.18f, width * 0.76f, height * 0.78f)
        paint.color = Color.rgb(255, 240, 248)
        canvas.drawRoundRect(panel, 34f, 34f, paint)
        paint.color = Color.rgb(255, 128, 189)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 5f
        canvas.drawRoundRect(panel, 34f, 34f, paint)
        paint.style = Paint.Style.FILL

        val title = if (gameState == GameState.Won) "Victory at the Castle!" else "The Princess Needs a Retry"
        textPaint.color = Color.rgb(111, 44, 110)
        textPaint.textSize = height * 0.06f
        canvas.drawText(title, width * 0.5f, panel.top + height * 0.12f, textPaint)

        smallTextPaint.textSize = height * 0.038f
        val lines = if (gameState == GameState.Won) {
            listOf(
                "Enemy score: $levelEnemyScore",
                "Time bonus: $finalTimingScore",
                "Level score: $finalLevelScore",
                "Total score: $totalScore",
            )
        } else {
            listOf(
                "Score accumulated so far: $totalScore",
                "Enemy score: $levelEnemyScore",
                "Time bonus: $finalTimingScore",
                "Tap anywhere to try again",
            )
        }
        lines.forEachIndexed { index, line ->
            canvas.drawText(line, width * 0.5f, panel.top + height * (0.22f + index * 0.075f), smallTextPaint)
        }

        if (gameState == GameState.Won) {
            layoutEndActionButton(panel, width, height)
            paint.color = Color.rgb(255, 128, 189)
            canvas.drawRoundRect(endActionButton, 26f, 26f, paint)
            paint.color = Color.rgb(255, 245, 251)
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 4f
            canvas.drawRoundRect(endActionButton, 26f, 26f, paint)
            paint.style = Paint.Style.FILL

            textPaint.color = Color.WHITE
            textPaint.textSize = height * 0.043f
            canvas.drawText(
                "Proceed",
                endActionButton.centerX(),
                endActionButton.centerY() - (textPaint.ascent() + textPaint.descent()) / 2f,
                textPaint,
            )
        }
    }

    private fun layoutControls() {
        val h = height.toFloat()
        val w = width.toFloat()
        val size = h * 0.18f
        val bottom = h - h * 0.05f
        leftControl.rect.set(w * 0.05f, bottom - size, w * 0.05f + size, bottom)
        rightControl.rect.set(w * 0.05f + size * 1.12f, bottom - size, w * 0.05f + size * 2.12f, bottom)
        attackControl.rect.set(w - w * 0.05f - size * 2.12f, bottom - size, w - w * 0.05f - size * 1.12f, bottom)
        jumpControl.rect.set(w - w * 0.05f - size, bottom - size, w - w * 0.05f, bottom)
    }

    private fun layoutEndActionButton(panel: RectF, width: Float, height: Float) {
        val buttonWidth = width * 0.20f
        val buttonHeight = height * 0.085f
        val top = panel.bottom - height * 0.13f
        endActionButton.set(
            width * 0.5f - buttonWidth / 2f,
            top,
            width * 0.5f + buttonWidth / 2f,
            top + buttonHeight,
        )
    }

    private fun handleTap(x: Float, y: Float) {
        if (gameState == GameState.Starting) {
            gameState = GameState.Playing
            pointerPositions.clear()
            return
        }
        if (gameState == GameState.Won) {
            val screenWidth = width.toFloat()
            val screenHeight = height.toFloat()
            val panel = RectF(screenWidth * 0.24f, screenHeight * 0.18f, screenWidth * 0.76f, screenHeight * 0.78f)
            layoutEndActionButton(panel, screenWidth, screenHeight)
            if (endActionButton.contains(x, y)) resetGame()
            pointerPositions.clear()
            return
        }
        if (gameState == GameState.Lost) {
            resetGame()
            pointerPositions.clear()
            return
        }
        layoutControls()
        if (jumpControl.rect.contains(x, y)) jump()
        if (attackControl.rect.contains(x, y)) attack()
    }

    private fun updateHeldControls() {
        if (gameState != GameState.Playing) {
            moveLeft = false
            moveRight = false
            leftControl.pressed = false
            rightControl.pressed = false
            jumpControl.pressed = false
            attackControl.pressed = false
            return
        }
        layoutControls()
        moveLeft = false
        moveRight = false
        leftControl.pressed = false
        rightControl.pressed = false
        jumpControl.pressed = false
        attackControl.pressed = false
        for ((x, y) in pointerPositions.values) {
            if (leftControl.rect.contains(x, y)) {
                moveLeft = true
                leftControl.pressed = true
            }
            if (rightControl.rect.contains(x, y)) {
                moveRight = true
                rightControl.pressed = true
            }
            if (jumpControl.rect.contains(x, y)) jumpControl.pressed = true
            if (attackControl.rect.contains(x, y)) attackControl.pressed = true
        }
    }

    private fun princessFrame(): Bitmap {
        return when {
            hurtTimer > 0f -> assets.princessHurt.frame(elapsed, 1f)
            attackTimer > 0f -> assets.princessAttack.frame(elapsed, 18f)
            !onGround -> assets.princessJump.frame(elapsed, 8f)
            moveLeft || moveRight -> assets.princessRun.frame(elapsed, 9f)
            else -> assets.princessIdle.frame(elapsed, 5f)
        }
    }

    private fun createPlatforms(): List<Platform> {
        return levelPlatformBlueprints().map { platform ->
            Platform(liftedRect(platform.left, platform.top, platform.right, platform.bottom), platform.asset.bitmap())
        }
    }

    private fun createEnemies(): MutableList<TeacupEnemy> {
        return levelPlatformBlueprints().mapIndexedNotNullTo(mutableListOf()) { index, platform ->
            platform.enemyX?.let { x ->
                TeacupEnemy(x = x, y = liftedY(platform.top), platformIndex = index)
            }
        }
    }

    private fun levelPlatformBlueprints(): List<PlatformBlueprint> {
        val platforms = mutableListOf<PlatformBlueprint>()

        fun add(
            left: Float,
            top: Float,
            right: Float,
            bottom: Float,
            asset: PlatformAsset,
            enemyX: Float? = null,
        ) {
            platforms += PlatformBlueprint(left, top, right, bottom, asset, enemyX)
        }

        add(0f, 650f, 580f, 720f, PlatformAsset.GrassLong)
        add(650f, 610f, 910f, 670f, PlatformAsset.GrassShort, enemyX = 790f)
        add(990f, 540f, 1260f, 600f, PlatformAsset.Cloud)
        add(1370f, 455f, 1640f, 525f, PlatformAsset.Crystal, enemyX = 1510f)
        add(1730f, 505f, 2020f, 575f, PlatformAsset.GrassRound)
        add(2120f, 610f, 2510f, 680f, PlatformAsset.FlowerBridge, enemyX = 2320f)
        add(2630f, 570f, 2920f, 640f, PlatformAsset.GrassRound)
        add(3050f, 650f, 3720f, 720f, PlatformAsset.GrassLong, enemyX = 3420f)

        add(3860f, 600f, 4160f, 660f, PlatformAsset.GrassShort)
        add(4300f, 500f, 4580f, 560f, PlatformAsset.Cloud, enemyX = 4450f)
        add(4710f, 390f, 4990f, 460f, PlatformAsset.Crystal)
        add(5120f, 470f, 5360f, 540f, PlatformAsset.GrassRound)
        add(5500f, 575f, 5910f, 645f, PlatformAsset.FlowerBridge, enemyX = 5720f)
        add(6060f, 520f, 6320f, 580f, PlatformAsset.Cloud)
        add(6450f, 650f, 7060f, 720f, PlatformAsset.GrassLong, enemyX = 6740f)

        add(7210f, 585f, 7510f, 645f, PlatformAsset.GrassShort)
        add(7640f, 510f, 7870f, 570f, PlatformAsset.Cloud)
        add(8000f, 445f, 8290f, 515f, PlatformAsset.Crystal, enemyX = 8150f)
        add(8410f, 360f, 8660f, 430f, PlatformAsset.Cloud)
        add(8790f, 420f, 9100f, 490f, PlatformAsset.GrassRound)
        add(9240f, 545f, 9650f, 615f, PlatformAsset.FlowerBridge, enemyX = 9460f)
        add(9800f, 650f, 10380f, 720f, PlatformAsset.GrassLong)

        add(10520f, 590f, 10790f, 650f, PlatformAsset.GrassShort, enemyX = 10660f)
        add(10930f, 520f, 11180f, 580f, PlatformAsset.Cloud)
        add(11340f, 625f, 11620f, 695f, PlatformAsset.GrassRound)
        add(11770f, 560f, 12140f, 630f, PlatformAsset.FlowerBridge, enemyX = 11960f)
        add(12280f, 475f, 12570f, 545f, PlatformAsset.Crystal)
        add(12700f, 405f, 12950f, 465f, PlatformAsset.Cloud)
        add(13090f, 505f, 13420f, 575f, PlatformAsset.GrassRound, enemyX = 13250f)
        add(13570f, 650f, 14160f, 720f, PlatformAsset.GrassLong)

        add(14290f, 590f, 14600f, 650f, PlatformAsset.GrassShort)
        add(14760f, 485f, 15030f, 545f, PlatformAsset.Cloud, enemyX = 14900f)
        add(15180f, 380f, 15460f, 450f, PlatformAsset.Crystal)
        add(15610f, 465f, 15880f, 535f, PlatformAsset.GrassRound)
        add(16020f, 555f, 16450f, 625f, PlatformAsset.FlowerBridge, enemyX = 16240f)
        add(16600f, 630f, 17120f, 700f, PlatformAsset.GrassLong, enemyX = 16870f)

        add(17280f, 560f, 17560f, 620f, PlatformAsset.Cloud)
        add(17710f, 470f, 17990f, 540f, PlatformAsset.Crystal, enemyX = 17850f)
        add(18130f, 535f, 18430f, 605f, PlatformAsset.GrassRound)
        add(18590f, 440f, 18850f, 500f, PlatformAsset.Cloud)
        add(18990f, 565f, 19410f, 635f, PlatformAsset.FlowerBridge, enemyX = 19200f)
        add(19580f, 650f, 20140f, 720f, PlatformAsset.GrassLong)

        add(20290f, 610f, 20560f, 670f, PlatformAsset.GrassShort)
        add(20710f, 520f, 20980f, 580f, PlatformAsset.Cloud, enemyX = 20850f)
        add(21130f, 430f, 21420f, 500f, PlatformAsset.Crystal)
        add(21570f, 365f, 21830f, 425f, PlatformAsset.Cloud)
        add(21980f, 450f, 22270f, 520f, PlatformAsset.GrassRound, enemyX = 22130f)
        add(22420f, 560f, 22830f, 630f, PlatformAsset.FlowerBridge)
        add(22990f, 650f, 23590f, 720f, PlatformAsset.GrassLong, enemyX = 23280f)

        add(23740f, 590f, 24020f, 650f, PlatformAsset.GrassShort)
        add(24180f, 515f, 24420f, 575f, PlatformAsset.Cloud)
        add(24570f, 610f, 24900f, 680f, PlatformAsset.GrassRound, enemyX = 24740f)
        add(25050f, 520f, 25420f, 590f, PlatformAsset.FlowerBridge)
        add(25580f, 425f, 25860f, 495f, PlatformAsset.Crystal, enemyX = 25720f)
        add(26000f, 490f, 26280f, 550f, PlatformAsset.Cloud)
        add(26420f, 650f, 27000f, 720f, PlatformAsset.GrassLong)

        add(27150f, 580f, 27440f, 640f, PlatformAsset.GrassShort, enemyX = 27300f)
        add(27590f, 480f, 27870f, 540f, PlatformAsset.Cloud)
        add(28020f, 390f, 28310f, 460f, PlatformAsset.Crystal)
        add(28480f, 470f, 28770f, 540f, PlatformAsset.GrassRound, enemyX = 28630f)
        add(28930f, 565f, 29340f, 635f, PlatformAsset.FlowerBridge)
        add(29510f, 625f, 30060f, 695f, PlatformAsset.GrassLong, enemyX = 29800f)

        add(30220f, 560f, 30520f, 620f, PlatformAsset.Cloud)
        add(30690f, 455f, 30970f, 525f, PlatformAsset.Crystal, enemyX = 30840f)
        add(31140f, 545f, 31440f, 615f, PlatformAsset.GrassRound)
        add(31610f, 485f, 31990f, 555f, PlatformAsset.FlowerBridge, enemyX = 31810f)
        add(32160f, 595f, 32510f, 665f, PlatformAsset.GrassLong)
        add(32680f, 520f, 32960f, 580f, PlatformAsset.Cloud)
        add(33100f, 560f, 33320f, 620f, PlatformAsset.Cloud)
        add(worldWidth - 580f, 650f, worldWidth, 720f, PlatformAsset.GrassLong)

        return platforms
    }

    private fun PlatformAsset.bitmap(): Bitmap {
        return when (this) {
            PlatformAsset.GrassShort -> assets.grassShort
            PlatformAsset.GrassLong -> assets.grassLong
            PlatformAsset.GrassRound -> assets.grassRound
            PlatformAsset.FlowerBridge -> assets.flowerBridge
            PlatformAsset.Crystal -> assets.crystal
            PlatformAsset.Cloud -> assets.cloud
        }
    }

    private fun liftedY(y: Float): Float = y - playfieldLift

    private fun liftedRect(left: Float, top: Float, right: Float, bottom: Float): RectF {
        return RectF(left, liftedY(top), right, liftedY(bottom))
    }

    private fun playerBody(): RectF = RectF(princessX - 32f, princessY - 126f, princessX + 32f, princessY - 10f)

    private fun viewportWidth(): Float = if (height == 0) worldWidth else width / worldScale()

    private fun worldScale(): Float = if (height == 0) 1f else height / worldHeight

    private fun timingScore(): Int = max(0f, (levelTimeLimit - elapsed) / 10f).roundToInt()

    override fun onDetachedFromWindow() {
        pause()
        assets.release()
        super.onDetachedFromWindow()
    }

    private data class Platform(val rect: RectF, val bitmap: Bitmap)

    private data class PlatformBlueprint(
        val left: Float,
        val top: Float,
        val right: Float,
        val bottom: Float,
        val asset: PlatformAsset,
        val enemyX: Float?,
    )

    private enum class PlatformAsset {
        GrassShort,
        GrassLong,
        GrassRound,
        FlowerBridge,
        Crystal,
        Cloud,
    }

    private data class Control(
        val name: String,
        val bitmap: Bitmap,
        val rect: RectF = RectF(),
        var pressed: Boolean = false,
    )

    private data class Projectile(var x: Float, var y: Float, val vx: Float) {
        fun rect(): RectF = RectF(x - 22f, y - 22f, x + 22f, y + 22f)
        fun drawRect(): RectF = RectF(x - 30f, y - 30f, x + 30f, y + 30f)
    }

    private data class TeacupEnemy(
        var x: Float,
        val y: Float,
        val platformIndex: Int,
        var direction: Int = -1,
        var health: Int = 2,
        var hitTimer: Float = 0f,
        var destroyed: Boolean = false,
        var destroyedTimer: Float = 0f,
    ) {
        fun body(): RectF = RectF(x - 38f, y - 78f, x + 38f, y - 6f)
    }

    private enum class GameState {
        Starting,
        Playing,
        Won,
        Lost,
    }
}
