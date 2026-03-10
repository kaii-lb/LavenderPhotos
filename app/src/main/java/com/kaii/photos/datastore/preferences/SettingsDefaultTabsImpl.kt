package com.kaii.photos.datastore.preferences

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.kaii.photos.datastore.BottomBarTab
import com.kaii.photos.datastore.DefaultTabs
import com.kaii.photos.datastore.StoredName
import com.kaii.photos.datastore.datastore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

private const val TAG = "com.kaii.photos.datastore.preferences.SettingsDefaultTabsImpl"

class SettingsDefaultTabsImpl(
    private val context: Context,
    private val scope: CoroutineScope
) {
    private val defaultTab = stringPreferencesKey("default_tabs_first")
    private val tabList = stringPreferencesKey("default_tabs_list")

    fun getTabList() = context.datastore.data.map { data ->
        var list = data[tabList] ?: defaultTabListJson

        try {
            if (list.contains("resourceId")) {
                val regex = """("resourceId"):"([^"]+)"""".toRegex()

                list = list.replace(regex) { matchResult ->
                    val value = matchResult.groupValues[2]
                    val index = StoredName.entries.map { it.name }.indexOf(value)

                    if (index != -1) {
                        "\"storedNameIndex\":\"$index\""
                    } else {
                        matchResult.value
                    }
                }

                setTabList(Json.decodeFromString<List<BottomBarTab>>(list))
            }

            Json.decodeFromString<List<BottomBarTab>>(list).map {
                if (it.storedNameIndex != null) {
                    it.copy(
                        name = context.resources.getString(
                            StoredName.entries[it.storedNameIndex].id
                        )
                    )
                } else {
                    it
                }
            }
        } catch (e: Throwable) {
            Log.e(TAG, "BottomBarTab Impl has been changed, resetting default tab list...")
            Log.e(TAG, e.toString())
            e.printStackTrace()

            setTabList(defaultTabList)
            setDefaultTab(defaultTabItem)

            defaultTabList
        }
    }

    fun setTabList(list: List<BottomBarTab>) = scope.launch {
        context.datastore.edit {
            if (list.isEmpty()) {
                it[tabList] = defaultTabListJson
                setDefaultTab(defaultTabItem)

                return@edit
            }

            val default = try {
                it[defaultTab]?.let { tab -> Json.decodeFromString<BottomBarTab>(tab) } ?: defaultTabItem
            } catch (e: Throwable) {
                Log.e(TAG, "BottomBarTab Impl has been changed, default tab can't be decoded, failing back to DefaultTabs.TabTypes.photos.")
                Log.e(TAG, e.toString())
                e.printStackTrace()

                defaultTabItem
            }

            if (default !in list) {
                setDefaultTab(list.first())
            }

            it[tabList] = Json.encodeToString(list)
        }
    }

    fun getDefaultTab() = context.datastore.data.map {
        val default = it[defaultTab]

        try {
            default?.let { string -> Json.decodeFromString<BottomBarTab>(string) } ?: defaultTabItem
        } catch (e: Throwable) {
            Log.e(TAG, "BottomBarTab Impl has been changed, resetting default tab...")
            Log.e(TAG, e.toString())
            e.printStackTrace()

            setDefaultTab(defaultTabItem)
            defaultTabItem
        }
    }

    fun setDefaultTab(tab: BottomBarTab) = scope.launch {
        context.datastore.edit {
            it[defaultTab] = Json.encodeToString(tab)
        }
    }

    val defaultTabList =
        DefaultTabs.defaultList.map {
            if (it.storedNameIndex != null) {
                it.copy(
                    name = context.resources.getString(
                        StoredName.entries[it.storedNameIndex].id
                    )
                )
            } else {
                it
            }
        }

    private val defaultTabListJson = Json.encodeToString(defaultTabList)

    val defaultTabItem =
        DefaultTabs.TabTypes.photos.copy(
            name = context.resources.getString(
                StoredName.entries[DefaultTabs.TabTypes.photos.storedNameIndex!!].id
            )
        )
}