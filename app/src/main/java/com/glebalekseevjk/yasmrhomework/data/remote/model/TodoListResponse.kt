package com.glebalekseevjk.yasmrhomework.data.remote.model

import com.glebalekseevjk.yasmrhomework.domain.entity.TodoItem

data class TodoListResponse(
    val list: List<TodoItem>,
    val revision: Long,
    val message: String,
)