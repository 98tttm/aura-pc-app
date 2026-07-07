package com.aurapc.admin.ui.users;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.aurapc.admin.R;
import com.aurapc.admin.data.api.Resource;
import com.aurapc.admin.data.api.UserApi;
import com.aurapc.admin.data.model.CustomerUser;
import com.aurapc.admin.di.ServiceLocator;
import com.aurapc.admin.utils.NetworkHelper;

import java.util.ArrayList;

public class UsersFragment extends Fragment {

    private SwipeRefreshLayout swipeRefresh;
    private RecyclerView rvUsers;
    private ProgressBar progress;
    private LinearLayout emptyState;
    private EditText etSearch;

    private LiveData<Resource<UserApi.UserListResponse>> usersLiveData;
    private UsersAdapter adapter;
    private String currentQuery = "";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_users, container, false);
        swipeRefresh = root.findViewById(R.id.swipeRefresh);
        rvUsers = root.findViewById(R.id.rvUsers);
        progress = root.findViewById(R.id.progress);
        emptyState = root.findViewById(R.id.emptyState);
        etSearch = root.findViewById(R.id.etSearch);

        adapter = new UsersAdapter(user -> {
            UserDetailActivity.start(requireContext(), user.id);
        });
        rvUsers.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvUsers.setAdapter(adapter);

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
        emptyState.setVisibility(View.GONE);

        if (usersLiveData != null) {
            usersLiveData.removeObservers(getViewLifecycleOwner());
        }
        usersLiveData = NetworkHelper.toLiveData(
                ServiceLocator.get().apiClient().userApi().getUsers(currentQuery, 1, 50));
        usersLiveData.observe(getViewLifecycleOwner(), result -> {
            if (result == null || result.isLoading()) {
                return;
            }
            progress.setVisibility(View.GONE);
            swipeRefresh.setRefreshing(false);
            if (result.isSuccess() && result.data != null) {
                ArrayList<CustomerUser> users = new ArrayList<>();
                if (result.data.items != null) users.addAll(result.data.items);
                adapter.setUsers(users);
                emptyState.setVisibility(users.isEmpty() ? View.VISIBLE : View.GONE);
            } else {
                emptyState.setVisibility(View.VISIBLE);
            }
        });
    }
}
