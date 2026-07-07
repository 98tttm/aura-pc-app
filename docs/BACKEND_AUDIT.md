# AuraPC Admin Backend — Audit & Extension

## 1. Endpoints `/api/admin/*` hiện có

| Method | Endpoint | Mô tả |
|--------|----------|-------|
| **Auth** |
| POST | `/api/admin/auth/login` | Đăng nhập admin email/password → JWT (isAdmin: true) |
| GET | `/api/admin/auth/me` | Lấy thông tin admin hiện tại |
| POST | `/api/admin/auth/seed` | Seed admin đầu tiên (bảo vệ bằng ADMIN_SEED_SECRET) |
| **Dashboard** |
| GET | `/api/admin/dashboard/stats` | Tổng quan: revenue (total/thisMonth/lastMonth), orders, users, products, recentOrders, ordersByStatus |
| GET | `/api/admin/dashboard/chart/orders` | Đếm đơn hàng + revenue theo ngày (7 ngày mặc định) |
| GET | `/api/admin/dashboard/chart/revenue` | Revenue + orders + newCustomers theo tháng (12 tháng mặc định) |
| GET | `/api/admin/dashboard/chart/revenue-weekly` | Revenue + orders theo tuần trong tháng hiện tại |
| GET | `/api/admin/dashboard/top-products` | Top sản phẩm bán chạy |
| **Products** |
| GET | `/api/admin/products` | List + filter (search, category, brand, stockStatus), pagination |
| GET | `/api/admin/products/category-stats` | Đếm sản phẩm theo root category |
| GET | `/api/admin/products/brands` | Danh sách brand |
| GET | `/api/admin/products/stock-stats` | Phân bố tồn kho |
| GET | `/api/admin/products/:id` | Chi tiết sản phẩm |
| POST | `/api/admin/products` | Tạo sản phẩm |
| PUT | `/api/admin/products/:id` | Cập nhật |
| DELETE | `/api/admin/products/:id` | Xóa |
| **Orders** |
| GET | `/api/admin/orders` | List + filter (status, from/to, search), pagination |
| GET | `/api/admin/orders/:orderNumber` | Chi tiết |
| PUT | `/api/admin/orders/:orderNumber/status` | Cập nhật status (chỉ processing/shipped) |
| PUT | `/api/admin/orders/:orderNumber/cancel` | Hủy đơn (admin) |
| PUT | `/api/admin/orders/:orderNumber/cancel-request` | Duyệt yêu cầu hủy |
| PUT | `/api/admin/orders/:orderNumber/return-request` | Duyệt yêu cầu hoàn trả |
| **Users** |
| GET | `/api/admin/users` | List + search, có orderCount |
| GET | `/api/admin/users/:id` | Chi tiết + recentOrders + totalSpent |
| **Categories** |
| GET | `/api/admin/categories` | List |
| GET/POST/PUT/DELETE | `/api/admin/categories[/:id]` | CRUD |
| **Blogs** |
| GET/POST/PUT/DELETE | `/api/admin/blogs[/:id]` | CRUD blog |
| POST | `/api/admin/blogs/backfill-covers` | Backfill ảnh bìa từ content HTML |
| **Hub (AuraHub)** |
| GET | `/api/admin/hub/posts` | List bài community (status filter, search, sort) |
| GET | `/api/admin/hub/posts/:id` | Chi tiết bài |
| PATCH | `/api/admin/hub/posts/:id/approve` | Duyệt bài |
| PATCH | `/api/admin/hub/posts/:id/reject` | Từ chối |
| DELETE | `/api/admin/hub/posts/:id` | Xóa |
| GET | `/api/admin/hub/posts/:id/comments` | Comments + replies |
| DELETE | `/api/admin/hub/comments/:id` | Xóa comment |
| **Support** |
| GET | `/api/admin/support` | List conversations (tab open/archived, search) |
| GET | `/api/admin/support/:conversationId` | Chi tiết + messages |
| PUT | `/api/admin/support/:conversationId/read` | Đánh dấu đã đọc |
| PUT | `/api/admin/support/:conversationId/archive` | Archive/unarchive |
| POST | `/api/admin/support/:conversationId/messages` | Gửi tin nhắn |
| **Warranty** |
| GET | `/api/admin/warranty` | List serial + filter status/search |
| GET | `/api/admin/warranty/stats` | Tổng số / valid / expired |
| **Promotions** |
| GET/POST/PUT/DELETE | `/api/admin/promotions[/:id]` | CRUD |
| **Notifications** |
| GET | `/api/admin/notifications` | List + unreadCount |
| PATCH | `/api/admin/notifications/:id/read` | Mark 1 đã đọc |
| PATCH | `/api/admin/notifications/read-all` | Mark all đã đọc |

## 2. Mongoose Models đang sử dụng

| Model | Trường chính |
|-------|--------------|
| Admin | email, password (bcrypt), name, role, avatar, lastLogin |
| User | phoneNumber, email, username, profile, addresses, avatar, active, social (googleId/facebookId), followers |
| Product | name, slug, price, salePrice, category (Mixed), category_id, category_ids, images (Mixed), specs, brand, stock, active, warrantyMonths |
| Order | orderNumber, user, items[], shippingAddress, status, total, shippingFee, discount, appliedPromotion, paymentMethod, isPaid, cancelRequest, returnRequest |
| Blog | coverImage, content, title, author, status... |
| Category | name, slug, category_id, parent_id |
| Promotion | code, description, discountPercent, maxDiscountAmount, minOrderAmount, maxUsage, startDate, endDate, isActive |
| SupportConversation | user, assignedAdmin, archived, unreadForAdmin, lastMessagePreview, lastMessageAt |
| SupportMessage | conversation, senderType, senderAdmin, content |
| AdminNotification | readBy[], type, title, message, metadata |
| UserNotification | user, type, title, message, read |
| Post (Hub) | author, content, status, topic, likeCount, commentCount |
| HubComment | post, author, parentComment, content |
| Share | post, user |
| Builder, Otp, PendingPayment, ProductReview, Faq | hỗ trợ thêm |

## 3. Hạn chế cần mở rộng

1. **Pagination/filter response không đồng nhất**: một số endpoint trả `{items, total, page, limit}`, số khác trả `{items, total, totalPages, limit}`. Cần chuẩn hóa.
2. **Thiếu các endpoint analytics nâng cao**:
   - `/api/admin/stats/overview` — đã có `/dashboard/stats`
   - `/api/admin/stats/revenue` — đã có `/dashboard/chart/revenue`
   - `/api/admin/stats/top-products` — đã có
   - **Thiếu**: `/api/admin/products/low-stock` (list), `/api/admin/orders/status-summary`, `/api/admin/products/bulk-update`, `/api/admin/orders/bulk-update`, `/api/admin/users/segment`, `/api/admin/reviews/flagged`, `/api/admin/support/assign`
3. **Role-based access**: middleware `requireAdmin` chỉ check `isAdmin: true`. Admin model mới chỉ có role mặc định `'admin'`. Cần thêm 4 role: super_admin / order_manager / product_manager / support_agent.
4. **Admin users CRUD**: không có endpoint tạo/sửa/xóa admin user (chỉ có seed).

## 4. Mở rộng thực hiện

- Mở rộng Admin model với `permissions[]`, `isActive`, role enum
- Thêm `requireRole(...)` middleware
- Endpoint `/api/admin/users` (admin users) CRUD
- `/api/admin/products/low-stock` — list sản phẩm sắp hết
- `/api/admin/orders/status-summary` — counts theo status
- `/api/admin/products/bulk-update` — cập nhật hàng loạt
- `/api/admin/orders/bulk-update` — bulk status update
- `/api/admin/users/segment` — phân nhóm theo tổng chi tiêu
- `/api/admin/reviews/flagged` — đánh giá flagged (ProductReview có field reported)
- `/api/admin/support/assign` — phân công admin