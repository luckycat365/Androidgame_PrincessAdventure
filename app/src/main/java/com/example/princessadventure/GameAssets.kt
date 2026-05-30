package com.example.princessadventure

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import java.io.IOException

class SpriteSequence(private val frames: List<Bitmap>) {
    fun frame(timeSeconds: Float, framesPerSecond: Float = 8f): Bitmap {
        val index = (timeSeconds * framesPerSecond).toInt().coerceAtLeast(0) % frames.size
        return frames[index]
    }
}

class GameAssets(private val context: Context) {
    private val bitmaps = mutableMapOf<String, Bitmap>()
    private val audioAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_GAME)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()
    private val soundPool = SoundPool.Builder()
        .setMaxStreams(5)
        .setAudioAttributes(audioAttributes)
        .build()
    private val sounds = mutableMapOf<String, Int>()
    private var musicPlayer: MediaPlayer? = null

    val startingView = bitmap("images/Starting View.png")
    val background = bitmap("images/PrincessStarAdventure/backgrounds/Level 1.png")
    val castle = bitmap("images/PrincessStarAdventure/castle/castle.png")
    val heart = bitmap("images/PrincessStarAdventure/ui/heart-health.png")
    val leftButton = bitmap("images/PrincessStarAdventure/ui/mobile-left-button.png")
    val rightButton = bitmap("images/PrincessStarAdventure/ui/mobile-right-button.png")
    val jumpButton = bitmap("images/PrincessStarAdventure/ui/mobile-jump-button.png")
    val attackButton = bitmap("images/PrincessStarAdventure/ui/mobile-attack-button.png")
    val starProjectile = bitmap("images/PrincessStarAdventure/projectiles/star/star-projectile.png")

    val princessIdle = sequence("images/PrincessStarAdventure/princess/standing", 6)
    val princessRun = sequence("images/PrincessStarAdventure/princess/running", 6)
    val princessJump = sequence("images/PrincessStarAdventure/princess/jumping", 6)
    val princessAttack = sequence("images/PrincessStarAdventure/princess/attacking", 6)
    val princessHurt = sequence("images/PrincessStarAdventure/princess/hurt", 1)

    val teacupWalk = sequence("images/PrincessStarAdventure/enemies/teacup-sentry/walking", 6)
    val teacupHit = bitmap("images/PrincessStarAdventure/enemies/teacup-sentry/hit/01.png")
    val teacupDestroyed = bitmap("images/PrincessStarAdventure/enemies/teacup-sentry/destroyed/01.png")

    val grassShort = bitmap("images/PrincessStarAdventure/platforms/level1/grass-short.png")
    val grassLong = bitmap("images/PrincessStarAdventure/platforms/level1/grass-long.png")
    val grassRound = bitmap("images/PrincessStarAdventure/platforms/level1/grass-round.png")
    val flowerBridge = bitmap("images/PrincessStarAdventure/platforms/level1/flower-bridge.png")
    val crystal = bitmap("images/PrincessStarAdventure/platforms/level1/crystal.png")
    val cloud = bitmap("images/PrincessStarAdventure/platforms/level1/cloud.png")

    init {
        loadSound("doubleJump", "sounds/PrincessStarAdventure/princess/princess double jump.wav")
        loadSound("starAttack", "sounds/PrincessStarAdventure/princess/star-attack.wav")
        loadSound("princessHurt", "sounds/PrincessStarAdventure/princess/hurt.wav")
        loadSound("teacupCrash", "sounds/PrincessStarAdventure/enemies/teacup/teacup-crash.wav")
        startMusic("music/ChasingLight.mp3")
    }

    fun playSound(name: String) {
        sounds[name]?.let { soundPool.play(it, 0.85f, 0.85f, 1, 0, 1f) }
    }

    fun pauseMusic() {
        musicPlayer?.pause()
    }

    fun resumeMusic() {
        musicPlayer?.start()
    }

    fun release() {
        musicPlayer?.release()
        musicPlayer = null
        soundPool.release()
        bitmaps.values.forEach { it.recycle() }
        bitmaps.clear()
    }

    private fun bitmap(path: String): Bitmap {
        return bitmaps.getOrPut(path) {
            try {
                context.assets.open(path).use(BitmapFactory::decodeStream)
            } catch (_: IOException) {
                placeholder()
            }
        }
    }

    private fun sequence(directory: String, count: Int): SpriteSequence {
        val frames = (1..count).map { index -> bitmap("$directory/${index.toString().padStart(2, '0')}.png") }
        return SpriteSequence(frames)
    }

    private fun loadSound(name: String, path: String) {
        try {
            context.assets.openFd(path).use { descriptor ->
                sounds[name] = soundPool.load(descriptor, 1)
            }
        } catch (_: IOException) {
            // Missing sound assets should not block first launch.
        }
    }

    private fun startMusic(path: String) {
        try {
            val descriptor = context.assets.openFd(path)
            musicPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build(),
                )
                setDataSource(descriptor.fileDescriptor, descriptor.startOffset, descriptor.length)
                isLooping = true
                setVolume(0.45f, 0.45f)
                prepare()
                start()
            }
            descriptor.close()
        } catch (_: IOException) {
            musicPlayer = null
        }
    }

    private fun placeholder(): Bitmap {
        val bitmap = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = Color.MAGENTA
        canvas.drawRect(0f, 0f, 64f, 64f, paint)
        paint.color = Color.WHITE
        paint.strokeWidth = 5f
        canvas.drawLine(0f, 0f, 64f, 64f, paint)
        canvas.drawLine(64f, 0f, 0f, 64f, paint)
        return bitmap
    }
}
