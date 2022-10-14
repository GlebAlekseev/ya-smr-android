package com.glebalekseevjk.yasmrhomework.data.remote

import com.glebalekseevjk.yasmrhomework.data.local.dao.TodoItemDao
import com.glebalekseevjk.yasmrhomework.data.remote.model.RefreshToken
import com.glebalekseevjk.yasmrhomework.domain.entity.Revision
import com.glebalekseevjk.yasmrhomework.domain.features.revision.RevisionStorage
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import retrofit2.awaitResponse
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

// Запросить новые данные со свежей ревизией, если не совпадает.

class RevisionFailedInterceptor(
    private val revisionStorage: RevisionStorage,
    private val todoService: TodoService,
    private val todoItemDao: TodoItemDao
): Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalResponse = chain.proceed(chain.request())
        return originalResponse
            .takeIf { it.code != 400 }
            ?: handleBadRevisionResponse(chain, originalResponse)
    }
    private fun handleBadRevisionResponse(
        chain: Interceptor.Chain,
        originalResponse: Response
    ): Response {
        val latch = getLatch()
        return when {
            latch != null && latch.count > 0 -> handleRevisionIsUpdating(chain, latch)
                ?: originalResponse
            else -> handleRevisionNeedRefresh(chain) ?: originalResponse
        }
    }

    private fun handleRevisionIsUpdating(
        chain: Interceptor.Chain,
        latch: CountDownLatch,
    ): Response? {
        return if (latch.await(REQUEST_TIMEOUT, TimeUnit.SECONDS)) {
            updateRevisionAndProceedChain(chain)
        } else {
            null
        }
    }

    private fun handleRevisionNeedRefresh(
        chain: Interceptor.Chain
    ): Response? {
        return if (refreshRevision()) {
            updateRevisionAndProceedChain(chain)
        } else {
            null
        }
    }

    private fun updateRevisionAndProceedChain(
        chain: Interceptor.Chain
    ): Response {
        val newRequest = updateOriginalCallWithNewRevision(chain.request())
        return chain.proceed(newRequest)
    }

    private fun updateOriginalCallWithNewRevision(request: Request): Request {
        return revisionStorage.getRevision()?.let { newRevision ->
            request
                .newBuilder()
                .header("X-Last-Known-Revision", newRevision.revision.toString())
                .build()
        } ?: request
    }

    private fun refreshRevision(): Boolean {
        initLatch()

        val revisionRefreshed = runBlocking {
            runCatching {
                todoService.getTodoList().awaitResponse()
            }.getOrNull()?.let { todoListResponse ->
                if (todoListResponse.code() == 200){
                    val todoList = todoListResponse.body()?.list ?: return@let false
                    val revision = todoListResponse.body()?.revision ?: return@let false
                    revisionStorage.setRevision(Revision(revision))
                    // set list in db
                    true
                }else{
                    false
                }
            } ?: false
        }

        getLatch()?.countDown()
        return revisionRefreshed
    }

    companion object{
        private const val REQUEST_TIMEOUT = 30L
        private var countDownLatch: CountDownLatch? = null
        @Synchronized
        fun initLatch() {
            countDownLatch = CountDownLatch(1)
        }
        @Synchronized
        fun getLatch() = countDownLatch
    }
}