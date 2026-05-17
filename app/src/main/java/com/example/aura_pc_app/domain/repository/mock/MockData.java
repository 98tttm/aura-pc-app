package com.example.aura_pc_app.domain.repository.mock;

import com.example.aura_pc_app.R;
import com.example.aura_pc_app.domain.model.Product;
import com.example.aura_pc_app.domain.model.ProductSpec;

import java.util.ArrayList;
import java.util.List;

public class MockData {
    public static Product getDetailProduct() {
        List<Integer> images = new ArrayList<>();
        images.add(R.drawable.pc_main_1);
        images.add(R.drawable.pc_main_2);
        images.add(R.drawable.pc_main_3);

        List<ProductSpec> specs = new ArrayList<>();
        specs.add(new ProductSpec("CPU", "Core i9-14900K", R.drawable.ic_cpu));
        specs.add(new ProductSpec("GPU", "RTX 4090 24GB", R.drawable.ic_gpu));
        specs.add(new ProductSpec("RAM", "64GB DDR5 6000MHz", R.drawable.ic_ram));
        specs.add(new ProductSpec("SSD", "2TB NVMe Gen5", R.drawable.ic_ssd));

        return new Product(
                "Aura Nova X9 - Hiệu năng vô cực",
                "Aura Nova X9 là cỗ máy trạm được thiết kế dành cho các nhà sáng tạo nội dung chuyên nghiệp và game thủ đam mê cấu hình khủng. Với khả năng xử lý đồ họa mạnh mẽ, tản nhiệt nước AIO 360mm tùy chỉnh và vỏ case hợp kim nhôm cao cấp, Nova X9 đảm bảo hiệu năng luôn mát mẻ kể cả khi render 3D nặng hay chơi game AAA ở độ phân giải 4K.",
                "54.990.000đ",
                "62.000.000đ",
                "-12%",
                4.9f,
                124,
                1200,
                images,
                specs
        );
    }

    public static List<Product> getRelatedProducts() {
        List<Product> list = new ArrayList<>();
        
        List<Integer> img1 = new ArrayList<>();
        img1.add(R.drawable.pc_main_1);
        list.add(new Product("Màn hình Aura 4K", "", "12.500.000đ", "", "", 0, 0, 0, img1, null));

        List<Integer> img2 = new ArrayList<>();
        img2.add(R.drawable.pc_main_2);
        list.add(new Product("Bàn phím cơ Aura", "", "2.450.000đ", "", "", 0, 0, 0, img2, null));
        
        return list;
    }

    public static List<Product> getViewedProducts() {
        List<Product> list = new ArrayList<>();
        
        List<Integer> img1 = new ArrayList<>();
        img1.add(R.drawable.pc_main_1);
        list.add(new Product("Chuột Aura Precision", "", "1.250.000đ", "", "", 0, 0, 0, img1, null));

        List<Integer> img2 = new ArrayList<>();
        img2.add(R.drawable.pc_main_2);
        list.add(new Product("Tai nghe Aura Pro", "", "3.800.000đ", "", "", 0, 0, 0, img2, null));

        List<Integer> img3 = new ArrayList<>();
        img3.add(R.drawable.pc_main_3);
        list.add(new Product("Lót chuột Aura", "", "850.000đ", "", "", 0, 0, 0, img3, null));

        return list;
    }
}
