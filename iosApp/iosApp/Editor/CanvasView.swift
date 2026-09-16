import SwiftUI
import UIKit
import Shared

struct CanvasView: UIViewRepresentable {
    let engine: ArtEngine?
    let canvasWidth: Int32
    let canvasHeight: Int32
    let isReady: Bool
    let onStrokeFinished: () -> Void

    func makeUIView(context: Context) -> ArtCanvasUIView {
        let view = ArtCanvasUIView(
            engine: engine,
            canvasWidth: canvasWidth,
            canvasHeight: canvasHeight,
            onStrokeFinished: onStrokeFinished
        )
        context.coordinator.canvasView = view
        return view
    }

    func updateUIView(_ uiView: ArtCanvasUIView, context: Context) {
        uiView.isInputEnabled = isReady && engine != nil
        uiView.updateCanvasSize(width: canvasWidth, height: canvasHeight)
        uiView.refreshCanvas()
    }

    func makeCoordinator() -> Coordinator { Coordinator() }

    final class Coordinator {
        weak var canvasView: ArtCanvasUIView?
    }
}

final class ArtCanvasUIView: UIView {
    private var engine: ArtEngine?
    private var renderer: MetalCanvasRenderer?
    private let imageView = UIImageView()
    private var onStrokeFinished: () -> Void
    var isInputEnabled = true
    private var isDrawing = false
    private var refreshWorkItem: DispatchWorkItem?

    init(
        engine: ArtEngine?,
        canvasWidth: Int32,
        canvasHeight: Int32,
        onStrokeFinished: @escaping () -> Void
    ) {
        self.engine = engine
        self.onStrokeFinished = onStrokeFinished
        super.init(frame: .zero)
        backgroundColor = UIColor(white: 0.12, alpha: 1)
        imageView.contentMode = .scaleAspectFit
        imageView.backgroundColor = .clear
        imageView.translatesAutoresizingMaskIntoConstraints = false
        addSubview(imageView)
        NSLayoutConstraint.activate([
            imageView.leadingAnchor.constraint(equalTo: leadingAnchor),
            imageView.trailingAnchor.constraint(equalTo: trailingAnchor),
            imageView.topAnchor.constraint(equalTo: topAnchor),
            imageView.bottomAnchor.constraint(equalTo: bottomAnchor),
        ])
        configureRenderer(engine: engine, width: canvasWidth, height: canvasHeight)
    }

    @available(*, unavailable)
    required init?(coder: NSCoder) { nil }

    func updateCanvasSize(width: Int32, height: Int32) {
        renderer?.canvasWidth = width
        renderer?.canvasHeight = height
    }

    private func configureRenderer(engine: ArtEngine?, width: Int32, height: Int32) {
        guard let engine else {
            renderer = nil
            imageView.image = nil
            return
        }
        let canvasRenderer = MetalCanvasRenderer(engine: engine, onReady: {})
        canvasRenderer.canvasWidth = width
        canvasRenderer.canvasHeight = height
        renderer = canvasRenderer
    }

    func refreshCanvas() {
        refreshWorkItem?.cancel()
        var work: DispatchWorkItem!
        work = DispatchWorkItem { [weak self] in
            guard let self, let renderer = self.renderer else { return }
            let image = renderer.compositeUIImage()
            let item = work
            DispatchQueue.main.async { [weak self] in
                guard let self, let item, !item.isCancelled else { return }
                self.imageView.image = image
            }
        }
        refreshWorkItem = work
        DispatchQueue.global(qos: .userInteractive).async(execute: work)
    }

    override func touchesBegan(_ touches: Set<UITouch>, with event: UIEvent?) {
        guard isInputEnabled, let engine, let renderer, let touch = touches.first else { return }
        let point = touch.location(in: self)
        let canvasPoint = renderer.screenToCanvas(
            screenX: Float(point.x),
            screenY: Float(point.y),
            viewWidth: Int32(bounds.width),
            viewHeight: Int32(bounds.height)
        )
        let x = canvasPoint.first?.floatValue ?? 0
        let y = canvasPoint.second?.floatValue ?? 0
        isDrawing = true
        engine.beginStroke()
        engine.addStrokePoint(point: StrokePoint(x: x, y: y, pressure: 1.0))
        refreshCanvas()
    }

    override func touchesMoved(_ touches: Set<UITouch>, with event: UIEvent?) {
        guard isInputEnabled, isDrawing, let engine, let renderer, let touch = touches.first else { return }
        let point = touch.location(in: self)
        let canvasPoint = renderer.screenToCanvas(
            screenX: Float(point.x),
            screenY: Float(point.y),
            viewWidth: Int32(bounds.width),
            viewHeight: Int32(bounds.height)
        )
        let x = canvasPoint.first?.floatValue ?? 0
        let y = canvasPoint.second?.floatValue ?? 0
        engine.addStrokePoint(point: StrokePoint(x: x, y: y, pressure: 1.0))
        refreshCanvas()
    }

    override func touchesEnded(_ touches: Set<UITouch>, with event: UIEvent?) {
        finishStroke()
    }

    override func touchesCancelled(_ touches: Set<UITouch>, with event: UIEvent?) {
        finishStroke()
    }

    private func finishStroke() {
        guard isDrawing, let engine else { return }
        isDrawing = false
        engine.endStroke()
        refreshCanvas()
        onStrokeFinished()
    }
}
