package com.buckstabue.stickynotes.idea.stickynotelist.contextmenu

import com.buckstabue.stickynotes.base.di.AppInjector
import com.buckstabue.stickynotes.idea.MainScope
import com.buckstabue.stickynotes.idea.fullyClearSelection
import com.buckstabue.stickynotes.idea.stickynotelist.StickyNoteListAnalytics
import com.buckstabue.stickynotes.idea.stickynotelist.panel.StickyNoteViewModel
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import kotlinx.coroutines.launch
import javax.swing.JList

@Suppress("NOTHING_TO_INLINE")
class SetStickyNoteActiveAction(
    private val stickyNoteJList: JList<StickyNoteViewModel>,
    private val analytics: StickyNoteListAnalytics
) : AnAction("Set Active"), DumbAware {
    companion object {
        private val logger = Logger.getInstance(SetStickyNoteActiveAction::class.java)
    }

    override fun actionPerformed(e: AnActionEvent) {
        val selectedValues = stickyNoteJList.selectedValuesList
        if (selectedValues.isEmpty()) {
            logger.error("There is no selection")
            return
        }
        if (selectedValues.size > 1) {
            logger.error("Cannot set multiply sticky notes active")
            return
        }

        val project = e.project
        if (project == null) {
            logger.error("project is null")
            return
        }
        val projectComponent = AppInjector.getProjectComponent(project)
        val stickyNoteInteractor = projectComponent.stickyNoteInteractor()
        val projectScope = projectComponent.projectScope()

        val selectedStickyNote = selectedValues.first().stickyNote
        projectScope.launch {
            stickyNoteInteractor.setStickyNoteActive(selectedStickyNote)
            MainScope().launch {
                if (stickyNoteJList.model.size > 0) {
                    val firstStickyNote = stickyNoteJList.model.getElementAt(0).stickyNote
                    if (firstStickyNote.isArchived) { // if editing an archived list
                        stickyNoteJList.fullyClearSelection()
                    } else { // if editing a backlog list
                        stickyNoteJList.selectedIndex = 0
                    }
                }
            }
        }
        analytics.setStickyNoteActive()
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = shouldActionBeEnabled(e.project)
    }

    private inline fun shouldActionBeEnabled(project: Project?): Boolean {
        if (project == null) {
            return false
        }
        if (stickyNoteJList.selectedValuesList.size != 1) { // multiple selection or no selection
            return false
        }
        val vcsService = AppInjector.getProjectComponent(project).vcsService()
        val currentBranchName = vcsService.getCurrentBranchName()
        val selectedStickyNote = stickyNoteJList.selectedValue.stickyNote
        return selectedStickyNote.isVisibleInBranch(currentBranchName)
                && !selectedStickyNote.isActive
    }
}
