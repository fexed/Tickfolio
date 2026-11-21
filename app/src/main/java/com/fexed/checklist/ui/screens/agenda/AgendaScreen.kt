package com.fexed.checklist.ui.screens.agenda

import android.text.format.DateFormat as AndroidDateFormat
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import com.fexed.checklist.R
import com.fexed.checklist.data.preferences.ThemeMode
import com.fexed.checklist.ui.theme.ChecklistTheme
import java.text.DateFormat
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.time.temporal.WeekFields
import java.util.Date
import java.util.Locale

@Composable
fun AgendaScreen(
    viewModel: AgendaViewModel,
    onNavigateBack: () -> Unit,
    onOpenChecklist: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    AgendaContent(
        uiState = uiState,
        onNavigateBack = onNavigateBack,
        onSelectDate = viewModel::selectDate,
        onOpenChecklist = onOpenChecklist,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AgendaContent(
    uiState: AgendaUiState,
    onNavigateBack: () -> Unit,
    onSelectDate: (LocalDate) -> Unit,
    onOpenChecklist: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val locale = Locale.getDefault()
    val zone = ZoneId.systemDefault()
    val monthFormatter = remember(locale) {
        DateTimeFormatter.ofPattern("LLLL yyyy", locale)
    }
    val fullDateFormatter = remember(locale) {
        DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(locale)
    }
    val shortDateFormatter = remember(locale) {
        DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale)
    }
    val eventsByDate = remember(uiState.events, zone) {
        uiState.events.groupBy { event ->
            Instant.ofEpochMilli(event.timestamp).atZone(zone).toLocalDate()
        }
    }
    val overdueEvents = if (uiState.selectedDate == uiState.today) {
        uiState.events.filter {
            it.kind == AgendaEventKind.ITEM_DUE && it.timestamp < uiState.nowMillis
        }
    } else {
        emptyList()
    }
    val overdueIds = remember(overdueEvents) { overdueEvents.mapTo(mutableSetOf()) { it.id } }
    val selectedDateEvents = remember(eventsByDate, uiState.selectedDate, overdueIds) {
        eventsByDate[uiState.selectedDate].orEmpty().filterNot { it.id in overdueIds }
    }
    val month = YearMonth.from(uiState.selectedDate)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                title = {
                    Text(
                        text = stringResource(R.string.agenda_title),
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                },
                actions = {
                    TextButton(onClick = { onSelectDate(uiState.today) }) {
                        Text(stringResource(R.string.agenda_today))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AgendaCalendar(
                month = month,
                selectedDate = uiState.selectedDate,
                today = uiState.today,
                eventDates = eventsByDate.keys,
                monthFormatter = monthFormatter,
                fullDateFormatter = fullDateFormatter,
                onSelectDate = onSelectDate,
                onMoveMonth = { amount ->
                    val targetMonth = month.plusMonths(amount.toLong())
                    onSelectDate(
                        targetMonth.atDay(
                            uiState.selectedDate.dayOfMonth.coerceAtMost(targetMonth.lengthOfMonth())
                        )
                    )
                },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            Text(
                text = stringResource(
                    R.string.agenda_events_for_date,
                    uiState.selectedDate.format(shortDateFormatter)
                ),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 4.dp)
            )

            if (uiState.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp, top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (overdueEvents.isNotEmpty()) {
                        item(key = "overdue_header") {
                            AgendaSectionHeader(stringResource(R.string.agenda_overdue))
                        }
                        items(overdueEvents, key = { "overdue_${it.id}" }) { event ->
                            AgendaEventCard(
                                event = event,
                                isOverdue = true,
                                onClick = { onOpenChecklist(event.checklistId) }
                            )
                        }
                    }

                    if (selectedDateEvents.isNotEmpty()) {
                        item(key = "scheduled_header") {
                            AgendaSectionHeader(stringResource(R.string.agenda_scheduled))
                        }
                        items(selectedDateEvents, key = { it.id }) { event ->
                            AgendaEventCard(
                                event = event,
                                isOverdue = event.kind == AgendaEventKind.ITEM_DUE &&
                                    event.timestamp < uiState.nowMillis,
                                onClick = { onOpenChecklist(event.checklistId) }
                            )
                        }
                    }

                    if (overdueEvents.isEmpty() && selectedDateEvents.isEmpty()) {
                        item(key = "agenda_empty") {
                            AgendaEmptyState(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(220.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AgendaCalendar(
    month: YearMonth,
    selectedDate: LocalDate,
    today: LocalDate,
    eventDates: Set<LocalDate>,
    monthFormatter: DateTimeFormatter,
    fullDateFormatter: DateTimeFormatter,
    onSelectDate: (LocalDate) -> Unit,
    onMoveMonth: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val locale = Locale.getDefault()
    val firstDayOfWeek = WeekFields.of(locale).firstDayOfWeek
    val monthDays = remember(month, firstDayOfWeek) {
        monthCalendarDates(month, firstDayOfWeek)
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { onMoveMonth(-1) },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ChevronLeft,
                        contentDescription = stringResource(R.string.agenda_previous_month),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = month.format(monthFormatter),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = { onMoveMonth(1) },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = stringResource(R.string.agenda_next_month),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(modifier = Modifier.fillMaxWidth()) {
                (0..6).forEach { offset ->
                    val day = firstDayOfWeek.plus(offset.toLong())
                    Text(
                        text = day.getDisplayName(TextStyle.NARROW, locale),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            LazyVerticalGrid(
                columns = GridCells.Fixed(7),
                userScrollEnabled = false,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
            ) {
                items(
                    count = monthDays.size,
                    key = { index -> monthDays[index]?.toString() ?: "empty_$index" }
                ) { index ->
                    val date = monthDays[index]
                    if (date == null) {
                        Spacer(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(40.dp)
                        )
                    } else {
                        AgendaCalendarDay(
                            date = date,
                            selected = date == selectedDate,
                            today = date == today,
                            hasEvents = date in eventDates,
                            fullDateFormatter = fullDateFormatter,
                            onClick = { onSelectDate(date) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AgendaCalendarDay(
    date: LocalDate,
    selected: Boolean,
    today: Boolean,
    hasEvents: Boolean,
    fullDateFormatter: DateTimeFormatter,
    onClick: () -> Unit
) {
    val description = stringResource(
        R.string.agenda_select_date_desc,
        date.format(fullDateFormatter)
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp),
        contentAlignment = Alignment.Center
    ) {
        val selectionShape = CircleShape
        Column(
            modifier = Modifier
                .size(36.dp)
                .clip(selectionShape)
                .then(
                    if (today && !selected) {
                        Modifier.border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.primary,
                            shape = selectionShape
                        )
                    } else {
                        Modifier
                    }
                )
                .background(
                    if (selected) MaterialTheme.colorScheme.primary else Color.Transparent
                )
                .selectable(
                    selected = selected,
                    onClick = onClick
                )
                .semantics { contentDescription = description },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = date.dayOfMonth.toString(),
                style = MaterialTheme.typography.bodySmall,
                color = if (selected) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
            if (hasEvents) {
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .clip(CircleShape)
                        .background(
                            if (selected) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.primary
                        )
                )
            } else {
                Spacer(modifier = Modifier.size(4.dp))
            }
        }
    }
}

@Composable
private fun AgendaSectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 2.dp)
    )
}

@Composable
private fun AgendaEventCard(
    event: AgendaEvent,
    isOverdue: Boolean,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val dateTimeFormatter = remember(context) {
        DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
    }
    val timeFormatter = remember(context) { AndroidDateFormat.getTimeFormat(context) }
    val timeLabel = if (isOverdue) {
        dateTimeFormatter.format(Date(event.timestamp))
    } else {
        timeFormatter.format(Date(event.timestamp))
    }
    val eventLabel = stringResource(
        when (event.kind) {
            AgendaEventKind.ITEM_DUE -> R.string.agenda_item_due
            AgendaEventKind.CHECKLIST_REMINDER -> R.string.agenda_checklist_reminder
        }
    )

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (event.kind) {
                        AgendaEventKind.ITEM_DUE -> Icons.Default.Event
                        AgendaEventKind.CHECKLIST_REMINDER -> Icons.Default.Notifications
                    },
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = event.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = event.checklistTitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = timeLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (isOverdue) stringResource(R.string.agenda_overdue) else eventLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isOverdue) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun AgendaEmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.CalendarMonth,
            contentDescription = null,
            modifier = Modifier.size(36.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Text(
            text = stringResource(R.string.agenda_empty_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(top = 12.dp)
        )
        Text(
            text = stringResource(R.string.agenda_empty_description),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 6.dp)
        )
    }
}

private fun monthCalendarDates(
    month: YearMonth,
    firstDayOfWeek: DayOfWeek
): List<LocalDate?> {
    val offset = (month.atDay(1).dayOfWeek.value - firstDayOfWeek.value + 7) % 7
    return List(42) { index ->
        val dayOfMonth = index - offset + 1
        if (dayOfMonth in 1..month.lengthOfMonth()) month.atDay(dayOfMonth) else null
    }
}

@Preview(name = "Agenda - Paper", showBackground = true, backgroundColor = 0xFFF8F4EC)
@Composable
private fun AgendaScreenPreview() {
    val today = LocalDate.now()
    val zone = ZoneId.systemDefault()
    val itemDueAt = today.atTime(10, 30).atZone(zone).toInstant().toEpochMilli()
    val reminderAt = today.atTime(14, 0).atZone(zone).toInstant().toEpochMilli()
    ChecklistTheme(themeMode = ThemeMode.LIGHT) {
        AgendaContent(
            uiState = AgendaUiState(
                selectedDate = today,
                today = today,
                nowMillis = today.atStartOfDay(zone).toInstant().toEpochMilli(),
                events = listOf(
                    AgendaEvent(
                        id = "preview_item",
                        timestamp = itemDueAt,
                        checklistId = 1L,
                        checklistTitle = "Weekend trip",
                        title = "Pack charger",
                        kind = AgendaEventKind.ITEM_DUE
                    ),
                    AgendaEvent(
                        id = "preview_reminder",
                        timestamp = reminderAt,
                        checklistId = 1L,
                        checklistTitle = "Weekend trip",
                        title = "Weekend trip",
                        kind = AgendaEventKind.CHECKLIST_REMINDER
                    )
                ),
                isLoading = false
            ),
            onNavigateBack = {},
            onSelectDate = {},
            onOpenChecklist = {}
        )
    }
}
