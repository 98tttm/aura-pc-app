package com.aura.pc.ui.profile;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.aura.pc.utils.BottomNavigationHelper;
import com.example.aura_pc_app.R;
import com.example.aura_pc_app.data.api.ApiClient;
import com.example.aura_pc_app.data.api.TokenManager;
import com.example.aura_pc_app.utils.LocaleManager;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.Map;

public class ProfileActivity extends AppCompatActivity {

    private ImageView imgAvatar, btnBack, btnChangeAvatar;
    private TextView tvUserName, tvUserPhone, tvFullName, tvPhone, tvEmail, tvBirthday, tvGender, btnEditProfile;

    private final ActivityResultLauncher<Intent> editProfileLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    // Reload data after editing
                    loadProfileData();
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
        setContentView(R.layout.activity_profile);

        initViews();
        loadProfileData();

        BottomNavigationHelper.setup(this, BottomNavigationHelper.TAB_PROFILE);
    }

    private void initViews() {
        imgAvatar = findViewById(R.id.imgAvatar);
        btnBack = findViewById(R.id.btnBack);
        btnChangeAvatar = findViewById(R.id.btnChangeAvatar);
        tvUserName = findViewById(R.id.tvUserName);
        tvUserPhone = findViewById(R.id.tvUserPhone);
        tvFullName = findViewById(R.id.tvFullName);
        tvPhone = findViewById(R.id.tvPhone);
        tvEmail = findViewById(R.id.tvEmail);
        tvBirthday = findViewById(R.id.tvBirthday);
        tvGender = findViewById(R.id.tvGender);
        btnEditProfile = findViewById(R.id.btnEditProfile);

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        if (btnEditProfile != null) {
            btnEditProfile.setOnClickListener(v -> {
                Intent intent = new Intent(this, EditProfileActivity.class);
                editProfileLauncher.launch(intent);
            });
        }

        if (btnChangeAvatar != null) {
            btnChangeAvatar.setOnClickListener(v -> {
                Intent intent = new Intent(this, EditProfileActivity.class);
                editProfileLauncher.launch(intent);
            });
        }
    }

    private void loadProfileData() {
        TokenManager tokenManager = TokenManager.getInstance(this);
        String userJson = tokenManager.getCurrentUserJson();

        if (userJson != null && !userJson.isEmpty()) {
            Gson gson = new Gson();
            Type type = new TypeToken<Map<String, Object>>(){}.getType();
            Map<String, Object> user = gson.fromJson(userJson, type);

            if (user != null) {
                bindUserData(user);
            }
        }

        // Also try to fetch fresh data from API
        ApiClient.getInstance(this).getApiService().getMyProfile().enqueue(new retrofit2.Callback<Map<String, Object>>() {
            @Override
            public void onResponse(retrofit2.Call<Map<String, Object>> call, retrofit2.Response<Map<String, Object>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Map<String, Object> userData = response.body();
                    bindUserData(userData);
                    // Update local cache
                    tokenManager.saveCurrentUserJson(new Gson().toJson(userData));
                }
            }

            @Override
            public void onFailure(retrofit2.Call<Map<String, Object>> call, Throwable t) {
                // Use cached data, already loaded above
            }
        });
    }

    private void bindUserData(Map<String, Object> user) {
        String name = getStr(user, "name", "fullName", "full_name");
        String phone = getStr(user, "phone", "phoneNumber", "phone_number");
        String email = getStr(user, "email");
        String birthday = getStr(user, "birthday", "birthDate", "birth_date", "dob");
        String gender = getStr(user, "gender");
        String avatar = getStr(user, "avatar", "avatarUrl", "avatar_url", "profileImage");

        if (tvUserName != null) tvUserName.setText(name.isEmpty() ? "Chưa cập nhật" : name);
        if (tvUserPhone != null) tvUserPhone.setText(phone.isEmpty() ? "" : phone);
        if (tvFullName != null) tvFullName.setText(name.isEmpty() ? "Chưa cập nhật" : name);
        if (tvPhone != null) tvPhone.setText(phone.isEmpty() ? "Chưa cập nhật" : phone);
        if (tvEmail != null) tvEmail.setText(email.isEmpty() ? "Chưa cập nhật" : email);
        if (tvBirthday != null) tvBirthday.setText(birthday.isEmpty() ? "Chưa cập nhật" : birthday);

        if (tvGender != null) {
            if (gender.isEmpty()) {
                tvGender.setText("Chưa cập nhật");
            } else if (gender.equalsIgnoreCase("male") || gender.equalsIgnoreCase("nam")) {
                tvGender.setText("Nam");
            } else if (gender.equalsIgnoreCase("female") || gender.equalsIgnoreCase("nữ") || gender.equalsIgnoreCase("nu")) {
                tvGender.setText("Nữ");
            } else {
                tvGender.setText(gender);
            }
        }

        if (imgAvatar != null && !avatar.isEmpty()) {
            Glide.with(this)
                    .load(avatar)
                    .placeholder(R.drawable.ic_person)
                    .circleCrop()
                    .into(imgAvatar);
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
