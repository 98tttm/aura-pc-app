const express = require('express');
const Order = require('../../models/Order');
const User = require('../../models/User');
const { requireAdmin } = require('../../middleware/auth');
const { requirePermission } = require('../../middleware/roleAuth');

const router = express.Router();
router.use(requireAdmin);

/**
 * GET /api/admin/extra-users/segment
 * Phân nhóm khách hàng theo tổng chi tiêu.
 */
router.get('/segment', requirePermission('users:read'), async (req, res) => {
  try {
    const segments = await Order.aggregate([
      { $match: { status: 'delivered' } },
      { $group: { _id: '$user', totalSpent: { $sum: '$total' }, orderCount: { $sum: 1 } } },
    ]);

    const map = { bronze: [], silver: [], gold: [], vip: [] };
    const userIds = segments.map((s) => s._id).filter(Boolean);

    for (const seg of segments) {
      const bucket =
        seg.totalSpent >= 50_000_000 ? 'vip' :
        seg.totalSpent >= 20_000_000 ? 'gold' :
        seg.totalSpent >= 5_000_000 ? 'silver' : 'bronze';
      map[bucket].push({
        userId: seg._id,
        totalSpent: seg.totalSpent,
        orderCount: seg.orderCount,
      });
    }

    const summary = Object.entries(map).map(([name, users]) => ({
      name,
      count: users.length,
      totalRevenue: users.reduce((acc, u) => acc + u.totalSpent, 0),
    }));

    const newCustomers = await User.countDocuments({
      createdAt: { $gte: new Date(Date.now() - 30 * 24 * 60 * 60 * 1000) },
    });

    res.json({
      summary,
      totalUsers: segments.length,
      totalCustomers: await User.countDocuments({}),
      newCustomersLast30Days: newCustomers,
      segments: map,
    });
  } catch (err) {
    res.status(500).json({ success: false, message: err.message });
  }
});

module.exports = router;