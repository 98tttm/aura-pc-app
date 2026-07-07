const express = require('express');
const Order = require('../../models/Order');
const { requireAdmin } = require('../../middleware/auth');
const { requirePermission } = require('../../middleware/roleAuth');

const router = express.Router();
router.use(requireAdmin);

/**
 * GET /api/admin/extra-orders/status-summary
 * Trả về counts theo status + tổng giá trị đang xử lý / chờ duyệt.
 */
router.get('/status-summary', requirePermission('orders:read'), async (req, res) => {
  try {
    const counts = await Order.aggregate([{ $group: { _id: '$status', count: { $sum: 1 }, total: { $sum: '$total' } } }]);
    const summary = {
      pending: { count: 0, total: 0 },
      confirmed: { count: 0, total: 0 },
      processing: { count: 0, total: 0 },
      shipped: { count: 0, total: 0 },
      delivered: { count: 0, total: 0 },
      cancelled: { count: 0, total: 0 },
    };
    let grandTotal = 0;
    for (const c of counts) {
      if (summary[c._id]) summary[c._id] = { count: c.count, total: c.total };
      grandTotal += c.count;
    }
    res.json({ byStatus: summary, totalOrders: grandTotal });
  } catch (err) {
    res.status(500).json({ success: false, message: err.message });
  }
});

/**
 * POST /api/admin/extra-orders/bulk-update
 * Body: { orderNumbers: [...], status: 'processing' | 'shipped' }
 * Chỉ cho phép status mà admin flow chấp nhận.
 */
router.post('/bulk-update', requirePermission('orders:write'), async (req, res) => {
  try {
    const { orderNumbers, status } = req.body || {};
    if (!Array.isArray(orderNumbers) || orderNumbers.length === 0) {
      return res.status(400).json({ success: false, message: 'orderNumbers[] không được rỗng' });
    }
    if (!['processing', 'shipped'].includes(status)) {
      return res.status(400).json({ success: false, message: 'Chỉ chấp nhận status: processing, shipped' });
    }
    const updated = [];
    const skipped = [];
    for (const orderNumber of orderNumbers) {
      const order = await Order.findOne({ orderNumber });
      if (!order) { skipped.push({ orderNumber, reason: 'not_found' }); continue; }
      // Logic đơn giản: nếu đang pending -> processing; nếu đang confirmed/processing -> shipped
      const TRANS = { pending: 'processing', confirmed: 'shipped', processing: 'shipped' };
      const expected = TRANS[order.status];
      if (expected !== status) {
        skipped.push({ orderNumber, reason: `cannot transition from ${order.status} to ${status}` });
        continue;
      }
      order.status = status;
      if (status === 'shipped' && !order.shippedAt) order.shippedAt = new Date();
      await order.save();
      updated.push(orderNumber);
    }
    res.json({ success: true, updated, skipped, updatedCount: updated.length });
  } catch (err) {
    res.status(500).json({ success: false, message: err.message });
  }
});

module.exports = router;