package com.fexed.checklist.widget

import android.content.Context
import androidx.glance.unit.ColorProvider
import com.fexed.checklist.ChecklistApplication
import com.fexed.checklist.EXTRA_OPEN_CHECKLIST_ID
import com.fexed.checklist.R
import com.fexed.checklist.data.model.ChecklistWithItems
import com.fexed.checklist.data.preferences.ThemeMode
import com.fexed.checklist.ui.theme.ParchmentDarkBackground
import com.fexed.checklist.ui.theme.ParchmentDarkInk
import com.fexed.checklist.ui.theme.ParchmentDarkInkMuted
import com.fexed.checklist.ui.theme.ParchmentDarkPrimary
import com.fexed.checklist.ui.theme.ParchmentDarkTertiary
import com.fexed.checklist.ui.theme.ParchmentLightInk
import com.fexed.checklist.ui.theme.ParchmentLightInkMuted
import com.fexed.checklist.ui.theme.ParchmentLightPrimary
import com.fexed.checklist.ui.theme.ParchmentLightSurface
import kotlinx.coroutines.flow.first

internal data class WidgetColors(
    val background: ColorProvider,
    val ink: ColorProvider,
    val mutedInk: ColorProvider,
    val accent: ColorProvider,
    val completed: ColorProvider
)

internal suspend fun widgetColors(context: Context): WidgetColors {
    val app = context.applicationContext as ChecklistApplication
    val themeMode = app.appPreferences.userPreferencesFlow.first().themeMode
    return when (themeMode) {
        ThemeMode.SYSTEM -> systemWidgetColors()
        ThemeMode.LIGHT -> lightWidgetColors()
        ThemeMode.DARK -> darkWidgetColors()
    }
}

private fun systemWidgetColors() = WidgetColors(
    background = ColorProvider(R.color.widget_background),
    ink = ColorProvider(R.color.widget_ink),
    mutedInk = ColorProvider(R.color.widget_muted_ink),
    accent = ColorProvider(R.color.widget_accent),
    completed = ColorProvider(R.color.widget_completed)
)

private fun lightWidgetColors() = WidgetColors(
    background = ColorProvider(ParchmentLightSurface),
    ink = ColorProvider(ParchmentLightInk),
    mutedInk = ColorProvider(ParchmentLightInkMuted),
    accent = ColorProvider(ParchmentLightPrimary),
    completed = ColorProvider(ParchmentLightPrimary)
)

private fun darkWidgetColors() = WidgetColors(
    background = ColorProvider(ParchmentDarkBackground),
    ink = ColorProvider(ParchmentDarkInk),
    mutedInk = ColorProvider(ParchmentDarkInkMuted),
    accent = ColorProvider(ParchmentDarkPrimary),
    completed = ColorProvider(ParchmentDarkTertiary)
)

internal object ChecklistWidgetPreferences {
    private const val PREFERENCES_NAME = "checklist_widget_preferences"

    fun setSelectedChecklist(context: Context, appWidgetId: Int, checklistId: Long) {
        val saved = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
            .edit()
            .putLong(appWidgetId.toString(), checklistId)
            .commit()
        check(saved) { "Failed to save checklist selection for widget $appWidgetId" }
    }

    fun getSelectedChecklist(context: Context, appWidgetId: Int): Long? {
        val selectedId = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
            .getLong(appWidgetId.toString(), INVALID_CHECKLIST_ID)
        return selectedId.takeIf { it != INVALID_CHECKLIST_ID }
    }

    fun clearSelectedChecklists(context: Context, appWidgetIds: IntArray) {
        context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
            .edit()
            .apply {
                appWidgetIds.forEach { remove(it.toString()) }
            }
            .apply()
    }

    private const val INVALID_CHECKLIST_ID = -1L
}

internal suspend fun loadAllChecklists(context: Context): List<ChecklistWithItems> {
    val app = context.applicationContext as ChecklistApplication
    return app.database.checklistDao().getAllChecklistsForWidget()
}

internal suspend fun toggleChecklistItem(
    context: Context,
    itemId: Long,
    completed: Boolean
) {
    val app = context.applicationContext as ChecklistApplication
    app.database.checklistDao().setItemCompleted(itemId, completed)
    ChecklistWidgetUpdater.updateAll(context)
}

internal suspend fun loadChecklist(context: Context, checklistId: Long): ChecklistWithItems? {
    val app = context.applicationContext as ChecklistApplication
    return app.database.checklistDao().getChecklistForWidget(checklistId)
}

internal fun checklistOpenParameters(checklistId: Long) =
    androidx.glance.action.actionParametersOf(
        androidx.glance.action.ActionParameters.Key<Long>(EXTRA_OPEN_CHECKLIST_ID) to checklistId
    )
