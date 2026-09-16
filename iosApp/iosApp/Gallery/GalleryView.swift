import SwiftUI
import Shared

@MainActor
final class GalleryViewModelWrapper: ObservableObject {
    private let viewModel: GalleryViewModel
    @Published var uiState: GalleryUiState

    init() {
        let context = createPlatformContext()
        let repository = ProjectRepository(context: context)
        viewModel = GalleryViewModel(repository: repository, scope: IosCoroutineScope.shared.mainScope())
        uiState = viewModel.uiState.value
        observeState()
        viewModel.refresh()
    }

    private func observeState() {
        Task {
            for try await state in viewModel.uiState {
                await MainActor.run {
                    self.uiState = state
                }
            }
        }
    }

    func refresh() { viewModel.refresh() }
    func createProject(onCreated: @escaping (String) -> Void) { viewModel.createProject(onCreated: onCreated) }
    func deleteProject(projectId: String) { viewModel.deleteProject(projectId: projectId) }
}

struct GalleryView: View {
    let onOpenProject: (String) -> Void
    @StateObject private var viewModel = GalleryViewModelWrapper()

    private let columns = [GridItem(.adaptive(minimum: 150), spacing: 12)]

    var body: some View {
        VStack {
            if viewModel.uiState.isLoading {
                ProgressView("Loading projects…")
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
            } else if viewModel.uiState.projects.isEmpty {
                VStack(spacing: 12) {
                    Image(systemName: "paintbrush.pointed")
                        .font(.system(size: 48))
                        .foregroundStyle(.secondary)
                    Text("No Projects")
                        .font(.title2)
                    Text("Tap + to create your first canvas.")
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                        .multilineTextAlignment(.center)
                }
                .frame(maxWidth: .infinity, maxHeight: .infinity)
                .padding()
            } else {
                ScrollView {
                    LazyVGrid(columns: columns, spacing: 12) {
                        ForEach(viewModel.uiState.projects, id: \.projectId) { project in
                            Button {
                                onOpenProject(project.projectId)
                            } label: {
                                VStack(alignment: .leading) {
                                    Rectangle()
                                        .fill(Color.gray.opacity(0.3))
                                        .aspectRatio(1, contentMode: .fit)
                                        .overlay(Text(project.title.prefix(1)).font(.largeTitle))
                                    Text(project.title)
                                        .font(.headline)
                                        .lineLimit(1)
                                }
                            }
                            .contextMenu {
                                Button(role: .destructive) {
                                    viewModel.deleteProject(projectId: project.projectId)
                                } label: {
                                    Label("Delete", systemImage: "trash")
                                }
                            }
                        }
                    }
                    .padding()
                }
            }
        }
        .navigationTitle("Art Studio")
        .toolbar {
            ToolbarItem(placement: .primaryAction) {
                Button {
                    viewModel.createProject { projectId in
                        onOpenProject(projectId)
                    }
                } label: {
                    Image(systemName: "plus")
                }
            }
        }
        .alert("Error", isPresented: .constant(viewModel.uiState.error != nil)) {
            Button("OK") { }
        } message: {
            Text(viewModel.uiState.error ?? "")
        }
    }
}
