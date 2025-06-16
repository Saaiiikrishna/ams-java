package com.example.entityadmin.di

import android.content.Context
import com.example.entityadmin.data.TokenManager
import com.example.entityadmin.data.DynamicApiService
import com.example.entityadmin.data.ServerManager
import com.example.entityadmin.data.api.ApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideHttpLoggingInterceptor(): HttpLoggingInterceptor {
        return HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(
        loggingInterceptor: HttpLoggingInterceptor,
        tokenManager: TokenManager
    ): OkHttpClient {
        val authInterceptor = Interceptor { chain ->
            val token = tokenManager.getToken()
            val request = if (token != null) {
                chain.request().newBuilder()
                    .addHeader("Authorization", "Bearer $token")
                    .build()
            } else chain.request()
            chain.proceed(request)
        }

        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .addInterceptor(authInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    /**
     * Provide DynamicApiService that handles server discovery and API service creation
     */
    @Provides
    @Singleton
    fun provideDynamicApiService(
        okHttpClient: OkHttpClient,
        @ApplicationContext context: Context
    ): DynamicApiService {
        return DynamicApiService(okHttpClient, context)
    }

    /**
     * Provide ServerManager that manages server discovery
     */
    @Provides
    @Singleton
    fun provideServerManager(
        @ApplicationContext context: Context,
        dynamicApiService: DynamicApiService
    ): ServerManager {
        return ServerManager(context, dynamicApiService)
    }

    /**
     * Provide ApiService through DynamicApiService
     */
    @Provides
    @Singleton
    fun provideApiService(dynamicApiService: DynamicApiService): ApiService {
        return dynamicApiService.getApiService()
    }
}
