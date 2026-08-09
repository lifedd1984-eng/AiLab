package com.example.quickchecklist.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalSize
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextDecoration
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.example.quickchecklist.MainActivity
import com.example.quickchecklist.data.ChecklistStore

class ChecklistWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ChecklistGlanceWidget()
}

private val ItemIdKey = ActionParameters.Key<String>("itemId")
private val CategoryIdKey = ActionParameters.Key<String>("categoryId")

/** 체크 토글 — 앱을 열지 않고 위젯에서 즉시 (iOS의 ToggleItemIntent에 대응) */
class ToggleItemAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        parameters[ItemIdKey]?.let { ChecklistStore.toggle(context, it) }
        ChecklistGlanceWidget().updateAll(context)
    }
}

/** 카테고리 칩 탭 — 위젯 안에서 전환 (iOS의 SelectWidgetCategoryIntent에 대응) */
class SelectCategoryAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        parameters[CategoryIdKey]?.let { ChecklistStore.setWidgetCategory(context, it) }
        ChecklistGlanceWidget().updateAll(context)
    }
}

class ChecklistGlanceWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Responsive(
        setOf(
            DpSize(250.dp, 60.dp),   // 1행 "긴 막대" — Android에선 가능
            DpSize(250.dp, 110.dp),  // 4x2
            DpSize(250.dp, 250.dp),  // 4x4
        )
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            GlanceTheme {
                WidgetContent(context)
            }
        }
    }

    @Composable
    private fun WidgetContent(context: Context) {
        val data = ChecklistStore.load(context)
        val selectedId = ChecklistStore.widgetCategoryId(context, data)
        val categories = data.categories.sortedBy { it.sortOrder }
        val items = data.items
            .filter { it.categoryId == selectedId && !it.isArchived }
            .sortedBy { it.sortOrder }

        val height = LocalSize.current.height
        val maxRows = when {
            height >= 220.dp -> 8
            height >= 100.dp -> 3
            else -> 1
        }

        val green = Color(0xFF1F9D5B)

        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(GlanceTheme.colors.widgetBackground)
                .cornerRadius(20.dp)
                .padding(12.dp)
                .clickable(actionStartActivity<MainActivity>())
        ) {
            // 카테고리 칩 행 — 탭하면 위젯 안에서 전환
            Row(verticalAlignment = Alignment.CenterVertically) {
                categories.take(3).forEach { cat ->
                    val selected = cat.id == selectedId
                    val remaining = data.items.count {
                        it.categoryId == cat.id && !it.isArchived && !it.isDone
                    }
                    Text(
                        text = "${cat.icon} ${cat.name} $remaining",
                        style = TextStyle(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (selected) ColorProvider(Color.White)
                                    else GlanceTheme.colors.onSurfaceVariant,
                        ),
                        modifier = GlanceModifier
                            .background(
                                if (selected) ColorProvider(green)
                                else GlanceTheme.colors.surfaceVariant
                            )
                            .cornerRadius(20.dp)
                            .padding(horizontal = 9.dp, vertical = 4.dp)
                            .clickable(
                                actionRunCallback<SelectCategoryAction>(
                                    actionParametersOf(CategoryIdKey to cat.id)
                                )
                            )
                    )
                    Spacer(GlanceModifier.width(5.dp))
                }
            }
            Spacer(GlanceModifier.height(6.dp))

            if (items.isEmpty()) {
                Text(
                    text = "목록이 비었어요 — 탭해서 추가",
                    style = TextStyle(fontSize = 13.sp, color = GlanceTheme.colors.onSurfaceVariant),
                    modifier = GlanceModifier.padding(top = 6.dp)
                )
            } else {
                items.take(maxRows).forEach { item ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = GlanceModifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp)
                            .clickable(
                                actionRunCallback<ToggleItemAction>(
                                    actionParametersOf(ItemIdKey to item.id)
                                )
                            )
                    ) {
                        Text(
                            text = if (item.isDone) "✓" else "○",
                            style = TextStyle(
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (item.isDone) ColorProvider(green)
                                        else GlanceTheme.colors.onSurfaceVariant,
                            ),
                        )
                        Spacer(GlanceModifier.width(8.dp))
                        Text(
                            text = item.title,
                            maxLines = 1,
                            style = TextStyle(
                                fontSize = 14.sp,
                                color = if (item.isDone) GlanceTheme.colors.onSurfaceVariant
                                        else GlanceTheme.colors.onSurface,
                                textDecoration = if (item.isDone) TextDecoration.LineThrough
                                                 else TextDecoration.None,
                            ),
                        )
                    }
                }
                if (items.size > maxRows) {
                    Text(
                        text = "+${items.size - maxRows}개 더",
                        style = TextStyle(fontSize = 11.sp, color = GlanceTheme.colors.onSurfaceVariant),
                        modifier = GlanceModifier.fillMaxWidth().padding(top = 2.dp)
                    )
                }
            }
        }
    }
}
