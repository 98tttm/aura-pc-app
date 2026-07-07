package com.aurapc.admin.ui.categories;

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
import com.aurapc.admin.data.api.Resource;
import com.aurapc.admin.data.model.Category;
import com.aurapc.admin.di.ServiceLocator;
import com.aurapc.admin.utils.NetworkHelper;

import java.util.List;

public class CategoriesFragment extends Fragment {

    private SwipeRefreshLayout swipeRefresh;
    private RecyclerView rvCategories;
    private ProgressBar progress;
    private CategoriesAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_categories, container, false);
        swipeRefresh = root.findViewById(R.id.swipeRefresh);
        rvCategories = root.findViewById(R.id.rvCategories);
        progress = root.findViewById(R.id.progress);

        adapter = new CategoriesAdapter();
        rvCategories.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvCategories.setAdapter(adapter);

        swipeRefresh.setColorSchemeResources(R.color.aura_orange);
        swipeRefresh.setOnRefreshListener(this::load);

        load();
        return root;
    }

    private void load() {
        progress.setVisibility(View.VISIBLE);
        ApiClient api = ServiceLocator.get().apiClient();
        LiveData<Resource<List<Category>>> ld = NetworkHelper.toLiveData(api.contentApi().listCategories());
        ld.observe(getViewLifecycleOwner(), result -> {
            progress.setVisibility(View.GONE);
            swipeRefresh.setRefreshing(false);
            if (result.isSuccess() && result.data != null) {
                adapter.setCategories(result.data);
            }
        });
    }
}