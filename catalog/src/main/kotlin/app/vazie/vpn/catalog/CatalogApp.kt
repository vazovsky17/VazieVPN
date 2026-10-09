package app.vazie.vpn.catalog

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import app.vazie.vpn.catalog.page.ActionsPage
import app.vazie.vpn.catalog.page.ColorsPage
import app.vazie.vpn.catalog.page.FeedbackPage
import app.vazie.vpn.catalog.page.FoundationPage
import app.vazie.vpn.catalog.page.InputPage
import app.vazie.vpn.catalog.page.ListsPage
import app.vazie.vpn.catalog.page.TechnicalPage
import app.vazie.vpn.catalog.page.TypographyPage
import app.vazie.vpn.core.designsystem.theme.Appearance
import app.vazie.vpn.core.designsystem.theme.VazieTheme
import app.vazie.vpn.core.designsystem.theme.colorsFor

private enum class CatalogPage(@StringRes val titleRes: Int) {
    Colors(R.string.catalog_page_colors),
    Typography(R.string.catalog_page_typography),
    Foundation(R.string.catalog_page_foundation),
    Actions(R.string.catalog_page_actions),
    Input(R.string.catalog_page_input),
    Lists(R.string.catalog_page_lists),
    Feedback(R.string.catalog_page_feedback),
    Technical(R.string.catalog_page_technical),
}

@get:StringRes
private val Appearance.labelRes: Int
    get() = when (this) {
        Appearance.NIGHT_INDIGO -> R.string.catalog_appearance_night_indigo
        Appearance.MILK -> R.string.catalog_appearance_milk
    }

@Composable
fun CatalogApp() {
    var appearance by rememberSaveable { mutableStateOf(Appearance.Default) }
    var page by rememberSaveable { mutableStateOf(CatalogPage.Colors) }

    VazieTheme(appearance = appearance) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(VazieTheme.colors.background)
                .windowInsetsPadding(WindowInsets.safeDrawing),
        ) {
            CatalogHeader(
                appearance = appearance,
                page = page,
                onAppearance = { appearance = it },
                onPage = { page = it },
            )
            when (page) {
                CatalogPage.Colors -> ColorsPage()
                CatalogPage.Typography -> TypographyPage()
                CatalogPage.Foundation -> FoundationPage()
                CatalogPage.Actions -> ActionsPage()
                CatalogPage.Input -> InputPage()
                CatalogPage.Lists -> ListsPage()
                CatalogPage.Feedback -> FeedbackPage()
                CatalogPage.Technical -> TechnicalPage()
            }
        }
    }
}

@Composable
private fun CatalogHeader(
    appearance: Appearance,
    page: CatalogPage,
    onAppearance: (Appearance) -> Unit,
    onPage: (CatalogPage) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(VazieTheme.colors.surface)
            .padding(
                horizontal = VazieTheme.spacing.screenHorizontal,
                vertical = VazieTheme.spacing.sm,
            ),
        verticalArrangement = Arrangement.spacedBy(VazieTheme.spacing.xs),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.catalog_title),
                style = VazieTheme.typography.label,
                color = VazieTheme.colors.primaryText,
            )
        }
        AppearanceSwitcher(selected = appearance, onSelect = onAppearance)
        Switcher(
            labelRes = R.string.catalog_axis_page,
            options = CatalogPage.entries.map { it to it.titleRes },
            selected = page,
            onSelect = onPage,
        )
    }
}

/** The palettes, each with a two-tone swatch of its own background and primary. */
@Composable
private fun AppearanceSwitcher(selected: Appearance, onSelect: (Appearance) -> Unit) {
    val colors = VazieTheme.colors
    Row {
        Text(
            text = stringResource(R.string.catalog_axis_appearance),
            style = VazieTheme.typography.monoSmall,
            color = colors.technicalLabel,
            modifier = Modifier
                .width(96.dp)
                .padding(top = VazieTheme.spacing.xs),
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(VazieTheme.spacing.xxs),
            verticalArrangement = Arrangement.spacedBy(VazieTheme.spacing.xxs),
            modifier = Modifier.selectableGroup(),
        ) {
            Appearance.entries.forEach { entry ->
                val isSelected = entry == selected
                val swatch = colorsFor(entry)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(VazieTheme.spacing.xxs),
                    modifier = Modifier
                        .heightIn(min = VazieTheme.spacing.minTouchTarget)
                        .clip(VazieTheme.shapes.pill)
                        .background(if (isSelected) colors.primary else colors.surfaceMuted)
                        .selectable(
                            selected = isSelected,
                            role = Role.RadioButton,
                            onClick = { onSelect(entry) },
                        )
                        .padding(horizontal = VazieTheme.spacing.xs, vertical = VazieTheme.spacing.xxs),
                ) {
                    Box(
                        Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(swatch.background)
                            .border(6.dp, swatch.primary, CircleShape),
                    )
                    Text(
                        text = stringResource(entry.labelRes),
                        style = VazieTheme.typography.labelSmall,
                        color = if (isSelected) colors.onPrimary else colors.textSecondary,
                    )
                }
            }
        }
    }
}

@Composable
private fun <T> Switcher(
    @StringRes labelRes: Int,
    options: List<Pair<T, Int>>,
    selected: T,
    onSelect: (T) -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = stringResource(labelRes),
            style = VazieTheme.typography.monoSmall,
            color = VazieTheme.colors.technicalLabel,
            modifier = Modifier.width(96.dp),
        )
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(VazieTheme.spacing.xxs),
        ) {
            options.forEach { (value, titleRes) ->
                val isSelected = value == selected
                Text(
                    text = stringResource(titleRes),
                    style = VazieTheme.typography.labelSmall,
                    color = if (isSelected) VazieTheme.colors.onPrimary else VazieTheme.colors.textSecondary,
                    modifier = Modifier
                        .clip(VazieTheme.shapes.pill)
                        .background(
                            if (isSelected) VazieTheme.colors.primary else VazieTheme.colors.surfaceMuted,
                        )
                        .clickable { onSelect(value) }
                        .padding(
                            horizontal = VazieTheme.spacing.sm,
                            vertical = VazieTheme.spacing.xxs,
                        ),
                )
            }
        }
    }
}
