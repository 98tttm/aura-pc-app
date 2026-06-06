package com.aura.pc.ui.address;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.aura_pc_app.R;
import com.example.aura_pc_app.utils.AuthGate;
import com.example.aura_pc_app.utils.LocaleManager;
import com.google.android.material.materialswitch.MaterialSwitch;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Màn Thêm / Sửa địa chỉ. Bao gồm validation, chọn Tỉnh→Quận→Phường theo tầng
 * (nguồn provinces.open-api.vn), công tắc đặt mặc định, và xóa khi đang sửa.
 */
public class AddressEditActivity extends AppCompatActivity {

    public static final String EXTRA_ADDRESS = "extra_address";

    // SĐT VN: 0xxxxxxxxx (10 số) hoặc 84xxxxxxxxx — khớp regex bên website.
    private static final Pattern PHONE_PATTERN = Pattern.compile("^(0\\d{9}|84\\d{9})$");

    private AddressRepository repository;
    private final LocationApi locationApi = LocationApi.Provider.get();

    private boolean editMode = false;
    private String editingId = null;

    // Views
    private Spinner spinnerLabel, spinnerCity, spinnerDistrict, spinnerWard;
    private EditText etFullName, etPhone, etAddress;
    private MaterialSwitch switchDefault;
    private TextView errFullName, errPhone, errCity, errDistrict, errWard, errAddress;

    // Location data
    private final List<VNLocation> provinces = new ArrayList<>();
    private final List<VNLocation> districts = new ArrayList<>();
    private final List<VNLocation> wards = new ArrayList<>();

    // Pending pre-selection (edit mode)
    private String pendingCity, pendingDistrict, pendingWard;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleManager.wrap(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!AuthGate.requireLogin(this, AddressEditActivity.class)) {
            finish();
            return;
        }
        setContentView(R.layout.activity_address_edit);
        repository = new AddressRepository(this);

        bindViews();
        setupLabelSpinner();
        setupLocationSpinners();

        Address editing = (Address) getIntent().getSerializableExtra(EXTRA_ADDRESS);
        if (editing != null) {
            enterEditMode(editing);
        }

        loadProvinces();

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnSave).setOnClickListener(v -> save());
        findViewById(R.id.btnDeleteAddress).setOnClickListener(v -> confirmDelete());
    }

    private void bindViews() {
        spinnerLabel = findViewById(R.id.spinnerLabel);
        spinnerCity = findViewById(R.id.spinnerCity);
        spinnerDistrict = findViewById(R.id.spinnerDistrict);
        spinnerWard = findViewById(R.id.spinnerWard);
        etFullName = findViewById(R.id.etFullName);
        etPhone = findViewById(R.id.etPhone);
        etAddress = findViewById(R.id.etAddress);
        switchDefault = findViewById(R.id.switchDefault);
        errFullName = findViewById(R.id.errFullName);
        errPhone = findViewById(R.id.errPhone);
        errCity = findViewById(R.id.errCity);
        errDistrict = findViewById(R.id.errDistrict);
        errWard = findViewById(R.id.errWard);
        errAddress = findViewById(R.id.errAddress);
    }

    private void setupLabelSpinner() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this, R.array.address_label_options, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerLabel.setAdapter(adapter);
    }

    private void setupLocationSpinners() {
        setLocationAdapter(spinnerCity, provinces);
        setLocationAdapter(spinnerDistrict, districts);
        setLocationAdapter(spinnerWard, wards);

        spinnerCity.setOnItemSelectedListener(new SimpleSelected() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                onCitySelected(position);
            }
        });
        spinnerDistrict.setOnItemSelectedListener(new SimpleSelected() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                onDistrictSelected(position);
            }
        });
    }

    private void setLocationAdapter(Spinner spinner, List<VNLocation> data) {
        ArrayAdapter<VNLocation> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, data);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
    }

    private void refresh(Spinner spinner) {
        ArrayAdapter<?> a = (ArrayAdapter<?>) spinner.getAdapter();
        if (a != null) a.notifyDataSetChanged();
    }

    // ── Edit mode ─────────────────────────────────────────
    private void enterEditMode(Address a) {
        editMode = true;
        editingId = a.id;
        ((TextView) findViewById(R.id.tvHeaderTitle)).setText(R.string.address_edit_title);
        ((TextView) findViewById(R.id.btnSave)).setText(R.string.address_save_update);
        findViewById(R.id.btnDeleteAddress).setVisibility(View.VISIBLE);

        // Pre-fill simple fields
        etFullName.setText(a.fullName);
        etPhone.setText(a.phone);
        etAddress.setText(a.address);
        switchDefault.setChecked(a.isDefault);
        selectLabel(a.label);

        // Remember location to pre-select once lists arrive
        pendingCity = a.city;
        pendingDistrict = a.district;
        pendingWard = a.ward;
    }

    private void selectLabel(String label) {
        if (label == null) return;
        ArrayAdapter adapter = (ArrayAdapter) spinnerLabel.getAdapter();
        for (int i = 0; i < adapter.getCount(); i++) {
            if (label.equals(String.valueOf(adapter.getItem(i)))) {
                spinnerLabel.setSelection(i);
                return;
            }
        }
    }

    // ── Provinces / cascade ───────────────────────────────
    private void loadProvinces() {
        provinces.clear();
        provinces.add(placeholder(getString(R.string.address_select_city)));
        refresh(spinnerCity);

        locationApi.getProvinces().enqueue(new Callback<List<VNLocation>>() {
            @Override
            public void onResponse(@NonNull Call<List<VNLocation>> call,
                                   @NonNull Response<List<VNLocation>> response) {
                if (response.body() != null) {
                    provinces.addAll(response.body());
                    refresh(spinnerCity);
                    if (pendingCity != null) {
                        int idx = indexOfName(provinces, pendingCity);
                        if (idx > 0) spinnerCity.setSelection(idx);
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<VNLocation>> call, @NonNull Throwable t) {
                Toast.makeText(AddressEditActivity.this,
                        R.string.address_location_error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void onCitySelected(int position) {
        // reset children
        districts.clear();
        districts.add(placeholder(getString(R.string.address_select_district)));
        wards.clear();
        wards.add(placeholder(getString(R.string.address_select_ward)));
        refresh(spinnerDistrict);
        refresh(spinnerWard);

        if (position <= 0 || position >= provinces.size()) return;
        VNLocation province = provinces.get(position);

        locationApi.getDistricts(province.code).enqueue(new Callback<VNLocation>() {
            @Override
            public void onResponse(@NonNull Call<VNLocation> call,
                                   @NonNull Response<VNLocation> response) {
                VNLocation body = response.body();
                if (body != null && body.districts != null) {
                    districts.addAll(body.districts);
                    refresh(spinnerDistrict);
                    if (pendingDistrict != null) {
                        int idx = indexOfName(districts, pendingDistrict);
                        pendingDistrict = null;
                        if (idx > 0) spinnerDistrict.setSelection(idx);
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<VNLocation> call, @NonNull Throwable t) { }
        });
    }

    private void onDistrictSelected(int position) {
        wards.clear();
        wards.add(placeholder(getString(R.string.address_select_ward)));
        refresh(spinnerWard);

        if (position <= 0 || position >= districts.size()) return;
        VNLocation district = districts.get(position);

        locationApi.getWards(district.code).enqueue(new Callback<VNLocation>() {
            @Override
            public void onResponse(@NonNull Call<VNLocation> call,
                                   @NonNull Response<VNLocation> response) {
                VNLocation body = response.body();
                if (body != null && body.wards != null) {
                    wards.addAll(body.wards);
                    refresh(spinnerWard);
                    if (pendingWard != null) {
                        int idx = indexOfName(wards, pendingWard);
                        pendingWard = null;
                        if (idx > 0) spinnerWard.setSelection(idx);
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<VNLocation> call, @NonNull Throwable t) { }
        });
    }

    // ── Save ──────────────────────────────────────────────
    private void save() {
        clearErrors();
        Address a = new Address();
        a.label = String.valueOf(spinnerLabel.getSelectedItem());
        a.fullName = etFullName.getText().toString().trim();
        a.phone = etPhone.getText().toString().trim();
        a.address = etAddress.getText().toString().trim();
        a.city = selectedName(spinnerCity, provinces);
        a.district = selectedName(spinnerDistrict, districts);
        a.ward = selectedName(spinnerWard, wards);
        a.isDefault = switchDefault.isChecked();

        if (!validate(a)) return;

        setSaving(true);
        AddressRepository.Callback2 cb = new AddressRepository.Callback2() {
            @Override
            public void onSuccess(List<Address> addresses) {
                setSaving(false);
                Toast.makeText(AddressEditActivity.this,
                        editMode ? R.string.address_updated : R.string.address_added,
                        Toast.LENGTH_SHORT).show();
                finish();
            }

            @Override
            public void onError(String message) {
                setSaving(false);
                Toast.makeText(AddressEditActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        };

        if (editMode && editingId != null) {
            repository.update(editingId, a, cb);
        } else {
            repository.add(a, cb);
        }
    }

    private boolean validate(Address a) {
        boolean ok = true;
        if (a.fullName.isEmpty()) { showError(errFullName, getString(R.string.address_err_fullname)); ok = false; }
        String phone = a.phone.replaceAll("\\s+", "");
        if (phone.isEmpty()) {
            showError(errPhone, getString(R.string.address_err_phone_empty)); ok = false;
        } else if (!PHONE_PATTERN.matcher(phone).matches()) {
            showError(errPhone, getString(R.string.address_err_phone_invalid)); ok = false;
        }
        if (a.city.isEmpty()) { showError(errCity, getString(R.string.address_err_city)); ok = false; }
        if (a.district.isEmpty()) { showError(errDistrict, getString(R.string.address_err_district)); ok = false; }
        if (a.ward.isEmpty()) { showError(errWard, getString(R.string.address_err_ward)); ok = false; }
        if (a.address.isEmpty()) { showError(errAddress, getString(R.string.address_err_detail)); ok = false; }
        return ok;
    }

    private void confirmDelete() {
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
            if (editingId == null) return;
            setSaving(true);
            repository.remove(editingId, new AddressRepository.Callback2() {
                @Override
                public void onSuccess(List<Address> addresses) {
                    setSaving(false);
                    Toast.makeText(AddressEditActivity.this,
                            R.string.address_deleted, Toast.LENGTH_SHORT).show();
                    finish();
                }

                @Override
                public void onError(String message) {
                    setSaving(false);
                    Toast.makeText(AddressEditActivity.this, message, Toast.LENGTH_SHORT).show();
                }
            });
        });
        dialog.show();
    }

    // ── Helpers ───────────────────────────────────────────
    private void setSaving(boolean saving) {
        TextView save = findViewById(R.id.btnSave);
        save.setEnabled(!saving);
        save.setAlpha(saving ? 0.6f : 1f);
    }

    private void clearErrors() {
        for (TextView t : new TextView[]{errFullName, errPhone, errCity, errDistrict, errWard, errAddress}) {
            t.setVisibility(View.GONE);
        }
    }

    private void showError(TextView view, String msg) {
        view.setText(msg);
        view.setVisibility(View.VISIBLE);
    }

    private VNLocation placeholder(String name) {
        VNLocation v = new VNLocation();
        v.name = name;
        v.code = 0;
        return v;
    }

    private int indexOfName(List<VNLocation> list, String name) {
        for (int i = 0; i < list.size(); i++) {
            if (name != null && name.equals(list.get(i).name)) return i;
        }
        return -1;
    }

    /** Lấy tên đơn vị đang chọn; trả "" nếu đang ở placeholder (code 0). */
    private String selectedName(Spinner spinner, List<VNLocation> data) {
        int pos = spinner.getSelectedItemPosition();
        if (pos <= 0 || pos >= data.size()) return "";
        VNLocation loc = data.get(pos);
        return loc.code == 0 ? "" : loc.name;
    }

    /** Listener tiện ích chỉ cần override onItemSelected. */
    private abstract static class SimpleSelected implements AdapterView.OnItemSelectedListener {
        @Override
        public void onNothingSelected(AdapterView<?> parent) { }
    }
}
