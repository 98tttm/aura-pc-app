package com.example.aura_pc_app.utils;

import com.example.aura_pc_app.data.db.entity.ProductEntity;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

public final class ProductSearchUtils {
    private static final Pattern DIACRITICS = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");

    private ProductSearchUtils() {
    }

    public static String sanitizeKeyword(String rawQuery) {
        if (rawQuery == null) {
            return "";
        }
        return rawQuery.trim().replaceAll("\\s+", " ");
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
