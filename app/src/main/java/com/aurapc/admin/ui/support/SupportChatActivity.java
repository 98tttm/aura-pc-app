package com.aurapc.admin.ui.support;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.aurapc.admin.R;
import com.aurapc.admin.data.api.ApiClient;
import com.aurapc.admin.data.api.ContentApi;
import com.aurapc.admin.data.api.Resource;
import com.aurapc.admin.data.model.SupportConversation;
import com.aurapc.admin.data.model.SupportMessage;
import com.aurapc.admin.data.socket.SocketManager;
import com.aurapc.admin.di.ServiceLocator;
import com.aurapc.admin.utils.NetworkHelper;
import com.google.android.material.appbar.MaterialToolbar;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SupportChatActivity extends AppCompatActivity {

    public static final String EXTRA_CONVERSATION_ID = "conversationId";

    private MaterialToolbar toolbar;
    private RecyclerView rvMessages;
    private ChatAdapter adapter;
    private String conversationId;
    private SupportConversation conversation;

    public static void start(android.content.Context ctx, String conversationId) {
        android.content.Intent i = new android.content.Intent(ctx, SupportChatActivity.class);
        i.putExtra(EXTRA_CONVERSATION_ID, conversationId);
        ctx.startActivity(i);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_support_chat);

        toolbar = findViewById(R.id.toolbar);
        rvMessages = findViewById(R.id.rvMessages);

        conversationId = getIntent().getStringExtra(EXTRA_CONVERSATION_ID);

        adapter = new ChatAdapter();
        rvMessages.setLayoutManager(new LinearLayoutManager(this));
        rvMessages.setAdapter(adapter);

        toolbar.setNavigationOnClickListener(v -> finish());

        findViewById(R.id.btnSend).setOnClickListener(v -> sendMessage());
        ((android.widget.EditText) findViewById(R.id.etMessage)).setOnEditorActionListener((v, id, e) -> {
            sendMessage();
            return true;
        });

        // Mark conversation as read
        if (conversationId != null) {
            ApiClient api = ServiceLocator.get().apiClient();
            api.contentApi().markSupportRead(conversationId).enqueue(new retrofit2.Callback<Map<String, Object>>() {
                @Override public void onResponse(retrofit2.Call<Map<String, Object>> call, retrofit2.Response<Map<String, Object>> response) {}
                @Override public void onFailure(retrofit2.Call<Map<String, Object>> call, Throwable t) {}
            });
            ServiceLocator.get().socketManager().joinConversation(conversationId);
        }

        loadDetail();

        // Listen for new messages
        ServiceLocator.get().socketManager().addSupportMessageListener(message -> {
            String convId = message.conversation != null ? message.conversation : null;
            if (conversationId != null && conversationId.equals(convId)) {
                runOnUiThread(() -> {
                    adapter.addMessage(message);
                    rvMessages.scrollToPosition(adapter.getItemCount() - 1);
                });
            }
        });
    }

    private void loadDetail() {
        ApiClient api = ServiceLocator.get().apiClient();
        NetworkHelper.toLiveData(api.contentApi().getSupportDetail(conversationId), (Resource<ContentApi.SupportDetailResponse> result) -> {
            if (result.isSuccess() && result.data != null) {
                conversation = result.data.conversation;
                if (conversation != null) {
                    toolbar.setTitle(conversation.userName());
                    toolbar.setSubtitle(conversation.adminName() != null ? "Đang xử lý: " + conversation.adminName() : "Chưa được nhận");
                }
                List<SupportMessage> msgs = result.data.messages != null ? result.data.messages : new ArrayList<>();
                adapter.setMessages(msgs);
                rvMessages.scrollToPosition(adapter.getItemCount() - 1);
            }
        });
    }

    private void sendMessage() {
        String text = ((android.widget.EditText) findViewById(R.id.etMessage)).getText().toString().trim();
        if (text.isEmpty()) return;

        Map<String, Object> body = new HashMap<>();
        body.put("content", text);

        ApiClient api = ServiceLocator.get().apiClient();
        ((android.widget.EditText) findViewById(R.id.etMessage)).setText("");
        NetworkHelper.toLiveData(api.contentApi().sendSupportMessage(conversationId, body), (Resource<ContentApi.SupportSendResponse> r) -> {
            if (r.isSuccess() && r.data != null && r.data.message != null) {
                adapter.addMessage(r.data.message);
                rvMessages.scrollToPosition(adapter.getItemCount() - 1);
            } else if (!r.isSuccess()) {
                Toast.makeText(this, "Gửi thất bại: " + (r.message != null ? r.message : ""), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (conversationId != null) {
            ServiceLocator.get().socketManager().leaveConversation(conversationId);
        }
    }
}