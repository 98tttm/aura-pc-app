package com.aura.pc.ui.cart;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.aura.pc.CheckoutActivity;
import com.aura.pc.ui.products.ProductListActivity;
import com.aura.pc.utils.BottomNavigationHelper;
import com.example.aura_pc_app.R;
import com.example.aura_pc_app.data.checkout.CheckoutVoucherService;
import com.example.aura_pc_app.data.db.entity.CartItemEntity;
import com.example.aura_pc_app.utils.AuthGate;
import com.example.aura_pc_app.utils.LocaleManager;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CartActivity extends AppCompatActivity {
    private static final double SHIPPING_FEE = 0;

    private CartItemAdapter cartItemAdapter;
    private CartViewModel viewModel;
    private CheckBox selectAllCheckbox;
    private EditText couponInput;
    private TextView couponMessage;
    private TextView discountText;
    private List<CartItemEntity> latestItems;
    private final CheckoutVoucherService voucherService = new CheckoutVoucherService();
    private final NumberFormat currencyFormat = NumberFormat.getNumberInstance(new Locale("vi", "VN"));
    private String appliedVoucherCode = "";
    private boolean voucherValid = true;
    private double currentSubtotal;
    private double currentDiscount;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleManager.wrap(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cart);

        BottomNavigationHelper.setup(this, BottomNavigationHelper.TAB_CART);
        BottomNavigationHelper.setupHeader(this);
        viewModel = new ViewModelProvider(this).get(CartViewModel.class);
        setupCartList();
        observeCartItems();
        observeActions();
        setupSelectAll();
        setupCoupon();

        findViewById(R.id.cartBackButton).setOnClickListener(v -> finish());
        findViewById(R.id.btnCheckout).setOnClickListener(v -> {
            ArrayList<String> selectedKeys = cartItemAdapter.getSelectedKeys();
            ArrayList<String> selectedProductIds = cartItemAdapter.getSelectedProductIds();
            if (selectedKeys.isEmpty()) {
                Toast.makeText(this, "Vui lòng chọn ít nhất một sản phẩm", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!applyCoupon(false)) {
                Toast.makeText(this, "Vui lòng kiểm tra mã giảm giá", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!AuthGate.requireLogin(this, CheckoutActivity.class)) {
                return;
            }
            Intent intent = new Intent(this, CheckoutActivity.class);
            intent.putStringArrayListExtra(CheckoutActivity.EXTRA_SELECTED_CART_KEYS, selectedKeys);
            intent.putStringArrayListExtra(CheckoutActivity.EXTRA_SELECTED_PRODUCT_IDS, selectedProductIds);
            intent.putExtra(CheckoutActivity.EXTRA_VOUCHER_CODE, appliedVoucherCode);
            startActivity(intent);
        });
        View browse = findViewById(R.id.btnBrowseProducts);
        if (browse != null) {
            browse.setOnClickListener(v -> startActivity(new Intent(this, ProductListActivity.class)));
        }
    }

    private void setupCoupon() {
        couponInput = findViewById(R.id.cartCouponInput);
        couponMessage = findViewById(R.id.tvCartCouponMessage);
        discountText = findViewById(R.id.tvCartDiscountPrice);
        View applyButton = findViewById(R.id.cartApplyCoupon);

        if (applyButton != null) {
            applyButton.setOnClickListener(v -> applyCoupon(true));
        }
        if (couponInput != null) {
            couponInput.setOnEditorActionListener((v, actionId, event) -> {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    applyCoupon(true);
                    return true;
                }
                return false;
            });
            couponInput.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    appliedVoucherCode = "";
                    voucherValid = true;
                    currentDiscount = 0;
                    hideCouponMessage();
                    updateTotalText();
                }

                @Override
                public void afterTextChanged(Editable s) {
                }
            });
        }
    }

    private void setupCartList() {
        cartItemAdapter = new CartItemAdapter(new CartItemAdapter.Listener() {
            @Override
            public void onIncrease(CartItemEntity item) {
                viewModel.increment(item);
            }

            @Override
            public void onDecrease(CartItemEntity item) {
                viewModel.decrement(item);
            }

            @Override
            public void onRemove(CartItemEntity item) {
                viewModel.remove(item);
            }

            @Override
            public void onSelectionChanged() {
                updateCartSummary(cartItemAdapter.getSelectedItems(), latestItems == null || latestItems.isEmpty());
                syncSelectAllState();
            }
        });
        RecyclerView cartList = findViewById(R.id.rvCartItems);
        cartList.setLayoutManager(new LinearLayoutManager(this));
        cartList.setAdapter(cartItemAdapter);
        cartList.setNestedScrollingEnabled(false);
    }

    private void observeCartItems() {
        viewModel.getCartItems().observe(this, items -> {
            latestItems = items;
            viewModel.refreshMissingPrices(items);
            cartItemAdapter.setItems(items);
            updateCartSummary(cartItemAdapter.getSelectedItems(), items == null || items.isEmpty());
            syncSelectAllState();
        });
    }

    private void setupSelectAll() {
        selectAllCheckbox = findViewById(R.id.cbSelectAll);
        if (selectAllCheckbox == null) return;
        selectAllCheckbox.setOnCheckedChangeListener((buttonView, isChecked) ->
                cartItemAdapter.setAllSelected(isChecked));
    }

    private void observeActions() {
        viewModel.errorMessage.observe(this, message -> {
            if (message != null && !message.isEmpty()) {
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateCartSummary(List<CartItemEntity> items, boolean cartEmpty) {
        int count = 0;
        currentSubtotal = 0;
        if (items != null) {
            for (CartItemEntity item : items) {
                int quantity = Math.max(0, item.quantity);
                count += quantity;
                currentSubtotal += item.unitPrice * quantity;
            }
        }

        TextView countText = findViewById(R.id.tvCartItemCount);
        if (countText != null) {
            countText.setText(getString(R.string.cart_item_count_short, count));
        }
        TextView subtotalText = findViewById(R.id.tvCartSubtotalPrice);
        if (subtotalText != null) {
            subtotalText.setText(formatPrice(currentSubtotal));
        }
        revalidateCouponAfterCartChange();
        updateTotalText();

        boolean empty = cartEmpty;
        setVisibility(R.id.emptyCartState, empty);
        setVisibility(R.id.rvCartItems, !empty);
        setVisibility(R.id.promoCodeRow, !empty);
        setVisibility(R.id.orderSummaryCard, !empty);
    }

    private boolean applyCoupon(boolean showToast) {
        String rawCode = couponInput == null ? "" : couponInput.getText().toString();
        CheckoutVoucherService.Result result = voucherService.validate(rawCode, currentSubtotal, SHIPPING_FEE);
        appliedVoucherCode = result.code;
        voucherValid = result.valid;
        currentDiscount = result.discount;

        if (!result.valid) {
            showCouponMessage(result.message, true);
            updateTotalText();
            return false;
        }

        if (TextUtils.isEmpty(appliedVoucherCode)) {
            hideCouponMessage();
        } else {
            showCouponMessage("Đã áp dụng mã " + appliedVoucherCode + " (-" + formatPrice(currentDiscount) + ")", false);
            if (showToast) {
                Toast.makeText(this, "Đã áp dụng mã giảm giá", Toast.LENGTH_SHORT).show();
            }
        }
        updateTotalText();
        return true;
    }

    private void revalidateCouponAfterCartChange() {
        String rawCode = couponInput == null ? "" : couponInput.getText().toString().trim();
        if (TextUtils.isEmpty(rawCode)) {
            currentDiscount = 0;
            appliedVoucherCode = "";
            voucherValid = true;
            hideCouponMessage();
            return;
        }
        if (!TextUtils.isEmpty(appliedVoucherCode) || !voucherValid) {
            applyCoupon(false);
        }
    }

    private void updateTotalText() {
        if (discountText != null) {
            discountText.setText("-" + formatPrice(currentDiscount));
        }
        TextView totalText = findViewById(R.id.tvCartTotalPrice);
        if (totalText != null) {
            totalText.setText(formatPrice(currentSubtotal + SHIPPING_FEE - currentDiscount));
        }
    }

    private void showCouponMessage(String message, boolean error) {
        if (couponMessage == null) return;
        couponMessage.setText(message);
        couponMessage.setTextColor(getColor(error ? R.color.discount_text : R.color.home_tag_green));
        couponMessage.setVisibility(View.VISIBLE);
    }

    private void hideCouponMessage() {
        if (couponMessage != null) {
            couponMessage.setVisibility(View.GONE);
        }
    }

    private void syncSelectAllState() {
        if (selectAllCheckbox == null) return;
        selectAllCheckbox.setOnCheckedChangeListener(null);
        selectAllCheckbox.setChecked(cartItemAdapter.areAllItemsSelected());
        selectAllCheckbox.setOnCheckedChangeListener((buttonView, isChecked) ->
                cartItemAdapter.setAllSelected(isChecked));
    }

    private void setVisibility(int id, boolean visible) {
        View view = findViewById(id);
        if (view != null) {
            view.setVisibility(visible ? View.VISIBLE : View.GONE);
        }
    }

    private String formatPrice(double price) {
        return currencyFormat.format(Math.max(0, price)) + "đ";
    }
}
