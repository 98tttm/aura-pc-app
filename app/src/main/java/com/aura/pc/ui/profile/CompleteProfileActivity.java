package com.aura.pc.ui.profile;

import android.content.Intent;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.lang.reflect.Field;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CompleteProfileActivity extends AppCompatActivity {

    private static final int MIN_BIRTH_YEAR = 1900;
    private static final Gson GSON = new GsonBuilder().create();
    private static final Pattern FULL_NAME_PATTERN = Pattern.compile("^[\\p{L}\\p{M}\\s'.-]{2,100}$");

    private EditText fullNameInput;
    private EditText emailInput;
    private EditText phoneNumberInput;
    private EditText addressInput;
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
    private boolean retryMode;

    private final Calendar selectedDate = Calendar.getInstance();
    private final SimpleDateFormat displayDateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
    private final SimpleDateFormat apiDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_complete_profile);

        configureSystemBars();
        selectedDate.setTime(Calendar.getInstance().getTime());
        selectedDate.set(Calendar.MILLISECOND, 0);

        bindViews();
        setupInteractions();
    }

    private void bindViews() {
        fullNameInput = findViewById(R.id.edtFullName);
        emailInput = findViewById(R.id.edtEmail);
        phoneNumberInput = findViewById(R.id.edtPhoneNumber);
        addressInput = findViewById(R.id.edtAddress);
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

    private void styleTermsText() {
        String fullText = "Bằng cách tiếp tục, bạn đồng ý với Điều khoản & Chính sách của chúng tôi.";
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
                dateError.setText("Ngày sinh không được là ngày trong tương lai");
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
                dateError.setText("Ngày sinh không được là ngày trong tương lai");
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
        int inactiveColor = ContextCompat.getColor(this, R.color.premium_nav_inactive);
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

        try {
            Field selectorWheelPaintField = NumberPicker.class.getDeclaredField("mSelectorWheelPaint");
            selectorWheelPaintField.setAccessible(true);
            Paint paint = (Paint) selectorWheelPaintField.get(picker);
            if (paint != null) {
                paint.setColor(inactiveColor);
                paint.setTextSize(dpToPx(20));
                paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
            }
            picker.invalidate();
        } catch (ReflectiveOperationException ignored) {
            picker.invalidate();
        }
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
        String address = addressInput.getText().toString().trim();
        UserProfileService.ProfileUpdateRequest request = new UserProfileService.ProfileUpdateRequest(
                fullNameInput.getText().toString(),
                email,
                apiDateOfBirth,
                apiGender,
                TextUtils.isEmpty(phoneNumber) ? null : normalizePhoneNumber(phoneNumber),
                address
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
        addressInput.setEnabled(!submitting);
        dateInput.setEnabled(!submitting);
        genderInput.setEnabled(!submitting);
        submitButton.setEnabled(!submitting);
        float formAlpha = submitting ? 0.72f : 1f;
        fullNameInput.setAlpha(formAlpha);
        emailInput.setAlpha(formAlpha);
        phoneNumberInput.setAlpha(formAlpha);
        addressInput.setAlpha(formAlpha);
        dateInput.setAlpha(formAlpha);
        genderInput.setAlpha(formAlpha);
        updateSubmitButtonLabel(submitting);
        submitProgress.setVisibility(submitting ? View.VISIBLE : View.GONE);
    }

    private void updateSubmitButtonLabel(boolean submitting) {
        if (submitting) {
            submitButton.setText("Đang lưu...");
            submitButton.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
            return;
        }

        submitButton.setText(retryMode ? "Thử lại" : "Hoàn tất");
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
        if (responseBody != null) {
            Object user = responseBody.get("user");
            if (user instanceof Map) {
                TokenManager.getInstance(this).saveCurrentUserJson(GSON.toJson(user));
            }
        }
        getSharedPreferences(CheckingAccountActivity.PROFILE_PREFS, MODE_PRIVATE)
                .edit()
                .putBoolean(CheckingAccountActivity.KEY_PROFILE_COMPLETED, true)
                .apply();
        successOverlay.setVisibility(View.VISIBLE);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            goHome();
        }, 1100);
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
