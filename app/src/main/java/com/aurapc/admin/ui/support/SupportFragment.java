package com.aurapc.admin.ui.support;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.aurapc.admin.R;
import com.aurapc.admin.data.api.ApiClient;
import com.aurapc.admin.data.api.ContentApi;
import com.aurapc.admin.data.api.Resource;
import com.aurapc.admin.data.model.SupportConversation;
import com.aurapc.admin.di.ServiceLocator;
import com.aurapc.admin.utils.NetworkHelper;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.util.ArrayList;

public class SupportFragment extends Fragment {

    private SwipeRefreshLayout swipeRefresh;
    private RecyclerView rvSupport;
    private ProgressBar progress;
    private ChipGroup tabChips;

    private SupportAdapter adapter;
    private String currentTab = "open";
    private String currentSearch = "";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_support, container, false);
        swipeRefresh = root.findViewById(R.id.swipeRefresh);
        rvSupport = root.findViewById(R.id.rvSupport);
        progress = root.findViewById(R.id.progress);
        tabChips = root.findViewById(R.id.tabChips);

        setupChips();
        adapter = new SupportAdapter(conv -> {
            SupportChatActivity.start(requireContext(), conv.id);
        });
        rvSupport.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvSupport.setAdapter(adapter);

        swipeRefresh.setColorSchemeResources(R.color.aura_orange);
        swipeRefresh.setOnRefreshListener(this::load);

        load();
        return root;
    }

    private void setupChips() {
        String[] labels = {"Đang mở", "Đã lưu trữ", "Chưa đọc"};
        String[] values = {"open", "archived", "unread"};

        for (int i = 0; i < labels.length; i++) {
            final String v = values[i];
            Chip chip = new Chip(requireContext());
            chip.setText(labels[i]);
            chip.setCheckable(true);
            chip.setCheckedIconVisible(false);
            chip.setOnClickListener(view -> {
                currentTab = v;
                load();
            });
            tabChips.addView(chip);
            if (i == 0) chip.setChecked(true);
        }
    }

    private void load() {
        progress.setVisibility(View.VISIBLE);
        ApiClient api = ServiceLocator.get().apiClient();
        String tab = currentTab.equals("unread") ? "open" : currentTab;
        LiveData<Resource<ContentApi.SupportListResponse>> ld = NetworkHelper.toLiveData(
                api.contentApi().listSupport(tab, currentSearch));
        ld.observe(getViewLifecycleOwner(), result -> {
            progress.setVisibility(View.GONE);
            swipeRefresh.setRefreshing(false);
            if (result.isSuccess() && result.data != null) {
                ArrayList<SupportConversation> items = new ArrayList<>();
                if (result.data.items != null) items.addAll(result.data.items);
                if (currentTab.equals("unread")) {
                    java.util.Iterator<SupportConversation> it = items.iterator();
                    while (it.hasNext()) {
                        SupportConversation c = it.next();
                        Integer unread = c.unreadForAdmin != null ? c.unreadForAdmin : 0;
                        if (unread <= 0) it.remove();
                    }
                }
                adapter.setConversations(items);
            }
        });
    }
}