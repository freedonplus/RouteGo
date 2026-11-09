<script setup>
import { computed, nextTick, reactive, ref, watch } from 'vue'
import { createMenu, getMenu, getMenuTree, updateMenu } from '@/api/system'
import { resolveMenuIcon } from '@/components/menu-icons'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  mode: { type: String, default: 'create' },
  menuId: { type: [Number, String], default: null },
  parentId: { type: [Number, String], default: 0 },
})
const emit = defineEmits(['update:modelValue', 'saved'])

const iconOptions = [
  { value: 'Settings', label: 'Settings 设置' },
  { value: 'User', label: 'User 用户' },
  { value: 'Team', label: 'Team 角色' },
  { value: 'Menu', label: 'Menu 菜单' },
  { value: 'Key', label: 'Key 权限' },
  { value: 'Book', label: 'Book 字典' },
  { value: 'SetUp', label: 'SetUp 参数' },
  { value: 'Bell', label: 'Bell 公告' },
  { value: 'Folder', label: 'Folder 文件' },
  { value: 'Document', label: 'Document 日志' },
  { value: 'Clock', label: 'Clock 时间' },
]

const formRef = ref()
const loading = ref(false)
const saving = ref(false)
const tree = ref([])

const form = reactive({
  parentId: 0,
  menuName: '',
  menuType: 'C',
  path: '',
  component: '',
  perms: '',
  icon: '',
  sort: 0,
  visible: 1,
  status: 1,
  version: 0,
})

const isEdit = computed(() => props.mode === 'edit')
const type = computed(() => form.menuType)

const rules = computed(() => ({
  menuName: [{ required: true, message: '请输入菜单名称', trigger: 'blur' }],
  path:
    type.value === 'F'
      ? []
      : [{ required: true, message: '请输入路由路径', trigger: 'blur' }],
  perms:
    type.value === 'F'
      ? [{ required: true, message: '按钮必须填写权限标识', trigger: 'blur' }]
      : [],
}))

function prune(nodes, excludeId) {
  return nodes
    .filter((node) => node.id !== excludeId)
    .map((node) => ({ ...node, children: prune(node.children || [], excludeId) }))
}

const parentOptions = computed(() => {
  const source = isEdit.value ? prune(tree.value, Number(props.menuId)) : tree.value
  return [{ id: 0, menuName: '顶级菜单', children: source }]
})

function reset() {
  Object.assign(form, {
    parentId: Number(props.parentId) || 0,
    menuName: '',
    menuType: 'C',
    path: '',
    component: '',
    perms: '',
    icon: '',
    sort: 0,
    visible: 1,
    status: 1,
    version: 0,
  })
  formRef.value?.clearValidate()
}

async function load() {
  loading.value = true
  try {
    tree.value = (await getMenuTree()) || []
    if (isEdit.value && props.menuId) {
      const detail = await getMenu(props.menuId)
      Object.assign(form, {
        parentId: detail.parentId,
        menuName: detail.menuName,
        menuType: detail.menuType,
        path: detail.path || '',
        component: detail.component || '',
        perms: detail.perms || '',
        icon: detail.icon || '',
        sort: detail.sort,
        visible: detail.visible,
        status: detail.status,
        version: detail.version,
      })
    }
    await nextTick()
    formRef.value?.clearValidate()
  } catch (error) {
    ElMessage.error(error.message || '菜单加载失败')
  } finally {
    loading.value = false
  }
}

watch(
  () => props.modelValue,
  (open) => {
    if (!open) return
    reset()
    load()
  }
)

async function submit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  const payload = {
    parentId: form.parentId,
    menuName: form.menuName,
    menuType: form.menuType,
    path: form.menuType === 'F' ? null : form.path,
    component: form.menuType === 'C' ? form.component || null : null,
    perms: form.menuType === 'M' ? null : form.perms || null,
    icon: form.menuType === 'F' ? null : form.icon || null,
    sort: form.sort,
    visible: form.menuType === 'F' ? 1 : form.visible,
    status: form.status,
  }
  try {
    if (isEdit.value) {
      await updateMenu(props.menuId, { ...payload, version: form.version })
      ElMessage.success('菜单已更新')
    } else {
      await createMenu({ ...payload, version: 0 })
      ElMessage.success('菜单已创建')
    }
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
    :title="isEdit ? '编辑菜单' : '新增菜单'"
    size="460px"
    append-to-body
    :close-on-click-modal="false"
    @update:model-value="emit('update:modelValue', $event)"
  >
    <el-form
      ref="formRef"
      v-loading="loading"
      :model="form"
      :rules="rules"
      label-position="top"
      size="small"
    >
      <el-form-item label="上级菜单" prop="parentId">
        <el-tree-select
          v-model="form.parentId"
          :data="parentOptions"
          node-key="id"
          :props="{ label: 'menuName', children: 'children' }"
          check-strictly
          :render-after-expand="false"
          class="w-full"
        />
      </el-form-item>

      <div class="grid grid-cols-2 gap-2">
        <el-form-item label="菜单类型" prop="menuType">
          <el-select v-model="form.menuType" class="w-full">
            <el-option label="目录 M" value="M" />
            <el-option label="菜单 C" value="C" />
            <el-option label="按钮 F" value="F" />
          </el-select>
        </el-form-item>
        <el-form-item label="排序" prop="sort">
          <el-input-number
            v-model="form.sort"
            :min="0"
            :max="999"
            controls-position="right"
            class="w-full"
          />
        </el-form-item>
      </div>

      <el-form-item label="菜单名称" prop="menuName">
        <el-input v-model="form.menuName" placeholder="显示名称" />
      </el-form-item>

      <el-form-item v-if="type !== 'F'" label="路由路径" prop="path">
        <el-input v-model="form.path" placeholder="/system/user" class="font-mono" />
      </el-form-item>

      <el-form-item v-if="type === 'C'" label="组件路径" prop="component">
        <el-input
          v-model="form.component"
          placeholder="system/user/index"
          class="font-mono"
        />
      </el-form-item>

      <el-form-item v-if="type !== 'M'" label="权限标识" prop="perms">
        <el-input
          v-model="form.perms"
          placeholder="system:user:list"
          class="font-mono"
        />
      </el-form-item>

      <el-form-item v-if="type !== 'F'" label="图标" prop="icon">
        <el-select v-model="form.icon" clearable placeholder="选择图标" class="w-full">
          <el-option
            v-for="option in iconOptions"
            :key="option.value"
            :label="option.label"
            :value="option.value"
          >
            <span class="flex items-center gap-2">
              <el-icon><component :is="resolveMenuIcon(option.value)" /></el-icon>
              <span class="text-xs">{{ option.label }}</span>
            </span>
          </el-option>
        </el-select>
      </el-form-item>

      <div v-if="type !== 'F'" class="grid grid-cols-2 gap-2">
        <el-form-item label="显示" prop="visible">
          <el-select v-model="form.visible" class="w-full">
            <el-option label="显示" :value="1" />
            <el-option label="隐藏" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-select v-model="form.status" class="w-full">
            <el-option label="启用" :value="1" />
            <el-option label="禁用" :value="0" />
          </el-select>
        </el-form-item>
      </div>

      <el-form-item v-else label="状态" prop="status">
        <el-select v-model="form.status" class="w-full">
          <el-option label="启用" :value="1" />
          <el-option label="禁用" :value="0" />
        </el-select>
      </el-form-item>

      <p class="text-[10px] leading-4 text-[#94a3b8]">
        目录用于分组，菜单对应页面，按钮用于接口级权限；按钮不参与侧栏渲染。
      </p>
    </el-form>

    <template #footer>
      <div class="flex justify-end gap-1">
        <el-button size="small" @click="emit('update:modelValue', false)">取消</el-button>
        <el-button size="small" type="primary" :loading="saving" @click="submit">
          {{ isEdit ? '保存修改' : '创建菜单' }}
        </el-button>
      </div>
    </template>
  </el-drawer>
</template>
