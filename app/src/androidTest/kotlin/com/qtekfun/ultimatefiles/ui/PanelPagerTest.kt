package com.qtekfun.ultimatefiles.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Scaffold
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import com.qtekfun.ultimatefiles.core.model.BreadcrumbSegment
import com.qtekfun.ultimatefiles.core.model.SortOrder
import com.qtekfun.ultimatefiles.core.model.ViewMode
import com.qtekfun.ultimatefiles.ui.browser.BrowserTopBar
import org.junit.Rule
import org.junit.Test

/**
 * Swiping the pager to a panel that was not composed yet used to crash the app ("LayoutNode … not found in RectList"):
 * Material's top app bar asked the breadcrumb, a lazy list, for its baseline while the pager measured the new page.
 */
@OptIn(ExperimentalFoundationApi::class)
class PanelPagerTest {
    @get:Rule val compose = createComposeRule()

    private fun path(vararg names: String) =
        names.runningFold("") { acc, name -> "$acc/$name" }.drop(1).mapIndexed { i, p -> BreadcrumbSegment(names[i], p) }

    @Test
    fun swipingToThePanelsThatAreNotComposedYetDoesNotCrash() {
        compose.setContent {
            MaterialTheme {
                val pager = rememberPagerState { 3 }
                HorizontalPager(state = pager, modifier = Modifier.testTag("pager")) { page ->
                    Scaffold(
                        topBar = {
                            BrowserTopBar(
                                segments = path("Internal", "Folder ${page + 1}", "Photos", "Holidays"),
                                sortOrder = SortOrder(),
                                viewMode = ViewMode.LIST,
                                searchQuery = null,
                                onMenuClick = {},
                                onSegmentClick = {},
                                onSearchClick = {},
                                onSearchQueryChange = {},
                                onNewFolder = {},
                                onNewFile = {},
                                onSortSelected = {},
                                onSelectAll = {},
                                onToggleViewMode = {},
                            )
                        },
                    ) { padding ->
                        LazyColumn(modifier = Modifier.padding(padding)) {
                            items(30) { Text("Item $it of panel ${page + 1}") }
                        }
                    }
                }
            }
        }

        repeat(2) { compose.onNodeWithTag("pager").performTouchInput { swipeLeft() } }
        compose.waitForIdle()
        compose.onNodeWithText("Item 0 of panel 3").assertIsDisplayed()

        repeat(2) { compose.onNodeWithTag("pager").performTouchInput { swipeRight() } }
        compose.waitForIdle()
        compose.onNodeWithText("Item 0 of panel 1").assertIsDisplayed()
    }
}
