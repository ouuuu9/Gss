// 业务字典：状态/类型的中文文案与标签颜色，供表格、筛选器统一使用

// 单证状态
export const EIR_STATUS = [
  { value: 'CREATED', label: '已录入', type: 'info' },
  { value: 'SUBMITTED', label: '待审核', type: 'warning' },
  { value: 'APPROVED', label: '审核通过', type: 'success' },
  { value: 'REJECTED', label: '已驳回', type: 'danger' },
  { value: 'DISPATCHED', label: '已派单', type: 'primary' },
  { value: 'APPOINTED', label: '已预约', type: 'warning' },
  { value: 'GATE_IN', label: '已进闸', type: 'primary' },
  { value: 'GATE_OUT', label: '已出闸', type: 'success' },
  { value: 'DONE', label: '已完成', type: 'success' },
  { value: 'CANCELLED', label: '已作废', type: 'danger' }
]

// 办单审核状态（review = 办单审核，与闸口审单 audit 区分）
export const REVIEW_STATUS = [
  { value: 'PENDING', label: '待审核', type: 'warning' },
  { value: 'APPROVED', label: '已通过', type: 'success' },
  { value: 'REJECTED', label: '已驳回', type: 'danger' }
]

// 业务类型
export const BUSINESS_TYPE = [
  { value: 'PI-F', label: '提重', type: 'primary' },
  { value: 'PI-E', label: '提空', type: 'info' },
  { value: 'GR-E', label: '还空', type: 'warning' },
  { value: 'GR-F', label: '还重', type: 'danger' }
]

// 预约状态
export const APPT_STATUS = [
  { value: 'ACTIVE', label: '有效', type: 'success' },
  { value: 'CANCELLED', label: '已取消', type: 'info' },
  { value: 'EXPIRED', label: '已过期', type: 'warning' }
]

// 缴费状态（2=已退）
export const PAY_STATUS = [
  { value: 0, label: '未缴', type: 'danger' },
  { value: 1, label: '已缴', type: 'success' },
  { value: 2, label: '已退', type: 'info' }
]

// 退款状态
export const REFUND_STATUS = [
  { value: 'PENDING', label: '待审批', type: 'warning' },
  { value: 'APPROVED', label: '已退款', type: 'success' },
  { value: 'REJECTED', label: '已驳回', type: 'danger' }
]

// 系统角色
export const ROLE = [
  { value: 'ADMIN', label: '码头管理员' },
  { value: 'GATE', label: '闸口办单员' },
  { value: 'COMPANY', label: '运输公司用户' },
  { value: 'DRIVER', label: '司机' }
]

// 通用字典查找：找不到时标签回退为原值，颜色回退为空
export function dictLabel(list, value) {
  const item = list.find((d) => d.value === value)
  return item?.label ?? value
}

export function dictType(list, value) {
  const item = list.find((d) => d.value === value)
  return item?.type ?? ''
}
