import Foundation

struct ChecklistItem: Codable, Identifiable, Equatable {
    let id: UUID
    var title: String
    var isDone: Bool
    var createdAt: Date
    var completedAt: Date?
    var sortOrder: Int

    init(title: String, sortOrder: Int) {
        self.id = UUID()
        self.title = String(title.prefix(100))
        self.isDone = false
        self.createdAt = Date()
        self.completedAt = nil
        self.sortOrder = sortOrder
    }
}
