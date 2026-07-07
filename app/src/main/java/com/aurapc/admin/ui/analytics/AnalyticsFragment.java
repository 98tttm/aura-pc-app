package com.aurapc.admin.ui.analytics;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.aurapc.admin.R;
import com.aurapc.admin.data.api.Resource;
import com.aurapc.admin.data.model.CustomerSegment;
import com.aurapc.admin.data.model.DashboardData;
import com.aurapc.admin.data.model.DashboardTopProduct;
import com.aurapc.admin.data.model.OrderStatusSummary;
import com.aurapc.admin.data.model.RevenueChartPoint;
import com.aurapc.admin.di.ServiceLocator;
import com.aurapc.admin.utils.Formatters;
import com.aurapc.admin.utils.NetworkHelper;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.PercentFormatter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AnalyticsFragment extends Fragment {

    private View rootView;
    private ProgressBar progress;
    private TextView tvRevenue, tvOrders, tvCustomers, tvAvgOrder;
    private LineChart chartRevenue;
    private PieChart chartSegments;
    private BarChart chartOrderStatus;
    private RecyclerView rvTopProducts;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_analytics, container, false);

        progress = rootView.findViewById(R.id.progress);
        tvRevenue = rootView.findViewById(R.id.tvRevenue);
        tvOrders = rootView.findViewById(R.id.tvOrders);
        tvCustomers = rootView.findViewById(R.id.tvCustomers);
        tvAvgOrder = rootView.findViewById(R.id.tvAvgOrder);
        chartRevenue = rootView.findViewById(R.id.chartRevenue);
        chartSegments = rootView.findViewById(R.id.chartSegments);
        chartOrderStatus = rootView.findViewById(R.id.chartOrderStatus);
        rvTopProducts = rootView.findViewById(R.id.rvTopProducts);

        rvTopProducts.setLayoutManager(new LinearLayoutManager(requireContext()));

        load();
        return rootView;
    }

    private void load() {
        progress.setVisibility(View.VISIBLE);
        com.aurapc.admin.data.api.ApiClient api = ServiceLocator.get().apiClient();

        NetworkHelper.toLiveData(api.dashboardApi().getStats(), (Resource<DashboardData> result) -> {
            if (result.isSuccess() && result.data != null) {
                bindStats(result.data);
            }
        });

        NetworkHelper.toLiveData(api.dashboardApi().getRevenueChart(12), (Resource<List<RevenueChartPoint>> result) -> {
            if (result.isSuccess() && result.data != null) {
                drawRevenueLine(result.data);
            }
        });

        NetworkHelper.toLiveData(api.dashboardApi().getOrderStatusSummary(), (Resource<OrderStatusSummary> result) -> {
            if (result.isSuccess() && result.data != null) {
                drawOrderStatus(result.data);
            }
        });

        NetworkHelper.toLiveData(api.dashboardApi().getCustomerSegment(), (Resource<CustomerSegment> result) -> {
            if (result.isSuccess() && result.data != null) {
                drawSegments(result.data);
            }
        });

        NetworkHelper.toLiveData(api.dashboardApi().getTopProducts(10), (Resource<List<DashboardTopProduct>> result) -> {
            progress.setVisibility(View.GONE);
            if (result.isSuccess() && result.data != null) {
                rvTopProducts.setAdapter(new com.aurapc.admin.ui.dashboard.TopProductsAdapter(result.data));
            }
        });
    }

    private void bindStats(DashboardData data) {
        View revCard = rootView.findViewById(R.id.cardRevenue);
        ((TextView) revCard.findViewById(R.id.tvLabel)).setText("Doanh thu tháng");
        ((TextView) revCard.findViewById(R.id.tvValue)).setText(Formatters.formatVnd(data.revenueThisMonth));
        ((TextView) revCard.findViewById(R.id.tvSub)).setText("tháng này");

        View orderCard = rootView.findViewById(R.id.cardOrders);
        ((TextView) orderCard.findViewById(R.id.tvLabel)).setText("Đơn hàng");
        ((TextView) orderCard.findViewById(R.id.tvValue)).setText(String.valueOf(data.ordersThisMonth != null ? data.ordersThisMonth : 0));
        ((TextView) orderCard.findViewById(R.id.tvSub)).setText("tháng này");

        View custCard = rootView.findViewById(R.id.cardCustomers);
        ((TextView) custCard.findViewById(R.id.tvLabel)).setText("Khách hàng");
        ((TextView) custCard.findViewById(R.id.tvValue)).setText(String.valueOf(data.totalUsers != null ? data.totalUsers : 0));
        ((TextView) custCard.findViewById(R.id.tvSub)).setText("tổng");

        View avgCard = rootView.findViewById(R.id.cardAvgOrder);
        ((TextView) avgCard.findViewById(R.id.tvLabel)).setText("Giá trị TB/đơn");
        double avg = (data.revenueThisMonth != null && data.ordersThisMonth != null && data.ordersThisMonth > 0)
                ? data.revenueThisMonth / data.ordersThisMonth : 0;
        ((TextView) avgCard.findViewById(R.id.tvValue)).setText(Formatters.formatVnd(avg));
        ((TextView) avgCard.findViewById(R.id.tvSub)).setText("tháng này");
    }

    private void drawRevenueLine(List<RevenueChartPoint> points) {
        List<Entry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        for (int i = 0; i < points.size(); i++) {
            RevenueChartPoint p = points.get(i);
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
        ds.setFillAlpha(40);

        LineData ld = new LineData(ds);
        chartRevenue.setData(ld);
        chartRevenue.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
        chartRevenue.getDescription().setEnabled(false);
        chartRevenue.getLegend().setEnabled(false);
        chartRevenue.invalidate();
    }

    private void drawOrderStatus(OrderStatusSummary summary) {
        if (summary.byStatus == null) return;
        List<BarEntry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        int i = 0;
        int[] colors = {0xFFEAB308, 0xFF0284C7, 0xFFEA580C, 0xFF38BDF8, 0xFF16A34A, 0xFFDC2626};
        for (Map.Entry<String, OrderStatusSummary.StatusBucket> e : summary.byStatus.entrySet()) {
            OrderStatusSummary.StatusBucket b = e.getValue();
            entries.add(new BarEntry(i, b.count));
            labels.add(getStatusLabel(e.getKey()));
            i++;
        }
        BarDataSet ds = new BarDataSet(entries, "Đơn hàng");
        ds.setColors(colors);
        ds.setDrawValues(false);
        BarData data = new BarData(ds);
        data.setBarWidth(0.5f);
        chartOrderStatus.setData(data);
        chartOrderStatus.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
        chartOrderStatus.getDescription().setEnabled(false);
        chartOrderStatus.getLegend().setEnabled(false);
        chartOrderStatus.invalidate();
    }

    private String getStatusLabel(String key) {
        switch (key) {
            case "pending": return "Chờ";
            case "confirmed": return "Xác nhận";
            case "processing": return "Xử lý";
            case "shipped": return "Giao";
            case "delivered": return "Hoàn thành";
            case "cancelled": return "Hủy";
            default: return key;
        }
    }

    private void drawSegments(CustomerSegment segment) {
        if (segment.summary == null) return;
        List<PieEntry> entries = new ArrayList<>();
        int[] colors = {0xFFCD7F32, 0xFFC0C0C0, 0xFFFFD700, 0xFFC084FC};
        for (CustomerSegment.SegmentBucket s : segment.summary) {
            entries.add(new PieEntry(s.count, getSegmentName(s.name)));
        }
        PieDataSet ds = new PieDataSet(entries, "Phân khúc");
        ds.setColors(colors);
        ds.setValueTextColor(Color.WHITE);
        ds.setValueTextSize(12f);
        PieData data = new PieData(ds);
        data.setValueFormatter(new PercentFormatter(chartSegments));
        chartSegments.setData(data);
        chartSegments.getDescription().setEnabled(false);
        chartSegments.setUsePercentValues(true);
        chartSegments.invalidate();
    }

    private String getSegmentName(String key) {
        switch (key) {
            case "bronze": return "Đồng";
            case "silver": return "Bạc";
            case "gold": return "Vàng";
            case "vip": return "VIP";
            default: return key;
        }
    }
}