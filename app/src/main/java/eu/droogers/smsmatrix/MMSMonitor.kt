package eu.droogers.smsmatrix

import android.content.ContentResolver
import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Message
import android.provider.Telephony
import android.util.Log
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.util.Objects

class MMSMonitor(private val mainActivity: MatrixService, mainContext: Context?) {
    private val contentResolver: ContentResolver = mainActivity.contentResolver
    private val mmsObserver: ContentObserver
    var monitorStatus: Boolean = false
    private var mmsCount = 0

    init {
        val mmshandler: Handler = MMSHandler()
        mmsObserver = MMSObserver(mmshandler)
        Log.i(TAG, "***** Start MMS Monitor *****")
    }


    fun startMMSMonitoring() {
        try {
            monitorStatus = false
            contentResolver.registerContentObserver(
                Uri.parse("content://mms"),
                true,
                mmsObserver
            )

            // Save the count of MMS messages on start-up.
            val uriMMSURI = Uri.parse("content://mms-sms")
            val mmsCur = mainActivity.contentResolver.query(
                uriMMSURI,
                null,
                Telephony.Mms.MESSAGE_BOX + " = " + Telephony.Mms.MESSAGE_BOX_INBOX,
                null,
                Telephony.Mms._ID
            )
            if (mmsCur != null && mmsCur.count > 0) {
                mmsCount = mmsCur.count
                Log.d(TAG, "Init MMSCount = $mmsCount")
            }
        } catch (e: Exception) {
            Log.e(TAG, Objects.requireNonNull(e.message))
        }
    }


    fun stopMMSMonitoring() {
        try {
            monitorStatus = false
            contentResolver.unregisterContentObserver(mmsObserver)
        } catch (e: Exception) {
            Log.e(TAG, Objects.requireNonNull(e.message))
        }
    }


    internal class MMSHandler : Handler() {
        override fun handleMessage(msg: Message) {
            //Log.i(TAG, "Handler");
        }
    }


    internal inner class MMSObserver(private val mms_handle: Handler) : ContentObserver(
        mms_handle
    ) {
        override fun onChange(bSelfChange: Boolean) {
            super.onChange(bSelfChange)
            Log.i(TAG, "Onchange")

            try {
                monitorStatus = true

                // Send message to Activity.
                val msg = Message()
                mms_handle.sendMessage(msg)

                // Get the MMS count.
                val uriMMSURI = Uri.parse("content://mms/")
                val mmsCur = mainActivity.contentResolver.query(
                    uriMMSURI,
                    null,
                    Telephony.Mms.MESSAGE_BOX + " = " + Telephony.Mms.MESSAGE_BOX_INBOX,
                    null,
                    Telephony.Mms._ID
                )

                var currMMSCount = 0
                if (mmsCur != null && mmsCur.count > 0) {
                    currMMSCount = mmsCur.count
                }

                // Proceed if there is a new message.
                if (currMMSCount > mmsCount) {
                    mmsCount = currMMSCount
                    checkNotNull(mmsCur)
                    mmsCur.moveToLast()

                    // Get the message id and subject.
                    val subject = mmsCur.getString(mmsCur.getColumnIndex(Telephony.Mms.SUBJECT))
                    val id = mmsCur.getString(mmsCur.getColumnIndex(Telephony.Mms._ID)).toInt()
                    Log.d(TAG, "_id = $id")
                    Log.d(TAG, "Subject = $subject")

                    var mediaData: ByteArray? = null
                    var message = ""
                    var address = ""
                    var fileName: String
                    var fileType = ""
                    var messageType = ""

                    // Get parts.
                    val uriMMSPart = Uri.parse("content://mms/part")
                    val curPart = checkNotNull(
                        mainActivity.contentResolver.query(
                            uriMMSPart,
                            null,
                            Telephony.Mms.Part.MSG_ID + " = " + id,
                            null,
                            Telephony.Mms.Part._ID
                        )
                    )
                    Log.d(TAG, "Parts records length = " + curPart.count)
                    curPart.moveToLast()
                    do {
                        val contentType =
                            curPart.getString(curPart.getColumnIndex(Telephony.Mms.Part.CONTENT_TYPE))
                        val partId =
                            curPart.getString(curPart.getColumnIndex(Telephony.Mms.Part._ID))
                        fileName =
                            curPart.getString(curPart.getColumnIndex(Telephony.Mms.Part.NAME))
                        Log.d(TAG, "partId = $partId")
                        Log.d(
                            TAG,
                            "Part mime type = $contentType"
                        )

                        if (contentType.equals("text/plain", ignoreCase = true)) {
                            // Get the message.

                            Log.i(TAG, "==== Get the message start ====")
                            messageType = Matrix.MESSAGE_TYPE_TEXT
                            val messageData = readMMSPart(partId)
                            if (messageData != null && messageData.size > 0) {
                                message = String(messageData)
                            }

                            if (message.isEmpty()) {
                                val curPart1 = mainActivity.contentResolver.query(
                                    uriMMSPart,
                                    null,
                                    Telephony.Mms.Part.MSG_ID + " = " + id + " and " + Telephony.Mms.Part._ID + " = " + partId,
                                    null,
                                    Telephony.Mms.Part._ID
                                )
                                for (i in 0..<Objects.requireNonNull(curPart1).columnCount) {
                                    Log.d(TAG, "Column Name : " + curPart1!!.getColumnName(i))
                                }
                                curPart1!!.moveToLast()
                                message = curPart1.getString(13)
                            }
                            Log.d(TAG, "Txt Message = $message")
                        } else if (isImageType(contentType) || isVideoType(contentType)) {
                            // Get the media.

                            if (isImageType(contentType)) {
                                messageType = Matrix.MESSAGE_TYPE_IMAGE
                            } else if (isVideoType(contentType)) {
                                messageType = Matrix.MESSAGE_TYPE_VIDEO
                            }
                            Log.i(TAG, "==== Get the media start ====")
                            fileType = contentType
                            mediaData = readMMSPart(partId)
                            Log.i(TAG, "Media data length == " + mediaData!!.size)
                        }
                    } while (curPart.moveToPrevious())


                    // Get the sender's address.
                    val uriMMSAddr = Uri.parse("content://mms/$id/addr")
                    val addrCur = mainActivity.contentResolver.query(
                        uriMMSAddr,
                        null,
                        Telephony.Mms.Addr.TYPE + " = 137",  // PduHeaders.FROM
                        null,
                        Telephony.Mms.Addr._ID
                    )
                    if (addrCur != null) {
                        addrCur.moveToLast()
                        do {
                            Log.d(TAG, "addrCur records length = " + addrCur.count)
                            if (addrCur.count > 0) {
                                address =
                                    addrCur.getString(addrCur.getColumnIndex(Telephony.Mms.Addr.ADDRESS))
                            }
                            Log.d(TAG, "address = $address")

                            if (!message.isEmpty()) {
                                Utilities.sendMatrix(mainActivity, message, address, messageType)
                            }
                            if (mediaData != null) {
                                Utilities.sendMatrix(
                                    mainActivity,
                                    mediaData,
                                    address,
                                    messageType,
                                    fileName,
                                    fileType
                                )
                            }
                        } while (addrCur.moveToPrevious())
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, Objects.requireNonNull(e.message))
            }
        }
    }


    private fun readMMSPart(partId: String): ByteArray? {
        var partData: ByteArray? = null
        val partURI = Uri.parse("content://mms/part/$partId")
        val baos = ByteArrayOutputStream()
        var `is`: InputStream? = null

        try {
            Log.i(TAG, "Entered into readMMSPart try.")
            val mContentResolver = mainActivity.contentResolver
            `is` = mContentResolver.openInputStream(partURI)

            val buffer = ByteArray(256)
            checkNotNull(`is`)
            var len = `is`.read(buffer)
            while (len >= 0) {
                baos.write(buffer, 0, len)
                len = `is`.read(buffer)
            }
            partData = baos.toByteArray()

            //Log.i(TAG, "Text Msg  :: " + new String(partData));
        } catch (e: IOException) {
            Log.e(TAG, "Exception == Failed to load part data")
        } finally {
            if (`is` != null) {
                try {
                    `is`.close()
                } catch (e: IOException) {
                    Log.e(TAG, "Exception :: Failed to close stream")
                }
            }
        }
        return partData
    }


    private fun isImageType(mime: String): Boolean {
        return mime.equals("image/jpg", ignoreCase = true)
                || mime.equals("image/jpeg", ignoreCase = true)
                || mime.equals("image/png", ignoreCase = true)
                || mime.equals("image/gif", ignoreCase = true)
                || mime.equals("image/bmp", ignoreCase = true)
    }


    private fun isVideoType(mime: String): Boolean {
        return mime.equals("video/3gpp", ignoreCase = true)
                || mime.equals("video/3gpp2", ignoreCase = true)
                || mime.equals("video/avi", ignoreCase = true)
                || mime.equals("video/mp4", ignoreCase = true)
                || mime.equals("video/mpeg", ignoreCase = true)
                || mime.equals("video/webm", ignoreCase = true)
    }

    companion object {
        private const val TAG = "MMSMonitor"
    }
}