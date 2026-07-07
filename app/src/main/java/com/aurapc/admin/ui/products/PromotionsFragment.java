package com.aurapc.admin.ui.products;

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
import com.aurapc.admin.data.model.Promotion;
import com.aurapc.admin.di.ServiceLocator;
import com.aurapc.admin.utils.NetworkHelper;

import java.util.ArrayList;

public class PromotionsFragment extends Fragment {

    private SwipeRefreshLayout swipeRefresh;
    private RecyclerView rvPromotions;
    private ProgressBar progress;

    private PromotionsAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_promotions, container, false);
        swipeRefresh = root.findViewById(R.id.swipeRefresh);
        rvPromotions = root.findViewById(R.id.rvPromotions);
        progress = root.findViewById(R.id.progress);

        adapter = new PromotionsAdapter();
        rvPromotions.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvPromotions.setAdapter(adapter);

        swipeRefresh.setColorSchemeResources(R.color.aura_orange);
        swipeRefresh.setOnRefreshListener(this::load);

        load();
        return root;
    }

    private void load() {
        progress.setVisibility(View.VISIBLE);
        ApiClient api = ServiceLocator.get().apiClient();
        LiveData<Resource<ContentApi.PromotionListResponse>> ld = NetworkHelper.toLiveData(
                api.contentApi().listPromotions(1, 50, ""));
        ld.observe(getViewLifecycleOwner(), result -> {
            progress.setVisibility(View.GONE);
            swipeRefresh.setRefreshing(false);
            if (result.isSuccess() && result.data != null) {
                ArrayList<Promotion> items = new ArrayList<>();
                if (result.data.items != null) items.addAll(result.data.items);
                adapter.setPromotions(items);
            }
        });
    }
}