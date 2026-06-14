package com.aura.pc.ui.profile;

import android.Manifest;
import android.app.DatePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Patterns;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.example.aura_pc_app.R;
import com.example.aura_pc_app.data.api.ApiClient;
import com.example.aura_pc_app.data.api.TokenManager;
import com.example.aura_pc_app.utils.LocaleManager;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class EditProfileActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 100;

    private ImageView imgEditAvatar, btnBack, btnPickAvatar;
    private TextInputEditText edtFullName, edtPhone, edtEmail, edtBirthday;
    private TextInputLayout tilEmail;
    private RadioGroup rgGender;
    private RadioButton rbMale, rbFemale, rbOther;
    private TextView btnSaveProfile;

    private Uri selectedAvatarUri = null;

    // Launcher for gallery pick
    private final ActivityResultLauncher<Intent> galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    selectedAvatarUri = result.getData().getData();
                    if (imgEditAvatar != null && selectedAvatarUri != null) {
                        Glide.with(this)
                                .load(selectedAvatarUri)
                                .circleCrop()
                                .into(imgEditAvatar);
                    }
                }
            }
    );

    // Launcher for camera capture
    private final ActivityResultLauncher<Intent> cameraLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Bundle extras = result.getData().getExtras();
                    if (extras != null && extras.get("data") != null) {
                        android.graphics.Bitmap bitmap = (android.graphics.Bitmap) extras.get("data");
                        if (imgEditAvatar != null) {
                            imgEditAvatar.setImageBitmap(bitmap);
                        }
                    }
                }
            }
    );

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleManager.wrap(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        initViews();
        loadCurrentData();
        setupListeners();
    }

    private void initViews() {
        imgEditAvatar = findViewById(R.id.imgEditAvatar);
        btnBack = findViewById(R.id.btnBack);
        btnPickAvatar = findViewById(R.id.btnPickAvatar);
        edtFullName = findViewById(R.id.edtFullName);
        edtPhone = findViewById(R.id.edtPhone);
        edtEmail = findViewById(R.id.edtEmail);
        edtBirthday = findViewById(R.id.edtBirthday);
        tilEmail = findViewById(R.id.tilEmail);
        rgGender = findViewById(R.id.rgGender);
        rbMale = findViewById(R.id.rbMale);
        rbFemale = findViewById(R.id.rbFemale);
        rbOther = findViewById(R.id.rbOther);
        btnSaveProfile = findViewById(R.id.btnSaveProfile);
    }

    private void loadCurrentData() {
        TokenManager tokenManager = TokenManager.getInstance(this);
        String userJson = tokenManager.getCurrentUserJson();

        if (userJson == null || userJson.isEmpty()) return;

        Gson gson = new Gson();
        Type type = new TypeToken<Map<String, Object>>(){}.getType();
        Map<String, Object> user = gson.fromJson(userJson, type);

        if (user == null) return;

        String name = getStr(user, "name", "fullName", "full_name");
        String phone = getStr(user, "phone", "phoneNumber", "phone_number");
        String email = getStr(user, "email");
        String birthday = getStr(user, "birthday", "birthDate", "birth_date", "dob");
        String gender = getStr(user, "gender");
        String avatar = getStr(user, "avatar", "avatarUrl", "avatar_url", "profileImage");

        if (edtFullName != null) edtFullName.setText(name);
        if (edtPhone != null) edtPhone.setText(phone);
        if (edtEmail != null) edtEmail.setText(email);
        if (edtBirthday != null) edtBirthday.setText(birthday);

        if (gender.equalsIgnoreCase("male") || gender.equalsIgnoreCase("nam")) {
            if (rbMale != null) rbMale.setChecked(true);
        } else if (gender.equalsIgnoreCase("female") || gender.equalsIgnoreCase("nữ") || gender.equalsIgnoreCase("nu")) {
            if (rbFemale != null) rbFemale.setChecked(true);
        } else if (!gender.isEmpty()) {
            if (rbOther != null) rbOther.setChecked(true);
        }

        if (imgEditAvatar != null && !avatar.isEmpty()) {
            Glide.with(this)
                    .load(avatar)
                    .placeholder(R.drawable.ic_person)
                    .circleCrop()
                    .into(imgEditAvatar);
        }
    }

    private void setupListeners() {
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        if (btnPickAvatar != null) {
            btnPickAvatar.setOnClickListener(v -> showImagePickerDialog());
        }

        if (edtBirthday != null) {
            edtBirthday.setOnClickListener(v -> showDatePicker());
        }

        if (btnSaveProfile != null) {
            btnSaveProfile.setOnClickListener(v -> validateAndSave());
        }
    }

    private void showImagePickerDialog() {
        String[] options = {"Chọn từ Thư viện", "Chụp ảnh"};
        new AlertDialog.Builder(this)
                .setTitle("Chọn ảnh đại diện")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        openGallery();
                    } else {
                        openCamera();
                    }
                })
                .show();
    }

    private void openGallery() {
        // Check permission for newer Android versions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_MEDIA_IMAGES}, PERMISSION_REQUEST_CODE);
                return;
            }
        }

        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        galleryLauncher.launch(intent);
    }

    private void openCamera() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, PERMISSION_REQUEST_CODE);
            return;
        }

        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        cameraLauncher.launch(intent);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Quyền đã được cấp, vui lòng thử lại", Toast.LENGTH_SHORT).show();
        }
    }

    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR) - 20; // Default ~20 years old
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        // Try to parse existing birthday
        if (edtBirthday != null && edtBirthday.getText() != null) {
            String existing = edtBirthday.getText().toString().trim();
            if (!existing.isEmpty()) {
                try {
                    String[] parts = existing.split("/");
                    if (parts.length == 3) {
                        day = Integer.parseInt(parts[0]);
                        month = Integer.parseInt(parts[1]) - 1;
                        year = Integer.parseInt(parts[2]);
                    }
                } catch (Exception ignored) {}
            }
        }

        DatePickerDialog datePickerDialog = new DatePickerDialog(this, (view, y, m, d) -> {
            String dateStr = String.format("%02d/%02d/%04d", d, m + 1, y);
            if (edtBirthday != null) edtBirthday.setText(dateStr);
        }, year, month, day);

        datePickerDialog.show();
    }

    private void validateAndSave() {
        // Get values
        String fullName = edtFullName != null ? edtFullName.getText().toString().trim() : "";
        String email = edtEmail != null ? edtEmail.getText().toString().trim() : "";
        String birthday = edtBirthday != null ? edtBirthday.getText().toString().trim() : "";

        // Validate email
        if (!email.isEmpty() && !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            if (tilEmail != null) {
                tilEmail.setError("Định dạng email không hợp lệ");
            }
            return;
        } else {
            if (tilEmail != null) tilEmail.setError(null);
        }

        // Get gender
        String gender = "";
        if (rgGender != null) {
            int checkedId = rgGender.getCheckedRadioButtonId();
            if (checkedId == R.id.rbMale) {
                gender = "male";
            } else if (checkedId == R.id.rbFemale) {
                gender = "female";
            } else if (checkedId == R.id.rbOther) {
                gender = "other";
            }
        }

        // Build payload
        Map<String, Object> payload = new HashMap<>();
        if (!fullName.isEmpty()) payload.put("name", fullName);
        if (!email.isEmpty()) payload.put("email", email);
        if (!birthday.isEmpty()) payload.put("birthday", birthday);
        if (!gender.isEmpty()) payload.put("gender", gender);

        // Call API
        ApiClient.getInstance(this).getApiService().updateMyProfile(payload).enqueue(new retrofit2.Callback<Map<String, Object>>() {
            @Override
            public void onResponse(retrofit2.Call<Map<String, Object>> call, retrofit2.Response<Map<String, Object>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    // Update local cache with new data
                    TokenManager tokenManager = TokenManager.getInstance(EditProfileActivity.this);
                    tokenManager.saveCurrentUserJson(new Gson().toJson(response.body()));

                    Toast.makeText(EditProfileActivity.this, "Cập nhật thông tin thành công!", Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
                } else {
                    // API lỗi thì vẫn save local để UX tốt hơn
                    saveToLocalCache(payload);
                    Toast.makeText(EditProfileActivity.this, "Đã lưu thông tin cục bộ", Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
                }
            }

            @Override
            public void onFailure(retrofit2.Call<Map<String, Object>> call, Throwable t) {
                // Offline? Save locally anyway
                saveToLocalCache(payload);
                Toast.makeText(EditProfileActivity.this, "Đã lưu thông tin cục bộ", Toast.LENGTH_SHORT).show();
                setResult(RESULT_OK);
                finish();
            }
        });
    }

    private void saveToLocalCache(Map<String, Object> newData) {
        TokenManager tokenManager = TokenManager.getInstance(this);
        String existingJson = tokenManager.getCurrentUserJson();
        Gson gson = new Gson();

        Map<String, Object> user;
        if (existingJson != null && !existingJson.isEmpty()) {
            Type type = new TypeToken<Map<String, Object>>(){}.getType();
            user = gson.fromJson(existingJson, type);
        } else {
            user = new HashMap<>();
        }

        if (user != null) {
            user.putAll(newData);
            tokenManager.saveCurrentUserJson(gson.toJson(user));
        }
    }

    private String getStr(Map<String, Object> map, String... keys) {
        for (String key : keys) {
            Object val = map.get(key);
            if (val instanceof String && !((String) val).isEmpty()) {
                return (String) val;
            }
        }
        return "";
    }
}
