package com.buckstabue.stickynotes.idea.stickynotelist

import com.buckstabue.stickynotes.base.di.AppInjector
import com.buckstabue.stickynotes.idea.StickyNotesWebHelpProvider
import com.buckstabue.stickynotes.idea.forEachTab
import com.buckstabue.stickynotes.idea.minWidth
import com.buckstabue.stickynotes.idea.stickynotelist.getstickynotesstrategy.StickyNotesObservable
import com.buckstabue.stickynotes.idea.stickynotelist.panel.StickyNotesPanel
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import javax.inject.Inject
import javax.swing.Action
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JTabbedPane

class StickyNoteListDialog(
    project: Project
) : DialogWrapper(project), StickyNoteListDialogView {
    companion object {
        const val CONTROLLER_PROPERTY = "StickyNoteListDialog_Controller"
    }

    private lateinit var contentPanel: JPanel

    private lateinit var tabs: JTabbedPane

    @Inject
    lateinit var presenter: StickyNoteListDialogPresenter

    private val daggerComponent = AppInjector.getProjectComponent(project)
        .plusStickyNoteListDialogComponent()

    init {
        daggerComponent.inject(this)
        init()
        title = "Sticky Notes"
        peer.contentPane?.minWidth = 350

        presenter.attachView(this)

        addTab(
            tabName = "Backlog(current branch)",
            observableType = StickyNotesObservable.Type.CURRENT_BRANCH_BACKLOG
        )
        addTab(
            tabName = "Archive",
            observableType = StickyNotesObservable.Type.ARCHIVED
        )
        addTab(
            tabName = "Backlog(all)",
            observableType = StickyNotesObservable.Type.ALL_BACKLOG
        )
    }

    private fun addTab(tabName: String, observableType: StickyNotesObservable.Type) {
        val stickyNotesPanel = StickyNotesPanel(
            parentDialog = this,
            stickyNoteListDialogComponent = daggerComponent,
            stickyNotesObservableType = observableType
        )
        stickyNotesPanel.getContentPanel()
            .putClientProperty(CONTROLLER_PROPERTY, stickyNotesPanel)
        tabs.addTab(
            tabName,
            null,
            stickyNotesPanel.getContentPanel()
        )
    }

    override fun createActions(): Array<Action> {
        return emptyArray()
    }

    override fun getPreferredFocusedComponent(): JComponent? {
        return contentPanel // return non null to make close-on-escape work without any selection
    }

    override fun createCenterPanel(): JComponent? {
        return contentPanel
    }

    override fun dispose() {
        tabs.forEachTab {
            (it.getClientProperty(CONTROLLER_PROPERTY) as? Disposable)?.dispose()
        }
        presenter.detachView()
        super.dispose()
    }

    override fun getDimensionServiceKey(): String? {
        return "STICKY_NOTE_LIST_DIALOG"
    }

    override fun getHelpId(): String? {
        return StickyNotesWebHelpProvider.GITHUB_HELP_TOPIC_ID
    }
}

