package dev.fumiwake.app.data

import android.content.Context
import android.content.res.AssetManager
import dev.fumiwake.core.catalog.Catalog
import dev.fumiwake.core.concept.ConceptGraph
import dev.fumiwake.core.exercise.ExerciseLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Loads the bundled exercise [Catalog] from APK assets.
 *
 * `:app`'s `sourceSets["main"].assets.srcDirs("../content/exercises")` (see
 * app/build.gradle.kts) publishes every bundled exercise YAML under asset paths like
 * `rhythm/rhythm.beat.001.yaml` — the assets root itself contains only domain
 * directories (docs/IMPLEMENTATION_PLAN.md §2), so this recurses one or more levels to
 * find every `.yaml` file regardless of how many domains are bundled.
 * (Note: no glob pattern in this comment — Kotlin block comments nest, so a literal
 * slash-star inside a doc comment opens a new comment and breaks the whole file.)
 *
 * All IO happens in [load], never in the constructor.
 */
class AssetExerciseRepository(private val context: Context) {

    suspend fun load(): Catalog = withContext(Dispatchers.IO) {
        val assets = context.assets
        val exercises = collectYamlPaths(assets, "").map { path ->
            val yaml = assets.open(path).bufferedReader().use { it.readText() }
            ExerciseLoader.fromYaml(yaml)
        }
        Catalog(ConceptGraph.bundled(), exercises)
    }

    /**
     * Recursively lists every `.yaml` file under [dir] (relative to the assets root).
     * [AssetManager.list] returns the immediate children of a directory, or an empty
     * array for both an empty directory and a plain file, so directories are
     * distinguished from files purely by the `.yaml` extension.
     */
    private fun collectYamlPaths(assets: AssetManager, dir: String): List<String> {
        val entries = assets.list(dir) ?: return emptyList()
        return entries.flatMap { name ->
            val path = if (dir.isEmpty()) name else "$dir/$name"
            if (name.endsWith(".yaml")) listOf(path) else collectYamlPaths(assets, path)
        }
    }
}
