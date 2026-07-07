const express = require('express');
const Product = require('../../models/Product');
const { requireAdmin } = require('../../middleware/auth');
const { requirePermission } = require('../../middleware/roleAuth');

const router = express.Router();
router.use(requireAdmin);

/**
 * GET /api/admin/extra-products/low-stock
 * List sản phẩm có stock thấp (< threshold).
 */
router.get('/low-stock', requirePermission('products:read'), async (req, res) => {
  try {
    const threshold = Math.max(0, parseInt(req.query.threshold, 10) || 10);
    const { page = 1, limit = 20 } = req.query;
    const filter = { stock: { $gt: 0, $lt: threshold } };
    const skip = (Math.max(1, parseInt(page, 10)) - 1) * parseInt(limit, 10);
    const [items, total] = await Promise.all([
      Product.find(filter).sort({ stock: 1, updatedAt: -1 }).skip(skip).limit(parseInt(limit, 10)).lean(),
      Product.countDocuments(filter),
    ]);
    res.json({ items, total, page: parseInt(page, 10), limit: parseInt(limit, 10), totalPages: Math.ceil(total / parseInt(limit, 10)), threshold });
  } catch (err) {
    res.status(500).json({ success: false, message: err.message });
  }
});

/**
 * POST /api/admin/extra-products/bulk-update
 * Body: { ids: [...], patch: { price?, salePrice?, stock?, active?, category?, ...} }
 */
router.post('/bulk-update', requirePermission('products:write'), async (req, res) => {
  try {
    const { ids, patch } = req.body || {};
    if (!Array.isArray(ids) || ids.length === 0) {
      return res.status(400).json({ success: false, message: 'ids[] không được rỗng' });
    }
    if (!patch || typeof patch !== 'object' || Object.keys(patch).length === 0) {
      return res.status(400).json({ success: false, message: 'patch không được rỗng' });
    }
    const ALLOWED = ['price', 'salePrice', 'old_price', 'stock', 'active', 'featured', 'brand', 'category_id', 'category_ids', 'primaryCategoryId', 'categoryIds', 'warrantyMonths'];
    const safePatch = {};
    for (const k of Object.keys(patch)) {
      if (ALLOWED.includes(k)) safePatch[k] = patch[k];
    }
    if (Object.keys(safePatch).length === 0) {
      return res.status(400).json({ success: false, message: 'Không có trường hợp lệ trong patch' });
    }
    safePatch.updatedAt = new Date();
    const result = await Product.updateMany({ _id: { $in: ids } }, { $set: safePatch });
    res.json({ success: true, modifiedCount: result.modifiedCount || 0, matchedCount: result.matchedCount || ids.length });
  } catch (err) {
    res.status(500).json({ success: false, message: err.message });
  }
});

module.exports = router;