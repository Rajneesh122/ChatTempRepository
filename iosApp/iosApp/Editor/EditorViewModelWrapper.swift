import SwiftUI
import Shared

@MainActor
final class EditorViewModelWrapper: ObservableObject {
    let viewModel: EditorViewModel
    @Published var uiState: EditorUiState
    var engine: ArtEngine? { viewModel.engineOrNull() }

    init(projectId: String) {
        let context = createPlatformContext()
        let repository = ProjectRepository(context: context)
        viewModel = EditorViewModel(
            repository: repository,
            platformContext: context,
            scope: IosCoroutineScope.shared.mainScope(),
            projectId: projectId
        )
        uiState = viewModel.uiState.value
        observeState()
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

    func undo() { viewModel.undo() }
    func redo() { viewModel.redo() }
    func toggleBrushStudio() { viewModel.toggleBrushStudio() }
    func toggleLayerPanel() { viewModel.toggleLayerPanel() }
    func toggleColorPanel() { viewModel.toggleColorPanel() }
    func toggleExportPanel() { viewModel.toggleExportPanel() }
}
