package eu.droogers.smsmatrix

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.ContactsContract
import android.telephony.SmsManager
import android.util.Log
import com.google.gson.JsonObject
import org.matrix.androidsdk.HomeServerConnectionConfig
import org.matrix.androidsdk.MXDataHandler
import org.matrix.androidsdk.MXSession
import org.matrix.androidsdk.core.callback.SimpleApiCallback
import org.matrix.androidsdk.core.model.MatrixError
import org.matrix.androidsdk.data.Room
import org.matrix.androidsdk.data.store.IMXStore
import org.matrix.androidsdk.data.store.IMXStoreListener
import org.matrix.androidsdk.data.store.MXFileStore
import org.matrix.androidsdk.data.store.MXMemoryStore
import org.matrix.androidsdk.listeners.IMXEventListener
import org.matrix.androidsdk.listeners.MXMediaUploadListener
import org.matrix.androidsdk.rest.client.LoginRestClient
import org.matrix.androidsdk.rest.model.CreatedEvent
import org.matrix.androidsdk.rest.model.Event
import org.matrix.androidsdk.rest.model.login.Credentials
import org.matrix.androidsdk.rest.model.message.Message
import java.io.ByteArrayInputStream

/**
 * Created by gerben on 6-10-17.
 */
class Matrix(
    var context: Context,
    url: String?,
    botUsername: String,
    botPassword: String,
    username: String,
    device: String,
    syncDelay: String,
    syncTimeout: String
) {
    private val syncDelay: Int
    private val syncTimeout: Int
    var hsConfig: HomeServerConnectionConfig
    var session: MXSession? = null
    var transaction: Int = 0
    private val notSendMessages: MutableList<NotSendMessage> = ArrayList()
    var dh: MXDataHandler? = null
    private var evLis: IMXEventListener? = null
    var store: IMXStore? = null
    var deviceName: String

    private val realUserid: String

    init {
        val builder = HomeServerConnectionConfig.Builder()

        hsConfig = builder.withHomeServerUri(Uri.parse(url)).build()

        realUserid = username
        deviceName = device
        this.syncDelay = syncDelay.toInt()
        this.syncTimeout = syncTimeout.toInt()

        login(botUsername, botPassword)
    }

    private fun login(username: String, password: String) {
        LoginRestClient(hsConfig).loginWithUser(
            username,
            password,
            deviceName,
            deviceName,
            object : SimpleApiCallback<Credentials?>() {
                override fun onSuccess(credentials: Credentials) {
                    onLogin(credentials)
                }

                override fun onMatrixError(e: MatrixError) {
                    Log.e(ContentValues.TAG, "onLogin MatrixError$e")
                }

                override fun onNetworkError(e: Exception) {
                    Log.e(ContentValues.TAG, "onLogin Network error$e")
                }

                override fun onUnexpectedError(e: Exception) {
                    Log.e(ContentValues.TAG, "onLogin Unexpected error$e")
                }
            })
    }

    private fun onLogin(credentials: Credentials) {
        hsConfig.credentials = credentials
        startEventStream()
    }

    fun startEventStream() {
        evLis = EventListener(this)


        store = if (false) {
            MXFileStore(hsConfig, false, context)
        } else {
            MXMemoryStore(hsConfig.credentials, context)
        }

        dh = MXDataHandler(store, hsConfig.credentials)

        //        NetworkConnectivityReceiver nwMan = new NetworkConnectivityReceiver();
        val builder = MXSession.Builder(hsConfig, dh!!, context)

        session = builder.build()
        session.setSyncDelay(syncDelay * 1000)
        session.setSyncTimeout(syncTimeout * 60 * 1000)
        Log.e(ContentValues.TAG, "onLogin:" + session.getSyncTimeout())



        if (store!!.isReady) {
            session.startEventStream(store.getEventStreamToken())
            session.getDataHandler().addListener(evLis)
        } else {
            store!!.addMXStoreListener(object : IMXStoreListener {
                override fun postProcess(s: String) {
                }

                override fun onStoreReady(s: String) {
                    session.startEventStream(store.getEventStreamToken())
                    session.getDataHandler().addListener(evLis)
                }

                override fun onStoreCorrupted(s: String, s1: String) {
                    Log.e(ContentValues.TAG, "onStoreCorrupted: $s")
                }

                override fun onStoreOOM(s: String, s1: String) {
                }

                override fun onReadReceiptsLoaded(s: String) {
                }
            })
        }
    }

    fun sendMessage(phoneNumber: String, body: String?, type: String) {
        if (session != null && session!!.isAlive) {
            val room = getRoomByPhonenumber(phoneNumber)
            if (room == null) {
                if (type != "m.notice") {
                    Log.e(ContentValues.TAG, "sendMessage: not found")
                    session!!.createDirectMessageRoom(
                        realUserid,
                        object : SimpleApiCallback<String?>() {
                            override fun onSuccess(info: String) {
                                session!!.roomsApiClient.updateTopic(
                                    info,
                                    phoneNumber,
                                    object : SimpleApiCallback<Void?>() {
                                        override fun onSuccess(aVoid: Void) {
                                        }
                                    })

                                changeDisplayname(info, getContactName(phoneNumber, context))
                                val room = store!!.getRoom(info)
                                SendMessageToRoom(room, body, type)
                            }
                        })
                }
            } else {
                changeDisplayname(room.getRoomId(), getContactName(phoneNumber, context))
                SendMessageToRoom(room, body, type)
            }
        } else {
            val tag = "Matrix"
            Log.e(tag, "Error with sending message")
            notSendMessages.add(NotSendMessage(phoneNumber, body, type))
        }
    }

    fun sendFile(
        phoneNumber: String,
        body: ByteArray?,
        type: String?,
        fileName: String?,
        contentType: String?
    ) {
        val uploadID = transaction.toString()
        transaction++
        session!!.mediaCache.uploadContent(
            ByteArrayInputStream(body),
            fileName,
            contentType,
            uploadID,
            object : MXMediaUploadListener() {
                override fun onUploadComplete(uploadId: String, contentUri: String) {
                    val room = getRoomByPhonenumber(phoneNumber)
                    val json = JsonObject()
                    json.addProperty("body", fileName)
                    json.addProperty("msgtype", type)
                    json.addProperty("url", contentUri)
                    val info = JsonObject()
                    info.addProperty("mimetype", contentType)
                    json.add("info", info)
                    checkNotNull(room)
                    session!!.roomsApiClient.sendEventToRoom(
                        transaction.toString(),
                        room.getRoomId(),
                        "m.room.message",
                        json,
                        object : SimpleApiCallback<CreatedEvent?>() {
                            override fun onSuccess(createdEvent: CreatedEvent) {
                            }
                        }
                    )
                    transaction++
                }
            }
        )
    }

    private fun changeDisplayname(roomId: String, displayname: String) {
        val params: MutableMap<String, Any> = HashMap()
        params["displayname"] = displayname
        params["membership"] = "join"
        session!!.roomsApiClient.sendStateEvent(
            roomId,
            "m.room.member",
            session.getMyUserId(),
            params,
            object : SimpleApiCallback<Void?>() {
                override fun onSuccess(aVoid: Void) {
                }
            })
    }

    fun SendMessageToRoom(room: Room, body: String?, type: String?) {
        val msg = Message()
        msg.body = body
        msg.msgtype = type
        session!!.roomsApiClient.sendMessage(
            transaction.toString(),
            room.getRoomId(),
            msg,
            object : SimpleApiCallback<CreatedEvent?>() {
                override fun onSuccess(createdEvent: CreatedEvent) {
                    Log.e(ContentValues.TAG, "sendMessage success")
                }

                override fun onMatrixError(e: MatrixError) {
                    Log.e(ContentValues.TAG, "sendMessage MatrixError$e")
                }

                override fun onNetworkError(e: Exception) {
                    Log.e(ContentValues.TAG, "sendMessage Network error$e")
                }

                override fun onUnexpectedError(e: Exception) {
                    Log.e(ContentValues.TAG, "sendMessage Unexpected error$e")
                }
            })
        transaction++
    }

    val unreadEvents: Unit
        get() {
            val types: MutableList<String> =
                ArrayList()
            types.add("m.room.message")

            val rooms = store!!.rooms
            for (room in rooms) {
                val roomsDirect =
                    store!!.unreadEvents(room.getRoomId(), types)
                for (event in roomsDirect) {
                    sendEvent(event)
                }
            }
        }

    fun sendEvent(event: Event) {
        if ((event.sender != null) && (event.sender == realUserid)) {
            val room = store!!.getRoom(event.roomId)
            val smsManager = SmsManager.getDefault()
            val json: JsonObject = event.getContent().getAsJsonObject()

            if (event.type == "m.room.message") {
                if (json["msgtype"].asString == MESSAGE_TYPE_TEXT) {
                    val body = smsManager.divideMessage(json["body"].asString)
                    smsManager.sendMultipartTextMessage(room.topic, null, body, null, null)
                } else {
                    val url = smsManager.divideMessage(
                        session!!.contentManager.getDownloadableUrl(
                            json["url"].asString
                        )
                    )
                    smsManager.sendMultipartTextMessage(room.topic, null, url, null, null)
                }
            } else if (event.type == "m.room.member") {
                if (json["membership"].asString == "leave") {
                    room.leave(object : SimpleApiCallback<Void?>() {
                        override fun onSuccess(aVoid: Void) {
                        }
                    })
                } else if (json["membership"].asString == "invite") {
                    room.join(object : SimpleApiCallback<Void?>() {
                        override fun onSuccess(aVoid: Void) {
                        }
                    })
                }
            } else {
                Log.e(ContentValues.TAG, "sendEvent: Event type not supported ")
            }


            room.markAllAsRead(object : SimpleApiCallback<Void?>() {
                override fun onSuccess(aVoid: Void) {
                }
            })
        }
    }


    fun onEventStreamLoaded() {
        sendMessageList(notSendMessages)
        notSendMessages.clear()
    }

    fun sendMessageList(messages: List<NotSendMessage>) {
        for (ms in messages) {
            sendMessage(ms.phone, ms.body, ms.type)
        }
    }

    private fun getRoomByPhonenumber(number: String): Room? {
        val rooms = store!!.rooms
        Log.e(ContentValues.TAG, "getRoomByPhonenumber: $number")
        Log.e(ContentValues.TAG, "getRoomByPhonenumber: " + rooms.size)
        for (room in rooms) {
            Log.e(ContentValues.TAG, "getRoomByPhonenumber: " + room.topic)
            if (room.topic != null && room.topic == number) {
                return room
            }
        }
        return null
    }

    private fun getContactName(phoneNumber: String, context: Context): String {
        val uri = Uri.withAppendedPath(
            ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
            Uri.encode(phoneNumber)
        )

        val projection = arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME)

        var contactName = ""
        val cursor = context.contentResolver.query(uri, projection, null, null, null)

        if (cursor != null) {
            if (cursor.moveToFirst()) {
                contactName = cursor.getString(0)
            }
            cursor.close()
        }

        if (contactName.isEmpty()) {
            contactName = phoneNumber
        }

        return contactName
    }

    fun destroy() {
        session!!.stopEventStream()
        dh!!.removeListener(evLis)
        store!!.close()
    }

    companion object {
        // Message type constants.
        const val MESSAGE_TYPE_TEXT: String = "m.text"
        const val MESSAGE_TYPE_IMAGE: String = "m.image"
        const val MESSAGE_TYPE_VIDEO: String = "m.video"
        const val MESSAGE_TYPE_NOTICE: String = "m.notice"
    }
}
