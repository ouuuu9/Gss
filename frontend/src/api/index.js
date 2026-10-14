import request from './request'

/* ==================== 认证 ==================== */
export const authApi = {
  login: (data) => request.post('/api/auth/login', data),
  logout: () => request.post('/api/auth/logout'),
  me: () => request.get('/api/auth/me')
}

/* ==================== 用户管理 ==================== */
export const userApi = {
  page: (params) => request.get('/api/users', { params }),
  detail: (id) => request.get(`/api/users/${id}`),
  create: (data) => request.post('/api/users', data),
  update: (id, data) => request.put(`/api/users/${id}`, data),
  delete: (id) => request.delete(`/api/users/${id}`),
  resetPassword: (id, password) => request.put(`/api/users/${id}/password`, { password })
}

/* ==================== 运输公司 ==================== */
export const companyApi = {
  page: (params) => request.get('/api/companies', { params }),
  all: () => request.get('/api/companies/all'),
  detail: (id) => request.get(`/api/companies/${id}`),
  create: (data) => request.post('/api/companies', data),
  update: (id, data) => request.put(`/api/companies/${id}`, data),
  delete: (id) => request.delete(`/api/companies/${id}`)
}

/* ==================== 拖车 ==================== */
export const tractorApi = {
  page: (params) => request.get('/api/tractors', { params }),
  detail: (id) => request.get(`/api/tractors/${id}`),
  create: (data) => request.post('/api/tractors', data),
  update: (id, data) => request.put(`/api/tractors/${id}`, data),
  delete: (id) => request.delete(`/api/tractors/${id}`)
}

/* ==================== 司机 ==================== */
export const driverApi = {
  page: (params) => request.get('/api/drivers', { params }),
  all: () => request.get('/api/drivers/all'),
  detail: (id) => request.get(`/api/drivers/${id}`),
  create: (data) => request.post('/api/drivers', data),
  update: (id, data) => request.put(`/api/drivers/${id}`, data),
  delete: (id) => request.delete(`/api/drivers/${id}`)
}

/* ==================== EIR 单证 ==================== */
export const eirApi = {
  page: (params) => request.get('/api/eir', { params }),
  detail: (id) => request.get(`/api/eir/${id}`),
  create: (data) => request.post('/api/eir', data),
  update: (id, data) => request.put(`/api/eir/${id}`, data),
  cancel: (id) => request.post(`/api/eir/${id}/cancel`),
  dispatch: (id, data) => request.post(`/api/eir/${id}/dispatch`, data),
  batchDispatch: (data) => request.post('/api/eir/batch-dispatch', data),
  cancelDispatch: (id) => request.post(`/api/eir/${id}/cancel-dispatch`),
  export: (params) => request.get('/api/eir/export', { params, responseType: 'blob' })
}

/* ==================== 预约 ==================== */
export const apptApi = {
  page: (params) => request.get('/api/appointments', { params }),
  slots: (params) => request.get('/api/appointments/slots', { params }),
  create: (data) => request.post('/api/appointments', data),
  cancel: (id) => request.post(`/api/appointments/${id}/cancel`),
  cancelByEir: (eirId) => request.post(`/api/appointments/cancel-by-eir/${eirId}`),
  export: (params) => request.get('/api/appointments/export', { params, responseType: 'blob' })
}

/* ==================== 操作审计日志 ==================== */
export const auditApi = {
  page: (params) => request.get('/api/audit-logs', { params })
}

/* ==================== 闸口办单 ==================== */
export const gateApi = {
  checkin: (icCardNo) => request.post('/api/gate/checkin', { icCardNo }),
  checkout: (icCardNo) => request.post('/api/gate/checkout', { icCardNo }),
  audit: (eirId) => request.post('/api/gate/audit', { eirId }),
  recentOps: () => request.get('/api/gate/recent-ops')
}

/* ==================== 收费 ==================== */
export const feeApi = {
  page: (params) => request.get('/api/fees', { params }),
  pay: (id) => request.post(`/api/fees/${id}/pay`),
  batchPay: (ids) => request.post('/api/fees/batch-pay', { ids }),
  report: (params) => request.get('/api/fees/report', { params }),
  export: (params) => request.get('/api/fees/export', { params, responseType: 'blob' })
}

/* ==================== 看板 ==================== */
export const dashboardApi = {
  get: () => request.get('/api/dashboard'),
  screen: () => request.get('/api/dashboard/screen')
}
