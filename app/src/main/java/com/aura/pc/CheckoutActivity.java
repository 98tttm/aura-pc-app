package com.aura.pc;

import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.aura.pc.utils.BottomNavigationHelper;
import com.example.aura_pc_app.R;
import com.example.aura_pc_app.utils.AuthGate;
import com.example.aura_pc_app.utils.LocaleManager;

public class CheckoutActivity extends AppCompatActivity {

    private LinearLayout paymentCard1, paymentCard2, paymentCard3;
    private ImageView radio1, radio2, radio3;

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

        initViews();
        setupPaymentMethods();
        
        // Initialize Bottom Navigation
        BottomNavigationHelper.setup(this, BottomNavigationHelper.TAB_CART);

        View back = findViewById(R.id.btnBack);
        if (back != null) {
            back.setOnClickListener(v -> finish());
        }

        View confirmOrder = findViewById(R.id.btnConfirmOrder);
        if (confirmOrder != null) {
            confirmOrder.setOnClickListener(v ->
                    Toast.makeText(this, getString(R.string.toast_order_confirmed), Toast.LENGTH_SHORT).show());
        }
    }

    private void initViews() {
        // Since we are using include, we need to find them within the layout hierarchy
        // For simplicity in this mock, we can just grab them by their positions or give them IDs in activity_checkout.xml
        // Let's update activity_checkout.xml to have unique IDs for payment method includes
    }

    private void setupPaymentMethods() {
        // Mocking the selection logic
        View card1 = findViewById(R.id.payment_method_1);
        View card2 = findViewById(R.id.payment_method_2);
        View card3 = findViewById(R.id.payment_method_3);

        if (card1 != null) {
            setupCard(card1, "Thẻ tín dụng / Ghi nợ", "**** **** **** 4090", R.drawable.ic_credit_card, true);
            card1.setOnClickListener(v -> selectPaymentMethod(1));
        }
        if (card2 != null) {
            setupCard(card2, "Ví điện tử (Momo / ZaloPay)", "Liên kết ví ngay", R.drawable.ic_account_balance_wallet, false);
            card2.setOnClickListener(v -> selectPaymentMethod(2));
        }
        if (card3 != null) {
            setupCard(card3, "Thanh toán khi nhận hàng (COD)", "Thanh toán bằng tiền mặt", R.drawable.ic_payments, false);
            card3.setOnClickListener(v -> selectPaymentMethod(3));
        }
    }

    private void setupCard(View view, String title, String subtitle, int iconRes, boolean isSelected) {
        TextView tvTitle = view.findViewById(R.id.tv_payment_title);
        TextView tvSubtitle = view.findViewById(R.id.tv_payment_subtitle);
        ImageView ivIcon = view.findViewById(R.id.iv_payment_icon);
        ImageView ivRadio = view.findViewById(R.id.iv_radio);

        tvTitle.setText(title);
        tvSubtitle.setText(subtitle);
        ivIcon.setImageResource(iconRes);
        
        view.setSelected(isSelected);
        ivRadio.setImageResource(isSelected ? R.drawable.bg_radio_selected : R.drawable.bg_radio_unselected);
    }

    private void selectPaymentMethod(int index) {
        View card1 = findViewById(R.id.payment_method_1);
        View card2 = findViewById(R.id.payment_method_2);
        View card3 = findViewById(R.id.payment_method_3);

        updateCardSelection(card1, index == 1);
        updateCardSelection(card2, index == 2);
        updateCardSelection(card3, index == 3);
    }

    private void updateCardSelection(View view, boolean isSelected) {
        if (view == null) return;
        view.setSelected(isSelected);
        ImageView ivRadio = view.findViewById(R.id.iv_radio);
        ivRadio.setImageResource(isSelected ? R.drawable.bg_radio_selected : R.drawable.bg_radio_unselected);
    }
}
