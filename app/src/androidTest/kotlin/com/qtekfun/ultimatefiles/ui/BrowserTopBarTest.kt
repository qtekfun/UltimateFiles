package com.qtekfun.ultimatefiles.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import com.qtekfun.ultimatefiles.R
import com.qtekfun.ultimatefiles.core.model.BreadcrumbSegment
import com.qtekfun.ultimatefiles.core.model.SortOrder
import com.qtekfun.ultimatefiles.core.model.ViewMode
import com.qtekfun.ultimatefiles.ui.browser.BrowserTopBar
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class BrowserTopBarTest {
    @get:Rule val compose = createComposeRule()

    private fun text(id: Int) = InstrumentationRegistry.getInstrumentation().targetContext.getString(id)

    private val calls = mutableListOf<String>()

    private fun show() = compose.setContent {
        MaterialTheme {
            BrowserTopBar(
                segments = listOf(BreadcrumbSegment("Internal", "/")),
                sortOrder = SortOrder(),
                viewMode = ViewMode.LIST,
                searchQuery = null,
                onMenuClick = {},
                onSegmentClick = {},
                onSearchClick = {},
                onSearchQueryChange = {},
                onNewFolder = { calls += "folder" },
                onNewFile = { calls += "file" },
                onSortSelected = {},
                onSelectAll = {},
                onToggleViewMode = {},
            )
        }
    }

    @Test
    fun newFolderIsInTheBarAndNewFileInTheMenu() {
        show()
        compose.onNodeWithContentDescription(text(R.string.action_new_folder)).performClick()
        compose.onNodeWithContentDescription(text(R.string.action_more)).performClick()
        compose.onNodeWithText(text(R.string.action_new_file)).performClick()
        assertEquals(listOf("folder", "file"), calls)
    }
}
