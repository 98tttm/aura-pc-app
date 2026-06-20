package com.aura.pc;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextWatcher;
import android.text.TextUtils;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.inputmethod.EditorInfo;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.widget.NestedScrollView;

import com.aura.pc.ui.address.Address;
import com.aura.pc.ui.address.AddressBookActivity;
import com.aura.pc.ui.address.AddressRepository;
import com.example.aura_pc_app.R;
import com.example.aura_pc_app.data.api.ApiClient;
import com.example.aura_pc_app.data.api.ApiService;
import com.example.aura_pc_app.data.api.TokenManager;
import com.example.aura_pc_app.data.checkout.CheckoutVoucherService;
import com.example.aura_pc_app.data.db.AppDatabase;
import com.example.aura_pc_app.data.db.entity.CartItemEntity;
import com.example.aura_pc_app.ui.home.HomeActivity;
import com.example.aura_pc_app.utils.AuthGate;
import com.example.aura_pc_app.utils.LocaleManager;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CheckoutActivity extends AppCompatActivity {
    private static final String TAG = "CheckoutActivity";
    public static final String EXTRA_SELECTED_CART_KEYS = "extra_selected_cart_keys";
    public static final String EXTRA_SELECTED_PRODUCT_IDS = "extra_selected_product_ids";
    public static final String EXTRA_VOUCHER_CODE = "extra_voucher_code";

    private static final int REQUEST_SELECT_ADDRESS = 19019;
    private static final int STEP_ADDRESS = 0;
    private static final int STEP_VOUCHER = 1;
    private static final int STEP_PAYMENT = 2;
    private static final Pattern PHONE_PATTERN = Pattern.compile("^(0\\d{9}|84\\d{9})$");
    private static final Pattern NAME_PATTERN = Pattern.compile("^[\\p{L}\\p{M}\\s'.-]{2,100}$");

    private NestedScrollView checkoutScroll;
    private View fixedCheckoutSummaryBar;
    private View fixedCheckoutTotalSection;
    private View orderTotalSection;
    private View btnConfirmOrder;
    private View checkoutStepper;
    private View stepAddressContent;
    private View stepVoucherContent;
    private View stepPaymentContent;
    private TextView stepAddress;
    private TextView stepVoucher;
    private TextView stepPayment;
    private TextView tvConfirmOrderText;
    private TextView tvCheckoutSubtotalLabel;
    private TextView tvCheckoutSubtotalPrice;
    private TextView tvCheckoutShippingPrice;
    private TextView tvCheckoutDiscountPrice;
    private TextView tvCheckoutTotalPrice;
    private TextView tvFixedCheckoutTotalPrice;
    private TextView btnChangeAddress;
    private TextView tvCheckoutAddressNamePhone;
    private TextView tvCheckoutAddressLine;
    private TextView tvCheckoutDefaultBadge;
    private TextView tvVoucherError;
    private TextView tvCheckoutSubmitError;
    private EditText etCheckoutVoucher;

    private AppDatabase database;
    private ApiService apiService;
    private TokenManager tokenManager;
    private AddressRepository addressRepository;
    private final CheckoutVoucherService voucherService = new CheckoutVoucherService();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Set<String> selectedCartKeys = new HashSet<>();
    private final Set<String> selectedProductIds = new HashSet<>();
    private final List<CartItemEntity> checkoutItems = new ArrayList<>();
    private final NumberFormat currencyFormatter = NumberFormat.getNumberInstance(new Locale("vi", "VN"));

    private Address selectedAddress;
    private double currentSubtotal;
    private double selectedShippingFee;
    private Integer selectedShippingMethodIndex;
    private double currentDiscount;
    private double currentTotal;
    private String voucherCode = "";
    private boolean voucherValid = true;
    private String selectedPaymentMethod;
    private int currentStep = STEP_ADDRESS;
    private boolean suppressNextAddressReload;
    private boolean submitting;
    private boolean cartLoaded;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleManager.wrap(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!AuthGate.requireLogin(this, CheckoutActivity.class)) {
            finish();
            return;
        }

        setContentView(R.layout.activity_checkout);
        database = AppDatabase.getInstance(this);
        ApiClient apiClient = ApiClient.getInstance(this);
        apiService = apiClient.getApiService();
        tokenManager = apiClient.getTokenManager();
        addressRepository = new AddressRepository(this);

        ArrayList<String> selectedKeys = getIntent().getStringArrayListExtra(EXTRA_SELECTED_CART_KEYS);
        if (selectedKeys != null) {
            for (String key : selectedKeys) {
                if (!TextUtils.isEmpty(key)) {
                    selectedCartKeys.add(key.trim());
                }
            }
        }
        ArrayList<String> productIds = getIntent().getStringArrayListExtra(EXTRA_SELECTED_PRODUCT_IDS);
        if (productIds != null) {
            for (String productId : productIds) {
                if (!TextUtils.isEmpty(productId)) {
                    selectedProductIds.add(productId.trim());
                }
            }
        }

        initViews();
        prefillVoucherFromIntent();
        setupShippingTexts();
        setupPaymentMethods();
        setupShippingMethods();
        setupVoucher();
        setupCheckoutSummaryBar();
        observeCartSummary();
        setupAddressActions();
        loadDefaultAddress();

        View back = findViewById(R.id.btnBack);
        if (back != null) {
            back.setOnClickListener(v -> finish());
        }

        if (btnConfirmOrder != null) {
            btnConfirmOrder.setOnClickListener(v -> handlePrimaryAction());
        }
        if (hasCheckoutStepper()) {
            selectStep(STEP_ADDRESS);
        } else {
            currentStep = STEP_PAYMENT;
            if (tvConfirmOrderText != null) {
                tvConfirmOrderText.setText("Xác nhận đơn hàng");
            }
            updateConfirmButtonState();
        }
    }

    private void prefillVoucherFromIntent() {
        if (etCheckoutVoucher == null) return;
        String cartVoucherCode = getIntent().getStringExtra(EXTRA_VOUCHER_CODE);
        if (!TextUtils.isEmpty(cartVoucherCode)) {
            etCheckoutVoucher.setText(cartVoucherCode);
            etCheckoutVoucher.setSelection(etCheckoutVoucher.getText().length());
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (suppressNextAddressReload) {
            suppressNextAddressReload = false;
            return;
        }
        if (addressRepository != null) {
            loadDefaultAddress();
        }
    }

    @Override
    protected void onDestroy() {
        executor.shutdown();
        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_SELECT_ADDRESS && resultCode == RESULT_OK && data != null) {
            Address address = (Address) data.getSerializableExtra(AddressBookActivity.EXTRA_SELECTED_ADDRESS);
            if (address != null) {
                suppressNextAddressReload = true;
                selectedAddress = address;
                renderCheckoutAddress(address);
                hideInlineError(tvCheckoutSubmitError);
                updateConfirmButtonState();
            }
        }
    }

    private void initViews() {
        checkoutScroll = findViewById(R.id.checkoutScroll);
        fixedCheckoutSummaryBar = findViewById(R.id.fixedCheckoutSummaryBar);
        fixedCheckoutTotalSection = findViewById(R.id.fixedCheckoutTotalSection);
        orderTotalSection = findViewById(R.id.orderTotalSection);
        btnConfirmOrder = findViewById(R.id.btnConfirmOrder);
        checkoutStepper = findViewById(R.id.checkoutStepper);
        stepAddressContent = findViewById(R.id.stepAddressContent);
        stepVoucherContent = findViewById(R.id.stepVoucherContent);
        stepPaymentContent = findViewById(R.id.stepPaymentContent);
        stepAddress = findViewById(R.id.stepAddress);
        stepVoucher = findViewById(R.id.stepVoucher);
        stepPayment = findViewById(R.id.stepPayment);
        tvConfirmOrderText = findViewById(R.id.tvConfirmOrderText);
        tvCheckoutSubtotalLabel = findViewById(R.id.tvCheckoutSubtotalLabel);
        tvCheckoutSubtotalPrice = findViewById(R.id.tvCheckoutSubtotalPrice);
        tvCheckoutShippingPrice = findViewById(R.id.tvCheckoutShippingPrice);
        tvCheckoutDiscountPrice = findViewById(R.id.tvCheckoutDiscountPrice);
        tvCheckoutTotalPrice = findViewById(R.id.tvCheckoutTotalPrice);
        tvFixedCheckoutTotalPrice = findViewById(R.id.tvFixedCheckoutTotalPrice);
        btnChangeAddress = findViewById(R.id.btnChangeAddress);
        tvCheckoutAddressNamePhone = findViewById(R.id.tvCheckoutAddressNamePhone);
        tvCheckoutAddressLine = findViewById(R.id.tvCheckoutAddressLine);
        tvCheckoutDefaultBadge = findViewById(R.id.tvCheckoutDefaultBadge);
        tvVoucherError = findViewById(R.id.tvVoucherError);
        tvCheckoutSubmitError = findViewById(R.id.tvCheckoutSubmitError);
        etCheckoutVoucher = findViewById(R.id.etCheckoutVoucher);
    }

    private void setupAddressActions() {
        View.OnClickListener openAddressBook = v -> {
            Intent intent = new Intent(this, AddressBookActivity.class);
            intent.putExtra(AddressBookActivity.EXTRA_SELECT_MODE, true);
            startActivityForResult(intent, REQUEST_SELECT_ADDRESS);
        };
        if (btnChangeAddress != null) {
            btnChangeAddress.setOnClickListener(openAddressBook);
        }
        View addressCard = findViewById(R.id.addressCard);
        if (addressCard != null) {
            addressCard.setOnClickListener(openAddressBook);
        }
    }

    private void setupVoucher() {
        View apply = findViewById(R.id.btnApplyVoucher);
        if (apply != null) {
            apply.setClickable(true);
            apply.setFocusable(true);
            apply.setOnClickListener(v -> applyVoucher(true));
        }
        if (etCheckoutVoucher != null) {
            etCheckoutVoucher.setOnClickListener(v -> focusVoucherInput());
            etCheckoutVoucher.setOnEditorActionListener((v, actionId, event) -> {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    applyVoucher(true);
                    return true;
                }
                return false;
            });
            etCheckoutVoucher.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    voucherCode = "";
                    voucherValid = true;
                    currentDiscount = 0;
                    hideInlineError(tvVoucherError);
                    updateOrderTotals();
                }

                @Override
                public void afterTextChanged(Editable s) {
                }
            });
            etCheckoutVoucher.setOnFocusChangeListener((v, hasFocus) -> {
                if (hasFocus) {
                    hideInlineError(tvVoucherError);
                    hideInlineError(tvCheckoutSubmitError);
                }
            });
        }
    }

    private void handlePrimaryAction() {
        hideInlineError(tvCheckoutSubmitError);
        if (!hasCheckoutStepper()) {
            submitOrder();
            return;
        }
        if (currentStep == STEP_ADDRESS) {
            if (checkoutItems.isEmpty()) {
                showInlineError(tvCheckoutSubmitError, "Giỏ hàng đang trống.");
                return;
            }
            if (!isAddressValid(selectedAddress)) {
                showInlineError(tvCheckoutSubmitError, "Vui lòng chọn hoặc thêm địa chỉ nhận hàng");
                scrollToView(findViewById(R.id.addressCard));
                return;
            }
            if (selectedShippingMethodIndex == null) {
                showInlineError(tvCheckoutSubmitError, "Vui lòng chọn phương thức giao hàng.");
                scrollToView(findViewById(R.id.shipping_method_1));
                return;
            }
            selectStep(STEP_VOUCHER);
            return;
        }
        if (currentStep == STEP_VOUCHER) {
            if (!applyVoucher(false)) {
                showInlineError(tvCheckoutSubmitError, "Vui lòng kiểm tra mã giảm giá.");
                scrollToView(etCheckoutVoucher);
                return;
            }
            selectStep(STEP_PAYMENT);
            return;
        }
        submitOrder();
    }

    private void selectStep(int step) {
        if (!hasCheckoutStepper()) {
            currentStep = STEP_PAYMENT;
            if (tvConfirmOrderText != null) {
                tvConfirmOrderText.setText("Xác nhận đơn hàng");
            }
            updateConfirmButtonState();
            return;
        }
        currentStep = step;
        setVisible(stepAddressContent, step == STEP_ADDRESS);
        setVisible(stepVoucherContent, step == STEP_VOUCHER);
        boolean voucherErrorVisible = tvVoucherError != null && tvVoucherError.getVisibility() == View.VISIBLE;
        setVisible(tvVoucherError, step == STEP_VOUCHER && voucherErrorVisible);
        setVisible(stepPaymentContent, step == STEP_PAYMENT);
        updateStepChip(stepAddress, step == STEP_ADDRESS);
        updateStepChip(stepVoucher, step == STEP_VOUCHER);
        updateStepChip(stepPayment, step == STEP_PAYMENT);
        if (tvConfirmOrderText != null) {
            tvConfirmOrderText.setText(step == STEP_PAYMENT ? "Xác nhận đơn hàng" : "Tiếp tục");
        }
        if (step == STEP_VOUCHER && etCheckoutVoucher != null) {
            etCheckoutVoucher.post(this::focusVoucherInput);
        }
        if (checkoutScroll != null) {
            checkoutScroll.post(() -> checkoutScroll.smoothScrollTo(0, 0));
        }
        updateConfirmButtonState();
    }

    private boolean hasCheckoutStepper() {
        return checkoutStepper != null && checkoutStepper.getVisibility() == View.VISIBLE;
    }

    private void focusVoucherInput() {
        if (etCheckoutVoucher == null) return;
        etCheckoutVoucher.requestFocus();
        etCheckoutVoucher.setSelection(etCheckoutVoucher.getText().length());
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.showSoftInput(etCheckoutVoucher, InputMethodManager.SHOW_IMPLICIT);
        }
    }

    private void updateStepChip(TextView view, boolean active) {
        if (view == null) return;
        view.setBackgroundResource(active ? R.drawable.bg_chip_active : R.drawable.bg_chip_default);
        view.setTextColor(getColor(active ? R.color.white : R.color.black));
    }

    private void setVisible(View view, boolean visible) {
        if (view != null) {
            view.setVisibility(visible ? View.VISIBLE : View.GONE);
        }
    }

    private void setupShippingTexts() {
        setBoldPrefix(R.id.tvShippingMethodFast,
                "Giao siêu tốc: trước 10 giờ ngày 15/06/2026",
                "Giao siêu tốc:");
        setBoldPrefix(R.id.tvShippingMethodNormal,
                "Giao thông thường: trước 12 giờ ngày 15/06/2026",
                "Giao thông thường:");
    }

    private void setBoldPrefix(int textViewId, String text, String prefix) {
        TextView textView = findViewById(textViewId);
        if (textView == null) return;
        SpannableString span = new SpannableString(text);
        span.setSpan(new StyleSpan(Typeface.BOLD), 0, prefix.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        textView.setText(span);
    }

    private void loadDefaultAddress() {
        if (addressRepository == null) return;
        addressRepository.load(new AddressRepository.Callback2() {
            @Override
            public void onSuccess(List<Address> addresses) {
                selectedAddress = pickCheckoutAddress(addresses);
                renderCheckoutAddress(selectedAddress);
            }

            @Override
            public void onError(String message) {
                selectedAddress = null;
                renderCheckoutAddress(null);
            }
        });
    }

    private Address pickCheckoutAddress(List<Address> addresses) {
        if (addresses == null || addresses.isEmpty()) return null;
        for (Address address : addresses) {
            if (address != null && address.isDefault) {
                return address;
            }
        }
        return addresses.get(0);
    }

    private void renderCheckoutAddress(Address address) {
        if (address == null) {
            if (tvCheckoutAddressNamePhone != null) {
                tvCheckoutAddressNamePhone.setText("Chưa có địa chỉ mặc định");
            }
            if (tvCheckoutAddressLine != null) {
                tvCheckoutAddressLine.setText("Chọn hoặc thêm địa chỉ nhận hàng để tiếp tục.");
            }
            if (tvCheckoutDefaultBadge != null) {
                tvCheckoutDefaultBadge.setVisibility(View.GONE);
            }
            updateConfirmButtonState();
            return;
        }

        if (tvCheckoutAddressNamePhone != null) {
            tvCheckoutAddressNamePhone.setText(address.fullName + " | " + address.phone);
        }
        if (tvCheckoutAddressLine != null) {
            tvCheckoutAddressLine.setText(address.formattedAddress());
        }
        if (tvCheckoutDefaultBadge != null) {
            tvCheckoutDefaultBadge.setVisibility(address.isDefault ? View.VISIBLE : View.GONE);
        }
        updateConfirmButtonState();
    }

    private void setupCheckoutSummaryBar() {
        if (checkoutScroll == null || fixedCheckoutSummaryBar == null || orderTotalSection == null) {
            return;
        }

        checkoutScroll.setOnScrollChangeListener((NestedScrollView.OnScrollChangeListener)
                (v, scrollX, scrollY, oldScrollX, oldScrollY) -> updateFixedBarVisibility());
        checkoutScroll.post(this::updateFixedBarVisibility);
    }

    private void observeCartSummary() {
        database.cartDao().getAllCartItemsLive().observe(this, this::renderCartSummary);
    }

    private void renderCartSummary(List<CartItemEntity> items) {
        int itemCount = 0;
        currentSubtotal = 0;
        checkoutItems.clear();
        cartLoaded = true;

        if (items != null) {
            for (CartItemEntity item : items) {
                if (!shouldUseCartItem(item)) {
                    continue;
                }
                checkoutItems.add(item);
                itemCount += Math.max(0, item.quantity);
                currentSubtotal += Math.max(0, item.unitPrice) * Math.max(0, item.quantity);
            }
        }

        if (tvCheckoutSubtotalLabel != null) {
            tvCheckoutSubtotalLabel.setText("Tạm tính (" + itemCount + " sản phẩm)");
        }
        if (tvCheckoutSubtotalPrice != null) {
            tvCheckoutSubtotalPrice.setText(formatPrice(currentSubtotal));
        }
        if (voucherValid) {
            applyVoucher(false);
        } else {
            updateOrderTotals();
        }
        updateConfirmButtonState();
    }

    private boolean shouldUseCartItem(CartItemEntity item) {
        if (item == null) return false;
        if (selectedCartKeys.isEmpty() && selectedProductIds.isEmpty()) {
            return true;
        }
        String key = cartKey(item);
        if (selectedCartKeys.contains(key)) {
            return true;
        }
        String productId = item.productId == null ? "" : item.productId.trim();
        return !TextUtils.isEmpty(productId) && selectedProductIds.contains(productId);
    }

    private boolean applyVoucher(boolean showToast) {
        String rawCode = etCheckoutVoucher == null ? "" : etCheckoutVoucher.getText().toString();
        CheckoutVoucherService.Result result = voucherService.validate(rawCode, currentSubtotal, selectedShippingFee);
        voucherCode = result.code;
        voucherValid = result.valid;
        currentDiscount = result.discount;

        if (!result.valid) {
            showVoucherMessage(result.message, true);
            updateOrderTotals();
            updateConfirmButtonState();
            return false;
        }

        if (TextUtils.isEmpty(voucherCode)) {
            hideInlineError(tvVoucherError);
        } else {
            showVoucherMessage("Đã áp dụng mã " + voucherCode + " (-" + formatPrice(currentDiscount) + ")", false);
        }
        updateOrderTotals();
        updateConfirmButtonState();
        if (showToast && !TextUtils.isEmpty(voucherCode)) {
            Toast.makeText(this, "Đã áp dụng mã giảm giá", Toast.LENGTH_SHORT).show();
        }
        return true;
    }

    private String formatPrice(double price) {
        return currencyFormatter.format(Math.max(0, price)) + "đ";
    }

    private String cartKey(CartItemEntity item) {
        String productId = item == null || item.productId == null ? "" : item.productId.trim();
        String variantId = item == null || item.variantId == null ? "" : item.variantId.trim();
        return productId + "\n" + variantId;
    }

    private String formatShippingPrice(double price) {
        return price <= 0 ? "Miễn phí" : formatPrice(price);
    }

    private void updateOrderTotals() {
        currentTotal = Math.max(0, currentSubtotal + selectedShippingFee - currentDiscount);
        String totalText = formatPrice(currentTotal);
        if (tvCheckoutShippingPrice != null) {
            tvCheckoutShippingPrice.setText(formatShippingPrice(selectedShippingFee));
        }
        if (tvCheckoutDiscountPrice != null) {
            tvCheckoutDiscountPrice.setText("-" + formatPrice(currentDiscount));
        }
        if (tvCheckoutTotalPrice != null) {
            tvCheckoutTotalPrice.setText(totalText);
        }
        if (tvFixedCheckoutTotalPrice != null) {
            tvFixedCheckoutTotalPrice.setText(totalText);
        }
        updateConfirmButtonState();
    }

    private void updateFixedBarVisibility() {
        if (checkoutScroll == null || fixedCheckoutSummaryBar == null
                || fixedCheckoutTotalSection == null || orderTotalSection == null) {
            return;
        }

        Rect scrollVisibleRect = new Rect();
        Rect totalVisibleRect = new Rect();
        boolean hasScrollRect = checkoutScroll.getGlobalVisibleRect(scrollVisibleRect);
        boolean hasTotalRect = orderTotalSection.getGlobalVisibleRect(totalVisibleRect);

        if (!hasScrollRect || !hasTotalRect) {
            fixedCheckoutTotalSection.setVisibility(View.VISIBLE);
            return;
        }

        int fixedBarHeight = fixedCheckoutSummaryBar.getHeight();
        if (fixedBarHeight > 0) {
            scrollVisibleRect.bottom = Math.max(scrollVisibleRect.top, scrollVisibleRect.bottom - fixedBarHeight);
        }

        boolean totalVisibleInContent = Rect.intersects(scrollVisibleRect, totalVisibleRect);
        fixedCheckoutTotalSection.setVisibility(totalVisibleInContent ? View.GONE : View.VISIBLE);
    }

    private void setupShippingMethods() {
        View option1 = findViewById(R.id.shipping_method_1);
        View option2 = findViewById(R.id.shipping_method_2);

        if (option1 != null) {
            option1.setOnClickListener(v -> selectShippingMethod(1));
        }
        if (option2 != null) {
            option2.setOnClickListener(v -> selectShippingMethod(2));
        }
        selectShippingMethod(2);
    }

    private void selectShippingMethod(int index) {
        selectedShippingMethodIndex = index;
        selectedShippingFee = index == 1 ? 80000 : 0;
        updateShippingSelection(findViewById(R.id.shipping_method_1), R.id.ivShippingRadio1, index == 1);
        updateShippingSelection(findViewById(R.id.shipping_method_2), R.id.ivShippingRadio2, index == 2);
        applyVoucher(false);
    }

    private void updateShippingSelection(View option, int radioId, boolean selected) {
        if (option == null) return;
        option.setSelected(selected);
        View marker = option.findViewById(radioId);
        if (marker != null) {
            marker.setVisibility(selected ? View.VISIBLE : View.GONE);
        }
    }

    private void setupPaymentMethods() {
        View card1 = findViewById(R.id.payment_method_1);
        View card2 = findViewById(R.id.payment_method_2);
        View card3 = findViewById(R.id.payment_method_3);
        View card4 = findViewById(R.id.payment_method_4);

        if (card1 != null) {
            setupCard(card1, "ATM / Thẻ ngân hàng", null, R.drawable.ic_checkout_card_bold, false);
            card1.setOnClickListener(v -> selectPaymentMethod(1));
        }
        if (card2 != null) {
            setupCard(card2, "Thanh toán bằng MoMo", null, R.drawable.ic_momo_checkout, false);
            card2.setOnClickListener(v -> selectPaymentMethod(2));
        }
        if (card3 != null) {
            setupCard(card3, "Thanh toán bằng ZaloPay", null, R.drawable.ic_zalopay_checkout, false);
            card3.setOnClickListener(v -> selectPaymentMethod(3));
        }
        if (card4 != null) {
            setupCard(card4, "Thanh toán khi nhận hàng (COD)", null, R.drawable.ic_checkout_cod, false);
            card4.setOnClickListener(v -> selectPaymentMethod(4));
        }
    }

    private void setupCard(View view, String title, String subtitle, int iconRes, boolean isSelected) {
        TextView tvTitle = view.findViewById(R.id.tv_payment_title);
        TextView tvSubtitle = view.findViewById(R.id.tv_payment_subtitle);
        ImageView ivIcon = view.findViewById(R.id.iv_payment_icon);
        ImageView ivRadio = view.findViewById(R.id.iv_radio);

        tvTitle.setText(title);
        if (subtitle == null || subtitle.trim().isEmpty()) {
            tvSubtitle.setVisibility(View.GONE);
        } else {
            tvSubtitle.setVisibility(View.VISIBLE);
            tvSubtitle.setText(subtitle);
        }
        ivIcon.setImageResource(iconRes);

        view.setSelected(isSelected);
        ivRadio.setImageResource(isSelected
                ? R.drawable.bg_checkout_radio_selected
                : R.drawable.bg_checkout_radio_unselected);
    }

    private void selectPaymentMethod(int index) {
        selectedPaymentMethod = paymentMethodForIndex(index);
        hideInlineError(tvCheckoutSubmitError);
        updateCardSelection(findViewById(R.id.payment_method_1), index == 1);
        updateCardSelection(findViewById(R.id.payment_method_2), index == 2);
        updateCardSelection(findViewById(R.id.payment_method_3), index == 3);
        updateCardSelection(findViewById(R.id.payment_method_4), index == 4);
        updateConfirmButtonState();
    }

    private String paymentMethodForIndex(int index) {
        if (index == 2) return "qr_momo";
        if (index == 3) return "zalopay";
        if (index == 4) return "cod";
        return "atm";
    }

    private void updateCardSelection(View view, boolean isSelected) {
        if (view == null) return;
        view.setSelected(isSelected);
        ImageView ivRadio = view.findViewById(R.id.iv_radio);
        ivRadio.setImageResource(isSelected
                ? R.drawable.bg_checkout_radio_selected
                : R.drawable.bg_checkout_radio_unselected);
    }

    private void submitOrder() {
        if (submitting) return;
        hideInlineError(tvCheckoutSubmitError);

        String validationError = validateBeforeSubmit();
        if (validationError != null) {
            showInlineError(tvCheckoutSubmitError, validationError);
            return;
        }

        submitting = true;
        updateConfirmButtonState();
        apiService.createOrder(buildOrderPayload()).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(@NonNull Call<Map<String, Object>> call,
                                   @NonNull Response<Map<String, Object>> response) {
                submitting = false;
                updateConfirmButtonState();
                if (response.isSuccessful()) {
                    clearSubmittedCartItems();
                    showOrderSuccess(extractOrderId(response.body()));
                    return;
                }
                showSubmitFailure(buildApiErrorMessage(response));
            }

            @Override
            public void onFailure(@NonNull Call<Map<String, Object>> call, @NonNull Throwable t) {
                submitting = false;
                updateConfirmButtonState();
                showSubmitFailure("Không thể kết nối máy chủ. Vui lòng thử lại.");
            }
        });
    }

    private String validateBeforeSubmit() {
        if (cartLoaded && checkoutItems.isEmpty()) {
            selectStep(STEP_ADDRESS);
            return "Giỏ hàng đang trống.";
        }
        if (!areOrderItemsValid()) {
            return "Sản phẩm trong giỏ hàng không hợp lệ. Vui lòng tải lại giỏ hàng.";
        }
        if (!isAddressValid(selectedAddress)) {
            selectStep(STEP_ADDRESS);
            scrollToView(findViewById(R.id.addressCard));
            return "Vui lòng chọn hoặc thêm địa chỉ nhận hàng";
        }
        if (selectedShippingMethodIndex == null) {
            selectStep(STEP_ADDRESS);
            scrollToView(findViewById(R.id.shipping_method_1));
            return "Vui lòng chọn phương thức giao hàng.";
        }
        if (!applyVoucher(false)) {
            selectStep(STEP_VOUCHER);
            scrollToView(etCheckoutVoucher);
            return "Vui lòng kiểm tra mã giảm giá.";
        }
        if (TextUtils.isEmpty(selectedPaymentMethod)) {
            selectStep(STEP_PAYMENT);
            scrollToView(findViewById(R.id.payment_method_1));
            return "Vui lòng chọn phương thức thanh toán.";
        }
        if (currentTotal < 0) {
            return "Tổng tiền không hợp lệ.";
        }
        if (tokenManager == null || !tokenManager.isLoggedIn()) {
            return "Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.";
        }
        return null;
    }

    private boolean areOrderItemsValid() {
        if (checkoutItems.isEmpty()) {
            return false;
        }
        for (CartItemEntity item : checkoutItems) {
            String productId = resolveOrderProductId(item);
            if (!isValidProductId(productId) || item.quantity <= 0 || item.unitPrice <= 0) {
                return false;
            }
        }
        return true;
    }

    private String resolveOrderProductId(CartItemEntity item) {
        if (item == null || item.productId == null) {
            return "";
        }
        return item.productId.trim();
    }

    private boolean isValidProductId(String productId) {
        if (TextUtils.isEmpty(productId)) {
            return false;
        }
        String normalized = productId.trim();
        return !"undefined".equalsIgnoreCase(normalized)
                && !"null".equalsIgnoreCase(normalized)
                && !normalized.startsWith("fallback-");
    }

    private String buildApiErrorMessage(Response<Map<String, Object>> response) {
        String fallback = "Không thể tạo đơn hàng. Mã lỗi: " + response.code();
        ResponseBody errorBody = response.errorBody();
        if (errorBody == null) {
            return fallback;
        }
        try {
            String rawError = errorBody.string();
            if (TextUtils.isEmpty(rawError)) {
                return fallback;
            }
            com.google.gson.JsonElement parsed = com.google.gson.JsonParser.parseString(rawError);
            if (parsed != null && parsed.isJsonObject()) {
                com.google.gson.JsonObject object = parsed.getAsJsonObject();
                String backendMessage = firstJsonString(object, "message", "error", "detail");
                if (!TextUtils.isEmpty(backendMessage)) {
                    return backendMessage;
                }
            }
            return fallback + ": " + rawError;
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private String firstJsonString(com.google.gson.JsonObject object, String first, String second, String third) {
        String value = jsonString(object, first);
        if (!TextUtils.isEmpty(value)) return value;
        value = jsonString(object, second);
        if (!TextUtils.isEmpty(value)) return value;
        return jsonString(object, third);
    }

    private String jsonString(com.google.gson.JsonObject object, String key) {
        if (!object.has(key) || object.get(key).isJsonNull()) {
            return null;
        }
        return object.get(key).isJsonPrimitive()
                ? object.get(key).getAsString()
                : object.get(key).toString();
    }

    private boolean canPlaceOrder() {
        return !checkoutItems.isEmpty()
                && isAddressValid(selectedAddress)
                && selectedShippingMethodIndex != null
                && !TextUtils.isEmpty(selectedPaymentMethod)
                && currentTotal >= 0;
    }

    private void updateConfirmButtonState() {
        if (btnConfirmOrder == null) return;
        boolean enabled;
        if (!hasCheckoutStepper()) {
            enabled = !submitting && canPlaceOrder();
        } else if (currentStep == STEP_ADDRESS) {
            enabled = !submitting
                    && !checkoutItems.isEmpty()
                    && isAddressValid(selectedAddress)
                    && selectedShippingMethodIndex != null
                    && currentTotal >= 0;
        } else if (currentStep == STEP_PAYMENT) {
            enabled = !submitting && canPlaceOrder();
        } else {
            enabled = !submitting
                    && !checkoutItems.isEmpty()
                    && isAddressValid(selectedAddress)
                    && selectedShippingMethodIndex != null
                    && currentTotal >= 0;
        }
        btnConfirmOrder.setEnabled(enabled);
        btnConfirmOrder.setAlpha(enabled ? 1f : 0.6f);

        if (tvCheckoutSubmitError == null) {
            return;
        }
        if (enabled) {
            hideInlineError(tvCheckoutSubmitError);
            return;
        }
        String message = null;
        if (cartLoaded && checkoutItems.isEmpty()) {
            message = "Giỏ hàng đang trống.";
        } else if (!hasCheckoutStepper()) {
            message = null;
        } else if (!isAddressValid(selectedAddress)) {
            message = "Vui lòng chọn hoặc thêm địa chỉ nhận hàng";
        } else if (selectedShippingMethodIndex == null) {
            message = "Vui lòng chọn phương thức giao hàng.";
        } else if (currentStep == STEP_PAYMENT && TextUtils.isEmpty(selectedPaymentMethod)) {
            message = "Vui lòng chọn phương thức thanh toán.";
        }
        if (message != null) {
            showInlineError(tvCheckoutSubmitError, message);
        }
    }

    private boolean isAddressValid(Address address) {
        if (address == null) return false;
        String fullName = trim(address.fullName);
        String phone = trim(address.phone).replaceAll("\\s+", "");
        return NAME_PATTERN.matcher(fullName).matches()
                && PHONE_PATTERN.matcher(phone).matches()
                && !trim(address.address).isEmpty()
                && !trim(address.ward).isEmpty()
                && !trim(address.district).isEmpty()
                && !trim(address.city).isEmpty();
    }

    private Map<String, Object> buildOrderPayload() {
        Map<String, Object> payload = new HashMap<>();
        List<Map<String, Object>> items = new ArrayList<>();
        for (CartItemEntity item : checkoutItems) {
            String productId = resolveOrderProductId(item);
            Map<String, Object> orderItem = new HashMap<>();
            orderItem.put("product", productId);
            orderItem.put("productId", productId);
            orderItem.put("quantity", Math.max(1, item.quantity));
            orderItem.put("price", Math.max(0, item.unitPrice));
            Log.d(TAG, "Order item: productId=" + productId
                    + ", quantity=" + orderItem.get("quantity")
                    + ", price=" + orderItem.get("price"));
            items.add(orderItem);
        }

        Map<String, Object> shippingAddress = new HashMap<>();
        shippingAddress.put("fullName", trim(selectedAddress.fullName));
        shippingAddress.put("phone", trim(selectedAddress.phone));
        shippingAddress.put("addressLine", trim(selectedAddress.address));
        shippingAddress.put("ward", trim(selectedAddress.ward));
        shippingAddress.put("district", trim(selectedAddress.district));
        shippingAddress.put("city", trim(selectedAddress.city));

        payload.put("items", items);
        payload.put("shippingAddress", shippingAddress);
        payload.put("voucherCode", voucherCode);
        payload.put("paymentMethod", selectedPaymentMethod);
        payload.put("subtotal", currentSubtotal);
        payload.put("shippingFee", selectedShippingFee);
        payload.put("discount", currentDiscount);
        payload.put("total", currentTotal);
        return payload;
    }

    private void clearSubmittedCartItems() {
        List<CartItemEntity> submittedItems = new ArrayList<>(checkoutItems);
        executor.execute(() -> {
            if (selectedCartKeys.isEmpty()) {
                database.cartDao().clearCart();
                return;
            }
            for (CartItemEntity item : submittedItems) {
                database.cartDao().deleteByCartKey(item.productId, item.variantId == null ? "" : item.variantId);
            }
        });
    }

    private String extractOrderId(Map<String, Object> body) {
        if (body == null) return null;
        Object orderId = firstNonNull(body.get("orderId"), body.get("_id"), body.get("id"));
        if (orderId != null) return String.valueOf(orderId);
        Object order = body.get("order");
        if (order instanceof Map) {
            Map<?, ?> orderMap = (Map<?, ?>) order;
            Object nestedId = firstNonNull(orderMap.get("orderId"), orderMap.get("_id"), orderMap.get("id"));
            if (nestedId != null) return String.valueOf(nestedId);
        }
        return null;
    }

    private Object firstNonNull(Object first, Object second, Object third) {
        if (first != null) return first;
        if (second != null) return second;
        return third;
    }

    private void showOrderSuccess(String orderId) {
        String message = TextUtils.isEmpty(orderId)
                ? "Đơn hàng đã được tạo thành công."
                : "Đơn hàng đã được tạo thành công.\nMã đơn hàng: " + orderId;
        new AlertDialog.Builder(this)
                .setTitle("Đặt hàng thành công")
                .setMessage(message)
                .setCancelable(false)
                .setPositiveButton("Về trang chủ", (dialog, which) -> {
                    Intent intent = new Intent(this, HomeActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    startActivity(intent);
                    finish();
                })
                .show();
    }

    private void showSubmitFailure(String message) {
        showInlineError(tvCheckoutSubmitError, message);
        new AlertDialog.Builder(this)
                .setTitle("Không thể đặt hàng")
                .setMessage(message)
                .setPositiveButton("Thử lại", null)
                .show();
    }

    private void showInlineError(TextView view, String message) {
        if (view == null) return;
        view.setText(message);
        view.setVisibility(View.VISIBLE);
    }

    private void showVoucherMessage(String message, boolean error) {
        if (tvVoucherError == null) return;
        tvVoucherError.setTextColor(getColor(error ? R.color.discount_text : R.color.home_tag_green));
        showInlineError(tvVoucherError, message);
    }

    private void hideInlineError(TextView view) {
        if (view != null) {
            view.setVisibility(View.GONE);
        }
    }

    private void scrollToView(View view) {
        if (checkoutScroll == null || view == null) return;
        checkoutScroll.post(() -> checkoutScroll.smoothScrollTo(0, Math.max(0, view.getTop() - 24)));
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }
}
