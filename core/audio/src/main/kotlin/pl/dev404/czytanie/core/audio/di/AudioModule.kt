package pl.dev404.czytanie.core.audio.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import pl.dev404.czytanie.core.audio.AndroidSpeechPlayer
import pl.dev404.czytanie.core.audio.ClipPlayer
import pl.dev404.czytanie.core.audio.MediaPlayerRecordingPlayer
import pl.dev404.czytanie.core.audio.RecordingPlayer
import pl.dev404.czytanie.core.audio.SoundPoolClipPlayer
import pl.dev404.czytanie.core.audio.TtsWordSpeech
import pl.dev404.czytanie.core.audio.WordSpeech
import pl.dev404.czytanie.core.domain.audio.SpeechPlayer

@Module
@InstallIn(SingletonComponent::class)
abstract class AudioModule {
    @Binds
    abstract fun bindSpeechPlayer(impl: AndroidSpeechPlayer): SpeechPlayer

    @Binds
    abstract fun bindClipPlayer(impl: SoundPoolClipPlayer): ClipPlayer

    @Binds
    abstract fun bindRecordingPlayer(impl: MediaPlayerRecordingPlayer): RecordingPlayer

    @Binds
    abstract fun bindWordSpeech(impl: TtsWordSpeech): WordSpeech
}
