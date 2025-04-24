package eu.droogers.smsmatrix

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.SmsMessage
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat
import eu.droogers.smsmatrix.MatrixService
import eu.droogers.smsmatrix.Utilities.sendMatrix
import java.util.Objects

/**
 * Created by gerben on 6-10-17.
 */
class ReceiverListener : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "android.provider.Telephony.SMS_RECEIVED") {
            handleIncomingSMS(context, intent)
        } else if (intent.action == "android.intent.action.PHONE_STATE") {
            handleIncomingCall(context, intent)
        } else if (intent.action == "android.intent.action.BOOT_COMPLETED") {
            val intentServ = Intent(context, MatrixService::class.java)
            ContextCompat.startForegroundService(context, intentServ)
        }
    }

    private fun handleIncomingSMS(context: Context, intent: Intent) {
        var msg: MutableMap<String?, String?>? = null
        val msgs: Array<SmsMessage?>
        val bundle = intent.extras

        if (bundle != null && bundle.containsKey("pdus")) {
            val pdus = bundle["pdus"] as Array<Any>?

            if (pdus != null) {
                val nbrOfpdus = pdus.size
                msg = HashMap(nbrOfpdus)
                msgs = arrayOfNulls(nbrOfpdus)

                // Send long SMS of same sender in one message
                for (i in 0..<nbrOfpdus) {
                    msgs[i] = SmsMessage.createFromPdu(pdus[i] as ByteArray)

                    val originationAddress = msgs[i].getOriginatingAddress()

                    // Check if index with number exists
                    if (!msg.containsKey(originationAddress)) {
                        // Index with number doesn't exist
                        msg[msgs[i].getOriginatingAddress()] = msgs[i].getMessageBody()
                    } else {
                        // Number is there.
                        val previousparts = msg[originationAddress]
                        val msgString = previousparts + msgs[i].getMessageBody()
                        msg[originationAddress] = msgString
                    }
                }
            }
        }
        checkNotNull(msg)
        for (originatinAddress in msg.keys) {
            sendMatrix(
                context,
                msg[originatinAddress], originatinAddress, Matrix.MESSAGE_TYPE_TEXT
            )
        }
    }

    private fun handleIncomingCall(context: Context, intent: Intent) {
        val cal_state =
            Objects.requireNonNull(intent.extras).getString(TelephonyManager.EXTRA_STATE)
        val cal_from = intent.extras!!.getString(TelephonyManager.EXTRA_INCOMING_NUMBER)
        var body = cal_from
        when (Objects.requireNonNull(cal_state)) {
            "IDLE" -> body += " end call"
            "OFFHOOK" -> body += " answered call"
            "RINGING" -> body += " is calling"
        }
        sendMatrix(context, body, cal_from, Matrix.MESSAGE_TYPE_NOTICE)
    }

    companion object {
        private const val TAG = "ReceiverListener"
    }
}
