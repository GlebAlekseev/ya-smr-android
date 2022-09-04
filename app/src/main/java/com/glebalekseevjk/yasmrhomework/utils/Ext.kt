package com.glebalekseevjk.yasmrhomework.utils

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import kotlin.reflect.KFunction0

fun <T> LifecycleOwner.observe(liveData: LiveData<T>, action: () -> Unit) {
    liveData.observe(this, Observer { it?.let { t -> action() } })
}