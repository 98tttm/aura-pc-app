# 📘 MOB-002 — Retrofit + OkHttp + JWT Auth Interceptor + Secure Storage

> **Author:** Phát Huỳnh Tấn  
> **Branch:** `MOB-002`  
> **Sprint:** 1 &nbsp;|&nbsp; **Story Points:** 5

---

## Mục lục

1. [Tổng quan](#tổng-quan)
2. [Kiến trúc & Sơ đồ luồng](#kiến-trúc--sơ-đồ-luồng)
3. [Danh sách file đã tạo/sửa](#danh-sách-file-đã-tạosửa)
4. [Chi tiết từng class](#chi-tiết-từng-class)
5. [Cách sử dụng từ module khác](#cách-sử-dụng-từ-module-khác)
6. [String resources](#string-resources)
7. [Unit Tests](#unit-tests)
8. [Backend API](#backend-api)
9. [Lưu ý quan trọng](#lưu-ý-quan-trọng)

---

## Tổng quan

Branch `MOB-002` cung cấp **tầng Network & Security** cho toàn bộ ứng dụng AuraPC. Mọi module khác (trang sản phẩm, giỏ hàng, profile…) sẽ gọi API thông qua lớp này mà **không cần tự xử lý token, mã hóa, hay retry khi token hết hạn**.

### Các tính năng chính

| # | Tính năng | File chính | Mô tả |
|---|-----------|-----------|-------|
| 1 | REST API Client | `ApiClient.java` | Singleton Retrofit + OkHttp + Gson đã cấu hình đầy đủ |
| 2 | Secure Token Storage | `TokenManager.java` | Lưu JWT bằng EncryptedSharedPreferences (AES-256) |
| 3 | Auto Bearer Injection | `AuthInterceptor.java` | Tự gắn `Authorization: Bearer <token>` vào mọi request |
| 4 | Silent Token Refresh | `TokenAuthenticator.java` | Bắt 401 → refresh token → retry, user không bị logout |
| 5 | Response Wrapper | `ApiResponse.java` | Đóng gói LOADING / SUCCESS / ERROR cho UI |
| 6 | Debug-only Logging | `ApiClient.java` | `HttpLoggingInterceptor` chỉ bật khi `BuildConfig.DEBUG` |

---

## Kiến trúc & Sơ đồ luồng

```
┌──────────────────────────────────────────────────────────────────┐
│                         App.java                                 │
│            ApiClient.getInstance(this)  ← khởi tạo 1 lần        │
└─────────────────────────┬────────────────────────────────────────┘
                          │
                          ▼
┌──────────────────────────────────────────────────────────────────┐
│                     ApiClient (Singleton)                         │
│                                                                   │
│  ┌────────────────┐  ┌──────────────────┐  ┌─────────────────┐   │
│  │AuthInterceptor │  │HttpLoggingInter. │  │TokenAuthenticator│   │
│  │ (gắn Bearer)   │  │ (DEBUG only)     │  │ (xử lý 401)    │   │
│  └───────┬────────┘  └──────────────────┘  └───────┬─────────┘   │
│          │                                          │             │
│          └──────────────┬───────────────────────────┘             │
│                         ▼                                         │
│              ┌────────────────────┐                               │
│              │   TokenManager     │                               │
│              │  (TokenProvider)   │                               │
│              │EncryptedSharedPrefs│                               │
│              │   AES-256 / GCM   │                               │
│              └────────────────────┘                               │
│                         │                                         │
│                         ▼                                         │
│              ┌────────────────────┐                               │
│              │    OkHttpClient    │                               │
│              └─────────┬──────────┘                               │
│                        │                                          │
│                        ▼                                          │
│              ┌────────────────────┐                               │
│              │     Retrofit       │                               │
│              │  → ApiService      │                               │
│              └────────────────────┘                               │
└──────────────────────────────────────────────────────────────────┘
                         │
                         ▼
              ┌────────────────────┐
              │  Backend Server    │
              │  (Render.com)      │
              └────────────────────┘
```

### Luồng xử lý 401 (Token hết hạn)

```
Request → AuthInterceptor (gắn token) → Server → 401 Unauthorized
                                                        │
                                            TokenAuthenticator bắt
                                                        │
                                              ┌─────────┴──────────┐
                                              │ Còn refresh token? │
                                              └─────────┬──────────┘
                                                   YES │         NO
                                                       │          │
                                              Gọi refresh API    return null
                                              Lưu token mới     → 401 về app
                                              Retry request      → chuyển Login
                                                       │
                                                       ▼
                                              ✅ Response OK!
                                              User không bị logout
```

---

## Danh sách file đã tạo/sửa

### File mới (NEW)

| File | Package | Mô tả |
|------|---------|-------|
| `TokenProvider.java` | `data.api` | Interface trừu tượng hóa việc đọc access token |
| `TokenManager.java` | `data.api` | Singleton lưu trữ token mã hóa AES-256 |
| `AuthInterceptor.java` | `data.api` | OkHttp Interceptor gắn Bearer header |
| `TokenAuthenticator.java` | `data.api` | OkHttp Authenticator xử lý 401 |
| `ApiResponse.java` | `data.api` | Wrapper LOADING/SUCCESS/ERROR |
| `AuthInterceptorTest.java` | `test` | Unit test cho AuthInterceptor |

### File sửa (MODIFIED)

| File | Thay đổi |
|------|----------|
| `ApiClient.java` | Thêm Context param, tích hợp Auth + Logging + Authenticator |
| `App.java` | Gọi `ApiClient.getInstance(this)` thay vì `getInstance()` |
| `AppModule.java` | Truyền context, thêm `provideTokenManager()` |
| `Constants.java` | Thêm `BASE_URL` thật, secure prefs keys, `MAX_TOKEN_RETRY` |
| `build.gradle.kts` | Bật `buildConfig = true`, thêm `mockwebserver` test dep |
| `strings.xml` | Thêm error/success messages (không hardcode string) |

---

## Chi tiết từng class

### TokenProvider.java

```java
// Interface — các module khác chỉ cần biết interface này
public interface TokenProvider {
    String getAccessToken();
}
```

**Tại sao cần interface?** Giúp `AuthInterceptor` không phụ thuộc trực tiếp vào `TokenManager`. Trong Unit Test có thể truyền lambda `() -> "fake_token"` mà không cần Android Context.

---

### TokenManager.java

```java
// Lấy instance (truyền context bất kỳ, tự chuyển thành applicationContext)
TokenManager tm = TokenManager.getInstance(context);

// Lưu token sau khi login thành công
tm.saveAccessToken("eyJhbGciOi...");
tm.saveRefreshToken("rf_abc123...");

// Đọc token (tự giải mã AES-256)
String token = tm.getAccessToken();

// Kiểm tra đăng nhập
if (tm.isLoggedIn()) { /* ... */ }

// Xóa session khi logout
tm.clearTokens();
```

**Lưu ý:** `TokenManager` là **Singleton**. Gọi `getInstance(context)` bao nhiêu lần cũng chỉ khởi tạo 1 lần duy nhất.

---

### AuthInterceptor.java

Tự động chạy cho **mọi request**. Không cần gọi thủ công.

```java
// Cách nó hoạt động bên trong (bạn KHÔNG cần gọi):
// 1. Đọc token từ TokenProvider
// 2. Nếu có token → thêm header "Authorization: Bearer <token>"
// 3. Nếu không có → gửi request bình thường (không có header)
```

---

### TokenAuthenticator.java

Tự động chạy **chỉ khi server trả 401**. Không cần gọi thủ công.

```java
// Bên trong:
// 1. Nhận 401 → đọc refresh token
// 2. Gọi refresh API (hiện đang giả lập, có TODO để swap API thật)
// 3. Lưu token mới → retry request gốc
// 4. Giới hạn retry = 2 lần (tránh vòng lặp vô hạn)
```

> **⚠️ TODO cho backend team:** Thay đoạn giả lập trong `authenticate()` bằng API refresh thật khi backend sẵn sàng.

---

### ApiResponse.java

```java
// Tạo response wrapper:
ApiResponse<List<Product>> loading  = ApiResponse.loading();
ApiResponse<List<Product>> success  = ApiResponse.success(productList);
ApiResponse<List<Product>> error    = ApiResponse.error("Network error");

// Sử dụng trong UI (Activity/Fragment):
switch (response.getStatus()) {
    case LOADING: showProgress(); break;
    case SUCCESS: showData(response.getData()); break;
    case ERROR:   showError(response.getMessage()); break;
}
```

---

### ApiClient.java

```java
// Lấy ApiService từ bất kỳ đâu:
ApiService api = ApiClient.getInstance(context).getApiService();

// Hoặc qua AppModule (DI):
ApiService api = new AppModule(context).provideApiService();

// Gọi API:
api.getProducts().enqueue(new Callback<>() { ... });
```

---

## Cách sử dụng từ module khác

### Kịch bản 1: Gọi API lấy danh sách sản phẩm

```java
ApiService api = ApiClient.getInstance(context).getApiService();
api.getProducts().enqueue(new Callback<List<Map<String, Object>>>() {
    @Override
    public void onResponse(Call<...> call, Response<...> response) {
        if (response.isSuccessful()) {
            // Token đã được AuthInterceptor gắn tự động
            // Data đã được Gson parse tự động
            List<Map<String, Object>> products = response.body();
        }
    }

    @Override
    public void onFailure(Call<...> call, Throwable t) {
        // Xử lý lỗi mạng
    }
});
```

### Kịch bản 2: Lưu token sau Login

```java
TokenManager tm = ApiClient.getInstance(context).getTokenManager();
tm.saveAccessToken(loginResponse.getAccessToken());
tm.saveRefreshToken(loginResponse.getRefreshToken());
// Từ giờ mọi request tự có Bearer header!
```

### Kịch bản 3: Logout

```java
TokenManager tm = ApiClient.getInstance(context).getTokenManager();
tm.clearTokens();
// Redirect về AuthActivity
```

### Kịch bản 4: Sử dụng ApiResponse trong ViewModel

```java
public class ProductViewModel extends BaseViewModel {
    private final MutableLiveData<ApiResponse<List<Product>>> products = new MutableLiveData<>();

    public void loadProducts() {
        products.setValue(ApiResponse.loading());
        api.getProducts().enqueue(new Callback<>() {
            @Override
            public void onResponse(...) {
                products.postValue(ApiResponse.success(response.body()));
            }
            @Override
            public void onFailure(...) {
                products.postValue(ApiResponse.error(t.getMessage()));
            }
        });
    }
}
```

---

## String resources

Tất cả chuỗi hiển thị cho người dùng đều nằm trong `res/values/strings.xml`. **Không có hardcoded string nào trong code Java.**

| Key | Giá trị | Sử dụng khi |
|-----|---------|-------------|
| `error_network` | Không thể kết nối mạng... | Mất kết nối Internet |
| `error_server` | Máy chủ gặp sự cố... | Server trả 5xx |
| `error_unauthorized` | Phiên đăng nhập đã hết hạn... | Refresh token thất bại |
| `error_unknown` | Đã xảy ra lỗi không xác định | Lỗi không phân loại được |
| `msg_token_saved` | Token đã được lưu an toàn | Debug/log sau login |
| `msg_token_cleared` | Đã xóa toàn bộ phiên đăng nhập | Sau logout |
| `msg_token_refreshed` | Token đã được làm mới thành công | Sau refresh 401 |
| `label_loading` | Đang tải… | UI loading state |

---

## Unit Tests

File: `app/src/test/java/com/example/aura_pc_app/AuthInterceptorTest.java`

| Test | Mô tả | Kỳ vọng |
|------|--------|---------|
| `withToken_shouldAddAuthorizationHeader` | TokenProvider trả token hợp lệ | Header `Authorization: Bearer <token>` có mặt |
| `withoutToken_shouldNotAddHeader` | TokenProvider trả `null` | Không có header Authorization |
| `withEmptyToken_shouldNotAddHeader` | TokenProvider trả `""` | Không có header Authorization |

### Chạy test

```bash
./gradlew testDebugUnitTest --tests "com.example.aura_pc_app.AuthInterceptorTest"
```

---

## Backend API

| Thông tin | Giá trị |
|-----------|---------|
| **Base URL** | `https://aurapc-backend.onrender.com/api/` |
| **Products** | `GET /products` |
| **Login** | `POST /auth/login` |
| **User** | `GET /users/{id}` |

> **Lưu ý:** Backend host trên Render.com free tier — lần gọi đầu tiên có thể mất ~30s để server "thức dậy" (cold start).

---

## Lưu ý quan trọng

1. **Không tự thêm header Authorization thủ công** — `AuthInterceptor` đã làm tự động.
2. **Không lưu token bằng SharedPreferences thường** — luôn dùng `TokenManager`.
3. **TokenAuthenticator chỉ chạy khi nhận 401** — khác với Interceptor chạy cho mọi request.
4. **Log HTTP chỉ hiện khi chạy Debug** — bản Release tự động tắt log để bảo mật.
5. **MAX_RETRY = 2** — nếu refresh token thất bại 2 lần → `clearTokens()` → user phải login lại.
6. **ApiClient cần Context để khởi tạo** — phải gọi `getInstance(context)` ít nhất 1 lần (đã được gọi trong `App.onCreate()`).
