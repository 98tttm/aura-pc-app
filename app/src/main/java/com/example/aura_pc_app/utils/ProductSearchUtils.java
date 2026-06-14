package com.example.aura_pc_app.utils;

import com.example.aura_pc_app.data.db.entity.ProductEntity;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

public final class ProductSearchUtils {
    private static final Pattern DIACRITICS = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");

    /**
     * Backend tìm kiếm phân biệt dấu tiếng Việt (search=chuot -> 0, search=chuột -> 66).
     * Bảng này khôi phục dấu cho các token tiếng Việt phổ biến trong cửa hàng máy tính,
     * giúp người dùng gõ không dấu vẫn ra kết quả. Token brand/english (asus, rtx, hp...)
     * không có trong bảng nên được giữ nguyên — không trùng với từ vựng tiếng Việt ở đây.
     */
    private static final Map<String, String> DIACRITIC_RESTORE = new HashMap<>();
    static {
        DIACRITIC_RESTORE.put("ban", "bàn");
        DIACRITIC_RESTORE.put("ghe", "ghế");
        DIACRITIC_RESTORE.put("cong", "công");
        DIACRITIC_RESTORE.put("thai", "thái");
        DIACRITIC_RESTORE.put("hoc", "học");
        DIACRITIC_RESTORE.put("gia", "giá");
        DIACRITIC_RESTORE.put("tot", "tốt");
        DIACRITIC_RESTORE.put("phim", "phím");
        DIACRITIC_RESTORE.put("chuot", "chuột");
        DIACRITIC_RESTORE.put("lot", "lót");
        DIACRITIC_RESTORE.put("cam", "cầm");
        DIACRITIC_RESTORE.put("dia", "đĩa");
        DIACRITIC_RESTORE.put("choi", "chơi");
        DIACRITIC_RESTORE.put("may", "máy");
        DIACRITIC_RESTORE.put("tinh", "tính");
        DIACRITIC_RESTORE.put("nguon", "nguồn");
        DIACRITIC_RESTORE.put("quat", "quạt");
        DIACRITIC_RESTORE.put("tan", "tản");
        DIACRITIC_RESTORE.put("nhiet", "nhiệt");
        DIACRITIC_RESTORE.put("man", "màn");
        DIACRITIC_RESTORE.put("hinh", "hình");
        DIACRITIC_RESTORE.put("dong", "động");
        DIACRITIC_RESTORE.put("do", "đồ");
        DIACRITIC_RESTORE.put("hoa", "họa");
        DIACRITIC_RESTORE.put("van", "văn");
        DIACRITIC_RESTORE.put("phong", "phòng");
        DIACRITIC_RESTORE.put("phu", "phụ");
        DIACRITIC_RESTORE.put("kien", "kiện");
        DIACRITIC_RESTORE.put("thiet", "thiết");
        DIACRITIC_RESTORE.put("mang", "mạng");
        DIACRITIC_RESTORE.put("vien", "viên");
        DIACRITIC_RESTORE.put("o", "ổ");
        DIACRITIC_RESTORE.put("cung", "cứng");
        DIACRITIC_RESTORE.put("vo", "vỏ");
        DIACRITIC_RESTORE.put("day", "dây");
        DIACRITIC_RESTORE.put("khong", "không");
        DIACRITIC_RESTORE.put("co", "cơ");
    }

    private ProductSearchUtils() {
    }

    public static String sanitizeKeyword(String rawQuery) {
        if (rawQuery == null) {
            return "";
        }
        return rawQuery.trim().replaceAll("\\s+", " ");
    }

    /**
     * Khôi phục dấu tiếng Việt cho từng token để gửi lên backend (vốn phân biệt dấu).
     * Token không nằm trong bảng (brand, spec, english, hoặc đã có dấu sẵn) được giữ nguyên.
     */
    public static String restoreDiacritics(String rawQuery) {
        String query = sanitizeKeyword(rawQuery);
        if (query.isEmpty()) {
            return query;
        }
        String[] tokens = query.split("\\s+");
        StringBuilder builder = new StringBuilder();
        for (String token : tokens) {
            String restored = DIACRITIC_RESTORE.get(normalize(token));
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(restored != null ? restored : token);
        }
        return builder.toString();
    }

    public static String normalize(String value) {
        String safe = sanitizeKeyword(value).toLowerCase(Locale.ROOT);
        String decomposed = Normalizer.normalize(safe, Normalizer.Form.NFD);
        return DIACRITICS.matcher(decomposed)
                .replaceAll("")
                .replace('\u0111', 'd')
                .replace('\u0110', 'd');
    }

    public static boolean matches(ProductEntity product, String rawQuery) {
        String query = normalize(rawQuery);
        if (query.isEmpty()) {
            return true;
        }

        String searchable = buildSearchableText(product);
        if (containsAllTokens(searchable, query)) {
            return true;
        }

        for (String alias : expandAliases(query)) {
            if (containsAllTokens(searchable, alias)) {
                return true;
            }
        }
        return false;
    }

    public static boolean contains(String value, String rawQuery) {
        String query = normalize(rawQuery);
        return !query.isEmpty() && normalize(value).contains(query);
    }

    private static String buildSearchableText(ProductEntity product) {
        StringBuilder builder = new StringBuilder();
        append(builder, product.name);
        append(builder, product.brand);
        append(builder, product.slug);
        append(builder, product.specs);
        append(builder, product.category_id);
        if (product.category_ids != null) {
            for (String categoryId : product.category_ids) {
                append(builder, categoryId);
            }
        }
        return normalize(builder.toString());
    }

    private static void append(StringBuilder builder, String value) {
        if (value != null && !value.trim().isEmpty()) {
            builder.append(' ').append(value);
        }
    }

    private static boolean containsAllTokens(String searchable, String query) {
        if (searchable.contains(query)) {
            return true;
        }
        // Tách theo mọi ký tự không phải chữ/số để các dấu phân cách như "|", "/", ","
        // (vd: "ACER | PREDATOR", "GIGABYTE | AORUS") không trở thành token vô nghĩa.
        String[] tokens = query.split("[^\\p{Alnum}]+");
        boolean hasToken = false;
        for (String token : tokens) {
            if (token.isEmpty()) {
                continue;
            }
            hasToken = true;
            if (!searchable.contains(token)) {
                return false;
            }
        }
        return hasToken;
    }

    private static List<String> expandAliases(String query) {
        Set<String> aliases = new LinkedHashSet<>();
        addAlias(aliases, query, "ban phim", "keyboard keychron");
        addAlias(aliases, query, "keyboard", "ban phim");
        addAlias(aliases, query, "chuot", "mouse logitech");
        addAlias(aliases, query, "mouse", "chuot");
        addAlias(aliases, query, "man hinh", "monitor asus rog");
        addAlias(aliases, query, "monitor", "man hinh");
        addAlias(aliases, query, "o cung", "ssd hdd nvme");
        addAlias(aliases, query, "linh kien", "cpu ram ssd vga mainboard");
        addAlias(aliases, query, "card do hoa", "vga gpu rtx");
        addAlias(aliases, query, "vga", "gpu rtx geforce");
        addAlias(aliases, query, "pc gaming", "pc gaming aura storm");
        addAlias(aliases, query, "laptop gaming", "laptop gaming rtx");
        addAlias(aliases, query, "tai nghe", "headphone audio");
        addAlias(aliases, query, "loa", "speaker audio");
        return new ArrayList<>(aliases);
    }

    private static void addAlias(Set<String> aliases, String query, String trigger, String alias) {
        if (query.contains(trigger)) {
            aliases.add(alias);
        }
    }
}
