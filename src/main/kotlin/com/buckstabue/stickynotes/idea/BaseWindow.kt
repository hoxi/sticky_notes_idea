package com.buckstabue.stickynotes.idea

import com.buckstabue.stickynotes.base.BasePresenter
import com.buckstabue.stickynotes.base.BaseView
import com.buckstabue.stickynotes.idea.toolwindow.di.StickyNoteToolWindowComponent
import javax.swing.JComponent

abstract class BaseWindow<VIEW : BaseView, PRESENTER : BasePresenter<VIEW>> {
    protected abstract val presenter: PRESENTER

    abstract val routingTag: String

    open fun onCreate(toolWindowComponent: StickyNoteToolWindowComponent) {
    }

    fun onAttach() {
        @Suppress("UNCHECKED_CAST")
        presenter.attachView(this as VIEW)
    }

    fun onDetach() {
        presenter.detachView()
    }

    abstract fun getContent(): JComponent
}
