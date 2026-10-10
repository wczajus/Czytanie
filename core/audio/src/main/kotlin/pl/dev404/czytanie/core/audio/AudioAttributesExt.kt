package pl.dev404.czytanie.core.audio

import android.media.AudioAttributes

/** Spoken learning content, played as media so it follows the media volume the child can hear. */
internal val SpeechAudioAttributes: AudioAttributes = AudioAttributes.Builder()
    .setUsage(AudioAttributes.USAGE_MEDIA)
    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
    .build()
