import WidgetKit
import SwiftUI
import AppIntents

// MARK: - 타임라인

struct ChecklistEntry: TimelineEntry {
    let date: Date
    let items: [ChecklistItem]
    let storeAvailable: Bool
}

struct ChecklistProvider: TimelineProvider {
    func placeholder(in context: Context) -> ChecklistEntry {
        ChecklistEntry(
            date: Date(),
            items: [
                ChecklistItem(title: "우유 사기", sortOrder: 1),
                ChecklistItem(title: "세탁물 찾기", sortOrder: 2),
                ChecklistItem(title: "전기요금 납부", sortOrder: 3),
            ],
            storeAvailable: true
        )
    }

    func getSnapshot(in context: Context, completion: @escaping (ChecklistEntry) -> Void) {
        completion(currentEntry())
    }

    func getTimeline(in context: Context, completion: @escaping (Timeline<ChecklistEntry>) -> Void) {
        // 데이터는 이벤트(체크/추가)로만 바뀌므로 단일 엔트리, 시간 기반 갱신 없음(§9).
        completion(Timeline(entries: [currentEntry()], policy: .never))
    }

    private func currentEntry() -> ChecklistEntry {
        let store = ChecklistStore.shared
        return ChecklistEntry(
            date: Date(),
            items: store.isAvailable ? store.activeItems() : [],
            storeAvailable: store.isAvailable
        )
    }
}

// MARK: - 뷰

struct ChecklistWidgetView: View {
    @Environment(\.widgetFamily) private var family
    let entry: ChecklistEntry

    private var maxRows: Int { family == .systemLarge ? 8 : 3 }

    var body: some View {
        Group {
            if !entry.storeAvailable {
                Text("앱을 열어 시작하세요")
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
            } else if entry.items.isEmpty {
                emptyView
            } else {
                listView
            }
        }
        .containerBackground(for: .widget) { Color(uiColor: .systemBackground) }
        .widgetURL(URL(string: "checklist://add")) // 헤더·빈 영역 탭 → 입력창 (F9)
    }

    private var emptyView: some View {
        VStack(spacing: 4) {
            Text("할 일 끝! ✨")
                .font(.subheadline.weight(.semibold))
            Text("탭해서 새 항목 추가")
                .font(.caption)
                .foregroundStyle(.secondary)
        }
    }

    private var listView: some View {
        VStack(alignment: .leading, spacing: family == .systemLarge ? 8 : 5) {
            HStack(alignment: .firstTextBaseline) {
                Text("체크리스트")
                    .font(.footnote.weight(.bold))
                Spacer()
                Text("\(entry.items.count)개")
                    .font(.caption.weight(.semibold))
                    .foregroundStyle(.green)
            }

            ForEach(entry.items.prefix(maxRows)) { item in
                HStack(spacing: 9) {
                    Button(intent: CompleteItemIntent(itemID: item.id)) {
                        Image(systemName: "circle")
                            .font(.body)
                            .foregroundStyle(.secondary)
                    }
                    .buttonStyle(.plain)

                    // 텍스트 탭 → 앱에서 해당 항목 열기 (F9)
                    Link(destination: URL(string: "checklist://item/\(item.id.uuidString)")!) {
                        Text(item.title)
                            .font(.subheadline)
                            .lineLimit(1)
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
            Spacer(minLength: 0)
        }
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
