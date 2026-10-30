package com.example.adshield.model

import org.json.JSONArray
import org.json.JSONObject

data class AdShieldConfig(
    val version: Int = 2,
    val lastUpdated: String = "",
    val relaxOnFailure: Boolean = false,
    val adPermissions: List<String> = emptyList(),
    val globalAllow: List<String> = emptyList(),
    val hardBlockPaths: List<String> = listOf(
        "/ads", "/adv", "/banner", "/pop", "/popunder", "/vast", "/prebid", "/adserver", "/pop.js", "/ad.js", "/advertisement"
    ),
    val hardBlockQueryKeys: List<String> = listOf(
        "ad_id", "zoneid", "pop_id", "vast_url", "aff_id"
    ),
    val profiles: List<PlayerProfile> = emptyList()
) {
    fun toJson(): String {
        val root = JSONObject()
        root.put("version", version)
        root.put("lastUpdated", lastUpdated)
        root.put("relaxOnFailure", relaxOnFailure)
        root.put("adPermissions", JSONArray(adPermissions))
        root.put("globalAllow", JSONArray(globalAllow))
        root.put("hardBlockPaths", JSONArray(hardBlockPaths))
        root.put("hardBlockQueryKeys", JSONArray(hardBlockQueryKeys))

        val profilesArr = JSONArray()
        profiles.forEach { p ->
            val pObj = JSONObject()
            pObj.put("id", p.id)
            pObj.put("displayName", p.displayName)
            pObj.put("hostPatterns", JSONArray(p.hostPatterns))
            pObj.put("allowedHosts", JSONArray(p.allowedHosts))
            pObj.put("mediaCdnHosts", JSONArray(p.mediaCdnHosts))
            profilesArr.put(pObj)
        }
        root.put("profiles", profilesArr)
        return root.toString(2)
    }

    companion object {
        fun parseJson(jsonString: String): AdShieldConfig {
            val root = JSONObject(jsonString)
            val version = root.optInt("version", 2)
            val lastUpdated = root.optString("lastUpdated", "")
            val relaxOnFailure = root.optBoolean("relaxOnFailure", false)

            val adPermissions = jsonArrayToList(root.optJSONArray("adPermissions"))
            val globalAllow = jsonArrayToList(root.optJSONArray("globalAllow"))
            val hardBlockPaths = jsonArrayToList(root.optJSONArray("hardBlockPaths"))
            val hardBlockQueryKeys = jsonArrayToList(root.optJSONArray("hardBlockQueryKeys"))

            val profilesList = mutableListOf<PlayerProfile>()
            val profilesArr = root.optJSONArray("profiles")
            if (profilesArr != null) {
                for (i in 0 until profilesArr.length()) {
                    val pObj = profilesArr.getJSONObject(i)
                    profilesList.add(
                        PlayerProfile(
                            id = pObj.optString("id", "profile_$i"),
                            displayName = pObj.optString("displayName", "Player $i"),
                            hostPatterns = jsonArrayToList(pObj.optJSONArray("hostPatterns")),
                            allowedHosts = jsonArrayToList(pObj.optJSONArray("allowedHosts")),
                            mediaCdnHosts = jsonArrayToList(pObj.optJSONArray("mediaCdnHosts"))
                        )
                    )
                }
            }

            return AdShieldConfig(
                version = version,
                lastUpdated = lastUpdated,
                relaxOnFailure = relaxOnFailure,
                adPermissions = adPermissions,
                globalAllow = globalAllow,
                hardBlockPaths = hardBlockPaths.ifEmpty {
                    listOf("/ads", "/adv", "/banner", "/pop", "/popunder", "/vast", "/prebid", "/adserver", "/pop.js", "/ad.js", "/advertisement")
                },
                hardBlockQueryKeys = hardBlockQueryKeys.ifEmpty {
                    listOf("ad_id", "zoneid", "pop_id", "vast_url", "aff_id")
                },
                profiles = profilesList
            )
        }

        private fun jsonArrayToList(arr: JSONArray?): List<String> {
            if (arr == null) return emptyList()
            val list = mutableListOf<String>()
            for (i in 0 until arr.length()) {
                val item = arr.optString(i, "")
                if (item.isNotBlank()) list.add(item)
            }
            return list
        }
    }
}

data class PlayerProfile(
    val id: String,
    val displayName: String,
    val hostPatterns: List<String> = emptyList(),
    val allowedHosts: List<String> = emptyList(),
    val mediaCdnHosts: List<String> = emptyList()
)
