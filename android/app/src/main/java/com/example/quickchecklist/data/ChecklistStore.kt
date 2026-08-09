package com.example.quickchecklist.data

import android.content.Context
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

/**
 * 앱과 위젯이 공유하는 저장소. Android는 위젯이 앱 프로세스에서 실행되므로
 * (iOS의 App Group 없이) filesDir의 store.json 하나가 단일 소스.
 */
object ChecklistStore {
    private const val FILE_NAME = "store.json"
    private const val PREFS = "widget_prefs"
    private const val KEY_WIDGET_CATEGORY = "widgetCategoryId"

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val lock = Any()

    // MARK: 조회

    fun load(context: Context): StoreData = synchronized(lock) { loadLocked(context) }

    fun items(context: Context, categoryId: String): List<ChecklistItem> =
        load(context).items
            .filter { it.categoryId == categoryId && !it.isArchived }
            .sortedBy { it.sortOrder }

    fun archivedItems(context: Context): List<ChecklistItem> =
        load(context).items.filter { it.isArchived }
            .sortedByDescending { it.completedAt ?: 0L }

    // MARK: 위젯 표시 카테고리 (칩 탭으로 전환)

    fun widgetCategoryId(context: Context, data: StoreData): String? {
        val saved = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_WIDGET_CATEGORY, null)
        if (saved != null && data.categories.any { it.id == saved }) return saved
        return data.categories.minByOrNull { it.sortOrder }?.id
    }

    fun setWidgetCategory(context: Context, categoryId: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY_WIDGET_CATEGORY, categoryId).apply()
    }

    // MARK: 변경

    fun addCategory(context: Context, name: String, icon: String): String? {
        val trimmed = name.trim().take(20)
        if (trimmed.isEmpty()) return null
        var newId: String? = null
        mutate(context) { d ->
            val next = (d.categories.maxOfOrNull { it.sortOrder } ?: 0) + 1
            val cat = Category(name = trimmed, icon = icon, sortOrder = next)
            d.categories.add(cat)
            newId = cat.id
        }
        return newId
    }

    fun addItem(context: Context, title: String, categoryId: String) {
        val trimmed = title.trim().take(100)
        if (trimmed.isEmpty()) return
        mutate(context) { d ->
            val next = (d.items.filter { it.categoryId == categoryId }
                .maxOfOrNull { it.sortOrder } ?: 0) + 1
            d.items.add(
                ChecklistItem(
                    categoryId = categoryId, title = trimmed,
                    createdAt = System.currentTimeMillis(), sortOrder = next
                )
            )
        }
    }

    /** 체크 토글 — 줄긋기 유지. 존재하지 않는 id는 no-op(§10). */
    fun toggle(context: Context, itemId: String) {
        mutate(context) { d ->
            val item = d.items.find { it.id == itemId } ?: return@mutate
            item.isDone = !item.isDone
            item.completedAt = if (item.isDone) System.currentTimeMillis() else null
        }
    }

    fun rename(context: Context, itemId: String, title: String) {
        val trimmed = title.trim().take(100)
        if (trimmed.isEmpty()) return
        mutate(context) { d -> d.items.find { it.id == itemId }?.title = trimmed }
    }

    fun removeItem(context: Context, itemId: String) {
        mutate(context) { d -> d.items.removeAll { it.id == itemId } }
    }

    /** "체크된 N개 정리" — 히스토리로 이동 (삭제 아님, 무기한 보관) */
    fun tidy(context: Context, categoryId: String) {
        mutate(context) { d ->
            d.items.filter { it.categoryId == categoryId && it.isDone && !it.isArchived }
                .forEach {
                    it.isArchived = true
                    if (it.completedAt == null) it.completedAt = System.currentTimeMillis()
                }
        }
    }

    /** 히스토리 "되돌리기" — 원래 카테고리 목록 맨 아래로 복원 */
    fun restore(context: Context, itemId: String) {
        mutate(context) { d ->
            val item = d.items.find { it.id == itemId } ?: return@mutate
            item.isArchived = false
            item.isDone = false
            item.completedAt = null
            item.sortOrder = (d.items
                .filter { it.categoryId == item.categoryId && !it.isArchived }
                .maxOfOrNull { it.sortOrder } ?: 0) + 1
        }
    }

    // MARK: 파일 입출력

    private fun file(context: Context) = File(context.filesDir, FILE_NAME)

    private fun loadLocked(context: Context): StoreData {
        val f = file(context)
        if (f.exists()) {
            runCatching { return json.decodeFromString<StoreData>(f.readText()) }
        }
        // 최초 실행: 기본 카테고리 시드 후 영속화 (iOS와 동일)
        val seeded = StoreData(
            categories = mutableListOf(
                Category(name = "살 것", icon = "🛒", sortOrder = 1),
                Category(name = "할 것", icon = "📝", sortOrder = 2),
            )
        )
        writeLocked(context, seeded)
        return seeded
    }

    private fun writeLocked(context: Context, data: StoreData) {
        runCatching { file(context).writeText(json.encodeToString(data)) }
    }

    private fun mutate(context: Context, change: (StoreData) -> Unit) {
        synchronized(lock) {
            val d = loadLocked(context)
            change(d)
            writeLocked(context, d)
        }
    }
}
