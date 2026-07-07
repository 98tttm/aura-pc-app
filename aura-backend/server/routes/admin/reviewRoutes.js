const express = require('express');
const ProductReview = require('../../models/ProductReview');
const { requireAdmin } = require('../../middleware/auth');
const { requirePermission } = require('../../middleware/roleAuth');

const router = express.Router();
router.use(requireAdmin);

/**
 * GET /api/admin/reviews/flagged
 * Danh sách review cần kiểm duyệt.
 */
router.get('/flagged', requirePermission('products:read'), async (req, res) => {
  try {
    const { page = 1, limit = 20 } = req.query;
    const skip = (Math.max(1, parseInt(page, 10)) - 1) * parseInt(limit, 10);
    const [items, total] = await Promise.all([
      ProductReview.find({ $or: [{ flagged: true }, { hidden: true }] })
        .populate('user', 'phoneNumber email profile.fullName avatar')
        .populate('product', 'name slug images')
        .populate('moderatedBy', 'email name')
        .sort({ updatedAt: -1 })
        .skip(skip)
        .limit(parseInt(limit, 10))
        .lean(),
      ProductReview.countDocuments({ $or: [{ flagged: true }, { hidden: true }] }),
    ]);
    res.json({ items, total, page: parseInt(page, 10), limit: parseInt(limit, 10), totalPages: Math.ceil(total / parseInt(limit, 10)) });
  } catch (err) {
    res.status(500).json({ success: false, message: err.message });
  }
});

/** POST /api/admin/reviews/:id/flag — đánh dấu review cần kiểm duyệt */
router.post('/:id/flag', requirePermission('products:write'), async (req, res) => {
  try {
    const { reason } = req.body || {};
    const review = await ProductReview.findByIdAndUpdate(
      req.params.id,
      { flagged: true, flagReason: typeof reason === 'string' ? reason.slice(0, 300) : '' },
      { new: true }
    ).lean();
    if (!review) return res.status(404).json({ success: false, message: 'Review không tồn tại' });
    res.json({ success: true, review });
  } catch (err) {
    res.status(500).json({ success: false, message: err.message });
  }
});

/** POST /api/admin/reviews/:id/hide — ẩn review */
router.post('/:id/hide', requirePermission('products:write'), async (req, res) => {
  try {
    const review = await ProductReview.findByIdAndUpdate(
      req.params.id,
      { hidden: true, moderatedBy: req.adminId, moderatedAt: new Date() },
      { new: true }
    ).lean();
    if (!review) return res.status(404).json({ success: false, message: 'Review không tồn tại' });
    res.json({ success: true, review });
  } catch (err) {
    res.status(500).json({ success: false, message: err.message });
  }
});

/** POST /api/admin/reviews/:id/restore — bỏ cờ, hiện lại */
router.post('/:id/restore', requirePermission('products:write'), async (req, res) => {
  try {
    const review = await ProductReview.findByIdAndUpdate(
      req.params.id,
      { flagged: false, hidden: false, flagReason: '', moderatedBy: req.adminId, moderatedAt: new Date() },
      { new: true }
    ).lean();
    if (!review) return res.status(404).json({ success: false, message: 'Review không tồn tại' });
    res.json({ success: true, review });
  } catch (err) {
    res.status(500).json({ success: false, message: err.message });
  }
});

module.exports = router;