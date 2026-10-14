import { ElMessage } from 'element-plus'

/**
 * 下载 Blob 响应为文件
 * @param {Blob} blob 二进制数据
 * @param {string} filename 文件名
 */
export function downloadBlob(blob, filename) {
  const url = window.URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = filename
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  window.URL.revokeObjectURL(url)
}

/**
 * 从响应头 Content-Disposition 提取文件名，无则用 fallback
 */
export function getFilenameFromResponse(response, fallback) {
  const disposition = response.headers['content-disposition']
  if (disposition) {
    const match = disposition.match(/filename\*=UTF-8''(.+)/)
    if (match) return decodeURIComponent(match[1])
  }
  return fallback
}

/**
 * 通用 Excel 导出：调用导出接口 → 下载 Blob → 统一成功/失败提示
 * @param {Function} apiFn 导出接口方法（返回 Blob 响应）
 * @param {Object} params 筛选参数
 * @param {string} fallback 默认文件名
 */
export async function exportExcel(apiFn, params, fallback) {
  try {
    const response = await apiFn(params)
    downloadBlob(response.data, getFilenameFromResponse(response, fallback))
    ElMessage.success('导出成功')
  } catch {
    ElMessage.error('导出失败')
  }
}
