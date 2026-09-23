export function formatDate(value) {
  if (!value) return '—'
  return String(value).replace('T', ' ').slice(0, 16)
}

export function statusTone(status) {
  if (['在库', '已通过', 'approved', '已归还'].includes(status)) return 'good'
  if (['已拒绝', 'rejected', '报废'].includes(status)) return 'danger'
  if (['维修中', '逾期', '待审核', '待归还确认', 'pending'].includes(status)) return 'warning'
  return 'neutral'
}
