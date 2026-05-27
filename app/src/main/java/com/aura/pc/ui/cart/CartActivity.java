package com.aura.pc.ui.cart;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.aura.pc.utils.BottomNavigationHelper;
import com.example.aura_pc_app.R;

public class CartActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cart);
        
        BottomNavigationHelper.setup(this, BottomNavigationHelper.TAB_CART);
    }
}