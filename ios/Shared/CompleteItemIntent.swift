import AppIntents
import WidgetKit

/// 위젯 체크박스 버튼이 실행하는 인텐트 (F8).
/// 위젯 익스텐션 프로세스에서 실행되므로 앱이 종료된 상태에서도 동작한다.
struct CompleteItemIntent: AppIntent {
    static var title: LocalizedStringResource = "항목 완료"
    static var isDiscoverable: Bool = false

    @Parameter(title: "항목 ID")
    var itemID: String

    init() {}

    init(itemID: UUID) {
        self.itemID = itemID.uuidString
    }

    func perform() async throws -> some IntentResult {
        if let id = UUID(uuidString: itemID) {
            ChecklistStore.shared.complete(id: id)
        }
        WidgetCenter.shared.reloadAllTimelines()
        return .result()
    }
}
