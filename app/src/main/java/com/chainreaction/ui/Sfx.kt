package com.chainreaction.ui

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import com.chainreaction.R

/*
 * The app's only noise: a short chain rattle on the menu buttons.
 *
 * SoundPool rather than MediaPlayer — the clip is tiny, it has to fire the instant a
 * finger lands, and taps can overlap. MediaPlayer would re-prepare each time and lag
 * behind the press.
 */

/** How many taps can overlap before the oldest is cut off. Two is plenty for a menu. */
private const val MAX_STREAMS = 2

class Sfx(context: Context) {

    private val pool = SoundPool.Builder()
        .setMaxStreams(MAX_STREAMS)
        .setAudioAttributes(
            AudioAttributes.Builder()
                // MEDIA, not SONIFICATION. Sonification routes to the system stream,
                // which the phone mutes outright on silent or vibrate — and a phone
                // that lives in a pocket on a course is on vibrate. It rattled on the
                // emulator, which never is, and nowhere else. Media follows the volume
                // rocker instead, which is the volume the in-app slider means.
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build(),
        )
        .build()

    private var chainsId = 0
    private var loaded = false

    init {
        pool.setOnLoadCompleteListener { _, _, status -> loaded = status == 0 }
        chainsId = pool.load(context, R.raw.chains, 1)
    }

    /**
     * Play the menu chain at [volume] (0f–1f). Silent at zero rather than played at
     * nothing, and a no-op until the clip finishes loading — a tap in the first
     * moments after launch makes no sound rather than blocking.
     */
    fun menuTap(volume: Float) {
        if (!loaded || volume <= 0f) return
        val v = volume.coerceIn(0f, 1f)
        pool.play(chainsId, v, v, 1, 0, 1f)
    }

    fun release() = pool.release()
}

/**
 * The player, reachable without threading it through every screen. Defaults to a
 * no-op so previews and tests don't need one.
 */
val LocalSfx = staticCompositionLocalOf<Sfx?> { null }

/** Creates a [Sfx] tied to the composition, released when it leaves. */
@Composable
fun rememberSfx(): Sfx {
    val context = LocalContext.current
    val sfx = remember { Sfx(context.applicationContext) }
    DisposableEffect(sfx) { onDispose { sfx.release() } }
    return sfx
}
