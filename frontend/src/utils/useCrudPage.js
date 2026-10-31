import { reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'

/**
 * 分页列表页共用的状态与操作：查询条件、分页数据、弹窗表单、提交、删除。
 * 13 个页面中有 6 个页面的这部分逻辑完全一致，集中在这里避免每个页面重复一份。
 *
 * @param api 需要提供 page(query)；使用弹窗或删除时还需要 create(form)、update(id, form)、delete(id)
 * @param options.defaultQuery 查询条件初始值（不含 page/size）
 * @param options.defaultForm  返回表单初始值的函数，只有新增/编辑弹窗的页面才需要
 *
 * 页面用法：const { loading, tableData, ... } = useCrudPage(api, {...})，然后在合适时机调用 loadData()。
 */
export function useCrudPage(api, { defaultQuery = {}, defaultForm = null } = {}) {
  const loading = ref(false)
  const submitting = ref(false)
  const tableData = ref([])
  const total = ref(0)
  const dialogVisible = ref(false)
  const formRef = ref()

  // 分页参数与业务筛选条件放在同一个对象里，直接作为请求参数传给后端
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

  // 重置筛选条件：恢复初始值并回到第一页。
  // resetExtraState 用于清理页面自己的控件（如日期范围选择器），与 query 不是同一份状态。
  function resetQuery(resetExtraState) {
    Object.assign(query, { page: 1 }, defaultQuery)
    if (resetExtraState) {
      resetExtraState()
    }
    loadData()
  }

  // 打开弹窗：传 row 表示编辑（先恢复初始值，再用行数据覆盖）；不传表示新增
  function openDialog(row) {
    Object.assign(form, defaultForm(), row || {})
    dialogVisible.value = true
  }

  // 校验通过后提交：有 id 表示更新，否则新增；成功后关闭弹窗并刷新列表
  async function handleSubmit(messages = {}) {
    const valid = await formRef.value.validate().catch(() => false)
    if (!valid) {
      return
    }

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
