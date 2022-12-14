package com.glebalekseevjk.yasmrhomework.di

import com.glebalekseevjk.yasmrhomework.di.module.ViewModelModule
import com.glebalekseevjk.yasmrhomework.di.scope.TodoFragmentSubcomponentScope
import com.glebalekseevjk.yasmrhomework.ui.fragment.TodoFragment
import dagger.Subcomponent

@TodoFragmentSubcomponentScope
@Subcomponent
interface TodoFragmentSubcomponent {
    fun inject(todoFragment: TodoFragment)
}