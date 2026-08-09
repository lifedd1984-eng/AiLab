import AppIntents
import WidgetKit

/// 위젯 체크박스 버튼 — 앱 실행 없이 체크 토글 (줄긋기 유지/해제).
struct ToggleItemIntent: AppIntent {
    static var title: LocalizedStringResource = "항목 체크"
    static var isDiscoverable: Bool = false

    @Parameter(title: "항목 ID")
    var itemID: String

    init() {}

    init(itemID: UUID) {
        self.itemID = itemID.uuidString
    }

    func perform() async throws -> some IntentResult {
        if let id = UUID(uuidString: itemID) {
            ChecklistStore.shared.toggle(id: id)
        }
        WidgetCenter.shared.reloadAllTimelines()
        return .result()
    }
}

/// 위젯 카테고리 칩 버튼 — 앱 실행 없이 위젯 표시 카테고리 전환.
struct SelectWidgetCategoryIntent: AppIntent {
    static var title: LocalizedStringResource = "카테고리 전환"
    static var isDiscoverable: Bool = false

    @Parameter(title: "카테고리 ID")
    var categoryID: String

    init() {}

    init(categoryID: UUID) {
        self.categoryID = categoryID.uuidString
    }

    func perform() async throws -> some IntentResult {
        if let id = UUID(uuidString: categoryID) {
            ChecklistStore.shared.widgetCategoryID = id
        }
        WidgetCenter.shared.reloadAllTimelines()
        return .result()
    }
}
