package com.rws.learningproject01.core.storage

import com.rws.learningproject01.core.model.AnimationSummary
import com.rws.learningproject01.core.model.AnimationTimeline
import com.rws.learningproject01.core.model.CanvasSpec
import com.rws.learningproject01.core.model.DEFAULT_TILE_SIZE
import com.rws.learningproject01.core.model.EditorState
import com.rws.learningproject01.core.model.GalleryEntry
import com.rws.learningproject01.core.model.GalleryIndex
import com.rws.learningproject01.core.model.LayerMeta
import com.rws.learningproject01.core.model.ProjectManifest
import com.rws.learningproject01.core.model.ProjectPalette
import com.rws.learningproject01.core.model.SaveJournal
import com.rws.learningproject01.core.model.TimelapseLog
import com.rws.learningproject01.platform.Lz4Codec
import com.rws.learningproject01.platform.ThumbnailGenerator
import com.rws.learningproject01.platform.PlatformContext
import com.rws.learningproject01.platform.ioDispatcher
import com.rws.learningproject01.platform.argbToHex
import com.rws.learningproject01.platform.parseColorHex
import com.rws.learningproject01.platform.platformFilesDir
import com.rws.learningproject01.platform.rgbaTileBuffer
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

data class OpenProject(
    var manifest: ProjectManifest,
    val directory: Path,
    val tileStore: TileStore,
)

class ProjectRepository(private val context: PlatformContext) {

    private val fs = FileSystem.SYSTEM
    private val filesRoot = platformFilesDir(context).toPath()
    private val projectsRoot = filesRoot.resolve("projects")
    private val galleryIndexFile = filesRoot.resolve("gallery_index.json")
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    init {
        fs.createDirectories(projectsRoot)
    }

    fun projectDir(projectId: String): Path =
        projectsRoot.resolve("$projectId.lpdoc")

    suspend fun listGallery(): List<GalleryEntry> = withContext(ioDispatcher) {
        var index = readGalleryIndex()
        val hasProjectsOnDisk = fs.list(projectsRoot).any { it.name.endsWith(".lpdoc") }
        if (index.projects.isEmpty() && hasProjectsOnDisk) {
            rebuildGalleryIndex()
            index = readGalleryIndex()
        }
        index.projects.sortedByDescending { it.modifiedAt }
    }

    @OptIn(ExperimentalUuidApi::class)
    suspend fun createProject(
        title: String = "Untitled",
        canvas: CanvasSpec = CanvasSpec(),
    ): OpenProject = withContext(ioDispatcher) {
        val now = StorageTime.nowIso()
        val projectId = Uuid.random().toString()
        val dir = projectDir(projectId)
        fs.createDirectories(dir)

        val layerId = Uuid.random().toString()
        val layer = LayerMeta(
            id = layerId,
            name = "Layer 1",
            bounds = com.rws.learningproject01.core.model.LayerBounds(
                minX = 0,
                minY = 0,
                maxX = canvas.width,
                maxY = canvas.height,
            ),
        )

        val manifest = ProjectManifest(
            projectId = projectId,
            title = title,
            createdAt = now,
            modifiedAt = now,
            canvas = canvas,
            layerOrder = listOf(layerId),
        )

        val tileStore = TileStore(dir)
        tileStore.writeLayerMeta(layer)
        writeManifest(dir, manifest)
        writeEditorState(dir, EditorState(activeLayerId = layerId))
        writeAnimationTimeline(dir, AnimationTimeline())
        writeJournal(dir, SaveJournal())

        val thumbFile = dir.resolve("thumb.webp")
        runCatching { ThumbnailGenerator.generatePlaceholder(canvas.width, canvas.height, thumbFile) }
        runCatching { updateGalleryEntry(manifest, thumbFile) }

        openProject(projectId) ?: error("Project was created but manifest could not be read")
    }

    suspend fun openProject(projectId: String): OpenProject? = withContext(ioDispatcher) {
        if (projectId.isBlank()) return@withContext null
        val dir = projectDir(projectId)
        if (!fs.exists(dir)) return@withContext null
        val manifestFile = dir.resolve("manifest.json")
        val manifest = decodeJson<ProjectManifest>(manifestFile) ?: return@withContext null
        OpenProject(manifest, dir, TileStore(dir))
    }

    suspend fun saveProject(
        openProject: OpenProject,
        layers: List<LayerMeta>,
        dirtyTiles: Map<String, List<Pair<Int, Int>>>,
        editorState: EditorState,
        title: String? = null,
    ) = withContext(ioDispatcher) {
        val dir = openProject.directory
        val tileStore = openProject.tileStore
        val now = StorageTime.nowIso()

        layers.forEach { tileStore.writeLayerMeta(it) }

        val manifest = openProject.manifest.copy(
            title = title ?: openProject.manifest.title,
            modifiedAt = now,
            layerOrder = layers.map { it.id },
        )
        writeManifest(dir, manifest)
        writeEditorState(dir, editorState)
        writeJournal(dir, SaveJournal())

        val thumbFile = dir.resolve("thumb.webp")
        updateGalleryEntry(manifest, thumbFile)
        openProject.manifest = manifest
    }

    suspend fun saveTile(
        openProject: OpenProject,
        layerId: String,
        col: Int,
        row: Int,
        rgba: ByteArray,
    ) = withContext(ioDispatcher) {
        openProject.tileStore.writeTile(layerId, col, row, rgba)
    }

    suspend fun loadAllTiles(
        openProject: OpenProject,
        layerId: String,
    ): Map<Pair<Int, Int>, ByteArray> = withContext(ioDispatcher) {
        openProject.tileStore.listTiles(layerId).associateWith { (col, row) ->
            openProject.tileStore.readTile(layerId, col, row) ?: Lz4Codec.rgbaTileBuffer()
        }
    }

    suspend fun readEditorState(openProject: OpenProject): EditorState =
        withContext(ioDispatcher) {
            decodeJson<EditorState>(openProject.directory.resolve("state.json")) ?: EditorState()
        }

    suspend fun deleteProject(projectId: String) = withContext(ioDispatcher) {
        val dir = projectDir(projectId)
        if (fs.exists(dir)) fs.deleteRecursively(dir)
        rebuildGalleryIndex()
    }

    suspend fun readAnimationTimeline(openProject: OpenProject): AnimationTimeline =
        withContext(ioDispatcher) {
            decodeJson<AnimationTimeline>(openProject.directory.resolve("animation/timeline.json"))
                ?: AnimationTimeline()
        }

    suspend fun saveAnimationTimeline(
        openProject: OpenProject,
        timeline: AnimationTimeline,
        summary: AnimationSummary,
    ) = withContext(ioDispatcher) {
        writeAnimationTimeline(openProject.directory, timeline)
        openProject.manifest = openProject.manifest.copy(animation = summary)
        writeManifest(openProject.directory, openProject.manifest)
    }

    suspend fun readProjectPalette(openProject: OpenProject): List<Int> =
        withContext(ioDispatcher) {
            val palette = decodeJson<ProjectPalette>(openProject.directory.resolve("palette.json"))
            palette?.colors?.mapNotNull { hex ->
                runCatching {
                    parseColorHex(if (hex.startsWith("#")) hex else "#$hex")
                }.getOrNull()
            }.orEmpty()
        }

    suspend fun saveProjectPalette(openProject: OpenProject, colors: List<Int>) = withContext(ioDispatcher) {
        val palette = ProjectPalette(
            colors = colors.map { argb -> argbToHex(argb) },
        )
        AtomicFileWriter.writeText(
            openProject.directory.resolve("palette.json"),
            json.encodeToString(palette),
        )
    }

    suspend fun readTimelapse(openProject: OpenProject): TimelapseLog? =
        withContext(ioDispatcher) { TimelapseStore(openProject.directory).read() }

    suspend fun saveTimelapse(openProject: OpenProject, log: TimelapseLog) =
        withContext(ioDispatcher) { TimelapseStore(openProject.directory).save(log) }

    suspend fun importPalette(fileName: String, bytes: ByteArray): List<Int> =
        withContext(ioDispatcher) { PaletteImporter.importFromBytes(bytes, fileName) }

    suspend fun createProjectFromPsd(fileName: String, bytes: ByteArray): OpenProject =
        withContext(ioDispatcher) {
            createProjectFromPsdInternal(fileName, PsdImporter.import(bytes, fileName))
        }

    @OptIn(ExperimentalUuidApi::class)
    private suspend fun createProjectFromPsdInternal(
        fileName: String,
        psd: PsdImporter.PsdImportResult,
    ): OpenProject {
        val now = StorageTime.nowIso()
        val projectId = Uuid.random().toString()
        val dir = projectDir(projectId)
        fs.createDirectories(dir)
        val tileStore = TileStore(dir)
        val layerIds = mutableListOf<String>()
        psd.layers.forEachIndexed { index, psdLayer ->
            val layerId = Uuid.random().toString()
            layerIds.add(layerId)
            val meta = LayerMeta(
                id = layerId,
                name = psdLayer.name.ifBlank { "Layer ${index + 1}" },
                bounds = com.rws.learningproject01.core.model.LayerBounds(
                    maxX = psd.canvas.width,
                    maxY = psd.canvas.height,
                ),
            )
            tileStore.writeLayerMeta(meta)
            writePixelsToTiles(tileStore, layerId, psdLayer.pixels, psd.canvas.width, psd.canvas.height)
        }
        val manifest = ProjectManifest(
            projectId = projectId,
            title = psd.title,
            createdAt = now,
            modifiedAt = now,
            canvas = psd.canvas,
            layerOrder = layerIds,
        )
        writeManifest(dir, manifest)
        writeEditorState(dir, EditorState(activeLayerId = layerIds.firstOrNull()))
        writeAnimationTimeline(dir, AnimationTimeline())
        writeJournal(dir, SaveJournal())
        val thumbFile = dir.resolve("thumb.webp")
        runCatching { ThumbnailGenerator.generatePlaceholder(psd.canvas.width, psd.canvas.height, thumbFile) }
        runCatching { updateGalleryEntry(manifest, thumbFile) }
        return openProject(projectId) ?: error("Imported project could not be opened")
    }

    suspend fun saveCelFrameTiles(
        openProject: OpenProject,
        frameIndex: Int,
        layerId: String,
        tiles: Map<Pair<Int, Int>, ByteArray>,
    ) = withContext(ioDispatcher) {
        CelFrameStore(openProject.directory).saveFrameTiles(frameIndex, layerId, tiles)
    }

    suspend fun loadCelFrameTiles(
        openProject: OpenProject,
        frameIndex: Int,
        layerId: String,
    ): Map<Pair<Int, Int>, ByteArray> = withContext(ioDispatcher) {
        CelFrameStore(openProject.directory).loadFrameTiles(frameIndex, layerId)
    }

    fun fontStore(): FontStore = FontStore(context)

    private fun writeManifest(dir: Path, manifest: ProjectManifest) {
        AtomicFileWriter.writeText(dir.resolve("manifest.json"), json.encodeToString(manifest))
    }

    private fun writeEditorState(dir: Path, state: EditorState) {
        AtomicFileWriter.writeText(dir.resolve("state.json"), json.encodeToString(state))
    }

    private fun writeAnimationTimeline(dir: Path, timeline: AnimationTimeline) {
        val animDir = dir.resolve("animation")
        fs.createDirectories(animDir)
        AtomicFileWriter.writeText(animDir.resolve("timeline.json"), json.encodeToString(timeline))
    }

    private fun writeJournal(dir: Path, journal: SaveJournal) {
        val autosaveDir = dir.resolve("autosave")
        fs.createDirectories(autosaveDir)
        AtomicFileWriter.writeText(autosaveDir.resolve("journal.json"), json.encodeToString(journal))
    }

    private fun updateGalleryEntry(manifest: ProjectManifest, thumbFile: Path) {
        val index = readGalleryIndex()
        val entry = GalleryEntry(
            projectId = manifest.projectId,
            title = manifest.title,
            modifiedAt = manifest.modifiedAt,
            thumbPath = relativePath(thumbFile),
        )
        val updated = index.copy(
            projects = index.projects
                .filterNot { it.projectId == manifest.projectId }
                .plus(entry),
        )
        AtomicFileWriter.writeText(galleryIndexFile, json.encodeToString(updated))
    }

    private fun relativePath(path: Path): String {
        val root = filesRoot.toString().trimEnd('/')
        val full = path.toString()
        return if (full.startsWith(root)) full.removePrefix(root).trimStart('/') else full
    }

    private fun readGalleryIndex(): GalleryIndex {
        val parsed = decodeJson<GalleryIndex>(galleryIndexFile)
        if (parsed != null) return parsed
        if (fs.exists(galleryIndexFile)) fs.delete(galleryIndexFile)
        return GalleryIndex()
    }

    private inline fun <reified T> decodeJson(file: Path): T? {
        if (!fs.exists(file) || fs.metadata(file).size == 0L) return null
        val text = fs.read(file) { readUtf8() }.trim()
        if (text.isEmpty()) return null
        return runCatching { json.decodeFromString<T>(text) }.getOrNull()
    }

    private fun rebuildGalleryIndex() {
        val entries = if (!fs.exists(projectsRoot)) {
            emptyList()
        } else {
            fs.list(projectsRoot)
                .filter { it.name.endsWith(".lpdoc") }
                .mapNotNull { dir ->
                    val manifestFile = dir.resolve("manifest.json")
                    val manifest = decodeJson<ProjectManifest>(manifestFile) ?: return@mapNotNull null
                    val thumb = dir.resolve("thumb.webp")
                    GalleryEntry(
                        projectId = manifest.projectId,
                        title = manifest.title,
                        modifiedAt = manifest.modifiedAt,
                        thumbPath = relativePath(thumb),
                    )
                }
        }
        AtomicFileWriter.writeText(galleryIndexFile, json.encodeToString(GalleryIndex(entries)))
    }

    private fun writePixelsToTiles(
        tileStore: TileStore,
        layerId: String,
        rgba: ByteArray,
        width: Int,
        height: Int,
    ) {
        val tileSize = DEFAULT_TILE_SIZE
        val cols = (width + tileSize - 1) / tileSize
        val rows = (height + tileSize - 1) / tileSize
        for (row in 0 until rows) {
            for (col in 0 until cols) {
                val tile = Lz4Codec.rgbaTileBuffer()
                for (py in 0 until tileSize) {
                    val destY = row * tileSize + py
                    if (destY >= height) continue
                    for (px in 0 until tileSize) {
                        val destX = col * tileSize + px
                        if (destX >= width) continue
                        val srcIdx = (destY * width + destX) * 4
                        val dstIdx = (py * tileSize + px) * 4
                        tile[dstIdx] = rgba[srcIdx]
                        tile[dstIdx + 1] = rgba[srcIdx + 1]
                        tile[dstIdx + 2] = rgba[srcIdx + 2]
                        tile[dstIdx + 3] = rgba[srcIdx + 3]
                    }
                }
                tileStore.writeTile(layerId, col, row, tile)
            }
        }
    }
}
