package com.buckstabue.stickynotes.base.di.project

import com.buckstabue.stickynotes.StickyNoteInteractor
import com.buckstabue.stickynotes.idea.stickynotelist.StickyNoteListDialog
import com.buckstabue.stickynotes.idea.toolwindow.StickyNoteToolWindowComponent
import com.buckstabue.stickynotes.vcs.VcsService
import com.intellij.openapi.project.Project
import dagger.BindsInstance
import dagger.Subcomponent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.ObsoleteCoroutinesApi

@PerProject
@Subcomponent(modules = [ProjectModule::class])
@ObsoleteCoroutinesApi
@ExperimentalCoroutinesApi
interface ProjectComponent {
    fun stickyNoteInteractor(): StickyNoteInteractor
    fun plusStickyNoteToolWindowComponent(): StickyNoteToolWindowComponent
    fun projectScope(): ProjectScope

    fun inject(stickyNoteListDialog: StickyNoteListDialog)
    fun vcsService(): VcsService

    @Subcomponent.Factory
    interface Factory {
        fun create(@BindsInstance project: Project): ProjectComponent
    }
}


