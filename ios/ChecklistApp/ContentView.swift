import SwiftUI
import WidgetKit

struct ContentView: View {
    @State private var data = StoreData(categories: [], items: [])
    @State private var selectedCategoryID: UUID?
    @State private var newTitle = ""
    @State private var showHistory = false
    @State private var showCategoryForm = false
    @State private var renameTarget: ChecklistItem?
    @State private var renameText = ""
    @FocusState private var inputFocused: Bool
    @Environment(\.scenePhase) private var scenePhase

    private let store = ChecklistStore.shared

    private var categories: [Category] {
        data.categories.sorted { $0.sortOrder < $1.sortOrder }
    }
    private var currentItems: [ChecklistItem] {
        guard let id = selectedCategoryID else { return [] }
        return data.items
            .filter { $0.categoryID == id && !$0.isArchived }
            .sorted { $0.sortOrder < $1.sortOrder }
    }
    private var checkedCount: Int { currentItems.filter(\.isDone).count }

    var body: some View {
        NavigationStack {
            VStack(spacing: 0) {
                chipRow
                List {
                    inputSection
                    itemSection
                    if checkedCount > 0 { tidySection }
                }
            }
            .navigationTitle("빠른 체크리스트")
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button {
                        showHistory = true
                    } label: {
                        Image(systemName: "clock.arrow.circlepath")
                    }
                    .accessibilityLabel("완료한 목록")
                }
            }
        }
        .sheet(isPresented: $showHistory, onDismiss: reload) {
            HistoryView()
        }
        .sheet(isPresented: $showCategoryForm) {
            CategoryFormView { name, icon in
                if let newID = store.addCategory(name: name, icon: icon) {
                    selectedCategoryID = newID
                }
                reload()
                inputFocused = true
            }
        }
        .alert("이름 바꾸기", isPresented: renameAlertShown, presenting: renameTarget) { item in
            TextField("항목 이름", text: $renameText)
            Button("저장") {
                store.rename(id: item.id, title: renameText)
                reload()
            }
            Button("취소", role: .cancel) {}
        }
        .onAppear {
            reload()
            if currentItems.isEmpty { inputFocused = true }
        }
        .onChange(of: scenePhase) { _, phase in
            if phase == .active { reload() }
        }
        .onOpenURL(perform: handleDeepLink)
    }

    // MARK: - 카테고리 칩

    private var chipRow: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 8) {
                ForEach(categories) { cat in
                    let selected = cat.id == selectedCategoryID
                    let remaining = data.items
                        .filter { $0.categoryID == cat.id && !$0.isArchived && !$0.isDone }
                        .count
                    Button {
                        selectedCategoryID = cat.id
                    } label: {
                        HStack(spacing: 5) {
                            Text("\(cat.icon) \(cat.name)")
                            Text("\(remaining)").opacity(0.7)
                        }
                        .font(.footnote.weight(.semibold))
                        .padding(.horizontal, 13)
                        .padding(.vertical, 7)
                        .background(
                            selected ? Color.green : Color(uiColor: .secondarySystemBackground),
                            in: Capsule()
                        )
                        .foregroundStyle(selected ? Color.white : Color.primary)
                    }
                    .buttonStyle(.plain)
                }
                Button {
                    showCategoryForm = true
                } label: {
                    Text("＋ 추가")
                        .font(.footnote.weight(.semibold))
                        .padding(.horizontal, 13)
                        .padding(.vertical, 7)
                        .overlay(Capsule().strokeBorder(Color.secondary.opacity(0.5), style: StrokeStyle(lineWidth: 1.5, dash: [4])))
                        .foregroundStyle(.secondary)
                }
                .buttonStyle(.plain)
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 8)
        }
    }

    // MARK: - 목록

    private var inputSection: some View {
        Section {
            HStack(spacing: 10) {
                Image(systemName: "plus")
                    .foregroundStyle(.green)
                    .fontWeight(.semibold)
                TextField("새 항목 입력…", text: $newTitle)
                    .focused($inputFocused)
                    .submitLabel(.done)
                    .onSubmit(addItem)
            }
        }
    }

    private var itemSection: some View {
        Section {
            if currentItems.isEmpty {
                Text("아래가 아니라 위! 입력창에 첫 항목을 적어보세요")
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
            }
            ForEach(currentItems) { item in
                HStack(spacing: 12) {
                    Button {
                        withAnimation { store.toggle(id: item.id); reload() }
                    } label: {
                        Image(systemName: item.isDone ? "checkmark.circle.fill" : "circle")
                            .font(.title3)
                            .foregroundStyle(item.isDone ? Color.green : Color.secondary)
                            .frame(width: 44, height: 44)
                    }
                    .buttonStyle(.plain)
                    Text(item.title)
                        .strikethrough(item.isDone)
                        .foregroundStyle(item.isDone ? Color.secondary : Color.primary)
                        .contentShape(Rectangle())
                        .onTapGesture {
                            guard !item.isDone else { return }
                            renameText = item.title
                            renameTarget = item
                        }
                    Spacer(minLength: 0)
                }
                .listRowInsets(EdgeInsets(top: 0, leading: 12, bottom: 0, trailing: 16))
            }
            .onDelete(perform: deleteItems)
        }
    }

    private var tidySection: some View {
        Section {
            Button {
                guard let id = selectedCategoryID else { return }
                withAnimation { store.tidy(categoryID: id); reload() }
            } label: {
                Text("체크된 \(checkedCount)개 정리 → 히스토리")
                    .font(.subheadline.weight(.semibold))
                    .frame(maxWidth: .infinity)
            }
        }
    }

    // MARK: - 동작

    private var renameAlertShown: Binding<Bool> {
        Binding(
            get: { renameTarget != nil },
            set: { if !$0 { renameTarget = nil } }
        )
    }

    private func addItem() {
        guard let id = selectedCategoryID else { return }
        store.addItem(title: newTitle, categoryID: id)
        newTitle = ""
        reload()
        inputFocused = true // 연속 입력 (F1)
    }

    private func deleteItems(at offsets: IndexSet) {
        withAnimation {
            for index in offsets {
                store.removeItem(id: currentItems[index].id)
            }
            reload()
        }
    }

    private func handleDeepLink(_ url: URL) {
        // checklist://add — 위젯 빈 영역/빈 상태 탭 → 위젯 카테고리로 전환 + 입력창 포커스
        // checklist://item/{uuid} — 위젯 텍스트 탭 → 해당 항목의 카테고리로 전환
        switch url.host() {
        case "add":
            if let widgetCat = store.effectiveWidgetCategoryID(in: data) {
                selectedCategoryID = widgetCat
            }
            inputFocused = true
        case "item":
            let idString = url.lastPathComponent
            if let id = UUID(uuidString: idString),
               let item = data.items.first(where: { $0.id == id }) {
                selectedCategoryID = item.categoryID
            }
        default:
            break
        }
    }

    private func reload() {
        data = store.data()
        if selectedCategoryID == nil || !data.categories.contains(where: { $0.id == selectedCategoryID }) {
            selectedCategoryID = categories.first?.id
        }
        WidgetCenter.shared.reloadAllTimelines()
    }
}

// MARK: - 카테고리 추가 시트

struct CategoryFormView: View {
    @Environment(\.dismiss) private var dismiss
    @State private var name = ""
    @State private var icon = "🛒"
    @FocusState private var nameFocused: Bool

    var onAdd: (String, String) -> Void

    private let emojis = ["🛒", "📝", "🏠", "💼", "🎯", "📚"]

    var body: some View {
        NavigationStack {
            Form {
                TextField("새 카테고리 이름 (예: 냉장고 채우기)", text: $name)
                    .focused($nameFocused)
                    .submitLabel(.done)
                    .onSubmit(submit)
                HStack(spacing: 10) {
                    ForEach(emojis, id: \.self) { e in
                        Button {
                            icon = e
                        } label: {
                            Text(e)
                                .font(.title3)
                                .frame(width: 40, height: 40)
                                .background(
                                    icon == e ? Color.green.opacity(0.2) : Color(uiColor: .secondarySystemBackground),
                                    in: RoundedRectangle(cornerRadius: 9)
                                )
                                .overlay(
                                    RoundedRectangle(cornerRadius: 9)
                                        .strokeBorder(icon == e ? Color.green : Color.clear, lineWidth: 1.5)
                                )
                        }
                        .buttonStyle(.plain)
                    }
                }
            }
            .navigationTitle("새 카테고리")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarLeading) {
                    Button("취소") { dismiss() }
                }
                ToolbarItem(placement: .topBarTrailing) {
                    Button("추가", action: submit)
                        .disabled(name.trimmingCharacters(in: .whitespaces).isEmpty)
                }
            }
        }
        .presentationDetents([.height(220)])
        .onAppear { nameFocused = true }
    }

    private func submit() {
        let trimmed = name.trimmingCharacters(in: .whitespaces)
        guard !trimmed.isEmpty else { return }
        onAdd(trimmed, icon)
        dismiss()
    }
}

// MARK: - 히스토리 (완료한 목록)

struct HistoryView: View {
    @Environment(\.dismiss) private var dismiss
    @State private var archived: [ChecklistItem] = []
    @State private var categoryByID: [UUID: Category] = [:]

    private let store = ChecklistStore.shared

    private var groups: [(label: String, items: [ChecklistItem])] {
        let calendar = Calendar.current
        let grouped = Dictionary(grouping: archived) { item in
            calendar.startOfDay(for: item.completedAt ?? item.createdAt)
        }
        return grouped.keys.sorted(by: >).map { day in
            (dateLabel(day), grouped[day] ?? [])
        }
    }

    var body: some View {
        NavigationStack {
            List {
                if archived.isEmpty {
                    Text("아직 완료한 항목이 없어요")
                        .foregroundStyle(.secondary)
                }
                ForEach(groups, id: \.label) { group in
                    Section(group.label) {
                        ForEach(group.items) { item in
                            HStack(spacing: 10) {
                                if let cat = categoryByID[item.categoryID] {
                                    Text("\(cat.icon) \(cat.name)")
                                        .font(.caption.weight(.semibold))
                                        .foregroundStyle(.secondary)
                                        .padding(.horizontal, 7)
                                        .padding(.vertical, 2)
                                        .background(Color(uiColor: .secondarySystemBackground), in: RoundedRectangle(cornerRadius: 6))
                                }
                                Text(item.title)
                                    .strikethrough()
                                    .foregroundStyle(.secondary)
                                    .lineLimit(1)
                                Spacer()
                                if let at = item.completedAt {
                                    Text(at, format: .dateTime.hour().minute())
                                        .font(.caption)
                                        .foregroundStyle(.tertiary)
                                }
                                Button("되돌리기") {
                                    withAnimation { store.restore(id: item.id); reload() }
                                }
                                .font(.caption.weight(.bold))
                                .foregroundStyle(.green)
                                .buttonStyle(.plain)
                            }
                        }
                    }
                }
            }
            .navigationTitle("완료한 목록")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarLeading) {
                    Button("닫기") { dismiss() }
                }
            }
        }
        .onAppear(perform: reload)
    }

    private func dateLabel(_ day: Date) -> String {
        let calendar = Calendar.current
        let formatter = DateFormatter()
        formatter.locale = Locale(identifier: "ko_KR")
        formatter.dateFormat = "M월 d일 (E)"
        let base = formatter.string(from: day)
        if calendar.isDateInToday(day) { return base + " · 오늘" }
        if calendar.isDateInYesterday(day) { return base + " · 어제" }
        return base
    }

    private func reload() {
        archived = store.archivedItems()
        categoryByID = Dictionary(uniqueKeysWithValues: store.categories().map { ($0.id, $0) })
        WidgetCenter.shared.reloadAllTimelines()
    }
}

#Preview {
    ContentView()
}
