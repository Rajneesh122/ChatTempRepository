import SwiftUI

struct BrushStudioSheet: View {
    @ObservedObject var viewModel: EditorViewModelWrapper

    var body: some View {
        NavigationStack {
            Form {
                Slider(value: Binding(
                    get: { Double(viewModel.uiState.brushSize) },
                    set: { viewModel.viewModel.setBrushSize(size: Float($0)) }
                ), in: 1...200) {
                    Text("Size")
                }
                Slider(value: Binding(
                    get: { Double(viewModel.uiState.brushOpacity) },
                    set: { viewModel.viewModel.setBrushOpacity(opacity: Float($0)) }
                ), in: 0...1) {
                    Text("Opacity")
                }
            }
            .navigationTitle("Brush Studio")
        }
    }
}

struct LayerPanelSheet: View {
    @ObservedObject var viewModel: EditorViewModelWrapper

    var body: some View {
        NavigationStack {
            List {
                ForEach(Array(viewModel.uiState.layers.enumerated()), id: \.element.id) { index, layer in
                    HStack {
                        Text(layer.name)
                        Spacer()
                        if layer.visible {
                            Image(systemName: "eye")
                        }
                    }
                    .onTapGesture { viewModel.viewModel.selectLayer(index: Int32(index)) }
                }
            }
            .navigationTitle("Layers")
            .toolbar {
                ToolbarItem(placement: .primaryAction) {
                    Button("Add") { viewModel.viewModel.addLayer() }
                }
            }
        }
    }
}

struct ColorPanelSheet: View {
    @ObservedObject var viewModel: EditorViewModelWrapper

    var body: some View {
        NavigationStack {
            VStack {
                ColorPicker("Brush Color", selection: .constant(.black))
                ForEach(viewModel.uiState.savedPalette, id: \.self) { color in
                    Button("Color") { viewModel.viewModel.selectColor(color: color.int32Value) }
                }
            }
            .padding()
            .navigationTitle("Color")
        }
    }
}

struct ExportSheet: View {
    @ObservedObject var viewModel: EditorViewModelWrapper

    var body: some View {
        NavigationStack {
            Form {
                Toggle("Transparent Background", isOn: Binding(
                    get: { viewModel.uiState.exportTransparentBackground },
                    set: { viewModel.viewModel.setExportTransparentBackground(enabled: $0) }
                ))
                Button("Export") {
                    viewModel.viewModel.startExport(onComplete: { _ in })
                }
            }
            .navigationTitle("Export")
        }
    }
}

struct FiltersSheet: View {
    @ObservedObject var viewModel: EditorViewModelWrapper

    var body: some View {
        NavigationStack {
            Button("Apply Filter") { viewModel.viewModel.applySelectedFilter() }
                .navigationTitle("Filters")
        }
    }
}

struct AnimationAssistSheet: View {
    @ObservedObject var viewModel: EditorViewModelWrapper

    var body: some View {
        NavigationStack {
            Toggle("Animation", isOn: Binding(
                get: { viewModel.uiState.animationEnabled },
                set: { viewModel.viewModel.setAnimationEnabled(enabled: $0) }
            ))
            .navigationTitle("Animation")
        }
    }
}

struct TextEditorSheet: View {
    @ObservedObject var viewModel: EditorViewModelWrapper

    var body: some View {
        NavigationStack {
            TextField("Text", text: Binding(
                get: { viewModel.uiState.textLayerContent },
                set: { viewModel.viewModel.setTextLayerContent(text: $0) }
            ))
            .navigationTitle("Text")
        }
    }
}
