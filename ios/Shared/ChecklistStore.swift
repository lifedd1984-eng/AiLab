import Foundation

/// 앱과 위젯 익스텐션이 공유하는 저장소.
/// App Group 컨테이너의 store.json 하나가 단일 소스 — 마지막 쓰기 승리(§10).
final class ChecklistStore {
    static let appGroupID = "group.com.lifedd.quickchecklist"
    static let shared = ChecklistStore()

    private let fileURL: URL?
    private let defaults: UserDefaults?
    private let lock = NSLock()
    private static let widgetCategoryKey = "widgetCategoryID"

    private init() {
        let container = FileManager.default
            .containerURL(forSecurityApplicationGroupIdentifier: Self.appGroupID)
        fileURL = container?.appendingPathComponent("store.json")
        defaults = UserDefaults(suiteName: Self.appGroupID)
    }

    /// App Group 설정 누락/오류 시 false — 위젯은 "앱을 열어 시작하세요" 표시(§10).
    var isAvailable: Bool { fileURL != nil }

    // MARK: - 조회

    func data() -> StoreData {
        lock.lock()
        defer { lock.unlock() }
        return loadLocked()
    }

    func categories() -> [Category] {
        data().categories.sorted { $0.sortOrder < $1.sortOrder }
    }

    /// 카테고리의 표시 목록 (정리되지 않은 항목 — 체크된 것 포함)
    func items(in categoryID: UUID) -> [ChecklistItem] {
        data().items
            .filter { $0.categoryID == categoryID && !$0.isArchived }
            .sorted { $0.sortOrder < $1.sortOrder }
    }

    /// 히스토리 (정리된 항목, 완료 시각 역순)
    func archivedItems() -> [ChecklistItem] {
        data().items.filter(\.isArchived)
            .sorted { ($0.completedAt ?? .distantPast) > ($1.completedAt ?? .distantPast) }
    }

    // MARK: - 위젯 표시 카테고리 (칩 탭으로 전환, App Group에 저장)

    var widgetCategoryID: UUID? {
        get { defaults?.string(forKey: Self.widgetCategoryKey).flatMap(UUID.init) }
        set { defaults?.set(newValue?.uuidString, forKey: Self.widgetCategoryKey) }
    }

    func effectiveWidgetCategoryID(in data: StoreData) -> UUID? {
        if let id = widgetCategoryID, data.categories.contains(where: { $0.id == id }) {
            return id
        }
        return data.categories.sorted { $0.sortOrder < $1.sortOrder }.first?.id
    }

    // MARK: - 변경

    @discardableResult
    func addCategory(name: String, icon: String) -> UUID? {
        let trimmed = name.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else { return nil }
        var newID: UUID?
        mutate { d in
            let next = (d.categories.map(\.sortOrder).max() ?? 0) + 1
            let cat = Category(name: trimmed, icon: icon, sortOrder: next)
            d.categories.append(cat)
            newID = cat.id
        }
        return newID
    }

    func addItem(title: String, categoryID: UUID) {
        let trimmed = title.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else { return }
        mutate { d in
            let next = (d.items.filter { $0.categoryID == categoryID }.map(\.sortOrder).max() ?? 0) + 1
            d.items.append(ChecklistItem(title: trimmed, categoryID: categoryID, sortOrder: next))
        }
    }

    /// 체크 토글 — 줄긋기 유지, 사라지지 않음. 존재하지 않는 id는 no-op(§10).
    func toggle(id: UUID) {
        mutate { d in
            guard let i = d.items.firstIndex(where: { $0.id == id }) else { return }
            d.items[i].isDone.toggle()
            d.items[i].completedAt = d.items[i].isDone ? Date() : nil
        }
    }

    func rename(id: UUID, title: String) {
        let trimmed = title.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else { return }
        mutate { d in
            guard let i = d.items.firstIndex(where: { $0.id == id }) else { return }
            d.items[i].title = String(trimmed.prefix(100))
        }
    }

    func removeItem(id: UUID) {
        mutate { d in d.items.removeAll { $0.id == id } }
    }

    /// "체크된 N개 정리" — 체크된 항목을 목록에서 치워 히스토리로 이동 (삭제 아님, 무기한 보관)
    func tidy(categoryID: UUID) {
        mutate { d in
            for i in d.items.indices
            where d.items[i].categoryID == categoryID && d.items[i].isDone && !d.items[i].isArchived {
                d.items[i].isArchived = true
                if d.items[i].completedAt == nil { d.items[i].completedAt = Date() }
            }
        }
    }

    /// 히스토리 "되돌리기" — 원래 카테고리 목록 맨 아래로 복원
    func restore(id: UUID) {
        mutate { d in
            guard let i = d.items.firstIndex(where: { $0.id == id }) else { return }
            let categoryID = d.items[i].categoryID
            d.items[i].isArchived = false
            d.items[i].isDone = false
            d.items[i].completedAt = nil
            d.items[i].sortOrder =
                (d.items.filter { $0.categoryID == categoryID && !$0.isArchived }.map(\.sortOrder).max() ?? 0) + 1
        }
    }

    // MARK: - 파일 입출력

    private func loadLocked() -> StoreData {
        guard let url = fileURL else { return StoreData(categories: [], items: []) }
        if let raw = try? Data(contentsOf: url),
           let decoded = try? JSONDecoder().decode(StoreData.self, from: raw) {
            return decoded
        }
        // 최초 실행: 기본 카테고리 시드 후 저장 (id가 프로세스마다 달라지지 않도록 반드시 영속화)
        let seeded = StoreData(
            categories: [
                Category(name: "살 것", icon: "🛒", sortOrder: 1),
                Category(name: "할 것", icon: "📝", sortOrder: 2),
            ],
            items: []
        )
        writeLocked(seeded)
        return seeded
    }

    private func writeLocked(_ data: StoreData) {
        guard let url = fileURL, let raw = try? JSONEncoder().encode(data) else { return }
        try? raw.write(to: url, options: .atomic)
    }

    private func mutate(_ change: (inout StoreData) -> Void) {
        lock.lock()
        defer { lock.unlock() }
        var d = loadLocked()
        change(&d)
        writeLocked(d)
    }
}
