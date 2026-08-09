import Foundation

struct Category: Codable, Identifiable, Equatable {
    let id: UUID
    var name: String
    var icon: String
    var sortOrder: Int

    init(name: String, icon: String, sortOrder: Int) {
        self.id = UUID()
        self.name = String(name.prefix(20))
        self.icon = icon
        self.sortOrder = sortOrder
    }
}

struct ChecklistItem: Codable, Identifiable, Equatable {
    let id: UUID
    var categoryID: UUID
    var title: String
    var isDone: Bool       // 체크됨 — 목록에 취소선으로 유지
    var isArchived: Bool   // "정리"로 히스토리로 이동됨
    var createdAt: Date
    var completedAt: Date?
    var sortOrder: Int

    init(title: String, categoryID: UUID, sortOrder: Int) {
        self.id = UUID()
        self.categoryID = categoryID
        self.title = String(title.prefix(100))
        self.isDone = false
        self.isArchived = false
        self.createdAt = Date()
        self.completedAt = nil
        self.sortOrder = sortOrder
    }
}

struct StoreData: Codable {
    var categories: [Category]
    var items: [ChecklistItem]
}
