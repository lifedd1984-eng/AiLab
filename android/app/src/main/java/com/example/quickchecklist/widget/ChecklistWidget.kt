package com.example.quickchecklist.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
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

// 고정 팔레트 — GlanceTheme의 동적(Material You) 색상은 매 렌더마다 시스템 팔레트를
// 다시 계산해 One UI 등 일부 런처에서 위젯 갱신이 눈에 띄게 느려진다. 값을 고정해서 제거.
private val Green = Color(0xFF1F9D5B)
private val CardLight = Color(0xFFFFFFFF)
private val CardDark = Color(0xFF1F2420)
private val InkLight = Color(0xFF1A211C)
private val InkDark = Color(0xFFE8ECE8)
private val MutedLight = Color(0xFF6C766F)
private val MutedDark = Color(0xFF98A19A)
private val ChipLight = Color(0xFFE9ECE8)
private val ChipDark = Color(0xFF262C27)

private val bg = ColorProvider(day = CardLight, night = CardDark)
private val ink = ColorProvider(day = InkLight, night = InkDark)
private val muted = ColorProvider(day = MutedLight, night = MutedDark)
private val chipBg = ColorProvider(day = ChipLight, night = ChipDark)
private val greenProvider = ColorProvider(Green)
private val whiteProvider = ColorProvider(Color.White)

class ChecklistGlanceWidget : GlanceAppWidget() {

    // Responsive는 정의된 모든 브레이크포인트를 매 갱신마다 전부 그려서 체크 한 번에
    // 위젯이 3중으로 렌더링됐다 — 실제 크기 하나만 그리는 Exact로 전환해 속도 개선.
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            WidgetContent(context)
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

        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(bg)
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
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (selected) whiteProvider else ink,
                        ),
                        maxLines = 1,
                        modifier = GlanceModifier
                            .background(if (selected) greenProvider else chipBg)
                            .cornerRadius(20.dp)
                            .padding(horizontal = 11.dp, vertical = 6.dp)
                            .clickable(
                                actionRunCallback<SelectCategoryAction>(
                                    actionParametersOf(CategoryIdKey to cat.id)
                                )
                            )
                    )
                    Spacer(GlanceModifier.width(6.dp))
                }
            }
            Spacer(GlanceModifier.height(8.dp))

            if (items.isEmpty()) {
                Text(
                    text = "목록이 비었어요 — 탭해서 추가",
                    style = TextStyle(fontSize = 15.sp, color = muted),
                    modifier = GlanceModifier.padding(top = 6.dp)
                )
            } else {
                items.take(maxRows).forEach { item ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = GlanceModifier
                            .fillMaxWidth()
                            .padding(vertical = 5.dp)
                            .clickable(
                                actionRunCallback<ToggleItemAction>(
                                    actionParametersOf(ItemIdKey to item.id)
                                )
                            )
                    ) {
                        Text(
                            text = if (item.isDone) "✓" else "○",
                            style = TextStyle(
                                fontSize = 19.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (item.isDone) greenProvider else muted,
                            ),
                        )
                        Spacer(GlanceModifier.width(10.dp))
                        Text(
                            text = item.title,
                            maxLines = 1,
                            style = TextStyle(
                                fontSize = 17.sp,
                                color = if (item.isDone) muted else ink,
                                textDecoration = if (item.isDone) TextDecoration.LineThrough
                                                 else TextDecoration.None,
                            ),
                        )
                    }
                }
                if (items.size > maxRows) {
                    Text(
                        text = "+${items.size - maxRows}개 더",
                        style = TextStyle(fontSize = 13.sp, color = muted),
                        modifier = GlanceModifier.fillMaxWidth().padding(top = 3.dp)
                    )
                }
            }
        }
    }
}
