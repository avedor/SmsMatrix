package eu.droogers.smsmatrix

import android.util.Log
import org.matrix.androidsdk.core.model.MatrixError
import org.matrix.androidsdk.data.MyUser
import org.matrix.androidsdk.data.RoomState
import org.matrix.androidsdk.listeners.IMXEventListener
import org.matrix.androidsdk.rest.model.Event
import org.matrix.androidsdk.rest.model.User
import org.matrix.androidsdk.rest.model.bingrules.BingRule
import org.matrix.androidsdk.rest.model.sync.AccountDataElement

/**
 * Created by gerben on 8-10-17.
 */
class EventListener(private val mx: Matrix) : IMXEventListener {
    private var loaded = false

    override fun onStoreReady() {
    }

    override fun onPresenceUpdate(event: Event, user: User) {
    }

    override fun onAccountInfoUpdate(myUser: MyUser) {
    }

    override fun onIgnoredUsersListUpdate() {
    }

    override fun onDirectMessageChatRoomsListUpdate() {
    }

    override fun onLiveEvent(event: Event, roomState: RoomState) {
        if (loaded) {
//            mx.getUnreadEvents();
            mx.sendEvent(event)
        }
        Log.e(TAG, "onLiveEvent: $event")
    }

    override fun onLiveEventsChunkProcessed(s: String, s1: String) {
    }

    override fun onBingEvent(event: Event, roomState: RoomState, bingRule: BingRule) {
    }

    override fun onEventSentStateUpdated(event: Event) {
    }

    override fun onEventSent(event: Event, s: String) {
    }

    override fun onEventDecrypted(s: String, s1: String) {
    }

    override fun onBingRulesUpdate() {
    }

    override fun onInitialSyncComplete(s: String) {
        loaded = true
        mx.onEventStreamLoaded()
        mx.getUnreadEvents()
    }

    override fun onSyncError(matrixError: MatrixError) {
    }

    override fun onCryptoSyncComplete() {
    }

    override fun onNewRoom(s: String) {
    }

    override fun onJoinRoom(s: String) {
    }

    override fun onRoomFlush(s: String) {
    }

    override fun onRoomInternalUpdate(s: String) {
    }

    override fun onNotificationCountUpdate(s: String) {
    }

    override fun onLeaveRoom(s: String) {
    }

    override fun onRoomKick(s: String) {
    }

    override fun onReceiptEvent(s: String, list: List<String>) {
    }

    override fun onRoomTagEvent(s: String) {
    }

    override fun onReadMarkerEvent(s: String) {
    }

    override fun onToDeviceEvent(event: Event) {
    }

    override fun onNewGroupInvitation(s: String) {
    }

    override fun onJoinGroup(s: String) {
    }

    override fun onLeaveGroup(s: String) {
    }

    override fun onGroupProfileUpdate(s: String) {
    }

    override fun onGroupRoomsListUpdate(s: String) {
    }

    override fun onGroupUsersListUpdate(s: String) {
    }

    override fun onGroupInvitedUsersListUpdate(s: String) {
    }

    override fun onAccountDataUpdated(accountDataElement: AccountDataElement) {
    }

    companion object {
        private const val TAG = "EventListener"
    }
}
