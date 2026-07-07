package com.aurapc.admin.ui.reviews;

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
import com.aurapc.admin.data.model.ProductReview;
import com.aurapc.admin.di.ServiceLocator;
import com.aurapc.admin.utils.NetworkHelper;

import java.util.ArrayList;

public class ReviewsFragment extends Fragment {

    private SwipeRefreshLayout swipeRefresh;
    private RecyclerView rvReviews;
    private ProgressBar progress;
    private ReviewsAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_reviews, container, false);
        swipeRefresh = root.findViewById(R.id.swipeRefresh);
        rvReviews = root.findViewById(R.id.rvReviews);
        progress = root.findViewById(R.id.progress);

        adapter = new ReviewsAdapter(this::onHide, this::onRestore);
        rvReviews.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvReviews.setAdapter(adapter);

        swipeRefresh.setColorSchemeResources(R.color.aura_orange);
        swipeRefresh.setOnRefreshListener(this::load);

        load();
        return root;
    }

    private void load() {
        progress.setVisibility(View.VISIBLE);
        ApiClient api = ServiceLocator.get().apiClient();
        LiveData<Resource<ContentApi.ReviewListResponse>> ld = NetworkHelper.toLiveData(
                api.contentApi().listFlaggedReviews(1, 50));
        ld.observe(getViewLifecycleOwner(), result -> {
            progress.setVisibility(View.GONE);
            swipeRefresh.setRefreshing(false);
            if (result.isSuccess() && result.data != null) {
                ArrayList<ProductReview> items = new ArrayList<>();
                if (result.data.items != null) items.addAll(result.data.items);
                adapter.setReviews(items);
            }
        });
    }

    private void onHide(ProductReview r) {
        ApiClient api = ServiceLocator.get().apiClient();
        NetworkHelper.toLiveData(api.contentApi().hideReview(r.id), r2 -> load());
    }

    private void onRestore(ProductReview r) {
        ApiClient api = ServiceLocator.get().apiClient();
        NetworkHelper.toLiveData(api.contentApi().restoreReview(r.id), r2 -> load());
    }
}