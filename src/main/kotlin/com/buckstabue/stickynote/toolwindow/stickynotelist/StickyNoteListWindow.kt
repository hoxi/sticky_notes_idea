package com.buckstabue.stickynote.toolwindow.stickynotelist

import com.buckstabue.stickynote.base.BaseWindow
import com.buckstabue.stickynote.base.addOnActionListener
import com.buckstabue.stickynote.toolwindow.StickyNoteToolWindowComponent
import javax.inject.Inject
import javax.swing.AbstractListModel
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JList
import javax.swing.JPanel

class StickyNoteListWindow : BaseWindow<StickyNoteListView, StickyNoteListPresenter>(), StickyNoteListView {
    private lateinit var stickyNoteList: JList<StickyNoteViewModel>
    private lateinit var contentPanel: JPanel
    private lateinit var backButton: JButton

    override val routingTag: String = "StickyNoteList"

    @Inject
    override lateinit var presenter: StickyNoteListPresenter

    init {
        stickyNoteList.model = StickyNoteListModel(emptyList())
        backButton.addActionListener {
            presenter.onBackButtonClick()
        }
        stickyNoteList.addOnActionListener {
            presenter.onItemSelected(it)
        }
    }

    override fun onCreate(toolWindowComponent: StickyNoteToolWindowComponent) {
        toolWindowComponent.inject(this)
        super.onCreate(toolWindowComponent)
    }

    override fun render(stickyNotes: List<StickyNoteViewModel>) {
        stickyNoteList.model = StickyNoteListModel(stickyNotes)
    }

    override fun getContent(): JComponent {
        return contentPanel
    }
}

class StickyNoteListModel(
    private val items: List<StickyNoteViewModel>
) : AbstractListModel<StickyNoteViewModel>() {
    override fun getElementAt(index: Int): StickyNoteViewModel {
        return items[index]
    }

    override fun getSize(): Int {
        return items.size
    }
}