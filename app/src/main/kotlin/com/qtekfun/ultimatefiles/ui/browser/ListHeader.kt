package com.qtekfun.ultimatefiles.ui.browser

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.qtekfun.ultimatefiles.R
import com.qtekfun.ultimatefiles.core.model.SortField
import com.qtekfun.ultimatefiles.core.model.SortOrder
import com.qtekfun.ultimatefiles.ui.components.labelRes

/** Above the list: how many entries it shows, and the order they are in, which can be changed from here. */
@Composable
fun ListHeader(count: Int, sortOrder: SortOrder, onSortSelected: (SortField) -> Unit, modifier: Modifier = Modifier) {
    var menuOpen by remember { mutableStateOf(false) }
    val arrow = if (sortOrder.ascending) Icons.Filled.ArrowUpward else Icons.Filled.ArrowDownward
    Row(modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = pluralStringResource(R.plurals.item_count, count, count),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Box {
            TextButton(onClick = { menuOpen = true }) {
                Text(stringResource(sortOrder.field.labelRes()))
                Icon(
                    arrow,
                    contentDescription = stringResource(R.string.action_sort_by),
                    modifier = Modifier.padding(start = 4.dp).size(16.dp),
                )
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                SortField.entries.forEach { field ->
                    DropdownMenuItem(
                        text = { Text(stringResource(field.labelRes())) },
                        trailingIcon = { if (field == sortOrder.field) Icon(arrow, contentDescription = null) },
                        onClick = { menuOpen = false; onSortSelected(field) },
                    )
                }
            }
        }
    }
}
