import SwiftUI
import WidgetKit

struct ContentView: View {
    @State private var active: [ChecklistItem] = []
    @State private var completed: [ChecklistItem] = []
    @State private var newTitle = ""
    @State private var showCompleted = false
    @FocusState private var inputFocused: Bool
    @Environment(\.scenePhase) private var scenePhase

    private let store = ChecklistStore.shared

    var body: some View {
        NavigationStack {
            List {
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

                Section {
                    if active.isEmpty {
                        Text("아래가 아니라 위! 입력창에 첫 항목을 적어보세요")
                            .font(.subheadline)
                            .foregroundStyle(.secondary)
                    }
                    ForEach(active) { item in
                        HStack(spacing: 12) {
                            Button {
                                complete(item)
                            } label: {
                                Image(systemName: "circle")
                                    .font(.title3)
                                    .foregroundStyle(.secondary)
                                    .frame(width: 44, height: 44)
                            }
                            .buttonStyle(.plain)
                            Text(item.title)
                        }
                        .listRowInsets(EdgeInsets(top: 0, leading: 12, bottom: 0, trailing: 16))
                    }
                    .onDelete(perform: deleteItems)
                }

                if !completed.isEmpty {
                    Section {
                        DisclosureGroup(isExpanded: $showCompleted) {
                            ForEach(completed) { item in
                                HStack(spacing: 12) {
                                    Button {
                                        restore(item)
                                    } label: {
                                        Image(systemName: "checkmark.circle.fill")
                                            .font(.title3)
                                            .foregroundStyle(.green)
                                            .frame(width: 44, height: 44)
                                    }
                                    .buttonStyle(.plain)
                                    Text(item.title)
                                        .strikethrough()
                                        .foregroundStyle(.secondary)
                                }
                                .listRowInsets(EdgeInsets(top: 0, leading: 12, bottom: 0, trailing: 16))
                            }
                        } label: {
                            Text("완료됨 \(completed.count)")
                                .font(.subheadline.weight(.semibold))
                                .foregroundStyle(.secondary)
                        }
                    }
                }
            }
            .navigationTitle("빠른 체크리스트")
        }
        .onAppear {
            reload()
            if active.isEmpty { inputFocused = true }
        }
        .onChange(of: scenePhase) { _, phase in
            if phase == .active { reload() }
        }
        .onOpenURL { url in
            // checklist://add — 위젯 헤더/빈 상태 탭 (F9)
            if url.host() == "add" { inputFocused = true }
        }
    }

    // MARK: - 동작

    private func addItem() {
        store.add(title: newTitle)
        newTitle = ""
        reload()
        inputFocused = true // 연속 입력 (F1)
    }

    private func complete(_ item: ChecklistItem) {
        withAnimation {
            store.complete(id: item.id)
            reload()
        }
    }

    private func restore(_ item: ChecklistItem) {
        withAnimation {
            store.restore(id: item.id)
            reload()
        }
    }

    private func deleteItems(at offsets: IndexSet) {
        withAnimation {
            for index in offsets {
                store.remove(id: active[index].id)
            }
            reload()
        }
    }

    private func reload() {
        active = store.activeItems()
        completed = store.completedItems()
        WidgetCenter.shared.reloadAllTimelines()
    }
}

#Preview {
    ContentView()
}
