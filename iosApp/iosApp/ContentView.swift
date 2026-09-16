import SwiftUI
import Shared

struct ContentView: View {
    @State private var selectedProjectId: String?
    @State private var showEditor = false

    var body: some View {
        NavigationStack {
            GalleryView(onOpenProject: { projectId in
                selectedProjectId = projectId
                showEditor = true
            })
            .navigationDestination(isPresented: $showEditor) {
                if let projectId = selectedProjectId {
                    EditorView(projectId: projectId)
                }
            }
        }
    }
}
