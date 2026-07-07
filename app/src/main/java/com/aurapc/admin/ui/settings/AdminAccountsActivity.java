package com.aurapc.admin.ui.settings;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.aurapc.admin.R;
import com.aurapc.admin.data.api.AuthApi;
import com.aurapc.admin.data.api.Resource;
import com.aurapc.admin.di.ServiceLocator;
import com.aurapc.admin.utils.NetworkHelper;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Manage admin accounts (super_admin only). Lists all admins, allows creating
 * and editing roles/permissions.
 */
public class AdminAccountsActivity extends AppCompatActivity {

    private MaterialToolbar toolbar;
    private RecyclerView rvAdmins;
    private FloatingActionButton fabAdd;
    private SearchView searchView;
    private View emptyState;
    private ProgressBar progress;

    private final List<AuthApi.AdminUser> allAdmins = new ArrayList<>();
    private AdminAdapter adapter;

    private static final String[] ROLES = {"super_admin", "order_manager", "product_manager", "support_agent"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_accounts);

        toolbar = findViewById(R.id.toolbar);
        rvAdmins = findViewById(R.id.rvAdmins);
        fabAdd = findViewById(R.id.fabAdd);
        searchView = findViewById(R.id.searchView);
        emptyState = findViewById(R.id.emptyState);
        progress = findViewById(R.id.progress);

        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        adapter = new AdminAdapter(this::showEditDialog);
        rvAdmins.setLayoutManager(new LinearLayoutManager(this));
        rvAdmins.setAdapter(adapter);

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override public boolean onQueryTextSubmit(String query) { return false; }
            @Override public boolean onQueryTextChange(String newText) {
                filter(newText);
                return true;
            }
        });

        fabAdd.setOnClickListener(v -> showEditDialog(null));

        load();
    }

    private void load() {
        progress.setVisibility(View.VISIBLE);
        NetworkHelper.toLiveData(
                ServiceLocator.get().apiClient().authApi().listAdmins(),
                (Resource<AuthApi.AdminListResponse> r) -> {
                    progress.setVisibility(View.GONE);
                    if (r.isSuccess() && r.data != null) {
                        allAdmins.clear();
                        List<AuthApi.AdminUser> list = r.data.admins != null ? r.data.admins : r.data.items;
                        if (list != null) allAdmins.addAll(list);
                        filter(searchView.getQuery().toString());
                        emptyState.setVisibility(allAdmins.isEmpty() ? View.VISIBLE : View.GONE);
                    } else {
                        Toast.makeText(this, "Tải thất bại: " + (r.message != null ? r.message : ""), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void filter(String query) {
        List<AuthApi.AdminUser> filtered = new ArrayList<>();
        String q = query == null ? "" : query.toLowerCase();
        for (AuthApi.AdminUser a : allAdmins) {
            boolean match = TextUtils.isEmpty(q) ||
                    (a.email != null && a.email.toLowerCase().contains(q)) ||
                    (a.name != null && a.name.toLowerCase().contains(q));
            if (match) filtered.add(a);
        }
        adapter.setItems(filtered);
        emptyState.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void showEditDialog(@Nullable AuthApi.AdminUser admin) {
        View v = LayoutInflater.from(this).inflate(R.layout.dialog_admin_form, null, false);
        EditText etName = v.findViewById(R.id.etName);
        EditText etEmail = v.findViewById(R.id.etEmail);
        EditText etPassword = v.findViewById(R.id.etPassword);
        Spinner spinnerRole = v.findViewById(R.id.spinnerRole);
        EditText etPermissions = v.findViewById(R.id.etPermissions);

        ArrayAdapter<String> roleAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item,
                new String[]{"Super Admin", "Quản lý đơn hàng", "Quản lý sản phẩm", "Hỗ trợ"});
        roleAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerRole.setAdapter(roleAdapter);

        String title = "Tạo admin mới";
        if (admin != null) {
            title = "Cập nhật: " + (admin.name != null ? admin.name : admin.email);
            etName.setText(admin.name);
            etEmail.setText(admin.email);
            etEmail.setEnabled(false);
            int idx = Arrays.asList(ROLES).indexOf(admin.role);
            spinnerRole.setSelection(idx >= 0 ? idx : 0);
            if (admin.permissions != null) etPermissions.setText(TextUtils.join(", ", admin.permissions));
        }

        new AlertDialog.Builder(this)
                .setTitle(title)
                .setView(v)
                .setPositiveButton(admin == null ? "Tạo" : "Lưu", (d, w) -> {
                    AuthApi.AdminUser body = new AuthApi.AdminUser();
                    body.name = etName.getText().toString().trim();
                    body.email = etEmail.getText().toString().trim();
                    body.role = ROLES[spinnerRole.getSelectedItemPosition()];
                    String perms = etPermissions.getText().toString().trim();
                    if (!perms.isEmpty()) {
                        body.permissions = Arrays.asList(perms.split("\\s*,\\s*"));
                    }
                    if (admin == null) {
                        String pw = etPassword.getText().toString();
                        if (pw.length() < 6) {
                            Toast.makeText(this, "Mật khẩu tối thiểu 6 ký tự", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        submitCreate(body, pw);
                    } else {
                        body._id = admin._id != null ? admin._id : admin.id;
                        submitUpdate(body);
                    }
                })
                .setNegativeButton("Huỷ", null)
                .show();
    }

    private void submitCreate(AuthApi.AdminUser body, String password) {
        Call<AuthApi.MeResponse> call = ServiceLocator.get().apiClient().authApi().createAdmin(body);
        call.enqueue(new Callback<AuthApi.MeResponse>() {
            @Override public void onResponse(Call<AuthApi.MeResponse> c, Response<AuthApi.MeResponse> r) {
                if (r.isSuccessful()) {
                    Toast.makeText(AdminAccountsActivity.this, "Đã tạo", Toast.LENGTH_SHORT).show();
                    load();
                } else {
                    Toast.makeText(AdminAccountsActivity.this, "Lỗi: " + r.code(), Toast.LENGTH_SHORT).show();
                }
            }
            @Override public void onFailure(Call<AuthApi.MeResponse> c, Throwable t) {
                Toast.makeText(AdminAccountsActivity.this, "Lỗi: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void submitUpdate(AuthApi.AdminUser body) {
        String id = body._id != null ? body._id : body.id;
        if (id == null) return;
        NetworkHelper.toLiveData(
                ServiceLocator.get().apiClient().authApi().updateAdmin(id, body),
                (Resource<AuthApi.MeResponse> r) -> {
                    if (r.isSuccess()) {
                        Toast.makeText(this, "Đã cập nhật", Toast.LENGTH_SHORT).show();
                        load();
                    } else {
                        Toast.makeText(this, "Lỗi: " + (r.message != null ? r.message : ""), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    static class AdminAdapter extends RecyclerView.Adapter<AdminAdapter.VH> {
        interface OnEdit { void onEdit(AuthApi.AdminUser admin); }
        private final List<AuthApi.AdminUser> items = new ArrayList<>();
        private final OnEdit onEdit;

        AdminAdapter(OnEdit onEdit) { this.onEdit = onEdit; }

        void setItems(List<AuthApi.AdminUser> list) {
            items.clear();
            if (list != null) items.addAll(list);
            notifyDataSetChanged();
        }

        @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_admin, parent, false);
            return new VH(v);
        }

        @Override public void onBindViewHolder(@NonNull VH h, int pos) {
            AuthApi.AdminUser a = items.get(pos);
            h.name.setText(a.name != null ? a.name : "—");
            h.email.setText(a.email != null ? a.email : "—");
            h.role.setText(roleLabel(a.role));
            h.role.setBackgroundResource(R.drawable.bg_badge_neutral);
            h.initial.setText((a.name == null || a.name.isEmpty() ? "?" : a.name.substring(0, 1)).toUpperCase());
            h.itemView.setOnClickListener(v -> onEdit.onEdit(a));
        }

        private String roleLabel(String r) {
            if (r == null) return "";
            switch (r) {
                case "super_admin": return "Super Admin";
                case "order_manager": return "Quản lý đơn";
                case "product_manager": return "Quản lý SP";
                case "support_agent": return "Hỗ trợ";
                default: return r;
            }
        }

        @Override public int getItemCount() { return items.size(); }

        static class VH extends RecyclerView.ViewHolder {
            TextView name, email, role, initial;
            VH(View v) {
                super(v);
                name = v.findViewById(R.id.tvName);
                email = v.findViewById(R.id.tvEmail);
                role = v.findViewById(R.id.tvRole);
                initial = v.findViewById(R.id.tvInitial);
            }
        }
    }
}