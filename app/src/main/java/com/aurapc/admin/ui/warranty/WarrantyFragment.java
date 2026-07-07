package com.aurapc.admin.ui.warranty;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

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
import com.aurapc.admin.data.model.WarrantyItem;
import com.aurapc.admin.di.ServiceLocator;
import com.aurapc.admin.utils.NetworkHelper;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.util.ArrayList;
import java.util.Map;

public class WarrantyFragment extends Fragment {

    private SwipeRefreshLayout swipeRefresh;
    private RecyclerView rvWarranty;
    private ProgressBar progress;
    private EditText etSearch;
    private ChipGroup statusChips;
    private TextView tvTotal, tvValid, tvExpired;

    private WarrantyAdapter adapter;
    private String currentStatus = "all";
    private String currentQuery = "";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_warranty, container, false);
        swipeRefresh = root.findViewById(R.id.swipeRefresh);
        rvWarranty = root.findViewById(R.id.rvWarranty);
        progress = root.findViewById(R.id.progress);
        etSearch = root.findViewById(R.id.etSearch);
        statusChips = root.findViewById(R.id.statusChips);
        tvTotal = root.findViewById(R.id.tvTotal);
        tvValid = root.findViewById(R.id.tvValid);
        tvExpired = root.findViewById(R.id.tvExpired);

        setupChips();
        adapter = new WarrantyAdapter();
        rvWarranty.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvWarranty.setAdapter(adapter);

        swipeRefresh.setColorSchemeResources(R.color.aura_orange);
        swipeRefresh.setOnRefreshListener(this::load);

        etSearch.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            public void afterTextChanged(Editable s) {
                currentQuery = s.toString().trim();
                load();
            }
        });

        loadStats();
        load();
        return root;
    }

    private void setupChips() {
        String[] labels = {"Tất cả", "Còn hạn", "Hết hạn", "Đã sử dụng"};
        String[] values = {"all", "active", "expired", "used"};

        for (int i = 0; i < labels.length; i++) {
            final String v = values[i];
            Chip chip = new Chip(requireContext());
            chip.setText(labels[i]);
            chip.setCheckable(true);
            chip.setCheckedIconVisible(false);
            chip.setOnClickListener(view -> {
                currentStatus = v;
                load();
            });
            statusChips.addView(chip);
            if (i == 0) chip.setChecked(true);
        }
    }

    private void load() {
        progress.setVisibility(View.VISIBLE);
        ApiClient api = ServiceLocator.get().apiClient();
        LiveData<Resource<ContentApi.WarrantyListResponse>> ld = NetworkHelper.toLiveData(
                api.contentApi().listWarranty(1, 50, currentStatus, currentQuery));
        ld.observe(getViewLifecycleOwner(), result -> {
            progress.setVisibility(View.GONE);
            swipeRefresh.setRefreshing(false);
            if (result.isSuccess() && result.data != null) {
                ArrayList<WarrantyItem> items = new ArrayList<>();
                if (result.data.items != null) items.addAll(result.data.items);
                adapter.setItems(items);
                tvTotal.setText(String.valueOf(result.data.total));
            }
        });
    }

    private void loadStats() {
        ApiClient api = ServiceLocator.get().apiClient();
        NetworkHelper.toLiveData(api.contentApi().warrantyStats(), (Resource<Map<String, Object>> result) -> {
            if (result.isSuccess() && result.data != null) {
                Object total = result.data.get("total");
                Object valid = result.data.get("active");
                Object expired = result.data.get("expired");
                if (total != null) tvTotal.setText(total.toString());
                if (valid != null) tvValid.setText(valid.toString());
                if (expired != null) tvExpired.setText(expired.toString());
            }
        });
    }
}