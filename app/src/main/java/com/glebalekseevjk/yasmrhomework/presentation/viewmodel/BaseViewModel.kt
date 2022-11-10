package com.glebalekseevjk.yasmrhomework.presentation.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.glebalekseevjk.yasmrhomework.domain.entity.TodoListViewState.Companion.OK
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

abstract class BaseViewModel(application: Application) : AndroidViewModel(application) {
    protected val _errorHandler = MutableStateFlow(OK)
    val errorHandler: StateFlow<Int>
        get() = _errorHandler
    abstract val coroutineExceptionHandler: CoroutineExceptionHandler
    protected fun CoroutineScope.launchWithExceptionHandler(block: suspend CoroutineScope.() -> Unit): Job {
        return this.launch(coroutineExceptionHandler) {
            block()
        }
    }

    protected fun <T> runBlockingWithExceptionHandler(block: suspend CoroutineScope.() -> T): T {
        return runBlocking(coroutineExceptionHandler) {
            block()
        }
    }
}