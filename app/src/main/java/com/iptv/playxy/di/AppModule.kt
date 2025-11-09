package com.iptv.playxy.di

import android.content.Context
import androidx.room.Room
import com.iptv.playxy.data.db.PlayxyDatabase
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

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
    fun provideOkHttpClient(): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
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
        .fallbackToDestructiveMigration(true)
        .build()
    }
    
    @Provides
    @Singleton
    fun provideFavoriteChannelDao(database: PlayxyDatabase) = database.favoriteChannelDao()
    
    @Provides
    @Singleton
    fun provideRecentChannelDao(database: PlayxyDatabase) = database.recentChannelDao()
}
