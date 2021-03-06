package com.buckstabue.stickynotes.idea

import com.buckstabue.stickynotes.FileBoundStickyNote
import com.buckstabue.stickynotes.FileLocation
import com.buckstabue.stickynotes.NonBoundStickyNote
import com.buckstabue.stickynotes.StickyNote
import com.buckstabue.stickynotes.StickyNotesService
import com.buckstabue.stickynotes.base.di.AppInjector
import com.buckstabue.stickynotes.base.di.project.ProjectScope
import com.buckstabue.stickynotes.idea.gutter.StickyNotesGutterManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.BroadcastChannel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.launch
import javax.inject.Inject

@State(
    name = "StickyNotes"
)
@UseExperimental(ExperimentalCoroutinesApi::class)
class StickyNotesServiceImpl(
    private val project: Project
) : StickyNotesService, PersistentStateComponent<StickyNotesServiceImpl.ServiceState> {
    companion object {
        private val logger = Logger.getInstance(StickyNotesServiceImpl::class.java)
    }

    private var state: ServiceState? = null
    private val loadedStickyNotesChannel = BroadcastChannel<List<StickyNote>>(Channel.CONFLATED)

    @Inject
    protected lateinit var projectScope: ProjectScope

    @Inject
    protected lateinit var stickyNotesGutterManager: StickyNotesGutterManager

    init {
        AppInjector.getProjectComponent(project).inject(this)
    }

    override suspend fun setStickyNotes(stickyNotes: List<StickyNote>) {
        MainScope().launch {
            state = ServiceState(
                stickyNotes = stickyNotes.map {
                    SerializedStickyNote.fromStickyNote(it)
                }
            )
            stickyNotesGutterManager.onStickyNotesChanged(stickyNotes)
        }
    }

    override fun observeLoadedStickyNotes(): ReceiveChannel<List<StickyNote>> {
        return loadedStickyNotesChannel.openSubscription()
    }

    override fun getState(): ServiceState? {
        return state
    }

    override fun loadState(state: ServiceState) {
        this.state = state
        projectScope.launch {
            loadedStickyNotesChannel.send(state.stickyNotes.map { it.toStickyNote(project) })
        }
    }


    data class ServiceState(
        var stickyNotes: List<SerializedStickyNote> = emptyList()
    )

    data class SerializedStickyNote(
        var type: StickyNoteType = StickyNoteType.NON_BOUND_STICKY_NOTE,
        var description: String = "",
        var isArchived: Boolean = false,
        var fileUrl: String? = null,
        var lineNumber: Int? = null,
        var boundBranchName: String? = null
    ) {
        fun toStickyNote(project: Project): StickyNote {
            return when (type) {
                StickyNoteType.NON_BOUND_STICKY_NOTE -> NonBoundStickyNote(
                    description = description,
                    isArchived = isArchived,
                    boundBranchName = boundBranchName
                )
                StickyNoteType.FILE_BOUND_STICKY_NOTE -> {
                    val fileLocation = extractFileLocation(project)
                    if (fileLocation == null) {
                        logger.warn("Couldn't parse file location from $this")
                        NonBoundStickyNote(
                            description = description,
                            isArchived = isArchived,
                            boundBranchName = boundBranchName
                        )
                    } else {
                        FileBoundStickyNote(
                            fileLocation = fileLocation,
                            description = description,
                            isArchived = isArchived,
                            boundBranchName = boundBranchName
                        )
                    }
                }
            }
        }

        private fun extractFileLocation(project: Project): FileLocation? {
            val fileUrl = fileUrl ?: return null
            val lineNumber = lineNumber ?: return null
            return IdeaFileLocation.fromFileUrl(
                project = project,
                fileUrl = fileUrl,
                lineNumber = lineNumber
            )

        }

        companion object {
            fun fromStickyNote(stickyNote: StickyNote): SerializedStickyNote {
                return when (stickyNote) {
                    is NonBoundStickyNote -> SerializedStickyNote(
                        type = StickyNoteType.NON_BOUND_STICKY_NOTE,
                        description = stickyNote.description,
                        isArchived = stickyNote.isArchived,
                        fileUrl = null,
                        lineNumber = null,
                        boundBranchName = stickyNote.boundBranchName
                    )
                    is FileBoundStickyNote -> SerializedStickyNote(
                        type = StickyNoteType.FILE_BOUND_STICKY_NOTE,
                        description = stickyNote.description,
                        isArchived = stickyNote.isArchived,
                        fileUrl = stickyNote.fileLocation.fileUrl,
                        lineNumber = stickyNote.fileLocation.lineNumber,
                        boundBranchName = stickyNote.boundBranchName
                    )
                }
            }
        }
    }

    enum class StickyNoteType {
        NON_BOUND_STICKY_NOTE,
        FILE_BOUND_STICKY_NOTE
    }
}
