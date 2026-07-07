package com.aurapc.admin.ui.blog;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
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
import com.aurapc.admin.data.model.BlogPost;
import com.aurapc.admin.di.ServiceLocator;
import com.aurapc.admin.utils.NetworkHelper;

import java.util.ArrayList;

public class BlogFragment extends Fragment {

    private SwipeRefreshLayout swipeRefresh;
    private RecyclerView rvBlogs;
    private ProgressBar progress;
    private EditText etSearch;

    private LiveData<Resource<ContentApi.BlogListResponse>> blogLiveData;
    private BlogAdapter adapter;
    private String currentQuery = "";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_blog, container, false);
        swipeRefresh = root.findViewById(R.id.swipeRefresh);
        rvBlogs = root.findViewById(R.id.rvBlogs);
        progress = root.findViewById(R.id.progress);
        etSearch = root.findViewById(R.id.etSearch);

        adapter = new BlogAdapter(post -> {
            // Open detail/edit
        });
        rvBlogs.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvBlogs.setAdapter(adapter);

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

        load();
        return root;
    }

    private void load() {
        progress.setVisibility(View.VISIBLE);
        ApiClient api = ServiceLocator.get().apiClient();
        blogLiveData = NetworkHelper.toLiveData(api.contentApi().listBlogs(1, 50));
        blogLiveData.observe(getViewLifecycleOwner(), result -> {
            progress.setVisibility(View.GONE);
            swipeRefresh.setRefreshing(false);
            if (result.isSuccess() && result.data != null) {
                ArrayList<BlogPost> items = new ArrayList<>();
                if (result.data.items != null) {
                    for (BlogPost p : result.data.items) {
                        if (currentQuery.isEmpty() ||
                                (p.title != null && p.title.toLowerCase().contains(currentQuery.toLowerCase()))) {
                            items.add(p);
                        }
                    }
                }
                adapter.setBlogs(items);
            }
        });
    }
}