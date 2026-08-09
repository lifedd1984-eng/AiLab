package com.example.quickchecklist

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.appwidget.updateAll
import com.example.quickchecklist.data.Category
import com.example.quickchecklist.data.ChecklistItem
import com.example.quickchecklist.data.ChecklistStore
import com.example.quickchecklist.widget.ChecklistGlanceWidget
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private val Green = Color(0xFF1F9D5B)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(Modifier.fillMaxSize()) {
                    ChecklistApp()
                }
            }
        }
    }
}

@Composable
fun ChecklistApp() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // tick 증가 → 저장소에서 다시 읽어 전체 갱신
    var tick by remember { mutableIntStateOf(0) }
    val data = remember(tick) { ChecklistStore.load(context) }
    val categories = remember(tick) { data.categories.sortedBy { it.sortOrder } }

    var selectedCategoryId by rememberSaveable { mutableStateOf<String?>(null) }
    if (selectedCategoryId == null || categories.none { it.id == selectedCategoryId }) {
        selectedCategoryId = categories.firstOrNull()?.id
    }
    val currentItems = remember(tick, selectedCategoryId) {
        data.items.filter { it.categoryId == selectedCategoryId && !it.isArchived }
            .sortedBy { it.sortOrder }
    }
    val checkedCount = currentItems.count { it.isDone }

    var showHistory by rememberSaveable { mutableStateOf(false) }
    var showCategoryForm by remember { mutableStateOf(false) }
    var renameTarget by remember { mutableStateOf<ChecklistItem?>(null) }

    fun refresh() {
        tick++
        scope.launch { ChecklistGlanceWidget().updateAll(context) }
    }

    if (showHistory) {
        HistoryScreen(onBack = { showHistory = false; refresh() }, context = context)
        return
    }

    Scaffold { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            // 타이틀 + 히스토리 버튼
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("빠른 체크리스트", style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.weight(1f))
                IconButton(onClick = { showHistory = true }) {
                    Icon(Icons.Filled.Refresh, contentDescription = "완료한 목록")
                }
            }

            CategoryChips(
                categories = categories,
                data = data,
                selectedId = selectedCategoryId,
                onSelect = { selectedCategoryId = it },
                onAdd = { showCategoryForm = true },
            )

            AddItemField(onAdd = { title ->
                selectedCategoryId?.let {
                    ChecklistStore.addItem(context, title, it)
                    refresh()
                }
            })

            LazyColumn(Modifier.weight(1f).padding(horizontal = 16.dp)) {
                if (currentItems.isEmpty()) {
                    item {
                        Text(
                            "아래가 아니라 위! 입력창에 첫 항목을 적어보세요",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                        )
                    }
                }
                items(currentItems, key = { it.id }) { item ->
                    ItemRow(
                        item = item,
                        onToggle = { ChecklistStore.toggle(context, item.id); refresh() },
                        onRename = { renameTarget = item },
                        onDelete = { ChecklistStore.removeItem(context, item.id); refresh() },
                    )
                }
                if (checkedCount > 0) {
                    item {
                        TextButton(
                            onClick = {
                                selectedCategoryId?.let { ChecklistStore.tidy(context, it); refresh() }
                            },
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        ) {
                            Text("체크된 ${checkedCount}개 정리 → 히스토리", color = Green)
                        }
                    }
                }
            }
        }
    }

    if (showCategoryForm) {
        CategoryFormDialog(
            onDismiss = { showCategoryForm = false },
            onAdd = { name, icon ->
                ChecklistStore.addCategory(context, name, icon)?.let { selectedCategoryId = it }
                showCategoryForm = false
                refresh()
            },
        )
    }

    renameTarget?.let { target ->
        RenameDialog(
            initial = target.title,
            onDismiss = { renameTarget = null },
            onSave = { newTitle ->
                ChecklistStore.rename(context, target.id, newTitle)
                renameTarget = null
                refresh()
            },
        )
    }
}

@Composable
private fun CategoryChips(
    categories: List<Category>,
    data: com.example.quickchecklist.data.StoreData,
    selectedId: String?,
    onSelect: (String) -> Unit,
    onAdd: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        categories.forEach { cat ->
            val selected = cat.id == selectedId
            val remaining = data.items.count { it.categoryId == cat.id && !it.isArchived && !it.isDone }
            Text(
                text = "${cat.icon} ${cat.name}  $remaining",
                fontSize = 13.sp,
                color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .background(
                        if (selected) Green else MaterialTheme.colorScheme.surfaceVariant,
                        RoundedCornerShape(50),
                    )
                    .clickable { onSelect(cat.id) }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
            )
        }
        Text(
            text = "＋ 추가",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(50))
                .clickable { onAdd() }
                .padding(horizontal = 14.dp, vertical = 8.dp),
        )
    }
}

@Composable
private fun AddItemField(onAdd: (String) -> Unit) {
    var text by rememberSaveable { mutableStateOf("") }
    OutlinedTextField(
        value = text,
        onValueChange = { if (it.length <= 100) text = it },
        placeholder = { Text("새 항목 입력…") },
        singleLine = true,
        keyboardActions = KeyboardActions(onDone = {
            if (text.isNotBlank()) { onAdd(text); text = "" } // 입력창 유지 → 연속 입력
        }),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

@Composable
private fun ItemRow(
    item: ChecklistItem,
    onToggle: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // 체크 원 (44dp 탭 영역)
        Text(
            text = if (item.isDone) "✓" else "",
            color = Color.White,
            fontSize = 14.sp,
            modifier = Modifier
                .size(28.dp)
                .background(
                    if (item.isDone) Green else Color.Transparent,
                    CircleShape,
                )
                .border(
                    2.dp,
                    if (item.isDone) Green else MaterialTheme.colorScheme.outline,
                    CircleShape,
                )
                .clickable { onToggle() }
                .padding(top = 3.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = item.title,
            fontSize = 16.sp,
            textDecoration = if (item.isDone) TextDecoration.LineThrough else TextDecoration.None,
            color = if (item.isDone) MaterialTheme.colorScheme.onSurfaceVariant
                    else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f).clickable(enabled = !item.isDone) { onRename() },
        )
        TextButton(onClick = onDelete) {
            Text("삭제", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun CategoryFormDialog(onDismiss: () -> Unit, onAdd: (String, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var icon by remember { mutableStateOf("🛒") }
    val emojis = listOf("🛒", "📝", "🏠", "💼", "🎯", "📚")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("새 카테고리") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { if (it.length <= 20) name = it },
                    placeholder = { Text("예: 냉장고 채우기") },
                    singleLine = true,
                )
                Spacer(Modifier.padding(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    emojis.forEach { e ->
                        Text(
                            e,
                            fontSize = 20.sp,
                            modifier = Modifier
                                .background(
                                    if (icon == e) Green.copy(alpha = 0.2f)
                                    else MaterialTheme.colorScheme.surfaceVariant,
                                    RoundedCornerShape(8.dp),
                                )
                                .border(
                                    1.5.dp,
                                    if (icon == e) Green else Color.Transparent,
                                    RoundedCornerShape(8.dp),
                                )
                                .clickable { icon = e }
                                .padding(8.dp),
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { if (name.isNotBlank()) onAdd(name, icon) },
                colors = ButtonDefaults.buttonColors(containerColor = Green),
            ) { Text("추가") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("취소") } },
    )
}

@Composable
private fun RenameDialog(initial: String, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var text by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("이름 바꾸기") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { if (it.length <= 100) text = it },
                singleLine = true,
            )
        },
        confirmButton = {
            Button(
                onClick = { if (text.isNotBlank()) onSave(text) },
                colors = ButtonDefaults.buttonColors(containerColor = Green),
            ) { Text("저장") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("취소") } },
    )
}

@Composable
private fun HistoryScreen(onBack: () -> Unit, context: Context) {
    var tick by remember { mutableIntStateOf(0) }
    val archived = remember(tick) { ChecklistStore.archivedItems(context) }
    val categoryById = remember(tick) {
        ChecklistStore.load(context).categories.associateBy { it.id }
    }

    val calendar = Calendar.getInstance()
    val dayFormat = SimpleDateFormat("M월 d일 (E)", Locale.KOREAN)
    val timeFormat = SimpleDateFormat("H:mm", Locale.KOREAN)

    fun dayLabel(at: Long): String {
        val base = dayFormat.format(Date(at))
        val today = Calendar.getInstance()
        calendar.timeInMillis = at
        return when {
            calendar.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                calendar.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR) -> "$base · 오늘"
            else -> base
        }
    }

    val groups = archived.groupBy { dayLabel(it.completedAt ?: 0L) }

    Scaffold { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onBack) { Text("‹ 뒤로", color = Green) }
                Text("완료한 목록", style = MaterialTheme.typography.titleLarge)
            }
            LazyColumn(Modifier.padding(horizontal = 16.dp)) {
                if (archived.isEmpty()) {
                    item {
                        Text(
                            "아직 완료한 항목이 없어요",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp),
                        )
                    }
                }
                groups.forEach { (label, itemsInDay) ->
                    item {
                        Text(
                            label,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 14.dp, bottom = 6.dp),
                        )
                    }
                    items(itemsInDay, key = { it.id }) { item ->
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            categoryById[item.categoryId]?.let { cat ->
                                Text(
                                    "${cat.icon} ${cat.name}",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier
                                        .background(
                                            MaterialTheme.colorScheme.surfaceVariant,
                                            RoundedCornerShape(6.dp),
                                        )
                                        .padding(horizontal = 6.dp, vertical = 2.dp),
                                )
                                Spacer(Modifier.width(8.dp))
                            }
                            Text(
                                item.title,
                                textDecoration = TextDecoration.LineThrough,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                            )
                            item.completedAt?.let {
                                Text(
                                    timeFormat.format(Date(it)),
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.outline,
                                )
                            }
                            TextButton(onClick = {
                                ChecklistStore.restore(context, item.id)
                                tick++
                            }) { Text("되돌리기", fontSize = 12.sp, color = Green) }
                        }
                    }
                }
            }
        }
    }
}
