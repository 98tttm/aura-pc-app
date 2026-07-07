const express = require('express');
const User = require('../../models/User');
const Order = require('../../models/Order');
const { requireAdmin } = require('../../middleware/auth');

const router = express.Router();
router.use(requireAdmin);

/** GET / - user list with pagination/search plus order stats. */
router.get('/', async (req, res) => {
  try {
    const { page = 1, limit = 20, search } = req.query;
    const pageNum = Math.max(1, parseInt(page, 10) || 1);
    const limitNum = Math.max(1, Math.min(100, parseInt(limit, 10) || 20));
    const filter = {};

    if (search && search.trim()) {
      const escaped = search.trim().replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
      filter.$or = [
        { phoneNumber: new RegExp(escaped, 'i') },
        { 'profile.fullName': new RegExp(escaped, 'i') },
        { email: new RegExp(escaped, 'i') },
        { username: new RegExp(escaped, 'i') },
      ];
    }

    const skip = (pageNum - 1) * limitNum;
    const [items, total] = await Promise.all([
      User.find(filter)
        .select('-__v')
        .sort({ createdAt: -1 })
        .skip(skip)
        .limit(limitNum)
        .lean(),
      User.countDocuments(filter),
    ]);

    const userIds = items.map((u) => u._id);
    const orderStats = await Order.aggregate([
      { $match: { user: { $in: userIds } } },
      {
        $group: {
          _id: '$user',
          orderCount: { $sum: 1 },
          totalSpent: {
            $sum: {
              $cond: [{ $eq: ['$status', 'delivered'] }, '$total', 0],
            },
          },
        },
      },
    ]);
    const statsMap = orderStats.reduce((acc, o) => {
      acc[o._id.toString()] = {
        orderCount: o.orderCount || 0,
        totalSpent: o.totalSpent || 0,
      };
      return acc;
    }, {});

    const enriched = items.map((u) => ({
      ...u,
      orderCount: statsMap[u._id.toString()]?.orderCount || 0,
      totalSpent: statsMap[u._id.toString()]?.totalSpent || 0,
    }));

    res.json({ items: enriched, total, page: pageNum, limit: limitNum });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

/** GET /:id - user detail plus recent orders and spend. */
router.get('/:id', async (req, res) => {
  try {
    const user = await User.findById(req.params.id).select('-__v').lean();
    if (!user) return res.status(404).json({ error: 'User not found' });

    const recentOrders = await Order.find({ user: req.params.id })
      .sort({ createdAt: -1 })
      .limit(10)
      .lean();

    const totalSpent = await Order.aggregate([
      { $match: { user: user._id, status: 'delivered' } },
      { $group: { _id: null, sum: { $sum: '$total' } } },
    ]);

    res.json({
      ...user,
      recentOrders,
      totalSpent: totalSpent[0]?.sum || 0,
      orderCount: await Order.countDocuments({ user: user._id }),
    });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

/** PUT /:id/status - lock/unlock a user account. */
router.put('/:id/status', async (req, res) => {
  try {
    const isActive = req.query.active === 'true' || req.body?.active === true || req.body?.isActive === true;
    const user = await User.findByIdAndUpdate(
      req.params.id,
      { isActive, active: isActive },
      { new: true, runValidators: true }
    ).select('-__v');

    if (!user) return res.status(404).json({ error: 'User not found' });
    res.json(user);
  } catch (err) {
    res.status(400).json({ error: err.message });
  }
});

module.exports = router;
