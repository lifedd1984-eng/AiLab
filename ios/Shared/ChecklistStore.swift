import Foundation

/// 앱과 위젯 익스텐션이 공유하는 저장소.
/// App Group 컨테이너의 items.json 하나가 단일 소스 — 마지막 쓰기 승리(§10).
final class ChecklistStore {
    static let appGroupID = "group.com.example.quickchecklist"
    static let shared = ChecklistStore()

    private let fileURL: URL?
    private let lock = NSLock()

    private init() {
        fileURL = FileManager.default
            .containerURL(forSecurityApplicationGroupIdentifier: Self.appGroupID)?
            .appendingPathComponent("items.json")
    }

    /// App Group 설정 누락/오류 시 false — 위젯은 "앱을 열어 시작하세요" 표시(§10).
    var isAvailable: Bool { fileURL != nil }

    // MARK: - 조회

    func activeItems() -> [ChecklistItem] {
        loadAll().filter { !$0.isDone }.sorted { $0.sortOrder < $1.sortOrder }
    }

    func completedItems() -> [ChecklistItem] {
        loadAll().filter(\.isDone)
            .sorted { ($0.completedAt ?? .distantPast) > ($1.completedAt ?? .distantPast) }
    }

    // MARK: - 변경

    func add(title: String) {
        let trimmed = title.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else { return }
        mutate { items in
            let nextOrder = (items.map(\.sortOrder).max() ?? 0) + 1
            items.append(ChecklistItem(title: trimmed, sortOrder: nextOrder))
        }
    }

    /// 존재하지 않는 id는 조용히 무시(no-op) — 위젯 체크와 앱 삭제가 겹친 경우(§10).
    func complete(id: UUID) {
        mutate { items in
            guard let i = items.firstIndex(where: { $0.id == id }) else { return }
            items[i].isDone = true
            items[i].completedAt = Date()
        }
    }

    func restore(id: UUID) {
        mutate { items in
            guard let i = items.firstIndex(where: { $0.id == id }) else { return }
            items[i].isDone = false
            items[i].completedAt = nil
            items[i].sortOrder = (items.filter { !$0.isDone }.map(\.sortOrder).max() ?? 0) + 1
        }
    }

    func update(id: UUID, title: String) {
        let trimmed = title.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else { return }
        mutate { items in
            guard let i = items.firstIndex(where: { $0.id == id }) else { return }
            items[i].title = String(trimmed.prefix(100))
        }
    }

    func remove(id: UUID) {
        mutate { items in items.removeAll { $0.id == id } }
    }

    /// 완료 후 7일 지난 항목 영구 삭제 — 앱 실행 시 호출(F4).
    func purgeExpired(olderThanDays days: Int = 7) {
        let cutoff = Date().addingTimeInterval(-Double(days) * 86_400)
        mutate { items in
            items.removeAll { $0.isDone && ($0.completedAt ?? .distantFuture) < cutoff }
        }
    }

    // MARK: - 파일 입출력

    private func loadAll() -> [ChecklistItem] {
        guard let url = fileURL, let data = try? Data(contentsOf: url) else { return [] }
        return (try? JSONDecoder().decode([ChecklistItem].self, from: data)) ?? []
    }

    private func mutate(_ change: (inout [ChecklistItem]) -> Void) {
        lock.lock()
        defer { lock.unlock() }
        guard let url = fileURL else { return }
        var items = loadAll()
        change(&items)
        if let data = try? JSONEncoder().encode(items) {
            try? data.write(to: url, options: .atomic)
        }
    }
}
