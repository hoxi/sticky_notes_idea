package com.buckstabue.stickynotes.idea

import com.buckstabue.stickynotes.FileBoundStickyNote
import com.buckstabue.stickynotes.StickyNote
import com.buckstabue.stickynotes.base.di.AppInjector
import com.buckstabue.stickynotes.base.di.project.PerProject
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.ex.MarkupModelEx
import com.intellij.openapi.editor.ex.RangeHighlighterEx
import com.intellij.openapi.editor.impl.DocumentMarkupModel
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.openapi.editor.markup.HighlighterLayer
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.IconLoader
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.swing.Icon

@PerProject
class StickyNotesGutterManager @Inject constructor(
    private val project: Project
) {
    companion object {
        private val GUTTER_ICON by lazy { IconLoader.getIcon("/note_gutter.svg") }
        private val logger = Logger.getInstance(StickyNotesGutterManager::class.java)
    }

    private val currentHighlighters = mutableMapOf<FileBoundStickyNote, RangeHighlighterEx>()

    fun onStickyNotesChanged(stickyNotes: List<StickyNote>) {
        MainScope().launch {
            val activeFileBoundStickyNotes = stickyNotes.filter { !it.isArchived }
                .filterIsInstance<FileBoundStickyNote>()
            val diff = StickyNotesDiff.calculate(
                oldNotes = currentHighlighters.keys,
                newNotes = activeFileBoundStickyNotes
            )
            diff.removedStickyNotes.forEach {
                currentHighlighters[it]?.dispose()
                currentHighlighters.remove(it)
            }
            diff.newStickyNotes.forEach { showStickerNoteGutterIcons(it) }
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
        currentHighlighters[stickyNote] = highlighter
    }

    private fun getCachedDocument(stickyNote: FileBoundStickyNote): Document? {
        val file = (stickyNote.fileLocation as IdeaFileLocation).fileDescriptor.file
        return FileDocumentManager.getInstance().getCachedDocument(file)
    }

    private class StickyNoteGutterIconRenderer(
        private val stickyNote: FileBoundStickyNote
    ) : GutterIconRenderer() {

        override fun getIcon(): Icon {
            return GUTTER_ICON
        }

        override fun getTooltipText(): String? {
            return stickyNote.description
        }

        override fun hashCode(): Int {
            return stickyNote.hashCode()
        }

        override fun getPopupMenuActions(): ActionGroup? {
            return DefaultActionGroup(
                EditStickyNoteFromGutterAction(stickyNote),
                SetStickyNoteActiveFromGutterAction(stickyNote)
            )
        }

        override fun equals(other: Any?): Boolean {
            return other is StickyNoteGutterIconRenderer && other.stickyNote == this.stickyNote
        }
    }

    private data class StickyNotesDiff(
        val newStickyNotes: List<FileBoundStickyNote>,
        val removedStickyNotes: List<FileBoundStickyNote>
    ) {
        companion object {
            fun calculate(
                oldNotes: Collection<FileBoundStickyNote>,
                newNotes: Collection<FileBoundStickyNote>
            ): StickyNotesDiff {
                val newElements = newNotes.minus(oldNotes)
                val removedElements = oldNotes.minus(newNotes)
                return StickyNotesDiff(
                    newStickyNotes = newElements,
                    removedStickyNotes = removedElements
                )
            }
        }
    }

    private class EditStickyNoteFromGutterAction(
        private val stickyNote: StickyNote
    ) : AnAction("Edit") {
        override fun actionPerformed(e: AnActionEvent) {
            val project = e.project
            if (project == null) {
                logger.error("Project is null")
                return
            }
            val editorScenario =
                AppInjector.getProjectComponent(project).editStickyNoteScenario()
            editorScenario.launch(stickyNote)
        }
    }

    private class SetStickyNoteActiveFromGutterAction(
        private val stickyNote: StickyNote
    ) : AnAction("Set Active") {
        override fun actionPerformed(e: AnActionEvent) {
            val project = e.project
            if (project == null) {
                logger.error("Project is null")
                return
            }
            val projectComponent = AppInjector.getProjectComponent(project)
            val projectScope = projectComponent.projectScope()
            val stickyNoteInteractor =
                projectComponent.stickyNoteInteractor()

            projectScope.launch {
                stickyNoteInteractor.setStickyNoteActive(stickyNote)
            }
        }

        override fun update(e: AnActionEvent) {
            e.presentation.isEnabled = !stickyNote.isActive
        }
    }
}

