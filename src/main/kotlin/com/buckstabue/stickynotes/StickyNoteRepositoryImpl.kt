package com.buckstabue.stickynotes

import com.buckstabue.stickynotes.service.StickyNotesService
import com.intellij.openapi.project.Project
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.channels.BroadcastChannel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.channels.distinct
import kotlinx.coroutines.launch
import java.util.concurrent.CopyOnWriteArrayList
import javax.inject.Inject

@PerProject
@ExperimentalCoroutinesApi
@ObsoleteCoroutinesApi
class StickyNoteRepositoryImpl @Inject constructor(
    private val projectScope: ProjectScope,
    project: Project
) : StickyNoteRepository {
    private val backlogStickyNotes: MutableList<StickyNote> = CopyOnWriteArrayList()
    private val archivedStickyNotes: MutableList<StickyNote> = CopyOnWriteArrayList()

    private val activeStickyNoteChannel = BroadcastChannel<StickyNote?>(Channel.CONFLATED).also { it.offer(null) }
    private val backlogStickyNoteListChannel =
        BroadcastChannel<List<StickyNote>>(Channel.CONFLATED).also { it.offer(emptyList()) }
    private val archivedStickyNoteListChannel =
        BroadcastChannel<List<StickyNote>>(Channel.CONFLATED).also { it.offer(emptyList()) }

    private val stickyNotesService = StickyNotesService.getInstance(project).also {
        projectScope.launch {
            it.observeLoadedStickyNotes().consumeEach {
                setStickNotes(it)
            }
        }
    }

    override suspend fun addStickyNote(stickyNote: StickyNote) {
        if (stickyNote.isArchived) {
            archivedStickyNotes.add(stickyNote)
        } else {
            backlogStickyNotes.add(stickyNote)
        }

        notifyStickyNotesChanged()
    }

    private suspend fun notifyStickyNotesChanged() {
        backlogStickyNoteListChannel.send(backlogStickyNotes)
        archivedStickyNoteListChannel.send(archivedStickyNotes)
        activeStickyNoteChannel.send(backlogStickyNotes.firstOrNull())

        val newStickyNoteList = backlogStickyNotes.toList().plus(archivedStickyNotes.toList())
        stickyNotesService.setStickyNotes(newStickyNoteList)
    }

    override suspend fun moveStickyNotes(stickyNotes: List<StickyNote>, insertionIndex: Int) {
        require(insertionIndex >= 0) {
            "Cannot insert at a negative position ($insertionIndex)"
        }
        if (stickyNotes.isEmpty()) {
            return
        }
        val targetList = if (stickyNotes.first().isArchived) archivedStickyNotes else backlogStickyNotes
        require(targetList.containsAll(stickyNotes)) {
            "Some of moved elements are not part of the actual sticky note list"
        }
        // if multiple elements selected preserve order of the source list
        val stickyNotes = stickyNotes.sortedBy { targetList.indexOf(it) }
        val indexToInsertAfterRemoving =
            calculateIndexToInsertAfterRemovingToMoveElements(targetList, stickyNotes, insertionIndex)
        targetList.removeAll(stickyNotes)
        targetList.addAll(indexToInsertAfterRemoving, stickyNotes)

        notifyStickyNotesChanged()
    }

    /**
     * Insertion index may changed after deletion of moved elements from a source list.
     * E.g. ["a", "b", "c"], we want to move "a" to index 2, but before moving we delete "a" from the collection and
     * we have ["b", "c"], so the actual insertion position is 1, not 2. To calculate that we simply count how many
     * elements are placed before the desired position and we subtract this count from the desired insertion position
     */
    private fun calculateIndexToInsertAfterRemovingToMoveElements(
        targetList: MutableList<StickyNote>,
        movedStickyNotes: List<StickyNote>,
        desiredIndexPosition: Int
    ): Int {
        val numberOfElementsBeforeDesiredIndexPosition = targetList.subList(0, desiredIndexPosition)
            .count { movedStickyNotes.contains(it) }
        return desiredIndexPosition - numberOfElementsBeforeDesiredIndexPosition
    }

    override suspend fun archiveStickyNote(stickyNote: StickyNote) {
        archiveStickyNotes(listOf(stickyNote))
    }

    override suspend fun removeStickyNotes(stickyNotes: List<StickyNote>) {
        backlogStickyNotes.removeAll(stickyNotes)
        archivedStickyNotes.removeAll(stickyNotes)
        notifyStickyNotesChanged()
    }

    override suspend fun setStickNotes(stickyNotes: List<StickyNote>) {
        backlogStickyNotes.clear()
        archivedStickyNotes.clear()

        val isArchivedToStickyNotes: Map<Boolean, List<StickyNote>> =
            stickyNotes.groupBy { it.isArchived }.toMap()
        backlogStickyNotes.addAll(isArchivedToStickyNotes[false].orEmpty())
        archivedStickyNotes.addAll(isArchivedToStickyNotes[true].orEmpty())

        notifyStickyNotesChanged()
    }

    override suspend fun setStickyNoteActive(stickyNote: StickyNote) {
        if (stickyNote.isArchived) {
            archivedStickyNotes.remove(stickyNote)
            backlogStickyNotes.add(0, stickyNote.setArchived(false))
        } else {
            if (backlogStickyNotes.firstOrNull() != stickyNote) {
                backlogStickyNotes.remove(stickyNote)
                backlogStickyNotes.add(0, stickyNote)
            }
        }

        notifyStickyNotesChanged()
    }

    override suspend fun archiveStickyNotes(stickyNotes: List<StickyNote>) {
        backlogStickyNotes.removeAll(stickyNotes)

        val newArchivedStickyNotes = stickyNotes.map { it.setArchived(true) }.minus(archivedStickyNotes)
        archivedStickyNotes.addAll(0, newArchivedStickyNotes)

        notifyStickyNotesChanged()
    }

    override suspend fun addStickyNotesToBacklog(stickyNotes: List<StickyNote>) {
        archivedStickyNotes.removeAll(stickyNotes) // some sticky notes can be archived, remove them from archive first

        val newBacklogStickyNotes = stickyNotes.map { it.setArchived(false) }.minus(backlogStickyNotes)
        backlogStickyNotes.addAll(newBacklogStickyNotes)

        notifyStickyNotesChanged()
    }

    override fun observeBacklogStickyNotes(): ReceiveChannel<List<StickyNote>> {
        return backlogStickyNoteListChannel.openSubscription()
    }

    override fun observeArchivedStickyNotes(): ReceiveChannel<List<StickyNote>> {
        return archivedStickyNoteListChannel.openSubscription()
    }

    override fun observeActiveStickyNote(): ReceiveChannel<StickyNote?> {
        return activeStickyNoteChannel.openSubscription().distinct()
    }
}