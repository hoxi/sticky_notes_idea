package com.buckstabue.stickynotes.idea.toolwindow

import com.buckstabue.stickynotes.base.di.AppInjector
import com.buckstabue.stickynotes.idea.toolwindow.activenote.ActiveNoteWindow
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory

class StickyNoteToolWindowFactory : ToolWindowFactory, DumbAware {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val toolWindowComponent = AppInjector.getProjectComponent(project)
            .plusStickyNoteToolWindowComponent()

        val activeNoteWindow = ActiveNoteWindow(toolWindowComponent)

        val contentFactory = ContentFactory.SERVICE.getInstance()
        val content = contentFactory.createContent(activeNoteWindow.getContent(), "", false)
        toolWindow.contentManager.addContent(content)

        toolWindow.setSplitMode(true, null)
        activeNoteWindow.onAttach()
    }
}
