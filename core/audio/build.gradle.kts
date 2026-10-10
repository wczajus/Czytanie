// SpeechPlayer implementations (SoundPool for phonemes, TextToSpeech for whole words) arrive in E4.
plugins {
    alias(libs.plugins.czytanie.android.library)
    alias(libs.plugins.czytanie.android.hilt)
}

dependencies {
    api(projects.core.domain)
}
