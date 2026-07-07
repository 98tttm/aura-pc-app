package com.aurapc.admin.ui.hub;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Toast;

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
import com.aurapc.admin.data.model.HubPost;
import com.aurapc.admin.di.ServiceLocator;
import com.aurapc.admin.utils.NetworkHelper;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class HubFragment extends Fragment {

    private SwipeRefreshLayout swipeRefresh;
    private RecyclerView rvHub;
    private ProgressBar progress;
    private ChipGroup statusChips;

    private LiveData<Resource<ContentApi.HubListResponse>> hubLiveData;
    private HubAdapter adapter;
    private String currentStatus = "pending";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_hub, container, false);
        swipeRefresh = root.findViewById(R.id.swipeRefresh);
        rvHub = root.findViewById(R.id.rvHub);
        progress = root.findViewById(R.id.progress);
        statusChips = root.findViewById(R.id.statusChips);

        setupChips();
        adapter = new HubAdapter(this::onApprove, this::onReject, this::onDelete);
        rvHub.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvHub.setAdapter(adapter);

        swipeRefresh.setColorSchemeResources(R.color.aura_orange);
        swipeRefresh.setOnRefreshListener(this::load);

        load();
        return root;
    }

    private void setupChips() {
        String[] labels = {"Cho duyet", "Da duyet", "Tu choi", "Tat ca"};
        String[] values = {"pending", "approved", "rejected", "all"};

        for (int i = 0; i < labels.length; i++) {
            final String value = values[i];
            Chip chip = new Chip(requireContext());
            chip.setText(labels[i]);
            chip.setCheckable(true);
            chip.setCheckedIconVisible(false);
            chip.setOnClickListener(view -> {
                currentStatus = value;
                load();
            });
            statusChips.addView(chip);
            if (i == 0) chip.setChecked(true);
        }
    }

    private void load() {
        progress.setVisibility(View.VISIBLE);
        ApiClient api = ServiceLocator.get().apiClient();
        if (hubLiveData != null) {
            hubLiveData.removeObservers(getViewLifecycleOwner());
        }
        hubLiveData = NetworkHelper.toLiveData(
                api.contentApi().listHubPosts(1, 50, currentStatus, null, null, "newest"));
        hubLiveData.observe(getViewLifecycleOwner(), result -> {
            if (result == null || result.isLoading()) {
                return;
            }
            progress.setVisibility(View.GONE);
            swipeRefresh.setRefreshing(false);
            if (result.isSuccess() && result.data != null) {
                ArrayList<HubPost> items = new ArrayList<>();
                if (result.data.items != null) items.addAll(result.data.items);
                adapter.setPosts(items);
            } else {
                adapter.setPosts(new ArrayList<>());
                Toast.makeText(requireContext(), result.message != null ? result.message : getString(R.string.error_generic), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void onApprove(HubPost post) {
        ApiClient api = ServiceLocator.get().apiClient();
        Map<String, Object> body = new HashMap<>();
        body.put("forcePublishNow", true);
        NetworkHelper.toLiveData(api.contentApi().approveHubPost(post.id, body), (Resource<HubPost> result) -> {
            if (result != null && result.isSuccess()) load();
        });
    }

    private void onReject(HubPost post) {
        ApiClient api = ServiceLocator.get().apiClient();
        Map<String, Object> body = new HashMap<>();
        body.put("reason", "Vi pham tieu chuan cong dong");
        NetworkHelper.toLiveData(api.contentApi().rejectHubPost(post.id, body), (Resource<HubPost> result) -> {
            if (result != null && result.isSuccess()) load();
        });
    }

    private void onDelete(HubPost post) {
        ApiClient api = ServiceLocator.get().apiClient();
        NetworkHelper.toLiveData(api.contentApi().deleteHubPost(post.id), result -> {
            if (result != null && result.isSuccess()) load();
        });
    }
}
