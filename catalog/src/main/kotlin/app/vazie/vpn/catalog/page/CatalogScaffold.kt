package app.vazie.vpn.catalog.page

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import app.vazie.vpn.core.designsystem.theme.VazieTheme

/** Scrolling page body with the catalog's standard padding. */
@Composable
internal fun CatalogPage(content: LazyListScope.() -> Unit) {
    LazyColumn(
        contentPadding = PaddingValues(
            start = VazieTheme.spacing.screenHorizontal,
            end = VazieTheme.spacing.screenHorizontal,
            top = VazieTheme.spacing.md,
            bottom = VazieTheme.spacing.xxxl,
        ),
        verticalArrangement = Arrangement.spacedBy(VazieTheme.spacing.sm),
        content = content,
    )
}

/** One labelled group of specimens. */
@Composable
internal fun Specimen(
    title: String,
    modifier: Modifier = Modifier,
    note: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(VazieTheme.spacing.xs),
    ) {
        Text(
            text = title,
            style = VazieTheme.typography.label,
            color = VazieTheme.colors.textSecondary,
        )
        if (note != null) {
            Text(
                text = note,
                style = VazieTheme.typography.caption,
                color = VazieTheme.colors.textSecondary,
            )
        }
        content()
    }
}

/** Label placed beside a single specimen so states can be told apart. */
@Composable
internal fun StateLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = VazieTheme.typography.monoSmall,
        color = VazieTheme.colors.technicalLabel,
        modifier = modifier,
    )
}

@Composable
internal fun SpecimenRow(
    label: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(VazieTheme.spacing.sm),
    ) {
        StateLabel(label, Modifier.weight(1f))
        content()
    }
}
