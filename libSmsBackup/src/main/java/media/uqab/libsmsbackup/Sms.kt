package media.uqab.libsmsbackup

import org.json.JSONObject

/**
 * Model class of an SMS.
 */
class Sms {
    var address: String = ""
    var body: String = ""
    var date: Long = -1
    var dateSent: Long = -1
    var read: Int = 0
    var seen: Int = 0
    var type: Type = Type.NONE

    enum class Type(val value: Int) {
        NONE(0),
        INBOX(1),
        SENT(2),
        DRAFT(3),
        OUTBOX(4),
        FAILED(5),
        QUEUED(6)
    }

    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("address", address)
            put("body", body)
            put("date", date)
            put("dateSent", dateSent)
            put("read", read)
            put("seen", seen)
            put("type", type.value)
        }
    }

    companion object {
        fun JSONObject.toSms(): Sms? {
            return safe {
                Sms().also {
                    it.address = getString("address")
                    it.body = getString("body")
                    it.date = getLong("date")
                    it.dateSent = getLong("dateSent")
                    it.read = getInt("read")
                    it.seen = getInt("seen")
                    it.type = when(getInt("type")) {
                        0 -> Type.NONE
                        1 -> Type.INBOX
                        2 -> Type.SENT
                        3 -> Type.DRAFT
                        4 -> Type.OUTBOX
                        5 -> Type.FAILED
                        6 -> Type.QUEUED
                        else -> Type.NONE
                    }
                }
            }
        }
    }
}