package pl.dev404.czytanie.core.data.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.time.Clock
import javax.inject.Singleton
import pl.dev404.czytanie.core.data.content.AssetDictionarySource
import pl.dev404.czytanie.core.data.content.DictionarySource
import pl.dev404.czytanie.core.data.content.RoomDictionaryRepository
import pl.dev404.czytanie.core.data.database.AttemptDao
import pl.dev404.czytanie.core.data.database.ChildProfileDao
import pl.dev404.czytanie.core.data.database.CzytanieDatabase
import pl.dev404.czytanie.core.data.database.DictionaryDao
import pl.dev404.czytanie.core.data.progress.RoomProgressRepository
import pl.dev404.czytanie.core.data.settings.DataStoreSettingsRepository
import pl.dev404.czytanie.core.domain.content.DictionaryRepository
import pl.dev404.czytanie.core.domain.progress.ProgressRepository
import pl.dev404.czytanie.core.domain.settings.SettingsRepository

@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {

    @Binds
    abstract fun bindSettingsRepository(impl: DataStoreSettingsRepository): SettingsRepository

    @Binds
    abstract fun bindDictionaryRepository(impl: RoomDictionaryRepository): DictionaryRepository

    @Binds
    abstract fun bindProgressRepository(impl: RoomProgressRepository): ProgressRepository

    @Binds
    abstract fun bindDictionarySource(impl: AssetDictionarySource): DictionarySource

    companion object {
        @Provides
        fun provideClock(): Clock = Clock.systemDefaultZone()

        @Provides
        @Singleton
        fun provideSettingsDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
            PreferenceDataStoreFactory.create { context.preferencesDataStoreFile("reading_settings") }

        @Provides
        @Singleton
        fun provideDatabase(@ApplicationContext context: Context): CzytanieDatabase = CzytanieDatabase.build(context)

        @Provides
        fun provideChildProfileDao(database: CzytanieDatabase): ChildProfileDao = database.childProfileDao()

        @Provides
        fun provideDictionaryDao(database: CzytanieDatabase): DictionaryDao = database.dictionaryDao()

        @Provides
        fun provideAttemptDao(database: CzytanieDatabase): AttemptDao = database.attemptDao()
    }
}
