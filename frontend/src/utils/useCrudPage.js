import { reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'

/**
 * CRUD 分页页面通用逻辑：查询条件、分页加载、重置、弹窗表单、提交、删除。
 *
 * @param api     需含 page(query)；若用弹窗/删除，还需 create(form) / update(id, form) / delete(id)
 * @param options.defaultQuery 查询字段默认值（不含 page/size）
 * @param options.defaultForm  () => 表单默认值，仅在需要新增/编辑弹窗时提供
 *
 * 页面使用：const { loading, tableData, ... } = useCrudPage(driverApi, {...})，然后调用 loadData()。
 */
export function useCrudPage(api, { defaultQuery = {}, defaultForm = null } = {}) {
  const loading = ref(false)
  const submitting = ref(false)
  const tableData = ref([])
  const total = ref(0)
  const dialogVisible = ref(false)
  const formRef = ref()

  const query = reactive({ page: 1, size: 10, ...defaultQuery })
  const form = defaultForm ? reactive(defaultForm()) : null

  async function loadData() {
    loading.value = true
    try {
      const data = await api.page(query)
      tableData.value = data.rows
      total.value = data.total
    } finally {
      loading.value = false
    }
  }

  /** 重置查询条件并重新加载（保留用户选择的每页条数）；extra 用于清理页面自有状态（如日期范围选择器） */
  function resetQuery(extra) {
    Object.assign(query, { page: 1 }, defaultQuery)
    if (extra) extra()
    loadData()
  }

  /** 打开弹窗：传 row 为编辑（复制行数据到表单），不传为新增 */
  function openDialog(row) {
    Object.assign(form, defaultForm(), row || {})
    dialogVisible.value = true
  }

  /** 校验并提交表单（有 id 走更新，否则新增），成功后关闭弹窗并刷新列表 */
  async function handleSubmit(messages = {}) {
    const valid = await formRef.value.validate().catch(() => false)
    if (!valid) return
    submitting.value = true
    try {
      if (form.id) {
        await api.update(form.id, form)
        ElMessage.success(messages.update || '修改成功')
      } else {
        await api.create(form)
        ElMessage.success(messages.create || '新增成功')
      }
      dialogVisible.value = false
      loadData()
    } finally {
      submitting.value = false
    }
  }

  async function handleDelete(row, message = '删除成功') {
    await api.delete(row.id)
    ElMessage.success(message)
    loadData()
  }

  return {
    loading, submitting, tableData, total, dialogVisible, formRef, query, form,
    loadData, resetQuery, openDialog, handleSubmit, handleDelete
  }
}
