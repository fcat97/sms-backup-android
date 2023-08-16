package media.uqab.libsmsbackup

inline fun <T> safe(block: () -> T): T? {
    return try {
        block()
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}
