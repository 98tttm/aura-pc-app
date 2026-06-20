package com.example.aura_pc_app.data.checkout;

import android.text.TextUtils;

public class CheckoutVoucherService {
    public Result validate(String rawCode, double subtotal, double shippingFee) {
        String code = rawCode == null ? "" : rawCode.trim().toUpperCase();
        if (TextUtils.isEmpty(code)) {
            return Result.valid("", 0);
        }

        if (subtotal <= 0) {
            return Result.invalid(code, "Không thể áp dụng mã giảm giá khi giỏ hàng trống.");
        }

        // Backend currently has no public voucher validation endpoint.
        // Empty voucher is allowed; entered codes must not create local-only discounts.
        return Result.invalid(code, "Chưa thể xác thực mã giảm giá với hệ thống. Vui lòng bỏ trống mã hoặc thử lại sau.");
    }

    public static class Result {
        public final boolean valid;
        public final String code;
        public final double discount;
        public final String message;

        private Result(boolean valid, String code, double discount, String message) {
            this.valid = valid;
            this.code = code;
            this.discount = message == null ? Math.max(0, discount) : discount;
            this.message = message;
        }

        static Result valid(String code, double discount) {
            return new Result(true, code, Math.max(0, discount), null);
        }

        static Result invalid(String code, String message) {
            return new Result(false, code, 0, message);
        }
    }
}
