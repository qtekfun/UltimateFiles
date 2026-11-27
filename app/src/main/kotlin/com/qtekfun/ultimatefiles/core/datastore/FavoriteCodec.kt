package com.qtekfun.ultimatefiles.core.datastore

import com.qtekfun.ultimatefiles.core.model.Favorite
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/** Favorites as one JSON string; paths may hold any character, so no separator would be safe. */
object FavoriteCodec {
    fun encode(favorites: List<Favorite>): String {
        val array = JSONArray()
        favorites.forEach {
            array.put(JSONObject().put("path", it.path).put("name", it.name).put("directory", it.isDirectory))
        }
        return array.toString()
    }

    /** A stored value that cannot be read gives no favorites instead of failing every preference read. */
    fun decode(text: String): List<Favorite> = try {
        val array = JSONArray(text)
        List(array.length()) { index ->
            val row = array.getJSONObject(index)
            Favorite(row.getString("path"), row.getString("name"), row.getBoolean("directory"))
        }
    } catch (e: JSONException) {
        emptyList()
    }
}
