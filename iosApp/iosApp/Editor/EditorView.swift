import SwiftUI
import Shared

struct EditorView: View {
    let projectId: String
    @StateObject private var viewModel: EditorViewModelWrapper

    init(projectId: String) {
        self.projectId = projectId
        _viewModel = StateObject(wrappedValue: EditorViewModelWrapper(projectId: projectId))
    }

    var body: some View {
        VStack(spacing: 0) {
            ZStack {
                CanvasView(
                    engine: viewModel.engine,
                    canvasWidth: viewModel.uiState.canvasWidth,
                    canvasHeight: viewModel.uiState.canvasHeight,
                    isReady: viewModel.uiState.isEngineReady,
                    onStrokeFinished: { viewModel.viewModel.onStrokeFinished() }
                )
                if viewModel.uiState.isLoading || !viewModel.uiState.isEngineReady {
                    ProgressView("Loading canvas…")
                        .frame(maxWidth: .infinity, maxHeight: .infinity)
                        .background(Color.black.opacity(0.2))
                }
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity)
            EditorToolbar(viewModel: viewModel)
        }
        .navigationTitle(viewModel.uiState.title)
        .navigationBarTitleDisplayMode(.inline)
        .sheet(isPresented: Binding(
            get: { viewModel.uiState.showBrushStudio },
            set: { _ in viewModel.toggleBrushStudio() }
        )) { BrushStudioSheet(viewModel: viewModel) }
        .sheet(isPresented: Binding(
            get: { viewModel.uiState.showLayerPanel },
            set: { _ in viewModel.toggleLayerPanel() }
        )) { LayerPanelSheet(viewModel: viewModel) }
        .sheet(isPresented: Binding(
            get: { viewModel.uiState.showColorPanel },
            set: { _ in viewModel.toggleColorPanel() }
        )) { ColorPanelSheet(viewModel: viewModel) }
        .sheet(isPresented: Binding(
            get: { viewModel.uiState.showExportPanel },
            set: { _ in viewModel.toggleExportPanel() }
        )) { ExportSheet(viewModel: viewModel) }
    }
}

struct EditorToolbar: View {
    @ObservedObject var viewModel: EditorViewModelWrapper

    var body: some View {
        HStack {
            Button { viewModel.undo() } label: { Image(systemName: "arrow.uturn.backward") }
            Button { viewModel.redo() } label: { Image(systemName: "arrow.uturn.forward") }
            Spacer()
            Button { viewModel.toggleBrushStudio() } label: { Image(systemName: "paintbrush") }
            Button { viewModel.toggleLayerPanel() } label: { Image(systemName: "square.stack.3d.up") }
            Button { viewModel.toggleColorPanel() } label: { Image(systemName: "paintpalette") }
            Button { viewModel.toggleExportPanel() } label: { Image(systemName: "square.and.arrow.up") }
        }
        .padding()
        .background(.ultraThinMaterial)
    }
}
