const express = require('express');
const Admin = require('../../models/Admin');
const { requireRole } = require('../../middleware/roleAuth');
const { requireAdmin } = require('../../middleware/auth');

const router = express.Router();
router.use(requireAdmin);

/**
 * GET /api/admin/admin-users
 * Chỉ super_admin mới xem được danh sách admin khác.
 */
router.get('/', requireRole('super_admin'), async (req, res) => {
  try {
    const { page = 1, limit = 20, search } = req.query;
    const filter = {};
    if (search && search.trim()) {
      const esc = search.trim().replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
      filter.$or = [
        { email: new RegExp(esc, 'i') },
        { name: new RegExp(esc, 'i') },
      ];
    }
    const skip = (Math.max(1, parseInt(page, 10)) - 1) * parseInt(limit, 10);
    const [items, total] = await Promise.all([
      Admin.find(filter).select('-password').sort({ createdAt: -1 }).skip(skip).limit(parseInt(limit, 10)).lean(),
      Admin.countDocuments(filter),
    ]);
    res.json({ items, total, page: parseInt(page, 10), limit: parseInt(limit, 10), totalPages: Math.ceil(total / parseInt(limit, 10)) });
  } catch (err) {
    res.status(500).json({ success: false, message: err.message });
  }
});

/** POST /api/admin/admin-users — tạo admin mới (chỉ super_admin) */
router.post('/', requireRole('super_admin'), async (req, res) => {
  try {
    const { email, password, name, role, permissions, isActive } = req.body;
    if (!email || !password) {
      return res.status(400).json({ success: false, message: 'Email và mật khẩu là bắt buộc' });
    }
    const existing = await Admin.findOne({ email: email.toLowerCase().trim() });
    if (existing) {
      return res.status(400).json({ success: false, message: 'Email đã tồn tại' });
    }
    if (role && !Admin.ROLES.includes(role)) {
      return res.status(400).json({ success: false, message: `Role không hợp lệ. Phải là một trong: ${Admin.ROLES.join(', ')}` });
    }
    const admin = new Admin({
      email: email.toLowerCase().trim(),
      password,
      name: name || 'Admin',
      role: role || 'super_admin',
      permissions: Array.isArray(permissions) ? permissions : [],
      isActive: isActive !== false,
      createdBy: req.adminId,
    });
    await admin.save();
    res.status(201).json({ success: true, admin: { ...admin.toObject(), password: undefined } });
  } catch (err) {
    res.status(500).json({ success: false, message: err.message });
  }
});

/** PUT /api/admin/admin-users/:id — cập nhật admin */
router.put('/:id', requireRole('super_admin'), async (req, res) => {
  try {
    const { name, role, permissions, isActive, password } = req.body;
    const update = {};
    if (typeof name === 'string') update.name = name;
    if (role) {
      if (!Admin.ROLES.includes(role)) {
        return res.status(400).json({ success: false, message: `Role không hợp lệ` });
      }
      update.role = role;
    }
    if (Array.isArray(permissions)) update.permissions = permissions;
    if (typeof isActive === 'boolean') update.isActive = isActive;
    if (password) update.password = password;

    const admin = await Admin.findById(req.params.id);
    if (!admin) return res.status(404).json({ success: false, message: 'Không tìm thấy admin' });

    // Không cho hạ cấp chính mình
    if (admin._id.toString() === req.adminId.toString() && update.isActive === false) {
      return res.status(400).json({ success: false, message: 'Không thể tự khóa tài khoản của mình' });
    }

    Object.assign(admin, update);
    await admin.save();
    res.json({ success: true, admin: { ...admin.toObject(), password: undefined } });
  } catch (err) {
    res.status(500).json({ success: false, message: err.message });
  }
});

/** DELETE /api/admin/admin-users/:id — xóa admin */
router.delete('/:id', requireRole('super_admin'), async (req, res) => {
  try {
    if (req.params.id === req.adminId.toString()) {
      return res.status(400).json({ success: false, message: 'Không thể tự xóa tài khoản' });
    }
    const admin = await Admin.findByIdAndDelete(req.params.id);
    if (!admin) return res.status(404).json({ success: false, message: 'Không tìm thấy admin' });
    res.json({ success: true });
  } catch (err) {
    res.status(500).json({ success: false, message: err.message });
  }
});

/** GET /api/admin/admin-users/roles — trả về danh sách role hợp lệ */
router.get('/meta/roles', (req, res) => {
  res.json({ roles: Admin.ROLES, permissions: Admin.ROLE_PERMISSIONS });
});

module.exports = router;