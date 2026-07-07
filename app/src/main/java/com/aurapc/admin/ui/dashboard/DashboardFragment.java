package com.aurapc.admin.ui.dashboard;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.aurapc.admin.R;
import com.aurapc.admin.data.api.ApiClient;
import com.aurapc.admin.data.api.Resource;
import com.aurapc.admin.data.model.DashboardData;
import com.aurapc.admin.data.model.DashboardTopProduct;
import com.aurapc.admin.data.model.Order;
import com.aurapc.admin.data.model.Product;
import com.aurapc.admin.di.ServiceLocator;
import com.aurapc.admin.utils.Formatters;
import com.aurapc.admin.utils.NetworkHelper;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class DashboardFragment extends Fragment {

    private View rootView;
    private ProgressBar progress;
    private View errorState;
    private TextView tvErrorMessage;
    private TextView tvAdminName;
    private LineChart chart;
    private TabLayout chartTabs;
    private RecyclerView rvRecent, rvTop, rvLowStock;

    private String currentChartMode = "weekly";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_dashboard, container, false);
        progress = rootView.findViewById(R.id.progress);
        errorState = rootView.findViewById(R.id.errorState);
        tvErrorMessage = rootView.findViewById(R.id.tvErrorMessage);
        tvAdminName = rootView.findViewById(R.id.tvAdminName);
        View btnRetry = rootView.findViewById(R.id.btnRetry);
        ImageButton btnNotifications = rootView.findViewById(R.id.btnNotifications);
        TextView btnSeeAllOrders = rootView.findViewById(R.id.btnSeeAllOrders);
        chart = rootView.findViewById(R.id.chart);
        chartTabs = rootView.findViewById(R.id.chartTabs);
        rvRecent = rootView.findViewById(R.id.rvRecentOrders);
        rvTop = rootView.findViewById(R.id.rvTopProducts);
        rvLowStock = rootView.findViewById(R.id.rvLowStock);

        String name = ServiceLocator.get().tokenManager().getAdminName();
        tvAdminName.setText(name != null ? name : "Admin");

        rvRecent.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvTop.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvLowStock.setLayoutManager(new LinearLayoutManager(requireContext()));

        chartTabs.addTab(chartTabs.newTab().setText("Tuần"));
        chartTabs.addTab(chartTabs.newTab().setText("Tháng"));
        chartTabs.addTab(chartTabs.newTab().setText("Năm"));
        chartTabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override public void onTabSelected(TabLayout.Tab tab) {
                int pos = tab.getPosition();
                if (pos == 0) {
                    currentChartMode = "weekly";
                    loadWeekly();
                } else if (pos == 1) {
                    currentChartMode = "yearly";
                    loadYearly();
                } else {
                    currentChartMode = "orders";
                    loadOrders(7);
                }
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });

        btnRetry.setOnClickListener(v -> loadDashboard());
        btnNotifications.setOnClickListener(v -> {
            android.content.Intent i = new android.content.Intent(requireContext(), com.aurapc.admin.ui.notifications.NotificationsActivity.class);
            startActivity(i);
        });
        btnSeeAllOrders.setOnClickListener(v -> {
            if (getActivity() instanceof com.aurapc.admin.ui.main.MainActivity) {
                ((com.aurapc.admin.ui.main.MainActivity) getActivity()).navigateToOrders();
            }
        });

        loadDashboard();
        return rootView;
    }

    private void loadDashboard() {
        progress.setVisibility(View.VISIBLE);
        errorState.setVisibility(View.GONE);

        // First try cache for instant UI
        String cached = ServiceLocator.get().localCache().getDashboardJson();
        if (cached != null) {
            try {
                DashboardData data = new com.google.gson.Gson().fromJson(cached, DashboardData.class);
                if (data != null) bindDashboard(data);
            } catch (Throwable ignored) {}
        }

        ApiClient api = ServiceLocator.get().apiClient();
        LiveData<Resource<DashboardData>> ld = NetworkHelper.toLiveData(api.dashboardApi().getStats());
        ld.observe(getViewLifecycleOwner(), result -> {
            if (result == null || result.isLoading()) {
                return;
            }
            progress.setVisibility(View.GONE);
            if (result.isSuccess()) {
                bindDashboard(result.data);
                errorState.setVisibility(View.GONE);
                if (result.data != null) ServiceLocator.get().localCache().putDashboard(result.data);
            } else {
                if (cached == null) errorState.setVisibility(View.VISIBLE);
                tvErrorMessage.setText(result.message != null ? result.message : getString(R.string.error_generic));
            }
        });

        loadWeekly();
        loadTopProducts();
        loadLowStock();
    }

    private void bindDashboard(DashboardData data) {
        if (data == null) return;

        View revenueCard = rootView.findViewById(R.id.statRevenue);
        ((TextView) revenueCard.findViewById(R.id.label)).setText("Doanh thu");
        ((TextView) revenueCard.findViewById(R.id.value)).setText(Formatters.formatVnd(data.revenueThisMonth));

        // Compute revenue growth from current vs last month
        double last = data.revenueLastMonth != null ? data.revenueLastMonth : 0d;
        double cur = data.revenueThisMonth != null ? data.revenueThisMonth : 0d;
        double revPct = last > 0 ? ((cur - last) / last) * 100d : 0d;
        String revGrowth = String.format(Locale.US, "%+.1f%%", revPct);
        TextView revSub = revenueCard.findViewById(R.id.subValue);
        revSub.setText(revGrowth);
        revSub.setTextColor(getResources().getColor(revGrowth.startsWith("-") ? R.color.status_danger : R.color.status_success, null));
        ((ImageView) revenueCard.findViewById(R.id.icon)).setImageResource(R.drawable.ic_trending_up);

        View ordersCard = rootView.findViewById(R.id.statOrders);
        ((TextView) ordersCard.findViewById(R.id.label)).setText(R.string.dashboard_orders_new);
        ((TextView) ordersCard.findViewById(R.id.value)).setText(String.valueOf(data.ordersThisMonth != null ? data.ordersThisMonth : 0));
        ((TextView) ordersCard.findViewById(R.id.subValue)).setText("tháng này");
        ((ImageView) ordersCard.findViewById(R.id.icon)).setImageResource(R.drawable.ic_orders);

        View customersCard = rootView.findViewById(R.id.statCustomers);
        ((TextView) customersCard.findViewById(R.id.label)).setText(R.string.dashboard_customers);
        ((TextView) customersCard.findViewById(R.id.value)).setText(String.valueOf(data.totalUsers != null ? data.totalUsers : 0));
        int usersThisMonth = data.usersThisMonth != null ? data.usersThisMonth : 0;
        ((TextView) customersCard.findViewById(R.id.subValue)).setText("+" + usersThisMonth + " tháng này");
        ((ImageView) customersCard.findViewById(R.id.icon)).setImageResource(R.drawable.ic_users);

        View productsCard = rootView.findViewById(R.id.statProducts);
        ((TextView) productsCard.findViewById(R.id.label)).setText(R.string.dashboard_products);
        ((TextView) productsCard.findViewById(R.id.value)).setText(String.valueOf(data.totalProducts != null ? data.totalProducts : 0));
        ((TextView) productsCard.findViewById(R.id.subValue)).setText("Active");
        ((ImageView) productsCard.findViewById(R.id.icon)).setImageResource(R.drawable.ic_products);

        // Recent orders
        List<Order> recentOrders = data.recentOrders != null ? data.recentOrders : new ArrayList<>();
        rvRecent.setAdapter(new SimpleOrderAdapter(recentOrders, order -> {
            android.content.Intent i = new android.content.Intent(requireContext(), com.aurapc.admin.ui.orders.OrderDetailActivity.class);
            i.putExtra("orderNumber", order.orderNumber);
            startActivity(i);
        }));

        // Low stock
        List<Product> lowProducts = data.lowStockProducts != null ? data.lowStockProducts : new ArrayList<>();
        rvLowStock.setAdapter(new LowStockAdapter(lowProducts));
    }

    private void loadWeekly() {
        ApiClient api = ServiceLocator.get().apiClient();
        NetworkHelper.toLiveData(api.dashboardApi().getRevenueWeekly(), result -> {
            if (result.isSuccess() && result.data != null) {
                drawWeeklyBar(result.data);
            }
        });
    }

    private void loadYearly() {
        ApiClient api = ServiceLocator.get().apiClient();
        NetworkHelper.toLiveData(api.dashboardApi().getRevenueChart(12), result -> {
            if (result.isSuccess() && result.data != null) {
                drawRevenueLine(result.data);
            }
        });
    }

    private void loadOrders(int days) {
        ApiClient api = ServiceLocator.get().apiClient();
        NetworkHelper.toLiveData(api.dashboardApi().getOrdersChart(days), result -> {
            if (result.isSuccess() && result.data != null) {
                drawOrdersLine(result.data);
            }
        });
    }

    private void loadTopProducts() {
        ApiClient api = ServiceLocator.get().apiClient();
        NetworkHelper.toLiveData(api.dashboardApi().getTopProducts(5), result -> {
            if (result.isSuccess() && result.data != null) {
                rvTop.setAdapter(new TopProductsAdapter(result.data));
            }
        });
    }

    private void loadLowStock() {
        ApiClient api = ServiceLocator.get().apiClient();
        NetworkHelper.toLiveData(api.productApi().getLowStockProducts(10), result -> {
            if (result.isSuccess() && result.data != null) {
                List<Product> products = result.data.items != null ? result.data.items : new ArrayList<>();
                rvLowStock.setAdapter(new LowStockAdapter(products));
            } else {
                loadLowStockFallback(api);
            }
        });
    }

    private void loadLowStockFallback(ApiClient api) {
        NetworkHelper.toLiveData(api.productApi().getProducts("", 1, 100), result -> {
            if (result.isSuccess() && result.data != null && result.data.items != null) {
                List<Product> products = new ArrayList<>();
                for (Product product : result.data.items) {
                    int stock = product.stock != null ? product.stock : 0;
                    if (stock > 0 && stock <= 10) {
                        products.add(product);
                    }
                }
                rvLowStock.setAdapter(new LowStockAdapter(products));
            }
        });
    }

    private void drawWeeklyBar(List<com.aurapc.admin.data.model.RevenueChartPoint> points) {
        if (chart == null) return;
        try {
            // Render as a bar-like display using line chart with stepped/vertical lines
            List<Entry> entries = new ArrayList<>();
            List<String> labels = new ArrayList<>();
            for (int i = 0; i < points.size(); i++) {
                com.aurapc.admin.data.model.RevenueChartPoint p = points.get(i);
                entries.add(new Entry(i, p.revenue != null ? p.revenue.floatValue() : 0f));
                String label = p.label != null ? p.label : (p._id != null ? p._id : "");
                labels.add(label);
            }
            LineDataSet ds = new LineDataSet(entries, "Doanh thu");
            ds.setColor(0xFFFF6B1A);
            ds.setCircleColor(0xFFFF6B1A);
            ds.setLineWidth(2f);
            ds.setCircleRadius(5f);
            ds.setDrawCircleHole(false);
            ds.setDrawValues(false);
            ds.setMode(LineDataSet.Mode.STEPPED);
            ds.setDrawFilled(true);
            ds.setFillColor(0xFFFF6B1A);
            ds.setFillAlpha(60);

            LineData data = new LineData(ds);
            chart.clear();
            chart.setData(data);
            if (chart instanceof LineChart) {
                ((LineChart) chart).getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
            }
            chart.invalidate();
            chart.getDescription().setEnabled(false);
            chart.getLegend().setEnabled(false);
        } catch (Throwable t) {
            android.util.Log.w("DashboardFragment", "drawWeeklyBar failed", t);
        }
    }

    private void drawRevenueLine(List<com.aurapc.admin.data.model.RevenueChartPoint> points) {
        List<Entry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        for (int i = 0; i < points.size(); i++) {
            com.aurapc.admin.data.model.RevenueChartPoint p = points.get(i);
            entries.add(new Entry(i, p.revenue != null ? p.revenue.floatValue() : 0f));
            labels.add(p._id != null ? p._id : "");
        }
        LineDataSet ds = new LineDataSet(entries, "Doanh thu");
        ds.setColor(0xFFFF6B1A);
        ds.setCircleColor(0xFFFF6B1A);
        ds.setLineWidth(2.5f);
        ds.setCircleRadius(4f);
        ds.setDrawValues(false);
        ds.setMode(LineDataSet.Mode.HORIZONTAL_BEZIER);
        ds.setDrawFilled(true);
        ds.setFillColor(0xFFFF6B1A);
        ds.setFillAlpha(50);

        LineData ld = new LineData(ds);
        chart.clear();
        if (chart instanceof LineChart) {
            LineChart lc = (LineChart) chart;
            lc.setData(ld);
            lc.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
        }
        chart.invalidate();
        chart.getDescription().setEnabled(false);
        chart.getLegend().setEnabled(false);
    }

    private void drawOrdersLine(List<com.aurapc.admin.data.model.OrdersChartPoint> points) {
        List<Entry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        for (int i = 0; i < points.size(); i++) {
            com.aurapc.admin.data.model.OrdersChartPoint p = points.get(i);
            entries.add(new Entry(i, p.count != null ? p.count : 0));
            labels.add(p._id != null && p._id.length() >= 5 ? p._id.substring(5) : p._id);
        }
        LineDataSet ds = new LineDataSet(entries, "Đơn hàng");
        ds.setColor(0xFF2196F3);
        ds.setCircleColor(0xFF2196F3);
        ds.setLineWidth(2.5f);
        ds.setCircleRadius(4f);
        ds.setDrawValues(false);
        ds.setMode(LineDataSet.Mode.HORIZONTAL_BEZIER);

        LineData ld = new LineData(ds);
        chart.clear();
        if (chart instanceof LineChart) {
            LineChart lc = (LineChart) chart;
            lc.setData(ld);
            lc.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
        }
        chart.invalidate();
        chart.getDescription().setEnabled(false);
        chart.getLegend().setEnabled(false);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        rootView = null;
    }
}
