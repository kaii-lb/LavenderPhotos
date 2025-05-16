package com.aks_labs.tulsi.helpers

/**
 * Extracts the parent directory from a file path.
 * For example, "DCIM/Camera" returns "DCIM"
 */
fun getParentFromPath(path: String): String {
    return if (path.contains("/")) {
        path.substringBefore("/")
    } else {
        path
    }
}

