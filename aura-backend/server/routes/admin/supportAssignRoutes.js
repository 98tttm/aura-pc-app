const express = require('express');
const { requireAdmin } = require('../../middleware/auth');
const { requirePermission } = require('../../middleware/roleAuth');
const { SupportConversation } = require('../../utils/supportChat');
const { emitSupportConversationUpdated } = require('../../socket');

const router = express.Router();
router.use(requireAdmin);

/**
 * POST /api/admin/support-assign/:conversationId
 * Body: { adminId: '...' } — có thể null để bỏ phân công.
 */
router.post('/:conversationId', requirePermission('support:write'), async (req, res) => {
  try {
    const { adminId } = req.body || {};
    const conversation = await SupportConversation.findByIdAndUpdate(
      req.params.conversationId,
      { $set: { assignedAdmin: adminId || null } },
      { new: true }
    )
      .populate('user', 'phoneNumber email username avatar profile.fullName')
      .populate('assignedAdmin', 'email name avatar role')
      .lean();

    if (!conversation) return res.status(404).json({ success: false, message: 'Conversation không tồn tại' });

    const { serializeConversation } = require('../../utils/supportChat');
    const serialized = serializeConversation(conversation);
    emitSupportConversationUpdated(serialized);
    res.json({ success: true, conversation: serialized });
  } catch (err) {
    res.status(500).json({ success: false, message: err.message });
  }
});

module.exports = router;