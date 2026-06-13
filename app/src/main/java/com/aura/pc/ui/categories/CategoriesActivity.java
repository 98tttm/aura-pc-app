package com.aura.pc.ui.categories;

import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.aura.pc.ui.cart.CartActivity;
import com.aura.pc.ui.products.ProductListActivity;
import com.aura.pc.ui.products.ProductSearchActivity;
import com.aura.pc.utils.BottomNavigationHelper;
import com.example.aura_pc_app.R;
import com.example.aura_pc_app.utils.LocaleManager;

public class CategoriesActivity extends AppCompatActivity {
    private String[] groupNames;
    private TypedArray groupItemArrays;
    private int selectedGroupIndex = 0;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleManager.wrap(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_categories);
        
        BottomNavigationHelper.setup(this, BottomNavigationHelper.TAB_CATEGORIES);
        setupHeaderActions();
        setupCategoryBrowser();
    }

    private void setupHeaderActions() {
        View cart = findViewById(R.id.categoryCartButton);
        View notifications = findViewById(R.id.categoryNotificationsButton);
        View menu = findViewById(R.id.categoryMenuButton);
        View search = findViewById(R.id.categorySearchBox);
        View filter = findViewById(R.id.categoryFilterButton);
        View builder = findViewById(R.id.categoryBuilderCard);

        if (cart != null) {
            cart.setOnClickListener(v -> startActivity(new Intent(this, CartActivity.class)));
        }
        if (notifications != null) {
            notifications.setOnClickListener(v ->
                    Toast.makeText(this, R.string.msg_notifications_pending, Toast.LENGTH_SHORT).show());
        }
        if (menu != null) {
            menu.setOnClickListener(v ->
                    Toast.makeText(this, R.string.msg_menu_pending, Toast.LENGTH_SHORT).show());
        }
        if (search != null) {
            search.setOnClickListener(v -> openProductSearch());
        }
        if (filter != null) {
            filter.setOnClickListener(v ->
                    Toast.makeText(this, R.string.category_filter_pending, Toast.LENGTH_SHORT).show());
        }
        if (builder != null) {
            builder.setOnClickListener(v ->
                    Toast.makeText(this, R.string.category_builder_pending, Toast.LENGTH_SHORT).show());
        }
    }

    private void setupCategoryBrowser() {
        groupNames = getResources().getStringArray(R.array.category_group_names);
        groupItemArrays = getResources().obtainTypedArray(R.array.category_group_item_arrays);
        renderCategoryTabs();
        renderSelectedCategoryItems();
    }

    private void renderCategoryTabs() {
        LinearLayout tabs = findViewById(R.id.categoryGroupTabs);
        if (tabs == null || groupNames == null) return;
        tabs.removeAllViews();

        for (int i = 0; i < groupNames.length; i++) {
            final int index = i;
            TextView tab = new TextView(this);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    dpToPx(34)
            );
            params.setMarginEnd(dpToPx(10));
            tab.setLayoutParams(params);
            tab.setGravity(android.view.Gravity.CENTER);
            tab.setPadding(dpToPx(18), 0, dpToPx(18), 0);
            tab.setText(groupNames[i]);
            tab.setTextSize(12);
            tab.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
            tab.setSingleLine(true);
            tab.setBackgroundResource(index == selectedGroupIndex
                    ? R.drawable.bg_chip_active
                    : R.drawable.bg_chip_default);
            tab.setTextColor(index == selectedGroupIndex
                    ? Color.WHITE
                    : getColor(R.color.aura_muted));
            tab.setOnClickListener(v -> {
                selectedGroupIndex = index;
                renderCategoryTabs();
                renderSelectedCategoryItems();
            });
            tabs.addView(tab);
        }
    }

    private void renderSelectedCategoryItems() {
        GridLayout grid = findViewById(R.id.categoryItemsGrid);
        if (grid == null || groupItemArrays == null) return;
        grid.removeAllViews();

        int arrayResId = groupItemArrays.getResourceId(selectedGroupIndex, 0);
        if (arrayResId == 0) return;

        LayoutInflater inflater = LayoutInflater.from(this);
        String[] items = getResources().getStringArray(arrayResId);
        for (String item : items) {
            View tile = inflater.inflate(R.layout.item_category_tile, grid, false);
            TextView title = tile.findViewById(R.id.categoryTileName);
            TextView subtitle = tile.findViewById(R.id.categoryTileCount);
            ImageView icon = tile.findViewById(R.id.categoryTileIcon);

            if (title != null) title.setText(item);
            if (subtitle != null) subtitle.setText(R.string.category_item_cta);
            if (icon != null) icon.setImageResource(getGroupIcon(selectedGroupIndex));
            tile.setOnClickListener(v -> openProductList(item));
            grid.addView(tile);
        }
    }

    private int getGroupIcon(int index) {
        switch (index) {
            case 1:
                return R.drawable.ic_grid;
            case 2:
                return R.drawable.ic_nav_home;
            case 3:
            case 4:
                return R.drawable.ic_grid;
            case 5:
                return R.drawable.ic_nav_home;
            case 6:
                return R.drawable.ic_categories;
            case 7:
            default:
                return R.drawable.ic_categories;
        }
    }

    private void openProductList() {
        startActivity(new Intent(this, ProductListActivity.class));
    }

    private void openProductList(String initialQuery) {
        Intent intent = new Intent(this, ProductListActivity.class);
        intent.putExtra(ProductListActivity.EXTRA_INITIAL_QUERY, initialQuery);
        intent.putExtra(ProductListActivity.EXTRA_FROM_SEARCH, true);
        startActivity(intent);
    }

    private void openProductSearch() {
        Intent intent = new Intent(this, ProductSearchActivity.class);
        intent.putExtra(ProductSearchActivity.EXTRA_SOURCE, ProductSearchActivity.SOURCE_CATEGORIES);
        startActivity(intent);
    }

    @Override
    protected void onDestroy() {
        if (groupItemArrays != null) {
            groupItemArrays.recycle();
            groupItemArrays = null;
        }
        super.onDestroy();
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }
}
