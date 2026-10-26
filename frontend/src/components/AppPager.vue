<template>
  <el-pagination
    v-model:current-page="query.page"
    v-model:page-size="query.size"
    :total="total"
    :page-sizes="[10, 20, 50]"
    layout="total, sizes, prev, pager, next, jumper"
    class="pager"
    @size-change="$emit('change')"
    @current-change="$emit('change')"
  />
</template>

<script setup>
/** 通用分页条：配合 useCrudPage 的 query/total/loadData 使用 */
defineProps({
  query: { type: Object, required: true },
  total: { type: Number, default: 0 }
})
defineEmits(['change'])
</script>

<style>
.pager {
  margin-top: 16px;
  justify-content: flex-end;
}
</style>
