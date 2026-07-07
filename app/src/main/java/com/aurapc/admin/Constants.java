package com.aurapc.admin;

/**
 * Application-wide constants.
 */
public final class Constants {

    private Constants() {}

    // API
    public static final String BASE_URL = BuildConfig.BACKEND_URL;
    public static final String API_PREFIX = "api/";
    public static final int DEFAULT_PAGE_SIZE = 20;

    // Auth
    public static final String PREF_FILE_SECURE = "aura_admin_secure_prefs";
    public static final String KEY_ACCESS_TOKEN = "access_token";
    public static final String KEY_ADMIN_ID = "admin_id";
    public static final String KEY_ADMIN_EMAIL = "admin_email";
    public static final String KEY_ADMIN_NAME = "admin_name";
    public static final String KEY_ADMIN_ROLE = "admin_role";
    public static final String KEY_ADMIN_AVATAR = "admin_avatar";
    public static final String KEY_BIOMETRIC_ENABLED = "biometric_enabled";

    // Notification channel
    public static final String CHANNEL_GENERAL = "aura_admin_general";
    public static final String CHANNEL_ORDERS = "aura_admin_orders";
    public static final String CHANNEL_SUPPORT = "aura_admin_support";

    // Socket events
    public static final String SOCKET_EVENT_ORDER_UPDATED = "orderUpdated";
    public static final String SOCKET_EVENT_SUPPORT_MESSAGE = "supportMessage";
    public static final String SOCKET_EVENT_SUPPORT_CONVERSATION = "supportConversationUpdated";

    // Order statuses (must match backend)
    public static final String[] ORDER_STATUSES = {
            "pending", "confirmed", "processing", "shipped", "delivered", "cancelled"
    };
    public static final String[] ORDER_STATUS_LABELS_VI = {
            "Chờ xác nhận", "Đã xác nhận", "Đang xử lý", "Đang giao", "Đã giao", "Đã hủy"
    };

    // Roles (must match backend)
    public static final String ROLE_SUPER_ADMIN = "super_admin";
    public static final String ROLE_ORDER_MANAGER = "order_manager";
    public static final String ROLE_PRODUCT_MANAGER = "product_manager";
    public static final String ROLE_SUPPORT_AGENT = "support_agent";

    // Date formats
    public static final String DATE_FORMAT_ISO = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX";
    public static final String DATE_FORMAT_DISPLAY = "dd/MM/yyyy HH:mm";
    public static final String DATE_FORMAT_DAY = "dd/MM";
}