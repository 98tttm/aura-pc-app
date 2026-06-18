package com.aura.pc.ui.profile;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.text.Editable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.NumberPicker;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.core.widget.NestedScrollView;

import com.example.aura_pc_app.R;
import com.example.aura_pc_app.data.api.TokenManager;
import com.example.aura_pc_app.data.api.UserProfileService;
import com.example.aura_pc_app.ui.home.HomeActivity;
import com.example.aura_pc_app.utils.AuthGate;
import com.example.aura_pc_app.utils.Constants;
import com.example.aura_pc_app.utils.LocaleManager;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.gson.reflect.TypeToken;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CompleteProfileActivity extends AppCompatActivity {

    public static final String EXTRA_EDIT_MODE = "com.aura.pc.ui.profile.EXTRA_EDIT_MODE";
    private static final int MIN_BIRTH_YEAR = 1900;
    private static final int REQUEST_PICK_AVATAR = 7104;
    private static final Gson GSON = new GsonBuilder().create();
    private static final Type USER_MAP_TYPE = new TypeToken<Map<String, Object>>() { }.getType();
    private static final Pattern FULL_NAME_PATTERN = Pattern.compile("^[\\p{L}\\p{M}\\s'.-]{2,100}$");

    private EditText fullNameInput;
    private EditText emailInput;
    private EditText phoneNumberInput;
    private ImageView avatarImage;
    private TextView avatarInitial;
    private View avatarPicker;
    private TextView dateInput;
    private TextView genderInput;
    private TextView fullNameError;
    private TextView emailError;
    private TextView phoneNumberError;
    private TextView dateError;
    private TextView errorBanner;
    private TextView submitButton;
    private TextView termsText;
    private ProgressBar submitProgress;
    private NestedScrollView scrollView;
    private View successOverlay;
    private View backButton;

    private String apiDateOfBirth;
    private String apiGender;
    private String selectedGenderLabel;
    private String selectedAvatarUri;
    private boolean retryMode;

    private final Calendar selectedDate = Calendar.getInstance();
    private final SimpleDateFormat displayDateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
    private final SimpleDateFormat apiDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleManager.wrap(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_complete_profile);

        configureSystemBars();
        selectedDate.setTime(Calendar.getInstance().getTime());
        selectedDate.set(Calendar.MILLISECOND, 0);

        bindViews();
        loadCurrentUserData();
        setupInteractions();
    }

    private void bindViews() {
        fullNameInput = findViewById(R.id.edtFullName);
        emailInput = findViewById(R.id.edtEmail);
        phoneNumberInput = findViewById(R.id.edtPhoneNumber);
        avatarImage = findViewById(R.id.editProfileAvatarImage);
        avatarInitial = findViewById(R.id.editProfileAvatarInitial);
        avatarPicker = findViewById(R.id.editProfileAvatarFrame);
        dateInput = findViewById(R.id.tvDateOfBirth);
        genderInput = findViewById(R.id.tvGender);
        fullNameError = findViewById(R.id.errorFullName);
        emailError = findViewById(R.id.errorEmail);
        phoneNumberError = findViewById(R.id.errorPhoneNumber);
        dateError = findViewById(R.id.errorDateOfBirth);
        errorBanner = findViewById(R.id.errorBanner);
        submitButton = findViewById(R.id.btnSubmitProfile);
        termsText = findViewById(R.id.tvTerms);
        submitProgress = findViewById(R.id.progressSubmit);
        scrollView = findViewById(R.id.scrollProfile);
        successOverlay = findViewById(R.id.successOverlay);
        backButton = findViewById(R.id.btnBackProfile);
    }

    private void setupInteractions() {
        backButton.setOnClickListener(v -> finish());
        if (avatarPicker != null) {
            avatarPicker.setOnClickListener(v -> openAvatarPicker());
        }
        dateInput.setOnClickListener(v -> {
            if (isFormEnabled()) showDatePicker();
        });
        genderInput.setOnClickListener(v -> {
            if (isFormEnabled()) showGenderPicker();
        });
        styleTermsText();
        updateSubmitButtonLabel(false);
        submitButton.setOnClickListener(v -> {
            if (!isFormEnabled()) return;
            if (validateForm()) submitProfile();
        });
    }

    @SuppressWarnings("unchecked")
    private void loadCurrentUserData() {
        Map<String, Object> user = readCurrentUser();
        String fullName = firstNonEmptyString(
                firstProfileString(user, "fullName", "full_name", "displayName", "name"),
                firstString(user, "fullName", "full_name", "displayName", "name", "username")
        );
        String email = firstString(user, "email");
        String phoneNumber = firstString(user, "phone", "phoneNumber", "phone_number", "mobile");
        String birthDate = firstProfileString(user, "dateOfBirth", "birthday", "dob", "birthDate", "birth_date");
        String gender = firstProfileString(user, "gender");
        String avatar = firstNonEmptyString(
                firstString(user, "avatar", "avatarUrl", "avatarURL", "photoUrl", "photoURL", "image", "profileImage", "profilePicture"),
                firstProfileString(user, "avatar", "avatarUrl", "avatarURL", "photoUrl", "photoURL", "image", "profileImage", "profilePicture")
        );

        fullNameInput.setText(fullName);
        emailInput.setText(email);
        phoneNumberInput.setText(phoneNumber);
        applyBirthDate(birthDate);
        applyGender(gender);
        bindEditAvatar(avatar, fullName);
    }

    private Map<String, Object> readCurrentUser() {
        String userJson = TokenManager.getInstance(this).getCurrentUserJson();
        if (TextUtils.isEmpty(userJson)) {
            return new HashMap<>();
        }
        try {
            Map<String, Object> user = GSON.fromJson(userJson, USER_MAP_TYPE);
            return user == null ? new HashMap<>() : user;
        } catch (RuntimeException ignored) {
            return new HashMap<>();
        }
    }

    private void applyBirthDate(String value) {
        if (TextUtils.isEmpty(value)) {
            return;
        }
        String normalized = value.trim();
        int timeIndex = normalized.indexOf('T');
        if (timeIndex > 0) {
            normalized = normalized.substring(0, timeIndex);
        }
        Calendar parsed = Calendar.getInstance();
        try {
            if (normalized.matches("^\\d{4}-\\d{2}-\\d{2}$")) {
                parsed.setTime(apiDateFormat.parse(normalized));
                apiDateOfBirth = normalized;
            } else {
                parsed.setTime(displayDateFormat.parse(normalized));
                apiDateOfBirth = apiDateFormat.format(parsed.getTime());
            }
            selectedDate.setTime(parsed.getTime());
            dateInput.setText(displayDateFormat.format(parsed.getTime()));
            dateInput.setTextColor(getColor(R.color.black));
        } catch (ParseException ignored) {
            dateInput.setText(value);
            dateInput.setTextColor(getColor(R.color.black));
            apiDateOfBirth = null;
        }
    }

    private void applyGender(String value) {
        if (TextUtils.isEmpty(value)) {
            return;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        if ("male".equals(normalized) || "m".equals(normalized) || "nam".equals(normalized)) {
            apiGender = "male";
            selectedGenderLabel = "Nam";
        } else if ("female".equals(normalized) || "f".equals(normalized) || "nu".equals(normalized) || "nữ".equals(normalized)) {
            apiGender = "female";
            selectedGenderLabel = "Nữ";
        } else {
            apiGender = value.trim();
            selectedGenderLabel = value.trim();
        }
        genderInput.setText(selectedGenderLabel);
        genderInput.setTextColor(getColor(R.color.black));
    }

    private void openAvatarPicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        startActivityForResult(intent, REQUEST_PICK_AVATAR);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQUEST_PICK_AVATAR || resultCode != RESULT_OK || data == null || data.getData() == null) {
            return;
        }
        Uri uri = data.getData();
        selectedAvatarUri = uri.toString();
        int flags = data.getFlags() & Intent.FLAG_GRANT_READ_URI_PERMISSION;
        if (flags != 0) {
            try {
                getContentResolver().takePersistableUriPermission(uri, flags);
            } catch (SecurityException ignored) {
                // Some pickers do not grant persistable access; keep the in-session preview.
            }
        }
        bindEditAvatar(selectedAvatarUri, fullNameInput.getText().toString());
    }

    private void bindEditAvatar(String avatar, String name) {
        if (avatarImage == null || avatarInitial == null) {
            return;
        }
        String safeName = TextUtils.isEmpty(name) ? fullNameInput.getText().toString() : name;
        avatarInitial.setText(initialFor(safeName));
        avatarImage.setVisibility(View.GONE);
        avatarInitial.setVisibility(View.VISIBLE);
        if (TextUtils.isEmpty(avatar)) {
            return;
        }
        if (avatar.startsWith("content://") || avatar.startsWith("file://")) {
            try (InputStream stream = getContentResolver().openInputStream(Uri.parse(avatar))) {
                Bitmap bitmap = BitmapFactory.decodeStream(stream);
                if (bitmap != null) {
                    avatarImage.setImageBitmap(bitmap);
                    avatarImage.setVisibility(View.VISIBLE);
                    avatarInitial.setVisibility(View.GONE);
                    selectedAvatarUri = avatar;
                }
            } catch (Exception ignored) {
                avatarImage.setVisibility(View.GONE);
                avatarInitial.setVisibility(View.VISIBLE);
            }
            return;
        }
        String imageUrl = absoluteAvatarUrl(avatar);
        if (TextUtils.isEmpty(imageUrl)) {
            return;
        }
        avatarImage.setTag(imageUrl);
        new Thread(() -> {
            try (InputStream stream = new URL(imageUrl).openStream()) {
                Bitmap bitmap = BitmapFactory.decodeStream(stream);
                runOnUiThread(() -> {
                    if (bitmap == null || avatarImage == null || avatarInitial == null) return;
                    Object latestTag = avatarImage.getTag();
                    if (!TextUtils.equals(imageUrl, latestTag == null ? "" : String.valueOf(latestTag))) return;
                    avatarImage.setImageBitmap(bitmap);
                    avatarImage.setVisibility(View.VISIBLE);
                    avatarInitial.setVisibility(View.GONE);
                });
            } catch (Exception ignored) {
                runOnUiThread(() -> {
                    if (avatarImage != null) avatarImage.setVisibility(View.GONE);
                    if (avatarInitial != null) avatarInitial.setVisibility(View.VISIBLE);
                });
            }
        }).start();
    }

    private String absoluteAvatarUrl(String avatar) {
        if (TextUtils.isEmpty(avatar)) {
            return "";
        }
        String trimmed = avatar.trim();
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")
                || trimmed.startsWith("content://") || trimmed.startsWith("file://")) {
            return trimmed;
        }
        String apiBase = Constants.BASE_URL;
        String hostBase = apiBase.endsWith("/api/")
                ? apiBase.substring(0, apiBase.length() - "/api/".length())
                : apiBase.replaceAll("/+$", "");
        return trimmed.startsWith("/") ? hostBase + trimmed : hostBase + "/" + trimmed;
    }

    private String initialFor(String name) {
        if (TextUtils.isEmpty(name)) {
            return getString(R.string.profile_default_initial);
        }
        String trimmed = name.trim();
        if (trimmed.isEmpty()) {
            return getString(R.string.profile_default_initial);
        }
        return trimmed.substring(0, 1).toUpperCase(Locale.ROOT);
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

    private void styleTermsText() {
        String fullText = getString(R.string.profile_edit_terms);
        String highlightedText = "Điều khoản & Chính sách";
        SpannableString spannable = new SpannableString(fullText);
        int start = fullText.indexOf(highlightedText);
        if (start >= 0) {
            int end = start + highlightedText.length();
            spannable.setSpan(
                    new ForegroundColorSpan(ContextCompat.getColor(this, R.color.premium_nav_active)),
                    start,
                    end,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            );
            spannable.setSpan(new StyleSpan(Typeface.BOLD), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        termsText.setText(spannable);
    }

    private boolean validateForm() {
        clearInlineErrors();
        boolean valid = true;
        View firstErrorView = null;

        String fullName = fullNameInput.getText().toString().trim();
        String email = emailInput.getText().toString().trim();
        String phoneNumber = phoneNumberInput.getText().toString().trim();

        if (TextUtils.isEmpty(fullName)) {
            showFieldError(fullNameInput, fullNameError, "Vui lòng nhập họ và tên");
            firstErrorView = fullNameInput;
            valid = false;
        } else if (!FULL_NAME_PATTERN.matcher(fullName).matches()) {
            showFieldError(fullNameInput, fullNameError, "Họ và tên không hợp lệ");
            firstErrorView = fullNameInput;
            valid = false;
        }

        if (!TextUtils.isEmpty(email) && !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showFieldError(emailInput, emailError, "Email không hợp lệ");
            if (firstErrorView == null) firstErrorView = emailInput;
            valid = false;
        }

        if (!TextUtils.isEmpty(phoneNumber) && !isValidPhoneNumber(phoneNumber)) {
            showFieldError(phoneNumberInput, phoneNumberError, "Số điện thoại không hợp lệ");
            if (firstErrorView == null) firstErrorView = phoneNumberInput;
            valid = false;
        }

        if (apiDateOfBirth != null) {
            Calendar today = Calendar.getInstance();
            if (selectedDate.after(today)) {
                dateInput.setBackgroundResource(R.drawable.bg_profile_input_error);
                dateInput.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_profile_error, 0);
                dateInput.setCompoundDrawablePadding(dpToPx(12));
                dateError.setText(getString(R.string.error_future_birthday));
                dateError.setVisibility(View.VISIBLE);
                if (firstErrorView == null) firstErrorView = dateInput;
                valid = false;
            }
        }

        if (!valid && firstErrorView != null) {
            View target = firstErrorView;
            target.requestFocus();
            scrollView.post(() -> scrollView.smoothScrollTo(0, Math.max(0, target.getTop() - 24)));
        }

        return valid;
    }

    private void clearInlineErrors() {
        errorBanner.setVisibility(View.GONE);
        fullNameInput.setBackgroundResource(R.drawable.bg_profile_input_state);
        emailInput.setBackgroundResource(R.drawable.bg_profile_input_state);
        phoneNumberInput.setBackgroundResource(R.drawable.bg_profile_input_state);
        dateInput.setBackgroundResource(R.drawable.bg_profile_input_state);
        clearErrorIcon(fullNameInput);
        clearErrorIcon(emailInput);
        clearErrorIcon(phoneNumberInput);
        dateInput.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_profile_calendar, 0);
        fullNameError.setVisibility(View.GONE);
        emailError.setVisibility(View.GONE);
        phoneNumberError.setVisibility(View.GONE);
        dateError.setVisibility(View.GONE);
    }

    private void showFieldError(EditText input, TextView errorView, String message) {
        input.setBackgroundResource(R.drawable.bg_profile_input_error);
        input.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_profile_error, 0);
        input.setCompoundDrawablePadding(dpToPx(12));
        errorView.setText(message);
        errorView.setVisibility(View.VISIBLE);
    }

    private void showDatePicker() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View sheet = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_date_picker, null);
        NumberPicker dayPicker = sheet.findViewById(R.id.pickerDay);
        NumberPicker monthPicker = sheet.findViewById(R.id.pickerMonth);
        NumberPicker yearPicker = sheet.findViewById(R.id.pickerYear);
        EditText manualDateInput = sheet.findViewById(R.id.edtManualBirthDate);
        TextView manualDateError = sheet.findViewById(R.id.tvManualDateError);

        Calendar workingDate = Calendar.getInstance();
        if (TextUtils.isEmpty(apiDateOfBirth)) {
            workingDate.set(2000, Calendar.JANUARY, 1, 0, 0, 0);
            workingDate.set(Calendar.MILLISECOND, 0);
        } else {
            workingDate.setTime(selectedDate.getTime());
        }
        Calendar today = Calendar.getInstance();

        configureNumberPicker(yearPicker, MIN_BIRTH_YEAR, today.get(Calendar.YEAR), workingDate.get(Calendar.YEAR), null, false);
        configureNumberPicker(monthPicker, 1, 12, workingDate.get(Calendar.MONTH) + 1, buildPaddedValues(1, 12), true);
        updateDayPicker(dayPicker, workingDate.get(Calendar.YEAR), workingDate.get(Calendar.MONTH), workingDate.get(Calendar.DAY_OF_MONTH));
        styleDatePickerNumber(dayPicker);
        styleDatePickerNumber(monthPicker);
        styleDatePickerNumber(yearPicker);
        manualDateInput.setText(formatPickerDate(dayPicker, monthPicker, yearPicker));
        final boolean[] syncingDateInput = {false};

        NumberPicker.OnValueChangeListener dateChangeListener = (picker, oldValue, newValue) -> {
            if (syncingDateInput[0]) return;
            int maxDay = getDaysInMonth(yearPicker.getValue(), monthPicker.getValue() - 1);
            if (dayPicker.getValue() > maxDay) {
                dayPicker.setValue(maxDay);
            }
            updateDayPicker(dayPicker, yearPicker.getValue(), monthPicker.getValue() - 1, dayPicker.getValue());
            styleDatePickerNumber(dayPicker);
            syncingDateInput[0] = true;
            manualDateInput.setText(formatPickerDate(dayPicker, monthPicker, yearPicker));
            manualDateInput.setSelection(manualDateInput.getText().length());
            syncingDateInput[0] = false;
            clearManualDateError(manualDateInput, manualDateError);
        };
        dayPicker.setOnValueChangedListener(dateChangeListener);
        monthPicker.setOnValueChangedListener(dateChangeListener);
        yearPicker.setOnValueChangedListener(dateChangeListener);

        manualDateInput.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) manualDateInput.selectAll();
        });
        manualDateInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
                if (syncingDateInput[0]) return;
                String value = editable.toString().trim();
                if (TextUtils.isEmpty(value) || value.length() < 10) {
                    clearManualDateError(manualDateInput, manualDateError);
                    return;
                }

                Calendar typedDate = parseManualBirthDate(value, today);
                if (typedDate == null) {
                    showManualDateError(manualDateInput, manualDateError, "Ngày sinh không hợp lệ. Vui lòng nhập dd/mm/yyyy");
                    return;
                }

                clearManualDateError(manualDateInput, manualDateError);
                syncingDateInput[0] = true;
                updatePickerFromDate(dayPicker, monthPicker, yearPicker, typedDate);
                syncingDateInput[0] = false;
            }
        });

        sheet.findViewById(R.id.btnCancelDate).setOnClickListener(v -> dialog.dismiss());
        sheet.findViewById(R.id.btnDoneDate).setOnClickListener(v -> {
            Calendar manualDate = parseManualBirthDate(manualDateInput.getText().toString().trim(), today);
            if (manualDate == null) {
                showManualDateError(manualDateInput, manualDateError, "Ngày sinh không hợp lệ. Vui lòng nhập dd/mm/yyyy");
                return;
            }

            Calendar pickedDate = Calendar.getInstance();
            pickedDate.set(manualDate.get(Calendar.YEAR), manualDate.get(Calendar.MONTH), manualDate.get(Calendar.DAY_OF_MONTH), 0, 0, 0);
            pickedDate.set(Calendar.MILLISECOND, 0);

            if (pickedDate.after(today)) {
                dateInput.setBackgroundResource(R.drawable.bg_profile_input_error);
                dateInput.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_profile_error, 0);
                dateInput.setCompoundDrawablePadding(dpToPx(12));
                dateError.setText(getString(R.string.error_future_birthday));
                dateError.setVisibility(View.VISIBLE);
                return;
            }

            selectedDate.setTime(pickedDate.getTime());
            apiDateOfBirth = apiDateFormat.format(pickedDate.getTime());
            dateInput.setText(displayDateFormat.format(pickedDate.getTime()));
            dateInput.setTextColor(getColor(R.color.black));
            dateInput.setBackgroundResource(R.drawable.bg_profile_input_state);
            dateInput.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_profile_calendar, 0);
            dateInput.setCompoundDrawablePadding(dpToPx(12));
            dateError.setVisibility(View.GONE);
            dialog.dismiss();
        });
        dialog.setContentView(sheet);
        dialog.setOnShowListener(dialogInterface -> {
            FrameLayout bottomSheet = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheet == null) return;
            BottomSheetBehavior<FrameLayout> behavior = BottomSheetBehavior.from(bottomSheet);
            behavior.setSkipCollapsed(true);
            behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        });
        dialog.show();
    }

    private void configureNumberPicker(NumberPicker picker, int min, int max, int value, String[] displayedValues, boolean wrap) {
        picker.setDisplayedValues(null);
        picker.setMinValue(min);
        picker.setMaxValue(max);
        picker.setWrapSelectorWheel(wrap);
        picker.setDisplayedValues(displayedValues);
        picker.setValue(Math.max(min, Math.min(value, max)));
    }

    private void updateDayPicker(NumberPicker dayPicker, int year, int month, int selectedDay) {
        int maxDay = getDaysInMonth(year, month);
        configureNumberPicker(dayPicker, 1, maxDay, selectedDay, buildPaddedValues(1, maxDay), true);
    }

    private void updatePickerFromDate(NumberPicker dayPicker, NumberPicker monthPicker, NumberPicker yearPicker, Calendar date) {
        yearPicker.setValue(date.get(Calendar.YEAR));
        monthPicker.setValue(date.get(Calendar.MONTH) + 1);
        updateDayPicker(dayPicker, date.get(Calendar.YEAR), date.get(Calendar.MONTH), date.get(Calendar.DAY_OF_MONTH));
        styleDatePickerNumber(dayPicker);
        styleDatePickerNumber(monthPicker);
        styleDatePickerNumber(yearPicker);
    }

    private String formatPickerDate(NumberPicker dayPicker, NumberPicker monthPicker, NumberPicker yearPicker) {
        return String.format(Locale.US, "%02d/%02d/%04d", dayPicker.getValue(), monthPicker.getValue(), yearPicker.getValue());
    }

    private Calendar parseManualBirthDate(String value, Calendar today) {
        if (TextUtils.isEmpty(value) || !value.matches("^\\d{2}/\\d{2}/\\d{4}$")) {
            return null;
        }

        SimpleDateFormat parser = new SimpleDateFormat("dd/MM/yyyy", Locale.US);
        parser.setLenient(false);
        try {
            Calendar parsed = Calendar.getInstance();
            parsed.setTime(parser.parse(value));
            parsed.set(Calendar.HOUR_OF_DAY, 0);
            parsed.set(Calendar.MINUTE, 0);
            parsed.set(Calendar.SECOND, 0);
            parsed.set(Calendar.MILLISECOND, 0);

            int year = parsed.get(Calendar.YEAR);
            if (year < MIN_BIRTH_YEAR || year > today.get(Calendar.YEAR)) {
                return null;
            }

            Calendar todayStart = Calendar.getInstance();
            todayStart.setTime(today.getTime());
            todayStart.set(Calendar.HOUR_OF_DAY, 0);
            todayStart.set(Calendar.MINUTE, 0);
            todayStart.set(Calendar.SECOND, 0);
            todayStart.set(Calendar.MILLISECOND, 0);
            return parsed.after(todayStart) ? null : parsed;
        } catch (ParseException e) {
            return null;
        }
    }

    private void showManualDateError(EditText input, TextView errorView, String message) {
        input.setBackgroundResource(R.drawable.bg_profile_input_error);
        errorView.setText(message);
        errorView.setVisibility(View.VISIBLE);
    }

    private void clearManualDateError(EditText input, TextView errorView) {
        input.setBackgroundResource(R.drawable.bg_profile_input_state);
        errorView.setVisibility(View.GONE);
    }

    private void styleDatePickerNumber(NumberPicker picker) {
        int activeColor = ContextCompat.getColor(this, R.color.premium_nav_active);
        for (int i = 0; i < picker.getChildCount(); i++) {
            View child = picker.getChildAt(i);
            if (child instanceof EditText) {
                EditText editText = (EditText) child;
                editText.setTextColor(activeColor);
                editText.setTextSize(24);
                editText.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
                editText.setGravity(android.view.Gravity.CENTER);
            }
        }
        picker.invalidate();
    }

    private int getDaysInMonth(int year, int month) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(year, month, 1);
        return calendar.getActualMaximum(Calendar.DAY_OF_MONTH);
    }

    private String[] buildPaddedValues(int min, int max) {
        String[] values = new String[max - min + 1];
        for (int value = min; value <= max; value++) {
            values[value - min] = String.format(Locale.US, "%02d", value);
        }
        return values;
    }

    private void showGenderPicker() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View sheet = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_gender_picker, null);

        final String[] pendingValue = {apiGender == null ? "male" : apiGender};
        final String[] pendingLabel = {selectedGenderLabel == null ? "Nam" : selectedGenderLabel};

        View[] rows = {
                sheet.findViewById(R.id.optionMale),
                sheet.findViewById(R.id.optionFemale),
                sheet.findViewById(R.id.optionOther),
                sheet.findViewById(R.id.optionPreferNotToSay)
        };
        TextView[] labels = {
                sheet.findViewById(R.id.labelMale),
                sheet.findViewById(R.id.labelFemale),
                sheet.findViewById(R.id.labelOther),
                sheet.findViewById(R.id.labelPreferNotToSay)
        };
        View[] checks = {
                sheet.findViewById(R.id.checkMale),
                sheet.findViewById(R.id.checkFemale),
                sheet.findViewById(R.id.checkOther),
                sheet.findViewById(R.id.checkPreferNotToSay)
        };
        String[] rowLabels = {"Nam", "Nữ", "Khác", "Không muốn trả lời"};
        String[] rowValues = {"male", "female", "other", "prefer_not_to_say"};

        updateGenderSheetSelection(rows, labels, checks, rowValues, pendingValue[0]);
        for (int i = 0; i < rows.length; i++) {
            final int index = i;
            rows[i].setOnClickListener(v -> {
                pendingValue[0] = rowValues[index];
                pendingLabel[0] = rowLabels[index];
                updateGenderSheetSelection(rows, labels, checks, rowValues, pendingValue[0]);
            });
        }

        sheet.findViewById(R.id.btnCancelGender).setOnClickListener(v -> dialog.dismiss());
        sheet.findViewById(R.id.btnDoneGender).setOnClickListener(v -> {
            apiGender = pendingValue[0];
            selectedGenderLabel = pendingLabel[0];
            genderInput.setText(selectedGenderLabel);
            genderInput.setTextColor(getColor(R.color.black));
            dialog.dismiss();
        });

        dialog.setContentView(sheet);
        dialog.setOnShowListener(dialogInterface -> {
            FrameLayout bottomSheet = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheet == null) return;
            BottomSheetBehavior<FrameLayout> behavior = BottomSheetBehavior.from(bottomSheet);
            behavior.setSkipCollapsed(true);
            behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        });

        dialog.show();
    }

    private void updateGenderSheetSelection(View[] rows, TextView[] labels, View[] checks, String[] values, String selectedValue) {
        for (int i = 0; i < rows.length; i++) {
            boolean selected = values[i].equals(selectedValue);
            rows[i].setBackgroundResource(selected ? R.drawable.bg_gender_option_selected : R.drawable.bg_gender_option_transparent);
            labels[i].setTextColor(getColor(selected ? R.color.premium_nav_active : R.color.text_primary_dark));
            labels[i].setTypeface(null, selected ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
            checks[i].setVisibility(selected ? View.VISIBLE : View.GONE);
        }
    }

    private void submitProfile() {
        setSubmitting(true);
        retryMode = false;

        String email = emailInput.getText().toString().trim();
        String phoneNumber = phoneNumberInput.getText().toString().trim();
        UserProfileService.ProfileUpdateRequest request = new UserProfileService.ProfileUpdateRequest(
                fullNameInput.getText().toString(),
                email,
                apiDateOfBirth,
                apiGender,
                TextUtils.isEmpty(phoneNumber) ? null : normalizePhoneNumber(phoneNumber),
                null
        );

        new UserProfileService(this).updateCurrentUserProfile(request).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (isProfileSaveSuccessful(response)) {
                    handleSuccess(response.body());
                } else {
                    handleApiError();
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                handleApiError();
            }
        });
    }

    private void setSubmitting(boolean submitting) {
        fullNameInput.setEnabled(!submitting);
        emailInput.setEnabled(!submitting);
        phoneNumberInput.setEnabled(!submitting);
        dateInput.setEnabled(!submitting);
        genderInput.setEnabled(!submitting);
        if (avatarPicker != null) avatarPicker.setEnabled(!submitting);
        submitButton.setEnabled(!submitting);
        float formAlpha = submitting ? 0.72f : 1f;
        fullNameInput.setAlpha(formAlpha);
        emailInput.setAlpha(formAlpha);
        phoneNumberInput.setAlpha(formAlpha);
        dateInput.setAlpha(formAlpha);
        genderInput.setAlpha(formAlpha);
        if (avatarPicker != null) avatarPicker.setAlpha(formAlpha);
        updateSubmitButtonLabel(submitting);
        submitProgress.setVisibility(submitting ? View.VISIBLE : View.GONE);
    }

    private void updateSubmitButtonLabel(boolean submitting) {
        if (submitting) {
            submitButton.setText(getString(R.string.label_saving));
            submitButton.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
            return;
        }

        submitButton.setText(retryMode ? "Thử lại" : getString(R.string.profile_edit_save_changes));
        submitButton.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
    }

    private boolean isFormEnabled() {
        return submitProgress.getVisibility() != View.VISIBLE;
    }

    private boolean isValidPhoneNumber(String phoneNumber) {
        String normalized = normalizePhoneNumber(phoneNumber);
        return normalized.matches("^0\\d{9}$") || normalized.matches("^\\+84\\d{9}$");
    }

    private String normalizePhoneNumber(String phoneNumber) {
        return phoneNumber == null ? "" : phoneNumber.trim().replaceAll("\\s+", "");
    }

    private void clearErrorIcon(EditText input) {
        input.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

    private void handleApiError() {
        getSharedPreferences(CheckingAccountActivity.PROFILE_PREFS, MODE_PRIVATE)
                .edit()
                .putBoolean(CheckingAccountActivity.KEY_PROFILE_COMPLETED, false)
                .apply();
        retryMode = true;
        setSubmitting(false);
        errorBanner.setVisibility(View.VISIBLE);
        updateSubmitButtonLabel(false);
    }

    private boolean isProfileSaveSuccessful(Response<Map<String, Object>> response) {
        if (!response.isSuccessful()) return false;
        Map<String, Object> body = response.body();
        if (body == null) return response.code() == 204;
        return !containsApiError(body);
    }

    @SuppressWarnings("unchecked")
    private boolean containsApiError(Map<String, Object> body) {
        Object success = body.get("success");
        if (Boolean.FALSE.equals(success)) return true;

        Object ok = body.get("ok");
        if (Boolean.FALSE.equals(ok)) return true;

        Object status = body.get("status");
        if (status instanceof String) {
            String normalizedStatus = ((String) status).trim().toLowerCase(Locale.US);
            if ("error".equals(normalizedStatus) ||
                    "failed".equals(normalizedStatus) ||
                    "fail".equals(normalizedStatus)) {
                return true;
            }
        }

        if (body.get("error") != null || body.get("errors") != null) {
            return true;
        }

        Object data = body.get("data");
        if (data instanceof Map) {
            return containsApiError((Map<String, Object>) data);
        }

        return false;
    }

    @SuppressWarnings("unchecked")
    private void handleSuccess(Map<String, Object> responseBody) {
        Map<String, Object> updatedUser = readCurrentUser();
        if (responseBody != null) {
            Object user = responseBody.get("user");
            if (user instanceof Map) {
                updatedUser = new HashMap<>((Map<String, Object>) user);
            }
        }
        mergeEditedUserData(updatedUser);
        TokenManager.getInstance(this).saveCurrentUserJson(GSON.toJson(updatedUser));
        getSharedPreferences(CheckingAccountActivity.PROFILE_PREFS, MODE_PRIVATE)
                .edit()
                .putBoolean(CheckingAccountActivity.KEY_PROFILE_COMPLETED, true)
                .apply();
        successOverlay.setVisibility(View.VISIBLE);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (getIntent().getBooleanExtra(EXTRA_EDIT_MODE, false)) {
                finish();
            } else {
                goHome();
            }
        }, 1100);
    }

    @SuppressWarnings("unchecked")
    private void mergeEditedUserData(Map<String, Object> user) {
        if (user == null) {
            return;
        }
        String fullName = fullNameInput.getText().toString().trim();
        String email = emailInput.getText().toString().trim();
        String phone = phoneNumberInput.getText().toString().trim();

        if (!TextUtils.isEmpty(email)) user.put("email", email);
        if (!TextUtils.isEmpty(phone)) {
            user.put("phoneNumber", normalizePhoneNumber(phone));
            user.put("phone", normalizePhoneNumber(phone));
        }
        Map<String, Object> profile;
        Object profileValue = user.get("profile");
        if (profileValue instanceof Map) {
            profile = new HashMap<>((Map<String, Object>) profileValue);
        } else {
            profile = new HashMap<>();
        }
        if (!TextUtils.isEmpty(fullName)) {
            profile.put("fullName", fullName);
            user.put("fullName", fullName);
        }
        if (!TextUtils.isEmpty(apiDateOfBirth)) {
            profile.put("dateOfBirth", apiDateOfBirth);
        }
        if (!TextUtils.isEmpty(apiGender)) {
            profile.put("gender", apiGender);
        }
        if (!TextUtils.isEmpty(selectedAvatarUri)) {
            user.put("avatar", selectedAvatarUri);
            user.put("avatarUrl", selectedAvatarUri);
            profile.put("avatar", selectedAvatarUri);
            profile.put("avatarUrl", selectedAvatarUri);
        }
        user.put("profile", profile);
    }

    private void goHome() {
        String redirectClassName = getIntent().getStringExtra(AuthGate.EXTRA_REDIRECT_CLASS_NAME);
        if (redirectClassName != null && !redirectClassName.isEmpty()) {
            try {
                Class<?> redirectClass = Class.forName(redirectClassName);
                Intent redirectIntent = new Intent(this, redirectClass);
                redirectIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(redirectIntent);
                return;
            } catch (ClassNotFoundException ignored) {
                // Fall back to Home if an old redirect target is no longer available.
            }
        }

        Intent intent = new Intent(this, HomeActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    private void configureSystemBars() {
        Window window = getWindow();
        window.setStatusBarColor(ContextCompat.getColor(this, R.color.premium_nav_active));
        window.setNavigationBarColor(ContextCompat.getColor(this, R.color.black));
        WindowInsetsControllerCompat controller =
                WindowCompat.getInsetsController(window, window.getDecorView());
        controller.setAppearanceLightStatusBars(false);
        controller.setAppearanceLightNavigationBars(false);
    }
}
