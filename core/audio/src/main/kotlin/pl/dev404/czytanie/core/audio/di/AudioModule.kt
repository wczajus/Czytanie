package pl.dev404.czytanie.core.audio.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import pl.dev404.czytanie.core.audio.SilentSpeechPlayer
import pl.dev404.czytanie.core.domain.audio.SpeechPlayer

@Module
@InstallIn(SingletonComponent::class)
abstract class AudioModule {
    @Binds
    abstract fun bindSpeechPlayer(impl: SilentSpeechPlayer): SpeechPlayer
}
