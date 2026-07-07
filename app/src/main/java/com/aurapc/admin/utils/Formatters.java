package com.aurapc.admin.utils;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Formatters {

    private static final NumberFormat VND = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
    private static final SimpleDateFormat DATE = new SimpleDateFormat("dd/MM/yyyy");
    private static final SimpleDateFormat DATETIME = new SimpleDateFormat("dd/MM/yyyy HH:mm");
    private static final SimpleDateFormat TIME = new SimpleDateFormat("HH:mm");

    public static String formatVnd(double amount) {
        return VND.format(amount).replace("₫", "").trim() + "đ";
    }

    public static String formatVnd(Double amount) {
        if (amount == null) return "0đ";
        return formatVnd(amount.doubleValue());
    }

    public static String formatDate(String isoDate) {
        if (isoDate == null || isoDate.isEmpty()) return "—";
        try {
            Date d = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).parse(isoDate);
            return d != null ? DATE.format(d) : isoDate;
        } catch (Exception e) {
            return isoDate;
        }
    }

    public static String formatDateTime(String isoDate) {
        if (isoDate == null || isoDate.isEmpty()) return "—";
        try {
            Date d = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).parse(isoDate);
            return d != null ? DATETIME.format(d) : isoDate;
        } catch (Exception e) {
            return isoDate;
        }
    }

    public static String formatDateTime(Object isoDate) {
        return formatDateTime(isoDate == null ? null : isoDate.toString());
    }

    public static String formatDate(Object isoDate) {
        return formatDate(isoDate == null ? null : isoDate.toString());
    }

    public static String formatTime(String isoDate) {
        if (isoDate == null || isoDate.isEmpty()) return "—";
        try {
            Date d = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).parse(isoDate);
            return d != null ? TIME.format(d) : "";
        } catch (Exception e) {
            return "";
        }
    }

    public static String formatNumber(Number num) {
        if (num == null) return "0";
        return NumberFormat.getNumberInstance(Locale.US).format(num);
    }

    public static String formatPhone(String phone) {
        if (phone == null || phone.isEmpty()) return "—";
        if (phone.length() >= 10) {
            return phone.substring(0, 4) + " " + phone.substring(4, 7) + " " + phone.substring(7);
        }
        return phone;
    }

    public static String truncate(String s, int maxLen) {
        if (s == null) return "";
        return s.length() > maxLen ? s.substring(0, maxLen) + "…" : s;
    }

    public static String initials(String name) {
        if (name == null || name.isEmpty()) return "?";
        String[] parts = name.trim().split("\\s+");
        if (parts.length == 1) return parts[0].substring(0, 1).toUpperCase();
        return (parts[0].substring(0, 1) + parts[parts.length - 1].substring(0, 1)).toUpperCase();
    }
}
