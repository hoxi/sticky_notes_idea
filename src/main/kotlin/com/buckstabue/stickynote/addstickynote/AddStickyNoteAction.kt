package com.buckstabue.stickynote.addstickynote

import com.buckstabue.stickynote.AppInjector
import com.buckstabue.stickynote.FileBoundStickyNote
import com.buckstabue.stickynote.NonBoundStickyNote
import com.buckstabue.stickynote.StickyNote
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileEditor.FileDocumentManager
import kotlinx.coroutines.runBlocking
import org.apache.log4j.Level
import javax.swing.JOptionPane

class AddStickyNoteAction : AnAction() {
    companion object {
        private val logger = Logger.getInstance(AddStickyNoteAction::class.java).also {
            it.setLevel(Level.DEBUG)
        }
    }

    override fun actionPerformed(event: AnActionEvent) = runBlocking {
        logger.debug("new event from place = ${event.place}")
        val project = event.project
        if (project == null) {
            logger.error("Project is null")
            return@runBlocking
        }
        val stickyNote = createStickyNote(event)
        if (stickyNote == null) {
            logger.debug("Sticky note creation cancelled")
            return@runBlocking
        }
        val stickyNoteInteractor = AppInjector.getProjectComponent(project).stickyNoteInteractor()
        stickyNoteInteractor.addStickyNote(stickyNote)
        logger.debug("Sticky note successfully added $stickyNote")
    }

    private fun createStickyNote(event: AnActionEvent): StickyNote? {
        val description = askUserToEnterStickyNoteDescription()

        @Suppress("FoldInitializerAndIfToElvis")
        if (description == null) {
            logger.debug("User cancelled sticky note description input")
            // user cancelled
            return null
        }
        val editorCaretLocation = extractEditorCaretLocation(event)
        return if (editorCaretLocation == null) {
            NonBoundStickyNote(
                description = description
            )
        } else {
            FileBoundStickyNote(
                fileUrl = editorCaretLocation.fileUrl,
                lineNumber = editorCaretLocation.lineNumber,
                description = description,
                isDone = false
            )
        }
    }

    private fun extractEditorCaretLocation(event: AnActionEvent): EditorCaretLocation? {
        val editor = CommonDataKeys.EDITOR.getData(event.dataContext)
        if (editor == null) {
            logger.debug("Could not extract editor from event")
            return null
        }
        val currentDocument = editor.document
        val currentLineNumber = editor.caretModel.logicalPosition.line
        val currentFile = FileDocumentManager.getInstance()
            .getFile(currentDocument)
        if (currentFile == null) {
            logger.error("Could not extract virtual file from document")
            return null
        }
        return EditorCaretLocation(
            fileUrl = currentFile.url,
            lineNumber = currentLineNumber
        )
    }

    private fun askUserToEnterStickyNoteDescription(): String? {
        return JOptionPane.showInputDialog(null, "Description:", "New Sticky Note", JOptionPane.QUESTION_MESSAGE)
    }

    private data class EditorCaretLocation(
        val fileUrl: String,
        val lineNumber: Int
    )
}
