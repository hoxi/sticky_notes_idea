package com.buckstabue.stickynote.idea

import com.buckstabue.stickynote.FileBoundStickyNote
import com.buckstabue.stickynote.StickyNote
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.ex.MarkupModelEx
import com.intellij.openapi.editor.impl.DocumentMarkupModel
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.openapi.editor.markup.HighlighterLayer
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.util.PlatformIcons
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import javax.swing.Icon

class StickyNotesGutterManager(
    private val project: Project
) {

    fun onStickyNotesChanged(stickyNotes: List<StickyNote>) {
        MainScope().launch {
            stickyNotes.filter { !it.isDone }
                .filterIsInstance<FileBoundStickyNote>()
                .forEach {
                    showStickerNoteGutterIcons(it)
                }
        }
    }

    private fun showStickerNoteGutterIcons(stickyNote: FileBoundStickyNote) {
        val cachedDocument = getCachedDocument(stickyNote) ?: return
        val markup = DocumentMarkupModel.forDocument(cachedDocument, project, true) as MarkupModelEx
        val highlighter =
            markup.addPersistentLineHighlighter(
                stickyNote.fileLocation.lineNumber,
                HighlighterLayer.ERROR + 1,
                null
            ) ?: return
        highlighter.gutterIconRenderer =
            StickyNoteGutterIconRenderer(stickyNote)
    }

    private fun getCachedDocument(stickyNote: FileBoundStickyNote): Document? {
        val file = (stickyNote.fileLocation as IdeaFileLocation).fileDescriptor.file
        return FileDocumentManager.getInstance().getCachedDocument(file)
    }

    private class StickyNoteGutterIconRenderer(
        private val stickyNote: FileBoundStickyNote
    ) : GutterIconRenderer() {

        override fun getIcon(): Icon {
            return PlatformIcons.CHECK_ICON
        }

        override fun getTooltipText(): String? {
            return stickyNote.description
        }

        override fun hashCode(): Int {
            return stickyNote.hashCode()
        }

        override fun equals(other: Any?): Boolean {
            return other is StickyNoteGutterIconRenderer && other.stickyNote == this.stickyNote
        }
    }
}
