const Admin = require('../models/Admin');

const { signToken, verifyToken } = require('./auth');

/**
 * Middleware: requireAdmin
 *  - Đã có ở middleware/auth.js — giữ nguyên cho backward compat.
 *  - Đặt req.adminRole để downstream dùng.
 */
function loadAdminFromToken(req) {
  const decoded = verifyToken(req);
  if (!decoded || !decoded.isAdmin || !decoded.adminId) return null;
  return decoded;
}

/**
 * Middleware: requireRole(...roles)
 *  - Cho phép nhiều role.
 *  - super_admin luôn được phép.
 */
function requireRole(...roles) {
  return async (req, res, next) => {
    try {
      const decoded = loadAdminFromToken(req);
      if (!decoded) {
        return res.status(401).json({ success: false, message: 'Unauthorized - admin access required' });
      }
      const admin = await Admin.findById(decoded.adminId).lean();
      if (!admin || !admin.isActive) {
        return res.status(401).json({ success: false, message: 'Admin account disabled' });
      }
      if (admin.role !== 'super_admin' && !roles.includes(admin.role)) {
        return res.status(403).json({
          success: false,
          message: `Forbidden - requires one of roles: ${roles.join(', ')}`,
        });
      }
      req.adminId = admin._id;
      req.adminRole = admin.role;
      req.admin = admin;
      next();
    } catch (err) {
      res.status(500).json({ success: false, message: err.message });
    }
  };
}

/**
 * Middleware: requirePermission(perm)
 *  - Check theo permissions[] hoặc role mặc định.
 *  - super_admin luôn pass.
 */
function requirePermission(perm) {
  return async (req, res, next) => {
    try {
      const decoded = loadAdminFromToken(req);
      if (!decoded) {
        return res.status(401).json({ success: false, message: 'Unauthorized - admin access required' });
      }
      const admin = await Admin.findById(decoded.adminId).lean();
      if (!admin || !admin.isActive) {
        return res.status(401).json({ success: false, message: 'Admin account disabled' });
      }
      if (admin.role === 'super_admin') {
        req.adminId = admin._id;
        req.adminRole = admin.role;
        req.admin = admin;
        return next();
      }
      const defaults = Admin.ROLE_PERMISSIONS[admin.role] || [];
      const allPerms = new Set([...defaults, ...(admin.permissions || [])]);
      if (!allPerms.has(perm) && !allPerms.has('*')) {
        return res.status(403).json({
          success: false,
          message: `Forbidden - missing permission: ${perm}`,
        });
      }
      req.adminId = admin._id;
      req.adminRole = admin.role;
      req.admin = admin;
      next();
    } catch (err) {
      res.status(500).json({ success: false, message: err.message });
    }
  };
}

/**
 * Optional admin — đặt req.admin nếu có token admin hợp lệ, không thì next().
 */
function optionalAdmin(req, res, next) {
  const decoded = loadAdminFromToken(req);
  if (!decoded) return next();
  Admin.findById(decoded.adminId).lean().then((admin) => {
    if (admin && admin.isActive) {
      req.adminId = admin._id;
      req.adminRole = admin.role;
      req.admin = admin;
    }
    next();
  }).catch(() => next());
}

module.exports = { requireRole, requirePermission, optionalAdmin, signToken };