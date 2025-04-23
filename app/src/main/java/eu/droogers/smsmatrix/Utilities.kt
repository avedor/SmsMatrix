package eu.droogers.smsmatrix

import android.content.Context
import android.content.Intent
import eu.droogers.smsmatrix.MatrixService

object Utilities {
    @JvmStatic
    fun sendMatrix(context: Context, body: String?, phone: String?, type: String?) {
        val intent = Intent(context, MatrixService::class.java)
        intent.putExtra("SendSms_phone", phone)
        intent.putExtra("SendSms_body", body)
        intent.putExtra("SendSms_type", type)
        context.startService(intent)
    }

    fun sendMatrix(
        context: Context,
        body: ByteArray?,
        phone: String?,
        type: String?,
        fileName: String?,
        contentType: String?
    ) {
        val intent = Intent(context, MatrixService::class.java)
        intent.putExtra("SendSms_phone", phone)
        intent.putExtra("SendSms_body", body)
        intent.putExtra("SendSms_type", type)
        intent.putExtra("SendSms_fileName", fileName)
        intent.putExtra("SendSms_contentType", contentType)
        context.startService(intent)
    }
}
