package com.aura.pc;

import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.StyleSpan;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.widget.NestedScrollView;

import com.aura.pc.ui.address.Address;
import com.aura.pc.ui.address.AddressBookActivity;
import com.aura.pc.ui.address.AddressRepository;
import com.example.aura_pc_app.R;
import com.example.aura_pc_app.data.db.AppDatabase;
import com.example.aura_pc_app.data.db.entity.CartItemEntity;
import com.example.aura_pc_app.utils.AuthGate;
import com.example.aura_pc_app.utils.LocaleManager;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class CheckoutActivity extends AppCompatActivity {
    public static final String EXTRA_SELECTED_CART_KEYS = "extra_selected_cart_keys";

    private NestedScrollView checkoutScroll;
    private View fixedCheckoutSummaryBar;
    private View fixedCheckoutTotalSection;
    private View orderTotalSection;
    private TextView tvCheckoutSubtotalLabel;
    private TextView tvCheckoutSubtotalPrice;
    private TextView tvCheckoutShippingPrice;
    private TextView tvCheckoutTotalPrice;
    private TextView tvFixedCheckoutTotalPrice;
    private TextView btnChangeAddress;
    private TextView tvCheckoutAddressNamePhone;
    private TextView tvCheckoutAddressLine;
    private TextView tvCheckoutDefaultBadge;
    private AddressRepository addressRepository;
    private final Set<String> selectedCartKeys = new HashSet<>();
    private double currentSubtotal;
    private double selectedShippingFee;
    private final NumberFormat currencyFormatter = NumberFormat.getNumberInstance(new Locale("vi", "VN"));

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
        addressRepository = new AddressRepository(this);
        ArrayList<String> selectedKeys = getIntent().getStringArrayListExtra(EXTRA_SELECTED_CART_KEYS);
        if (selectedKeys != null) {
            selectedCartKeys.addAll(selectedKeys);
        }

        initViews();
        setupShippingTexts();
        setupPaymentMethods();
        setupShippingMethods();
        setupCheckoutSummaryBar();
        observeCartSummary();
        setupAddressActions();
        loadDefaultAddress();

        View back = findViewById(R.id.btnBack);
        if (back != null) {
            back.setOnClickListener(v -> finish());
        }

        View confirmOrder = findViewById(R.id.btnConfirmOrder);
        if (confirmOrder != null) {
            confirmOrder.setOnClickListener(v ->
                    Toast.makeText(this, "Order confirmed", Toast.LENGTH_SHORT).show());
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (addressRepository != null) {
            loadDefaultAddress();
        }
    }

    private void initViews() {
        checkoutScroll = findViewById(R.id.checkoutScroll);
        fixedCheckoutSummaryBar = findViewById(R.id.fixedCheckoutSummaryBar);
        fixedCheckoutTotalSection = findViewById(R.id.fixedCheckoutTotalSection);
        orderTotalSection = findViewById(R.id.orderTotalSection);
        tvCheckoutSubtotalLabel = findViewById(R.id.tvCheckoutSubtotalLabel);
        tvCheckoutSubtotalPrice = findViewById(R.id.tvCheckoutSubtotalPrice);
        tvCheckoutShippingPrice = findViewById(R.id.tvCheckoutShippingPrice);
        tvCheckoutTotalPrice = findViewById(R.id.tvCheckoutTotalPrice);
        tvFixedCheckoutTotalPrice = findViewById(R.id.tvFixedCheckoutTotalPrice);
        btnChangeAddress = findViewById(R.id.btnChangeAddress);
        tvCheckoutAddressNamePhone = findViewById(R.id.tvCheckoutAddressNamePhone);
        tvCheckoutAddressLine = findViewById(R.id.tvCheckoutAddressLine);
        tvCheckoutDefaultBadge = findViewById(R.id.tvCheckoutDefaultBadge);
    }

    private void setupAddressActions() {
        View.OnClickListener openAddressBook = v ->
                startActivity(new Intent(this, AddressBookActivity.class));
        if (btnChangeAddress != null) {
            btnChangeAddress.setOnClickListener(openAddressBook);
        }
        View addressCard = findViewById(R.id.addressCard);
        if (addressCard != null) {
            addressCard.setOnClickListener(openAddressBook);
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
                renderCheckoutAddress(pickCheckoutAddress(addresses));
            }

            @Override
            public void onError(String message) {
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
        AppDatabase.getInstance(this).cartDao().getAllCartItemsLive().observe(this, this::renderCartSummary);
    }

    private void renderCartSummary(List<CartItemEntity> items) {
        int itemCount = 0;
        currentSubtotal = 0;

        if (items != null) {
            for (CartItemEntity item : items) {
                if (!selectedCartKeys.isEmpty() && !selectedCartKeys.contains(cartKey(item))) {
                    continue;
                }
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
        updateOrderTotals();
    }

    private String formatPrice(double price) {
        return currencyFormatter.format(Math.max(0, price)) + "đ";
    }

    private String cartKey(CartItemEntity item) {
        String productId = item == null || item.productId == null ? "" : item.productId;
        String variantId = item == null || item.variantId == null ? "" : item.variantId;
        return productId + "\n" + variantId;
    }

    private String formatShippingPrice(double price) {
        return price <= 0 ? "Miễn phí" : formatPrice(price);
    }

    private void updateOrderTotals() {
        String totalText = formatPrice(currentSubtotal + selectedShippingFee);
        if (tvCheckoutShippingPrice != null) {
            tvCheckoutShippingPrice.setText(formatShippingPrice(selectedShippingFee));
        }
        if (tvCheckoutTotalPrice != null) {
            tvCheckoutTotalPrice.setText(totalText);
        }
        if (tvFixedCheckoutTotalPrice != null) {
            tvFixedCheckoutTotalPrice.setText(totalText);
        }
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
        selectedShippingFee = index == 1 ? 80000 : 0;
        updateShippingSelection(findViewById(R.id.shipping_method_1), R.id.ivShippingRadio1, index == 1);
        updateShippingSelection(findViewById(R.id.shipping_method_2), R.id.ivShippingRadio2, index == 2);
        updateOrderTotals();
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
            setupCard(card1, "Thẻ tín dụng / Ghi nợ", "**** **** **** 4090", R.drawable.ic_checkout_card_bold, true);
            card1.setOnClickListener(v -> selectPaymentMethod(1));
        }
        if (card2 != null) {
            setupCard(card2, "Thanh toán bằng Momo", null, R.drawable.ic_momo_checkout, false);
            card2.setOnClickListener(v -> selectPaymentMethod(2));
        }
        if (card3 != null) {
            setupCard(card3, "Thanh toán bằng Zalopay", null, R.drawable.ic_zalopay_checkout, false);
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
        updateCardSelection(findViewById(R.id.payment_method_1), index == 1);
        updateCardSelection(findViewById(R.id.payment_method_2), index == 2);
        updateCardSelection(findViewById(R.id.payment_method_3), index == 3);
        updateCardSelection(findViewById(R.id.payment_method_4), index == 4);
    }

    private void updateCardSelection(View view, boolean isSelected) {
        if (view == null) return;
        view.setSelected(isSelected);
        ImageView ivRadio = view.findViewById(R.id.iv_radio);
        ivRadio.setImageResource(isSelected
                ? R.drawable.bg_checkout_radio_selected
                : R.drawable.bg_checkout_radio_unselected);
    }
}
