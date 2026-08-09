import SwiftUI

@main
struct ChecklistApp: App {
    init() {
        ChecklistStore.shared.purgeExpired()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
