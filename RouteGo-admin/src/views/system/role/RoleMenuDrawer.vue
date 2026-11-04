<script setup>
import { nextTick, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { assignRoleMenus, getMenuTree, getRoleMenus } from '@/api/system'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  role: { type: Object, default: null },
})
const emit = defineEmits(['update:modelValue', 'saved'])

const treeRef = ref()
const tree = ref([])
const loading = ref(false)
const saving = ref(false)

const typeLabels = { M: '目录', C: '菜单', F: '按钮' }

function collectLeafIds(nodes, output = []) {
  nodes.forEach((node) => {
    if (!node.children?.length) output.push(node.id)
    else collectLeafIds(node.children, output)
  })
  return output
}

async function load() {
  if (!props.role) return
  loading.value = true
  try {
    const [treeData, checkedIds] = await Promise.all([
      getMenuTree(),
      getRoleMenus(props.role.id),
    ])
    tree.value = treeData || []
    await nextTick()
    const leaves = new Set(collectLeafIds(tree.value))
    treeRef.value?.setCheckedKeys((checkedIds || []).filter((id) => leaves.has(id)))
  } catch (error) {
    ElMessage.error(error.message || '菜单加载失败')
  } finally {
    loading.value = false
  }
}

watch(
  () => props.modelValue,
  (open) => {
    if (open) load()
  }
)

function checkAll() {
  treeRef.value?.setCheckedKeys(collectLeafIds(tree.value))
}

function clearAll() {
  treeRef.value?.setCheckedKeys([])
}

async function submit() {
  saving.value = true
  try {
    const checked = treeRef.value?.getCheckedKeys(false) || []
    const half = treeRef.value?.getHalfCheckedKeys() || []
    await assignRoleMenus(props.role.id, [...new Set([...checked, ...half])])
    ElMessage.success('菜单权限已保存')
    emit('saved')
    emit('update:modelValue', false)
  } catch (error) {
    ElMessage.error(error.message || '保存失败')
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <el-drawer
    :model-value="modelValue"
    title="菜单权限"
    size="480px"
    append-to-body
    @update:model-value="emit('update:modelValue', $event)"
  >
    <div v-loading="loading">
      <div class="mb-2 flex flex-wrap items-center gap-2 border-b border-[#e2e8f0] pb-2">
        <div class="min-w-0">
          <p class="truncate text-xs font-medium text-[#1e293b]">{{ role?.roleName }}</p>
          <p class="font-mono text-[10px] text-[#94a3b8]">{{ role?.roleCode }}</p>
        </div>
        <div class="ml-auto flex gap-1">
          <el-button size="small" @click="checkAll">全选</el-button>
          <el-button size="small" @click="clearAll">清空</el-button>
        </div>
      </div>

      <div class="max-h-[calc(100vh-220px)] overflow-y-auto rounded border border-[#e2e8f0] p-2">
        <el-tree
          ref="treeRef"
          :data="tree"
          show-checkbox
          node-key="id"
          default-expand-all
          :props="{ label: 'menuName', children: 'children' }"
          :expand-on-click-node="false"
        >
          <template #default="{ data }">
            <span class="flex min-w-0 items-center gap-1.5">
              <span class="truncate text-xs text-[#1e293b]">{{ data.menuName }}</span>
              <span
                class="rounded bg-[#f8fafc] px-1 text-[10px] text-[#64748b]"
              >
                {{ typeLabels[data.menuType] || data.menuType }}
              </span>
              <span v-if="data.perms" class="truncate font-mono text-[10px] text-[#94a3b8]">
                {{ data.perms }}
              </span>
            </span>
          </template>
        </el-tree>
      </div>

      <p class="mt-2 text-[10px] leading-4 text-[#94a3b8]">
        勾选菜单或按钮后保存；父级目录会随子级自动半选并一并提交。
      </p>
    </div>

    <template #footer>
      <div class="flex justify-end gap-1">
        <el-button size="small" @click="emit('update:modelValue', false)">取消</el-button>
        <el-button size="small" type="primary" :loading="saving" @click="submit">
          保存菜单权限
        </el-button>
      </div>
    </template>
  </el-drawer>
</template>
