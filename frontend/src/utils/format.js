/**
 * 格式化后端 LocalDateTime 字符串：2026-10-12T08:30:00 → 2026-10-12 08:30:00
 * 空值显示占位符
 */
export function formatTime(t) {
  if (!t) return '—'
  return t.replace('T', ' ').substring(0, 19)
}
