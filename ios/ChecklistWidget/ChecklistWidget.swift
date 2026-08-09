import WidgetKit
import SwiftUI
import AppIntents

// MARK: - 타임라인

struct ChecklistEntry: TimelineEntry {
    let date: Date
    let categories: [Category]
    let selectedCategoryID: UUID?
    let items: [ChecklistItem]          // 선택된 카테고리의 표시 목록 (체크 포함, 정리 제외)
    let remainingCounts: [UUID: Int]    // 카테고리별 미체크 개수 (칩 숫자)
    let storeAvailable: Bool
}

struct ChecklistProvider: TimelineProvider {
    func placeholder(in context: Context) -> ChecklistEntry {
        let cat = Category(name: "살 것", icon: "🛒", sortOrder: 1)
        let items = ["우유", "계란", "화장지"].enumerated().map { i, t in
            ChecklistItem(title: t, categoryID: cat.id, sortOrder: i + 1)
        }
        return ChecklistEntry(
            date: Date(), categories: [cat], selectedCategoryID: cat.id,
            items: items, remainingCounts: [cat.id: items.count], storeAvailable: true
        )
    }

    func getSnapshot(in context: Context, completion: @escaping (ChecklistEntry) -> Void) {
        completion(currentEntry())
    }

    func getTimeline(in context: Context, completion: @escaping (Timeline<ChecklistEntry>) -> Void) {
        // 데이터는 이벤트(체크/추가/칩 전환)로만 바뀌므로 단일 엔트리, 시간 기반 갱신 없음(§9).
        completion(Timeline(entries: [currentEntry()], policy: .never))
    }

    private func currentEntry() -> ChecklistEntry {
        let store = ChecklistStore.shared
        guard store.isAvailable else {
            return ChecklistEntry(date: Date(), categories: [], selectedCategoryID: nil,
                                  items: [], remainingCounts: [:], storeAvailable: false)
        }
        let data = store.data()
        let categories = data.categories.sorted { $0.sortOrder < $1.sortOrder }
        let selectedID = store.effectiveWidgetCategoryID(in: data)
        let items = selectedID.map { store.items(in: $0) } ?? []
        var counts: [UUID: Int] = [:]
        for cat in categories {
            counts[cat.id] = data.items.filter { $0.categoryID == cat.id && !$0.isArchived && !$0.isDone }.count
        }
        return ChecklistEntry(date: Date(), categories: categories, selectedCategoryID: selectedID,
                              items: items, remainingCounts: counts, storeAvailable: true)
    }
}

// MARK: - 뷰

struct ChecklistWidgetView: View {
    @Environment(\.widgetFamily) private var family
    let entry: ChecklistEntry

    private var maxRows: Int { family == .systemLarge ? 8 : 3 }
    private var maxChips: Int { family == .systemLarge ? 5 : 3 }

    var body: some View {
        Group {
            if !entry.storeAvailable {
                Text("앱을 열어 시작하세요")
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
            } else {
                VStack(alignment: .leading, spacing: family == .systemLarge ? 7 : 4) {
                    chipRow
                    if entry.items.isEmpty {
                        emptyView
                    } else {
                        itemRows
                    }
                    Spacer(minLength: 0)
                }
            }
        }
        .containerBackground(for: .widget) { Color(uiColor: .systemBackground) }
        .widgetURL(URL(string: "checklist://add")) // 빈 영역 탭 → 앱 입력창 (F9)
    }

    // 카테고리 칩 — 탭하면 앱 실행 없이 위젯 안에서 전환
    private var chipRow: some View {
        HStack(spacing: 5) {
            ForEach(entry.categories.prefix(maxChips)) { cat in
                let selected = cat.id == entry.selectedCategoryID
                Button(intent: SelectWidgetCategoryIntent(categoryID: cat.id)) {
                    HStack(spacing: 3) {
                        Text("\(cat.icon) \(cat.name)")
                        Text("\(entry.remainingCounts[cat.id] ?? 0)")
                            .opacity(0.7)
                    }
                    .font(.caption2.weight(.bold))
                    .lineLimit(1)
                    .padding(.horizontal, 8)
                    .padding(.vertical, 3.5)
                    .background(
                        selected ? Color.green : Color(uiColor: .secondarySystemBackground),
                        in: Capsule()
                    )
                    .foregroundStyle(selected ? Color.white : Color.secondary)
                }
                .buttonStyle(.plain)
            }
            Spacer(minLength: 0)
        }
    }

    @ViewBuilder
    private var itemRows: some View {
        ForEach(entry.items.prefix(maxRows)) { item in
            HStack(spacing: 9) {
                Button(intent: ToggleItemIntent(itemID: item.id)) {
                    Image(systemName: item.isDone ? "checkmark.circle.fill" : "circle")
                        .font(.body)
                        .foregroundStyle(item.isDone ? Color.green : Color.secondary)
                }
                .buttonStyle(.plain)

                // 텍스트 탭 → 앱에서 해당 항목 열기 (F9)
                Link(destination: URL(string: "checklist://item/\(item.id.uuidString)")!) {
                    Text(item.title)
                        .font(.subheadline)
                        .lineLimit(1)
                        .strikethrough(item.isDone)
                        .foregroundStyle(item.isDone ? Color.secondary : Color.primary)
                }
                Spacer(minLength: 0)
            }
        }
        if entry.items.count > maxRows {
            HStack {
                Spacer()
                Text("+\(entry.items.count - maxRows)개 더")
                    .font(.caption2)
                    .foregroundStyle(.secondary)
            }
        }
    }

    private var emptyView: some View {
        VStack(spacing: 3) {
            Text("목록이 비었어요")
                .font(.subheadline.weight(.semibold))
            Text("탭해서 새 항목 추가")
                .font(.caption)
                .foregroundStyle(.secondary)
        }
        .frame(maxWidth: .infinity)
        .padding(.top, 8)
    }
}

// MARK: - 위젯 정의

struct ChecklistWidget: Widget {
    let kind = "ChecklistWidget"

    var body: some WidgetConfiguration {
        StaticConfiguration(kind: kind, provider: ChecklistProvider()) { entry in
            ChecklistWidgetView(entry: entry)
        }
        .configurationDisplayName("빠른 체크리스트")
        .description("앱을 열지 않고 홈 화면에서 바로 체크하세요.")
        .supportedFamilies([.systemMedium, .systemLarge])
    }
}

@main
struct ChecklistWidgetBundle: WidgetBundle {
    var body: some Widget {
        ChecklistWidget()
    }
}
