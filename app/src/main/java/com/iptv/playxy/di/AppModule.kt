package com.iptv.playxy.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import com.iptv.playxy.data.db.PlayxyDatabase
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import android.content.pm.ApplicationInfo
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit
import javax.inject.Singleton
import com.iptv.playxy.data.repository.PreferencesManager

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    
    @Provides
    @Singleton
    fun provideMoshi(): Moshi {
        return Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()
    }
    
    @Provides
    @Singleton
    fun provideOkHttpClient(@ApplicationContext context: Context): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            // Reducir ruido en Logcat: sólo cabeceras en debug, nada en release.
            val isDebuggable = (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
            level = if (isDebuggable) {
                HttpLoggingInterceptor.Level.BASIC
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }
        
        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }
    

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): PlayxyDatabase {
        return Room.databaseBuilder(
            context,
            PlayxyDatabase::class.java,
            "playxy_database"
        )
        .setJournalMode(RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING)
        // Serializar consultas para evitar cursores activos mientras se reemplazan tablas grandes.
        .setQueryExecutor(java.util.concurrent.Executors.newSingleThreadExecutor())
        .fallbackToDestructiveMigration(true)
        .build()
    }
    
    @Provides
    @Singleton
    fun provideFavoriteChannelDao(database: PlayxyDatabase) = database.favoriteChannelDao()
    
    @Provides
    @Singleton
    fun provideRecentChannelDao(database: PlayxyDatabase) = database.recentChannelDao()

    @Provides
    @Singleton
    fun provideFavoriteVodDao(database: PlayxyDatabase) = database.favoriteVodDao()

    @Provides
    @Singleton
    fun provideRecentVodDao(database: PlayxyDatabase) = database.recentVodDao()

    @Provides
    @Singleton
    fun provideFavoriteSeriesDao(database: PlayxyDatabase) = database.favoriteSeriesDao()

    @Provides
    @Singleton
    fun provideRecentSeriesDao(database: PlayxyDatabase) = database.recentSeriesDao()

    @Provides
    @Singleton
    fun provideMovieProgressDao(database: PlayxyDatabase) = database.movieProgressDao()

    @Provides
    @Singleton
    fun provideSeriesProgressDao(database: PlayxyDatabase) = database.seriesProgressDao()

    @Provides
    @Singleton
    fun provideEpisodeProgressDao(database: PlayxyDatabase) = database.episodeProgressDao()

    @Provides
    @Singleton
    fun providePreferencesManager(@ApplicationContext context: Context) = PreferencesManager(context)
}
