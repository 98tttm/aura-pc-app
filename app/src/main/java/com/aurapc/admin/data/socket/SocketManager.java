package com.aurapc.admin.data.socket;

import android.content.Context;
import android.util.Log;

import com.aurapc.admin.BuildConfig;
import com.aurapc.admin.Constants;
import com.aurapc.admin.data.local.TokenManager;
import com.aurapc.admin.data.model.SupportMessage;
import com.aurapc.admin.utils.SingleLiveEvent;

import org.json.JSONObject;

import java.net.URISyntaxException;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

/**
 * Wraps Socket.IO connection for admin order + support updates.
 *
 * <p>Auto-connects when a token is present and emits typed LiveData events
 * for {@link Constants#SOCKET_EVENT_ORDER_UPDATED} and support messages.</p>
 */
public final class SocketManager {

    private static final String TAG = "SocketManager";
    private static volatile SocketManager instance;

    private final TokenManager tokenManager;
    private Socket socket;

    public final SingleLiveEvent<OrderUpdate> orderUpdates = new SingleLiveEvent<>();
    public final SingleLiveEvent<SupportUpdate> supportUpdates = new SingleLiveEvent<>();
    public final SingleLiveEvent<SupportMessage> supportMessages = new SingleLiveEvent<>();

    private SocketManager(Context ctx) {
        this.tokenManager = new TokenManager(ctx);
    }

    public static SocketManager get(Context ctx) {
        if (instance == null) {
            synchronized (SocketManager.class) {
                if (instance == null) instance = new SocketManager(ctx.getApplicationContext());
            }
        }
        return instance;
    }

    public synchronized void connect() {
        if (!tokenManager.isLoggedIn()) return;
        if (socket != null && socket.connected()) return;
        try {
            IO.Options options = IO.Options.builder()
                    .setReconnection(true)
                    .setReconnectionDelay(2_000)
                    .setReconnectionDelayMax(10_000)
                    .setForceNew(false)
                    .build();
            socket = IO.socket(BuildConfig.BACKEND_URL, options);

            socket.on(Socket.EVENT_CONNECT, args -> {
                String adminId = tokenManager.getAdminId();
                if (adminId != null) {
                    JSONObject payload = new JSONObject();
                    try {
                        payload.put("adminId", adminId);
                        socket.emit("admin:register", payload);
                    } catch (Exception ignored) {}
                }
                Log.d(TAG, "Socket connected");
            });
            socket.on(Socket.EVENT_CONNECT_ERROR, args -> Log.w(TAG, "Socket connect error"));
            socket.on(Constants.SOCKET_EVENT_ORDER_UPDATED, onOrderUpdated);
            socket.on(Constants.SOCKET_EVENT_SUPPORT_MESSAGE, onSupportMessage);
            socket.on(Constants.SOCKET_EVENT_SUPPORT_CONVERSATION, onSupportConversation);

            socket.connect();
        } catch (URISyntaxException ex) {
            Log.e(TAG, "Socket connect failed", ex);
        }
    }

    public synchronized void disconnect() {
        if (socket != null) {
            socket.disconnect();
            socket.off();
            socket = null;
        }
    }

    private final Emitter.Listener onOrderUpdated = args -> {
        try {
            JSONObject obj = (JSONObject) args[0];
            String orderNumber = obj.optString("orderNumber");
            String status = obj.optString("status");
            String userId = obj.optString("userId", null);
            orderUpdates.postValue(new OrderUpdate(orderNumber, status, userId));
        } catch (Exception ex) {
            Log.w(TAG, "onOrderUpdated parse failed", ex);
        }
    };

    private final Emitter.Listener onSupportMessage = args -> {
        try {
            JSONObject obj = (JSONObject) args[0];
            JSONObject convObj = obj.optJSONObject("conversation");
            JSONObject msgObj = obj.optJSONObject("message");
            String conversationId = convObj != null ? convObj.optString("_id") : null;
            String preview = msgObj != null ? msgObj.optString("content") : "";
            supportUpdates.postValue(new SupportUpdate(conversationId, preview, true));
            // Emit full message detail if we can parse it
            if (msgObj != null) {
                SupportMessage m = new SupportMessage();
                m.id = msgObj.optString("_id");
                m.conversation = conversationId;
                m.content = msgObj.optString("content");
                m.senderType = msgObj.optString("senderType");
                m.createdAt = msgObj.optString("createdAt");
                supportMessages.postValue(m);
            }
        } catch (Exception ex) {
            Log.w(TAG, "onSupportMessage parse failed", ex);
        }
    };

    private final Emitter.Listener onSupportConversation = args -> {
        try {
            JSONObject obj = (JSONObject) args[0];
            String conversationId = obj.optString("_id");
            String preview = obj.optString("lastMessagePreview", "");
            supportUpdates.postValue(new SupportUpdate(conversationId, preview, false));
        } catch (Exception ex) {
            Log.w(TAG, "onSupportConversation parse failed", ex);
        }
    };

    public static class OrderUpdate {
        public final String orderNumber;
        public final String status;
        public final String userId;
        public OrderUpdate(String orderNumber, String status, String userId) {
            this.orderNumber = orderNumber;
            this.status = status;
            this.userId = userId;
        }
    }

    public static class SupportUpdate {
        public final String conversationId;
        public final String preview;
        public final boolean newMessage;
        public SupportUpdate(String conversationId, String preview, boolean newMessage) {
            this.conversationId = conversationId;
            this.preview = preview;
            this.newMessage = newMessage;
        }
    }

    public void joinConversation(String conversationId) {
        if (socket == null || !socket.connected() || conversationId == null) return;
        try {
            JSONObject payload = new JSONObject();
            payload.put("conversationId", conversationId);
            socket.emit("support:join", payload);
        } catch (Exception ignored) {}
    }

    public void leaveConversation(String conversationId) {
        if (socket == null || !socket.connected() || conversationId == null) return;
        try {
            JSONObject payload = new JSONObject();
            payload.put("conversationId", conversationId);
            socket.emit("support:leave", payload);
        } catch (Exception ignored) {}
    }

    public void addSupportMessageListener(SingleLiveEvent.Listener<SupportMessage> listener) {
        supportMessages.observeForever(message -> {
            if (message != null) listener.onChanged(message);
        });
    }
}