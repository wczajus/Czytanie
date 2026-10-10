package pl.dev404.czytanie.core.data.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room.Room
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import pl.dev404.czytanie.core.data.database.ChildProfileDao
import pl.dev404.czytanie.core.data.database.CzytanieDatabase
import pl.dev404.czytanie.core.data.settings.DataStoreSettingsRepository
import pl.dev404.czytanie.core.domain.settings.SettingsRepository

@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {

    @Binds
    abstract fun bindSettingsRepository(impl: DataStoreSettingsRepository): SettingsRepository

    companion object {
        @Provides
        @Singleton
        fun provideSettingsDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
            PreferenceDataStoreFactory.create { context.preferencesDataStoreFile("reading_settings") }

        @Provides
        @Singleton
        fun provideDatabase(@ApplicationContext context: Context): CzytanieDatabase =
            Room.databaseBuilder(context, CzytanieDatabase::class.java, CzytanieDatabase.NAME).build()

        @Provides
        fun provideChildProfileDao(database: CzytanieDatabase): ChildProfileDao = database.childProfileDao()
    }
}
