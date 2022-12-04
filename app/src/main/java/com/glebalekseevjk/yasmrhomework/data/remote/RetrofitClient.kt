package com.glebalekseevjk.yasmrhomework.data.remote

import android.util.Log
import com.glebalekseevjk.yasmrhomework.data.local.dao.TodoItemDao
import com.glebalekseevjk.yasmrhomework.data.local.model.TodoItemDbModel
import com.glebalekseevjk.yasmrhomework.data.preferences.SharedPreferencesRevisionStorage
import com.glebalekseevjk.yasmrhomework.data.preferences.SharedPreferencesSynchronizedStorage
import com.glebalekseevjk.yasmrhomework.data.preferences.SharedPreferencesTokenStorage
import com.glebalekseevjk.yasmrhomework.domain.entity.TodoItem
import com.glebalekseevjk.yasmrhomework.domain.mapper.Mapper
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class RetrofitClient {
    companion object{
        const val BASE_URL = "https://192.168.0.102/"
    }
    private var retrofit: Retrofit.Builder? = null
    private var basicTodoApiClient: OkHttpClient? = null
    private var advancedTodoApiClient: OkHttpClient? = null
    private var authApiClient: OkHttpClient? = null

    private lateinit var todoApiWithBasicClient: TodoService

    lateinit var todoApi: TodoService
    lateinit var authApi: AuthService

    fun init(
        tokenStorage: SharedPreferencesTokenStorage,
        revisionStorage: SharedPreferencesRevisionStorage,
        synchronizedStorage: SharedPreferencesSynchronizedStorage,
        todoItemDao: TodoItemDao,
        mapper: Mapper<TodoItem,TodoItemDbModel>
    ) {
        retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())

        authApiClient = OkHttpClient.Builder()
            .connectTimeout(200,TimeUnit.MILLISECONDS)
            .build()
        authApi = retrofit!!
            .client(authApiClient)
            .build()
            .create(AuthService::class.java)

        basicTodoApiClient = OkHttpClient.Builder()
            .connectTimeout(200,TimeUnit.MILLISECONDS)
            .addNetworkInterceptor(
                HttpLoggingInterceptor {
                    Log.d("Network", it)
                }
                    .setLevel(HttpLoggingInterceptor.Level.BODY)
            )
            .addInterceptor(
                AuthorizationFailedInterceptor(
                    tokenStorage,
                    revisionStorage,
                    synchronizedStorage,
                    authApi,
                    todoItemDao
                )
            )
            .addNetworkInterceptor(AuthorizationInterceptor(tokenStorage))
            .addNetworkInterceptor(RevisionInterceptor(revisionStorage))
            .build()
        todoApiWithBasicClient = retrofit!!
            .client(basicTodoApiClient!!)
            .build()
            .create(TodoService::class.java)

        advancedTodoApiClient = OkHttpClient.Builder()
            .connectTimeout(200,TimeUnit.MILLISECONDS)
            .addNetworkInterceptor(
                HttpLoggingInterceptor {
                    Log.d("Network", it)
                }
                    .setLevel(HttpLoggingInterceptor.Level.BODY)
            )
            .addNetworkInterceptor(
                SynchronizedInterceptor(
                    synchronizedStorage,
                    revisionStorage,
                    todoApiWithBasicClient,
                    todoItemDao,
                    mapper
                )
            )
            .addInterceptor(RevisionFailedInterceptor(revisionStorage, todoApiWithBasicClient))
            .addInterceptor(
                AuthorizationFailedInterceptor(
                    tokenStorage,
                    revisionStorage,
                    synchronizedStorage,
                    authApi,
                    todoItemDao
                )
            )
            .addNetworkInterceptor(AuthorizationInterceptor(tokenStorage))
            .addNetworkInterceptor(RevisionInterceptor(revisionStorage))
            .build()

        todoApi = retrofit!!
            .client(advancedTodoApiClient!!)
            .build()
            .create(TodoService::class.java)
    }
}