package com.buckstabue.stickynote.toolwindow.stickynotelist.contextmenu

import com.buckstabue.stickynote.AppInjector
import com.buckstabue.stickynote.toolwindow.stickynotelist.StickyNoteViewModel
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.diagnostic.Logger
import kotlinx.coroutines.launch
import javax.swing.JList

class ArchiveStickyNoteAction(
    private val stickyNoteJList: JList<StickyNoteViewModel>
) : AnAction("Archive") {
    companion object {
        private val logger = Logger.getInstance(ArchiveStickyNoteAction::class.java)
    }

    override fun actionPerformed(e: AnActionEvent) {
        val selectedStickyNotes = stickyNoteJList.selectedValuesList.map { it.stickyNote }
        if (selectedStickyNotes.isEmpty()) {
            logger.error("Sticky Notes list is empty, nothing to archive")
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

        projectScope.launch {
            stickyNoteInteractor.archiveStickyNotes(selectedStickyNotes)
        }
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = stickyNoteJList.selectedValuesList
            .any { !it.stickyNote.isArchived }
    }
}
