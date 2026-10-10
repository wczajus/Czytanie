package pl.dev404.czytanie.core.data.content

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/** Where the bundled dictionary comes from; replaced by a fake in tests. */
fun interface DictionarySource {
    fun read(): String
}

class AssetDictionarySource @Inject constructor(@param:ApplicationContext private val context: Context) :
    DictionarySource {
    override fun read(): String = context.assets.open(PATH).bufferedReader().use { it.readText() }

    companion object {
        const val PATH = "content/dictionary.json"
    }
}
