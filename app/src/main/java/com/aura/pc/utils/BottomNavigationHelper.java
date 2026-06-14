package com.aura.pc.utils;

import android.app.Activity;
import android.content.Intent;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import com.example.aura_pc_app.R;
import com.example.aura_pc_app.data.db.AppDatabase;

public class BottomNavigationHelper {

    public static final String TAB_HOME = "home";
    public static final String TAB_CATEGORIES = "categories";
    public static final String TAB_CART = "cart";
    public static final String TAB_BLOG = "blog";
    public static final String TAB_PROFILE = "profile";

    public static void setup(final Activity activity, String activeTab) {
        View home = activity.findViewById(R.id.nav_home_container);
        View categories = activity.findViewById(R.id.nav_categories_container);
        View cart = activity.findViewById(R.id.nav_cart_container);
        View blog = activity.findViewById(R.id.nav_blog_container);
        View profile = activity.findViewById(R.id.nav_profile_container);

        if (home == null) return;

        setActiveTab(activity, activeTab);
        setupCartBadge(activity);

        home.setOnClickListener(v -> navigate(activity, TAB_HOME));
        categories.setOnClickListener(v -> navigate(activity, TAB_CATEGORIES));
        cart.setOnClickListener(v ->
                Toast.makeText(activity, "Dùng biểu tượng giỏ hàng ở header", Toast.LENGTH_SHORT).show());
        blog.setOnClickListener(v -> navigate(activity, TAB_BLOG));
        profile.setOnClickListener(v -> navigate(activity, TAB_PROFILE));
    }

    public static void setupHeader(final Activity activity) {
        View logo = activity.findViewById(R.id.logoText);
        View cart = activity.findViewById(R.id.cartContainer);
        View search = activity.findViewById(R.id.searchIcon);
        View menu = activity.findViewById(R.id.menuIcon);

        if (logo != null) {
            logo.setOnClickListener(v -> navigate(activity, TAB_HOME));
        }
        if (cart != null) {
            cart.setOnClickListener(v -> navigate(activity, TAB_CART));
        }
        if (search != null) {
            search.setOnClickListener(v -> navigate(activity, TAB_CATEGORIES));
        }
        if (menu != null) {
            menu.setOnClickListener(v ->
                    Toast.makeText(activity, "Menu is coming soon", Toast.LENGTH_SHORT).show());
        }
        setupCartBadge(activity);
    }

    private static void setActiveTab(Activity activity, String activeTab) {
        resetTabs(activity);
        int activeColor = ContextCompat.getColor(activity, R.color.premium_nav_active);

        switch (activeTab) {
            case TAB_HOME:
                highlightTab(activity, R.id.nav_home_icon, R.id.nav_home_indicator, activeColor);
                break;
            case TAB_CATEGORIES:
                highlightTab(activity, R.id.nav_categories_icon, R.id.nav_categories_indicator, activeColor);
                break;
            case TAB_CART:
                highlightTab(activity, R.id.nav_cart_icon, R.id.nav_cart_indicator, activeColor);
                break;
            case TAB_BLOG:
                highlightTab(activity, R.id.nav_blog_icon, R.id.nav_blog_indicator, activeColor);
                break;
            case TAB_PROFILE:
                highlightTab(activity, R.id.nav_profile_icon, R.id.nav_profile_indicator, activeColor);
                break;
        }
    }

    private static void resetTabs(Activity activity) {
        int inactiveColor = ContextCompat.getColor(activity, R.color.premium_nav_inactive);
        int[] icons = {R.id.nav_home_icon, R.id.nav_categories_icon, R.id.nav_cart_icon, R.id.nav_blog_icon, R.id.nav_profile_icon};
        int[] indicators = {R.id.nav_home_indicator, R.id.nav_categories_indicator, R.id.nav_cart_indicator, R.id.nav_blog_indicator, R.id.nav_profile_indicator};

        for (int i = 0; i < icons.length; i++) {
            ImageView icon = activity.findViewById(icons[i]);
            View indicator = activity.findViewById(indicators[i]);
            if (icon != null) icon.setColorFilter(inactiveColor);
            if (indicator != null) indicator.setVisibility(View.INVISIBLE);
        }
    }

    private static void highlightTab(Activity activity, int iconId, int indicatorId, int color) {
        ImageView icon = activity.findViewById(iconId);
        View indicator = activity.findViewById(indicatorId);
        if (icon != null) icon.setColorFilter(color);
        if (indicator != null) indicator.setVisibility(View.VISIBLE);
    }

    private static void setupCartBadge(Activity activity) {
        if (!(activity instanceof LifecycleOwner)) {
            return;
        }
        TextView headerBadge = activity.findViewById(R.id.header_cart_badge);
        if (headerBadge == null) {
            return;
        }
        AppDatabase.getInstance(activity)
                .cartDao()
                .getCartItemCountLive()
                .observe((LifecycleOwner) activity, count -> {
                    int safeCount = count == null ? 0 : count;
                    updateBadge(headerBadge, safeCount);
                });
    }

    private static void updateBadge(TextView badge, int count) {
        if (badge == null) return;
        if (count <= 0) {
            badge.setVisibility(View.GONE);
            return;
        }
        badge.setVisibility(View.VISIBLE);
        badge.setText(count > 99 ? "99+" : String.valueOf(count));
    }

    private static void navigate(Activity activity, String target) {
        Class<?> targetClass = null;
        try {
            switch (target) {
                case TAB_HOME: targetClass = Class.forName("com.example.aura_pc_app.ui.home.HomeActivity"); break;
                case TAB_CATEGORIES: targetClass = Class.forName("com.aura.pc.ui.categories.CategoriesActivity"); break;
                case TAB_CART: targetClass = Class.forName("com.aura.pc.ui.cart.CartActivity"); break;
                case TAB_BLOG: targetClass = Class.forName("com.aura.pc.ui.blog.BlogActivity"); break;
                case TAB_PROFILE: targetClass = Class.forName("com.aura.pc.ui.profile.ProfileActivity"); break;
            }
        } catch (ClassNotFoundException e) {
            return;
        }

        if (targetClass != null && !activity.getClass().equals(targetClass)) {
            Intent intent = new Intent(activity, targetClass);
            intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            activity.startActivity(intent);
            activity.overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        }
    }
}
