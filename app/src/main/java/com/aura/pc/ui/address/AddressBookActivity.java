package com.aura.pc.ui.address;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.aura_pc_app.R;
import com.example.aura_pc_app.utils.AuthGate;
import com.example.aura_pc_app.utils.LocaleManager;

import java.util.List;

/**
 * Màn "Quản lý sổ địa chỉ" — danh sách địa chỉ, thêm/sửa/xóa, đặt mặc định.
 * Tương đương tab "Sổ địa chỉ" của trang Tài khoản trên website.
 */
public class AddressBookActivity extends AppCompatActivity implements AddressAdapter.Listener {
    public static final String EXTRA_SELECT_MODE = "extra_select_mode";
    public static final String EXTRA_SELECTED_ADDRESS = "extra_selected_address";

    private AddressRepository repository;
    private AddressAdapter adapter;

    private RecyclerView recycler;
    private View emptyState;
    private ProgressBar progressBar;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleManager.wrap(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!AuthGate.requireLogin(this, AddressBookActivity.class)) {
            finish();
            return;
        }
        setContentView(R.layout.activity_address_book);

        repository = new AddressRepository(this);
        adapter = new AddressAdapter(this);

        recycler = findViewById(R.id.recyclerAddresses);
        emptyState = findViewById(R.id.emptyState);
        progressBar = findViewById(R.id.progressBar);

        recycler.setLayoutManager(new LinearLayoutManager(this));
        recycler.setAdapter(adapter);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnAddAddress).setOnClickListener(v ->
                startActivity(new Intent(this, AddressEditActivity.class)));
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadAddresses();
    }

    private void loadAddresses() {
        progressBar.setVisibility(View.VISIBLE);
        emptyState.setVisibility(View.GONE);
        repository.load(new AddressRepository.Callback2() {
            @Override
            public void onSuccess(List<Address> addresses) {
                progressBar.setVisibility(View.GONE);
                adapter.submit(addresses);
                boolean empty = addresses == null || addresses.isEmpty();
                emptyState.setVisibility(empty ? View.VISIBLE : View.GONE);
                recycler.setVisibility(empty ? View.GONE : View.VISIBLE);
            }

            @Override
            public void onError(String message) {
                progressBar.setVisibility(View.GONE);
                emptyState.setVisibility(View.VISIBLE);
                recycler.setVisibility(View.GONE);
                Toast.makeText(AddressBookActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ── Adapter callbacks ─────────────────────────────────

    @Override
    public void onSelect(Address address) {
        if (!getIntent().getBooleanExtra(EXTRA_SELECT_MODE, false)) {
            return;
        }
        Intent result = new Intent();
        result.putExtra(EXTRA_SELECTED_ADDRESS, address);
        setResult(RESULT_OK, result);
        finish();
    }

    @Override
    public void onEdit(Address address) {
        Intent i = new Intent(this, AddressEditActivity.class);
        i.putExtra(AddressEditActivity.EXTRA_ADDRESS, address);
        startActivity(i);
    }

    @Override
    public void onDelete(Address address) {
        showDeleteConfirm(address);
    }

    @Override
    public void onSetDefault(Address address) {
        if (address.id == null) return;
        progressBar.setVisibility(View.VISIBLE);
        repository.setDefault(address.id, new AddressRepository.Callback2() {
            @Override
            public void onSuccess(List<Address> addresses) {
                progressBar.setVisibility(View.GONE);
                adapter.submit(addresses);
                Toast.makeText(AddressBookActivity.this,
                        R.string.address_set_default_done, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(String message) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(AddressBookActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showDeleteConfirm(Address address) {
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_confirm_delete);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            dialog.getWindow().setLayout(
                    (int) (getResources().getDisplayMetrics().widthPixels * 0.85),
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        dialog.findViewById(R.id.btnCancel).setOnClickListener(v -> dialog.dismiss());
        dialog.findViewById(R.id.btnConfirm).setOnClickListener(v -> {
            dialog.dismiss();
            doDelete(address);
        });
        dialog.show();
    }

    private void doDelete(Address address) {
        if (address.id == null) return;
        progressBar.setVisibility(View.VISIBLE);
        repository.remove(address.id, new AddressRepository.Callback2() {
            @Override
            public void onSuccess(List<Address> addresses) {
                progressBar.setVisibility(View.GONE);
                adapter.submit(addresses);
                boolean empty = addresses == null || addresses.isEmpty();
                emptyState.setVisibility(empty ? View.VISIBLE : View.GONE);
                recycler.setVisibility(empty ? View.GONE : View.VISIBLE);
                Toast.makeText(AddressBookActivity.this,
                        R.string.address_deleted, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(String message) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(AddressBookActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
