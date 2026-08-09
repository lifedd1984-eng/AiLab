package com.example.quickchecklist.data

import kotlinx.serialization.Serializable
import java.util.UUID

// iOS와 동일한 스키마 (기획안 §8, §15)

@Serializable
data class Category(
    val id: String = UUID.randomUUID().toString(),
    var name: String,
    var icon: String,
    var sortOrder: Int,
)

@Serializable
data class ChecklistItem(
    val id: String = UUID.randomUUID().toString(),
    val categoryId: String,
    var title: String,
    var isDone: Boolean = false,      // 체크됨 — 목록에 취소선으로 유지
    var isArchived: Boolean = false,  // "정리"로 히스토리로 이동됨
    val createdAt: Long = 0L,
    var completedAt: Long? = null,
    var sortOrder: Int,
)

@Serializable
data class StoreData(
    val categories: MutableList<Category> = mutableListOf(),
    val items: MutableList<ChecklistItem> = mutableListOf(),
)
