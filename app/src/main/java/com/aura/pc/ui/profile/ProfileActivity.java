package com.aura.pc.ui.profile;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.aura.pc.ui.cart.CartActivity;
import com.aura.pc.utils.BottomNavigationHelper;
import com.example.aura_pc_app.R;
import com.example.aura_pc_app.data.api.ApiClient;
import com.example.aura_pc_app.data.api.TokenManager;
import com.example.aura_pc_app.ui.auth.AuthActivity;
import com.example.aura_pc_app.utils.AuthGate;
import com.example.aura_pc_app.utils.Constants;
import com.example.aura_pc_app.utils.LocaleManager;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.InputStream;
import java.lang.reflect.Type;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileActivity extends AppCompatActivity {
    private static final Gson GSON = new Gson();
    private static final Type USER_MAP_TYPE = new TypeToken<Map<String, Object>>() { }.getType();

    private TokenManager tokenManager;
    private View loggedInContent;
    private View loggedOutContent;
    private TextView nameText;
    private TextView memberText;
    private TextView idText;
    private TextView contactText;
    private TextView initialText;
    private ImageView avatarImage;
    private TextView userFieldCountText;
    private TextView userFieldKeysText;
    private TextView userFieldIdText;
    private TextView userFieldUsernameText;
    private TextView userFieldNameText;
    private TextView userFieldPhoneText;
    private TextView userFieldEmailText;
    private TextView userFieldAvatarText;
    private TextView userFieldActiveText;
    private TextView userFieldAddressesText;
    private TextView userFieldMembershipText;
    private TextView userFieldPremiumText;
    private TextView userFieldFollowersText;
    private TextView userFieldFollowingText;
    private TextView userFieldHubPostsText;
    private TextView userFieldHubRepostsText;
    private TextView userFieldProfileText;
    private TextView userFieldBirthDateText;
    private TextView userFieldGenderText;
    private TextView userFieldCreatedText;
    private TextView userFieldUpdatedText;
    private TextView userFieldLastLoginText;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleManager.wrap(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        tokenManager = TokenManager.getInstance(this);
        bindViews();
        setupActions();
        BottomNavigationHelper.setup(this, BottomNavigationHelper.TAB_PROFILE);
        renderProfileState();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (tokenManager != null) {
            renderProfileState();
        }
    }

    private void bindViews() {
        loggedInContent = findViewById(R.id.profileLoggedInContent);
        loggedOutContent = findViewById(R.id.profileLoggedOutContent);
        nameText = findViewById(R.id.profileNameText);
        memberText = findViewById(R.id.profileMemberText);
        idText = findViewById(R.id.profileIdText);
        contactText = findViewById(R.id.profileContactText);
        initialText = findViewById(R.id.profileInitialText);
        avatarImage = findViewById(R.id.profileAvatarImage);
        userFieldCountText = findViewById(R.id.profileUserFieldCountText);
        userFieldKeysText = findViewById(R.id.profileUserFieldKeysText);
        userFieldIdText = findViewById(R.id.profileUserFieldIdText);
        userFieldUsernameText = findViewById(R.id.profileUserFieldUsernameText);
        userFieldNameText = findViewById(R.id.profileUserFieldNameText);
        userFieldPhoneText = findViewById(R.id.profileUserFieldPhoneText);
        userFieldEmailText = findViewById(R.id.profileUserFieldEmailText);
        userFieldAvatarText = findViewById(R.id.profileUserFieldAvatarText);
        userFieldActiveText = findViewById(R.id.profileUserFieldActiveText);
        userFieldAddressesText = findViewById(R.id.profileUserFieldAddressesText);
        userFieldMembershipText = findViewById(R.id.profileUserFieldMembershipText);
        userFieldPremiumText = findViewById(R.id.profileUserFieldPremiumText);
        userFieldFollowersText = findViewById(R.id.profileUserFieldFollowersText);
        userFieldFollowingText = findViewById(R.id.profileUserFieldFollowingText);
        userFieldHubPostsText = findViewById(R.id.profileUserFieldHubPostsText);
        userFieldHubRepostsText = findViewById(R.id.profileUserFieldHubRepostsText);
        userFieldProfileText = findViewById(R.id.profileUserFieldProfileText);
        userFieldBirthDateText = findViewById(R.id.profileUserFieldBirthDateText);
        userFieldGenderText = findViewById(R.id.profileUserFieldGenderText);
        userFieldCreatedText = findViewById(R.id.profileUserFieldCreatedText);
        userFieldUpdatedText = findViewById(R.id.profileUserFieldUpdatedText);
        userFieldLastLoginText = findViewById(R.id.profileUserFieldLastLoginText);
    }

    private void setupActions() {
        View loginButton = findViewById(R.id.profileLoginButton);
        View notifications = findViewById(R.id.profileNotificationsButton);
        View cart = findViewById(R.id.profileCartButton);

        if (loginButton != null) {
            loginButton.setOnClickListener(v -> {
                Intent intent = new Intent(this, AuthActivity.class);
                intent.putExtra(AuthGate.EXTRA_REDIRECT_CLASS_NAME, ProfileActivity.class.getName());
                startActivity(intent);
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            });
        }
        if (notifications != null) {
            notifications.setOnClickListener(v ->
                    Toast.makeText(this, R.string.msg_notifications_pending, Toast.LENGTH_SHORT).show());
        }
        if (cart != null) {
            cart.setOnClickListener(v -> startActivity(new Intent(this, CartActivity.class)));
        }
    }

    private void renderProfileState() {
        if (!tokenManager.isLoggedIn()) {
            showLoggedOut();
            return;
        }

        showLoggedIn();
        Map<String, Object> cachedUser = readCachedUser();
        bindUser(cachedUser);
        refreshUserFromApi(cachedUser);
    }

    private void showLoggedOut() {
        if (loggedOutContent != null) loggedOutContent.setVisibility(View.VISIBLE);
        if (loggedInContent != null) loggedInContent.setVisibility(View.GONE);
    }

    private void showLoggedIn() {
        if (loggedOutContent != null) loggedOutContent.setVisibility(View.GONE);
        if (loggedInContent != null) loggedInContent.setVisibility(View.VISIBLE);
    }

    private Map<String, Object> readCachedUser() {
        String userJson = tokenManager.getCurrentUserJson();
        if (TextUtils.isEmpty(userJson)) {
            return new HashMap<>();
        }
        try {
            Map<String, Object> user = GSON.fromJson(userJson, USER_MAP_TYPE);
            return normalizeUserPhoneFields(user == null ? new HashMap<>() : user);
        } catch (RuntimeException ignored) {
            return new HashMap<>();
        }
    }

    private void refreshUserFromApi(Map<String, Object> cachedUser) {
        String cachedId = firstString(cachedUser, "_id", "id", "userId", "user_id");
        ApiClient.getInstance(this).getApiService().getCurrentUser().enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                Map<String, Object> user = extractUser(response.body());
                if (!user.isEmpty()) {
                    cacheAndBindUser(user);
                    return;
                }
                refreshUserById(cachedId);
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                refreshUserById(cachedId);
            }
        });
    }

    private void refreshUserById(String userId) {
        if (TextUtils.isEmpty(userId)) {
            return;
        }
        ApiClient.getInstance(this).getApiService().getUserById(userId).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                Map<String, Object> user = extractUser(response.body());
                if (!user.isEmpty()) {
                    cacheAndBindUser(user);
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                // Cached login user remains visible.
            }
        });
    }

    private void cacheAndBindUser(Map<String, Object> user) {
        Map<String, Object> normalizedUser = normalizeUserPhoneFields(user);
        tokenManager.saveCurrentUserJson(GSON.toJson(normalizedUser));
        bindUser(normalizedUser);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> extractUser(Map<String, Object> body) {
        if (body == null || body.isEmpty()) {
            return new HashMap<>();
        }
        Object user = body.get("user");
        if (user instanceof Map) {
            return (Map<String, Object>) user;
        }
        Object data = body.get("data");
        if (data instanceof Map) {
            Map<String, Object> dataMap = (Map<String, Object>) data;
            Object nestedUser = dataMap.get("user");
            if (nestedUser instanceof Map) {
                return (Map<String, Object>) nestedUser;
            }
            return dataMap;
        }
        return body;
    }

    private void bindUser(Map<String, Object> user) {
        String phone = normalizePhoneForDisplay(firstString(user, "phone", "phoneNumber", "phone_number", "mobile"));
        String email = firstString(user, "email");
        String avatar = avatarUrl(user);
        String name = normalizePhoneForDisplay(firstNonEmptyString(
                firstProfileString(user, "fullName", "full_name", "displayName", "name"),
                firstString(user, "fullName", "full_name", "displayName", "name", "username")
        ));
        if (TextUtils.isEmpty(name)) {
            name = !TextUtils.isEmpty(phone) ? phone : getString(R.string.profile_my_account);
        }

        String userId = firstString(user, "_id", "id", "userId", "user_id");
        String shortId = shortMemberId(userId, phone);
        String memberLabel = membershipLabel(user);
        String contact = !TextUtils.isEmpty(phone)
                ? phone
                : (!TextUtils.isEmpty(email) ? email : getString(R.string.profile_contact_missing));

        if (nameText != null) nameText.setText(name);
        if (memberText != null) memberText.setText(memberLabel);
        if (idText != null) idText.setText(getString(R.string.profile_member_id, shortId));
        if (contactText != null) contactText.setText(contact);
        if (initialText != null) initialText.setText(initialFor(name));
        bindAvatar(avatar, name);
        bindUserFields(user, userId, name, phone, email, avatar, memberLabel);
    }

    private void bindUserFields(Map<String, Object> user, String userId, String name, String phone,
                                String email, String avatar, String memberLabel) {
        if (userFieldCountText != null) {
            int count = user == null ? 0 : user.size();
            userFieldCountText.setText(getString(R.string.profile_user_fields_count, count));
        }
        if (userFieldKeysText != null) {
            userFieldKeysText.setText(getString(R.string.profile_user_fields_keys, userKeys(user)));
        }
        if (userFieldIdText != null) {
            userFieldIdText.setText(fieldLine(R.string.profile_user_field_id, userId));
        }
        if (userFieldUsernameText != null) {
            userFieldUsernameText.setText(fieldLine(R.string.profile_user_field_username, firstString(user, "username")));
        }
        if (userFieldNameText != null) {
            userFieldNameText.setText(fieldLine(R.string.profile_user_field_name, name));
        }
        if (userFieldPhoneText != null) {
            userFieldPhoneText.setText(fieldLine(R.string.profile_user_field_phone, phone));
        }
        if (userFieldEmailText != null) {
            String emailText = TextUtils.isEmpty(email) ? getString(R.string.profile_user_add_email) : email;
            userFieldEmailText.setText(fieldLine(R.string.profile_user_field_email, emailText));
        }
        if (userFieldAvatarText != null) {
            String avatarText = TextUtils.isEmpty(avatar)
                    ? ""
                    : getString(R.string.profile_user_avatar_synced);
            userFieldAvatarText.setText(fieldLine(R.string.profile_user_field_avatar, avatarText));
        }
        if (userFieldActiveText != null) {
            userFieldActiveText.setText(fieldLine(R.string.profile_user_field_active, activeText(user)));
        }
        if (userFieldAddressesText != null) {
            userFieldAddressesText.setText(fieldLine(R.string.profile_user_field_addresses, addressBookSummary(valueFor(user, "addresses"))));
        }
        if (userFieldMembershipText != null) {
            userFieldMembershipText.setText(fieldLine(R.string.profile_user_field_membership, memberLabel));
        }
        if (userFieldPremiumText != null) {
            Object premium = user == null ? null : user.get("premium");
            String premiumText = premium == null ? "" : String.valueOf(premium);
            userFieldPremiumText.setText(fieldLine(R.string.profile_user_field_premium, premiumText));
        }
        if (userFieldFollowersText != null) {
            userFieldFollowersText.setText(fieldLine(R.string.profile_user_field_followers, firstNonEmptySummary(user, "followerCount", "followers")));
        }
        if (userFieldFollowingText != null) {
            userFieldFollowingText.setText(fieldLine(R.string.profile_user_field_following, firstNonEmptySummary(user, "followingCount", "following")));
        }
        if (userFieldHubPostsText != null) {
            userFieldHubPostsText.setText(fieldLine(R.string.profile_user_field_hub_posts, summarizeValue(valueFor(user, "hubPosts"))));
        }
        if (userFieldHubRepostsText != null) {
            userFieldHubRepostsText.setText(fieldLine(R.string.profile_user_field_hub_reposts, summarizeValue(valueFor(user, "hubReposts"))));
        }
        if (userFieldProfileText != null) {
            userFieldProfileText.setText(fieldLine(R.string.profile_user_field_profile, profileSummary(user)));
        }
        if (userFieldBirthDateText != null) {
            userFieldBirthDateText.setText(fieldLine(R.string.profile_user_field_birth_date, formatIsoDate(firstProfileString(user, "dateOfBirth", "birthday", "dob"))));
        }
        if (userFieldGenderText != null) {
            userFieldGenderText.setText(fieldLine(R.string.profile_user_field_gender, genderText(firstProfileString(user, "gender"))));
        }
        if (userFieldCreatedText != null) {
            userFieldCreatedText.setText(fieldLine(R.string.profile_user_field_created, formatIsoDateTime(summarizeValue(valueFor(user, "createdAt")))));
        }
        if (userFieldUpdatedText != null) {
            userFieldUpdatedText.setText(fieldLine(R.string.profile_user_field_updated, formatIsoDateTime(summarizeValue(valueFor(user, "updatedAt")))));
        }
        if (userFieldLastLoginText != null) {
            userFieldLastLoginText.setText(fieldLine(R.string.profile_user_field_last_login, formatIsoDateTime(summarizeValue(valueFor(user, "lastLogin")))));
        }
    }

    private String membershipLabel(Map<String, Object> user) {
        String tier = firstString(user, "membership", "membershipTier", "tier", "rank", "role");
        if (!TextUtils.isEmpty(tier)) {
            String normalized = tier.toLowerCase(Locale.ROOT);
            if (normalized.contains("premium") || normalized.contains("vip")) {
                return getString(R.string.profile_member_premium);
            }
            return tier;
        }
        Object premium = user.get("premium");
        if (premium instanceof Boolean && (Boolean) premium) {
            return getString(R.string.profile_member_premium);
        }
        return getString(R.string.profile_member_standard);
    }

    @SuppressWarnings("unchecked")
    private String avatarUrl(Map<String, Object> user) {
        String avatar = firstString(
                user,
                "avatar",
                "avatarUrl",
                "avatarURL",
                "photoUrl",
                "photoURL",
                "image",
                "profileImage",
                "profilePicture"
        );
        if (!TextUtils.isEmpty(avatar)) {
            return avatar;
        }
        Object profile = user == null ? null : user.get("profile");
        if (profile instanceof Map) {
            return firstString(
                    (Map<String, Object>) profile,
                    "avatar",
                    "avatarUrl",
                    "avatarURL",
                    "photoUrl",
                    "photoURL",
                    "image",
                    "profileImage",
                    "profilePicture"
            );
        }
        return "";
    }

    private void bindAvatar(String avatar, String name) {
        if (avatarImage == null || initialText == null) {
            return;
        }
        String imageUrl = absoluteAvatarUrl(avatar);
        avatarImage.setTag(imageUrl);
        avatarImage.setVisibility(View.GONE);
        initialText.setVisibility(View.VISIBLE);
        if (TextUtils.isEmpty(imageUrl)) {
            return;
        }
        new Thread(() -> {
            try (InputStream stream = new URL(imageUrl).openStream()) {
                Bitmap bitmap = BitmapFactory.decodeStream(stream);
                runOnUiThread(() -> {
                    if (bitmap == null || avatarImage == null || initialText == null) {
                        return;
                    }
                    Object latestTag = avatarImage.getTag();
                    if (!TextUtils.equals(imageUrl, latestTag == null ? "" : String.valueOf(latestTag))) {
                        return;
                    }
                    avatarImage.setImageBitmap(bitmap);
                    avatarImage.setVisibility(View.VISIBLE);
                    initialText.setVisibility(View.GONE);
                });
            } catch (Exception ignored) {
                runOnUiThread(() -> {
                    if (avatarImage != null) {
                        avatarImage.setVisibility(View.GONE);
                    }
                    if (initialText != null) {
                        initialText.setVisibility(View.VISIBLE);
                        initialText.setText(initialFor(name));
                    }
                });
            }
        }).start();
    }

    private String absoluteAvatarUrl(String avatar) {
        if (TextUtils.isEmpty(avatar)) {
            return "";
        }
        String trimmed = avatar.trim();
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            return trimmed;
        }
        String apiBase = Constants.BASE_URL;
        String hostBase = apiBase.endsWith("/api/")
                ? apiBase.substring(0, apiBase.length() - "/api/".length())
                : apiBase.replaceAll("/+$", "");
        if (trimmed.startsWith("/")) {
            return hostBase + trimmed;
        }
        return hostBase + "/" + trimmed;
    }

    private String initialFor(String name) {
        if (TextUtils.isEmpty(name)) return getString(R.string.profile_default_initial);
        String trimmed = name.trim();
        if (trimmed.isEmpty()) return getString(R.string.profile_default_initial);
        return trimmed.substring(0, 1).toUpperCase(Locale.ROOT);
    }

    private String shortMemberId(String userId, String phone) {
        String source = !TextUtils.isEmpty(userId) ? userId : phone;
        if (TextUtils.isEmpty(source)) {
            return getString(R.string.profile_default_member_code);
        }
        String compact = source.replaceAll("[^A-Za-z0-9]", "").toUpperCase(Locale.ROOT);
        if (compact.length() > 6) {
            compact = compact.substring(compact.length() - 6);
        }
        return TextUtils.isEmpty(compact) ? getString(R.string.profile_default_member_code) : compact;
    }

    private String firstString(Map<String, Object> data, String... keys) {
        if (data == null) return "";
        for (String key : keys) {
            Object value = data.get(key);
            if (value == null) continue;
            String text = String.valueOf(value).trim();
            if (!text.isEmpty() && !"null".equalsIgnoreCase(text)) {
                return text;
            }
        }
        return "";
    }

    @SuppressWarnings("unchecked")
    private String firstProfileString(Map<String, Object> user, String... keys) {
        Object profile = user == null ? null : user.get("profile");
        if (profile instanceof Map) {
            return firstString((Map<String, Object>) profile, keys);
        }
        return "";
    }

    private String firstNonEmptyString(String... values) {
        for (String value : values) {
            if (!TextUtils.isEmpty(value)) {
                return value;
            }
        }
        return "";
    }

    private Object valueFor(Map<String, Object> data, String key) {
        return data == null ? null : data.get(key);
    }

    private String firstNonEmptySummary(Map<String, Object> data, String... keys) {
        if (data == null) return "";
        for (String key : keys) {
            String summary = summarizeValue(data.get(key));
            if (!TextUtils.isEmpty(summary)) {
                return summary;
            }
        }
        return "";
    }

    private String activeText(Map<String, Object> user) {
        Object active = valueFor(user, "active");
        if (active instanceof Boolean) {
            return getString((Boolean) active
                    ? R.string.profile_user_active_yes
                    : R.string.profile_user_active_no);
        }
        return summarizeValue(active);
    }

    @SuppressWarnings("unchecked")
    private String addressBookSummary(Object value) {
        if (!(value instanceof Collection)) {
            return summarizeValue(value);
        }
        Collection<?> addresses = (Collection<?>) value;
        if (addresses.isEmpty()) {
            return getString(R.string.profile_user_collection_count, 0);
        }
        Object first = addresses.iterator().next();
        if (!(first instanceof Map)) {
            return getString(R.string.profile_user_collection_count, addresses.size());
        }
        Map<String, Object> address = (Map<String, Object>) first;
        String label = firstString(address, "label", "name", "title");
        String ward = firstString(address, "ward");
        String district = firstString(address, "district");
        String city = firstString(address, "city", "province");
        StringBuilder preview = new StringBuilder();
        if (!TextUtils.isEmpty(label)) {
            preview.append(label);
        }
        appendAddressPart(preview, ward);
        appendAddressPart(preview, district);
        appendAddressPart(preview, city);
        if (preview.length() == 0) {
            return getString(R.string.profile_user_collection_count, addresses.size());
        }
        return getString(R.string.profile_user_addresses_summary, addresses.size(), preview.toString());
    }

    private void appendAddressPart(StringBuilder builder, String value) {
        if (TextUtils.isEmpty(value)) {
            return;
        }
        if (builder.length() > 0) {
            builder.append(", ");
        }
        builder.append(value);
    }

    private String profileSummary(Map<String, Object> user) {
        Object profile = valueFor(user, "profile");
        if (!(profile instanceof Map)) {
            return summarizeValue(profile);
        }
        String fullName = firstProfileString(user, "fullName", "full_name", "displayName", "name");
        String birthDate = formatIsoDate(firstProfileString(user, "dateOfBirth", "birthday", "dob"));
        String gender = genderText(firstProfileString(user, "gender"));
        StringBuilder summary = new StringBuilder();
        if (!TextUtils.isEmpty(fullName)) {
            summary.append(fullName);
        }
        appendAddressPart(summary, birthDate);
        appendAddressPart(summary, gender);
        return summary.length() == 0 ? getString(R.string.profile_user_empty_object) : summary.toString();
    }

    private String genderText(String gender) {
        if (TextUtils.isEmpty(gender)) {
            return "";
        }
        String normalized = gender.trim().toLowerCase(Locale.ROOT);
        if ("male".equals(normalized) || "m".equals(normalized) || "nam".equals(normalized)) {
            return getString(R.string.profile_user_gender_male);
        }
        if ("female".equals(normalized) || "f".equals(normalized) || "nu".equals(normalized) || "nữ".equals(normalized)) {
            return getString(R.string.profile_user_gender_female);
        }
        return gender.trim();
    }

    private String formatIsoDate(String value) {
        if (TextUtils.isEmpty(value) || value.length() < 10 || value.charAt(4) != '-' || value.charAt(7) != '-') {
            return value;
        }
        return value.substring(8, 10) + "/" + value.substring(5, 7) + "/" + value.substring(0, 4);
    }

    private String formatIsoDateTime(String value) {
        if (TextUtils.isEmpty(value) || value.length() < 16 || value.charAt(4) != '-' || value.charAt(7) != '-') {
            return value;
        }
        String date = formatIsoDate(value);
        int timeStart = value.indexOf('T');
        if (timeStart < 0 || value.length() < timeStart + 6) {
            return date;
        }
        return date + " " + value.substring(timeStart + 1, timeStart + 6);
    }

    private String summarizeValue(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof Number) {
            double number = ((Number) value).doubleValue();
            if (number == Math.rint(number)) {
                return String.valueOf((long) number);
            }
            return String.valueOf(number);
        }
        if (value instanceof Boolean) {
            return String.valueOf(value);
        }
        if (value instanceof Collection) {
            Collection<?> collection = (Collection<?>) value;
            if (collection.isEmpty()) {
                return getString(R.string.profile_user_collection_count, 0);
            }
            return getString(
                    R.string.profile_user_collection_with_preview,
                    collection.size(),
                    truncate(GSON.toJson(collection))
            );
        }
        if (value instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) value;
            if (map.isEmpty()) {
                return getString(R.string.profile_user_empty_object);
            }
            return truncate(GSON.toJson(map));
        }
        String text = String.valueOf(value).trim();
        if ("null".equalsIgnoreCase(text)) {
            return "";
        }
        return truncate(text);
    }

    private String truncate(String text) {
        if (TextUtils.isEmpty(text) || text.length() <= 180) {
            return text;
        }
        return text.substring(0, 177) + "...";
    }

    private String fieldLine(int labelResId, String value) {
        String safeValue = TextUtils.isEmpty(value) ? getString(R.string.profile_user_field_missing) : value;
        return getString(R.string.profile_user_field_line, getString(labelResId), safeValue);
    }

    private String userKeys(Map<String, Object> user) {
        if (user == null || user.isEmpty()) {
            return getString(R.string.profile_user_field_missing);
        }
        Set<String> keys = new TreeSet<>(user.keySet());
        return TextUtils.join(", ", keys);
    }

    private Map<String, Object> normalizeUserPhoneFields(Map<String, Object> user) {
        if (user == null || user.isEmpty()) {
            return new HashMap<>();
        }
        Map<String, Object> normalized = new HashMap<>(user);
        normalizePhoneField(normalized, "phone");
        normalizePhoneField(normalized, "phoneNumber");
        normalizePhoneField(normalized, "phone_number");
        normalizePhoneField(normalized, "mobile");
        normalizePhoneField(normalized, "username");
        normalizePhoneField(normalized, "fullName");
        normalizePhoneField(normalized, "full_name");
        normalizePhoneField(normalized, "displayName");
        normalizePhoneField(normalized, "name");
        return normalized;
    }

    private void normalizePhoneField(Map<String, Object> user, String key) {
        Object value = user.get(key);
        if (value == null) {
            return;
        }
        String normalized = normalizePhoneForDisplay(String.valueOf(value));
        if (!TextUtils.equals(String.valueOf(value), normalized)) {
            user.put(key, normalized);
        }
    }

    private String normalizePhoneForDisplay(String value) {
        if (TextUtils.isEmpty(value)) {
            return "";
        }
        String digits = value.replaceAll("[^0-9]", "");
        if (digits.matches("^0[0-9]{9}$")) {
            return digits;
        }
        if (digits.matches("^84[0-9]{9}$")) {
            return "0" + digits.substring(2);
        }
        if (digits.matches("^[1-9][0-9]{8}$")) {
            return "0" + digits;
        }
        return value.trim();
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
