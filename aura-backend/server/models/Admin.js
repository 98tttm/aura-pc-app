const mongoose = require('mongoose');
const bcrypt = require('bcryptjs');

/**
 * Admin user schema.
 * Roles:
 *   - super_admin: toàn quyền (mọi thao tác)
 *   - order_manager: đơn hàng + user (read-only) + analytics
 *   - product_manager: sản phẩm + danh mục + blog + hub moderation
 *   - support_agent: hỗ trợ khách hàng + warranty
 */
const ROLES = ['super_admin', 'order_manager', 'product_manager', 'support_agent'];

const adminSchema = new mongoose.Schema(
  {
    email: { type: String, required: true, unique: true, trim: true, lowercase: true },
    password: { type: String, required: true },
    name: { type: String, default: 'Admin' },
    role: {
      type: String,
      enum: ROLES,
      default: 'super_admin',
    },
    permissions: { type: [String], default: [] },
    avatar: { type: String, default: '' },
    isActive: { type: Boolean, default: true },
    lastLogin: { type: Date, default: null },
    createdBy: { type: mongoose.Schema.Types.ObjectId, ref: 'Admin', default: null },
  },
  { timestamps: true }
);

adminSchema.pre('save', async function () {
  if (!this.isModified('password')) return;
  this.password = await bcrypt.hash(this.password, 10);
});

adminSchema.methods.comparePassword = function (candidate) {
  return bcrypt.compare(candidate, this.password);
};

adminSchema.methods.hasRole = function (...roles) {
  return roles.includes(this.role);
};

/**
 * Map role -> default permissions (extra fine-grained scope on top of role gate).
 */
const ROLE_PERMISSIONS = {
  super_admin: ['*'],
  order_manager: [
    'orders:read', 'orders:write', 'orders:cancel',
    'users:read',
    'analytics:read',
    'notifications:read',
  ],
  product_manager: [
    'products:read', 'products:write', 'products:delete',
    'categories:read', 'categories:write',
    'blogs:read', 'blogs:write', 'blogs:delete',
    'hub:moderate',
    'promotions:read', 'promotions:write',
    'analytics:read',
  ],
  support_agent: [
    'support:read', 'support:write',
    'warranty:read', 'warranty:write',
    'orders:read',
    'users:read',
    'notifications:read',
  ],
};

adminSchema.statics.ROLE_PERMISSIONS = ROLE_PERMISSIONS;
adminSchema.statics.ROLES = ROLES;

module.exports = mongoose.model('Admin', adminSchema);