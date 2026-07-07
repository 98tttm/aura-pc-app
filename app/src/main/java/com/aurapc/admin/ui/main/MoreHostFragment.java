package com.aurapc.admin.ui.main;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.aurapc.admin.R;
import com.aurapc.admin.databinding.FragmentMoreBinding;
import com.aurapc.admin.ui.blog.BlogFragment;
import com.aurapc.admin.ui.categories.CategoriesFragment;
import com.aurapc.admin.ui.hub.HubFragment;
import com.aurapc.admin.ui.products.PromotionsFragment;
import com.aurapc.admin.ui.reviews.ReviewsFragment;
import com.aurapc.admin.ui.settings.SettingsFragment;
import com.aurapc.admin.ui.support.SupportFragment;
import com.aurapc.admin.ui.warranty.WarrantyFragment;

import androidx.recyclerview.widget.LinearLayoutManager;

import java.util.Arrays;

public class MoreHostFragment extends Fragment {

    private FragmentMoreBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentMoreBinding.inflate(inflater, container, false);
        setupMoreMenu();
        return binding.getRoot();
    }

    private void setupMoreMenu() {
        MoreMenuAdapter adapter = new MoreMenuAdapter(Arrays.asList(
                new MoreMenuAdapter.MenuItem(R.drawable.ic_users, "Khách hàng", "Quản lý tài khoản người dùng", () -> replace(new com.aurapc.admin.ui.users.UsersFragment())),
                new MoreMenuAdapter.MenuItem(R.drawable.ic_blog, "Blog", "Quản lý bài viết", () -> replace(new BlogFragment())),
                new MoreMenuAdapter.MenuItem(R.drawable.ic_hub, "AuraHub", "Bài viết cộng đồng", () -> replace(new HubFragment())),
                new MoreMenuAdapter.MenuItem(R.drawable.ic_support, "Hỗ trợ", "Chat hỗ trợ khách hàng", () -> replace(new SupportFragment())),
                new MoreMenuAdapter.MenuItem(R.drawable.ic_warranty, "Bảo hành", "Quản lý phiếu bảo hành", () -> replace(new WarrantyFragment())),
                new MoreMenuAdapter.MenuItem(R.drawable.ic_inventory, "Danh mục", "Quản lý danh mục sản phẩm", () -> replace(new CategoriesFragment())),
                new MoreMenuAdapter.MenuItem(R.drawable.ic_promotions, "Khuyến mãi", "Mã giảm giá & ưu đãi", () -> replace(new PromotionsFragment())),
                new MoreMenuAdapter.MenuItem(R.drawable.ic_review, "Đánh giá", "Kiểm duyệt đánh giá sản phẩm", () -> replace(new ReviewsFragment())),
                new MoreMenuAdapter.MenuItem(R.drawable.ic_settings, "Cài đặt", "Tài khoản & cài đặt app", () -> replace(new SettingsFragment()))
        ));
        binding.rvMore.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvMore.setAdapter(adapter);
    }

    private void replace(Fragment fragment) {
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentHost, fragment)
                .addToBackStack(null)
                .commit();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}