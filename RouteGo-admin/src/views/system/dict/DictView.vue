<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh, Search } from '@element-plus/icons-vue'
import {
  deleteDictData,
  deleteDictType,
  pageDictData,
  pageDictTypes,
  setDictDataDefault,
  setDictDataStatus,
  setDictTypeStatus,
} from '@/api/system'
import PageHeader from '@/components/PageHeader.vue'
import StatusTag from '@/components/StatusTag.vue'
import DictTypeDrawer from './DictTypeDrawer.vue'
import DictDataDrawer from './DictDataDrawer.vue'

const typeLoading = ref(false)
const types = ref([])
const typeFilters = reactive({ dictName: '', dictType: '', status: '' })
const typePage = reactive({ pageNum: 1, pageSize: 10, total: 0 })

const dataLoading = ref(false)
const dataRows = ref([])
const selectedType = ref(null)
const dataFilters = reactive({ dictLabel: '', status: '' })
const dataPage = reactive({ pageNum: 1, pageSize: 10, total: 0 })

const typeDrawer = ref(false)
const typeMode = ref('create')
const typeId = ref(null)
const dataDrawer = ref(false)
const dataMode = ref('create')
const dataId = ref(null)

async function loadTypes() {
  typeLoading.value = true
  try {
    const result = await pageDictTypes({
      dictName: typeFilters.dictName || undefined,
      dictType: typeFilters.dictType || undefined,
      status: typeFilters.status === '' ? undefined : typeFilters.status,
      pageNum: typePage.pageNum,
      pageSize: typePage.pageSize,
    })
    types.value = result.records || []
    typePage.total = result.total || 0
  } catch (error) {
    ElMessage.error(error.message || '字典类型加载失败')
    types.value = []
    typePage.total = 0
  } finally {
    typeLoading.value = false
  }
}

async function loadData() {
  if (!selectedType.value) {
    dataRows.value = []
    dataPage.total = 0
    return
  }
  dataLoading.value = true
  try {
    const result = await pageDictData({
      dictType: selectedType.value.dictType,
      dictLabel: dataFilters.dictLabel || undefined,
      status: dataFilters.status === '' ? undefined : dataFilters.status,
      pageNum: dataPage.pageNum,
      pageSize: dataPage.pageSize,
    })
    dataRows.value = result.records || []
    dataPage.total = result.total || 0
  } catch (error) {
    ElMessage.error(error.message || '字典数据加载失败')
    dataRows.value = []
    dataPage.total = 0
  } finally {
    dataLoading.value = false
  }
}

function searchTypes() {
  typePage.pageNum = 1
  loadTypes()
}

function resetTypeFilters() {
  Object.assign(typeFilters, { dictName: '', dictType: '', status: '' })
  typePage.pageNum = 1
  loadTypes()
}

function selectType(row) {
  if (!row) return
  selectedType.value = row
  dataPage.pageNum = 1
  dataFilters.dictLabel = ''
  dataFilters.status = ''
  loadData()
}

function searchData() {
  dataPage.pageNum = 1
  loadData()
}

function openTypeCreate() {
  typeMode.value = 'create'
  typeId.value = null
  typeDrawer.value = true
}

function openTypeEdit(row) {
  typeMode.value = 'edit'
  typeId.value = row.id
  typeDrawer.value = true
}

function openDataCreate() {
  if (!selectedType.value) {
    ElMessage.warning('请先选择字典类型')
    return
  }
  dataMode.value = 'create'
  dataId.value = null
  dataDrawer.value = true
}

function openDataEdit(row) {
  dataMode.value = 'edit'
  dataId.value = row.id
  dataDrawer.value = true
}

async function toggleTypeStatus(row, enabled) {
  try {
    await setDictTypeStatus(row.id, enabled ? 1 : 0)
    row.status = enabled ? 1 : 0
    ElMessage.success(enabled ? '已启用' : '已禁用')
  } catch (error) {
    ElMessage.error(error.message || '操作失败')
    loadTypes()
  }
}

async function toggleDataStatus(row, enabled) {
  try {
    await setDictDataStatus(row.id, enabled ? 1 : 0)
    row.status = enabled ? 1 : 0
    ElMessage.success(enabled ? '已启用' : '已禁用')
  } catch (error) {
    ElMessage.error(error.message || '操作失败')
    loadData()
  }
}

async function setDefault(row) {
  try {
    await setDictDataDefault(row.id)
    ElMessage.success('已设为默认项')
    loadData()
  } catch (error) {
    ElMessage.error(error.message || '设置失败')
  }
}

async function removeType(row) {
  try {
    await ElMessageBox.confirm(`确定删除字典类型 ${row.dictType} 吗？`, '删除字典类型', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消',
    })
  } catch {
    return
  }
  try {
    await deleteDictType(row.id)
    ElMessage.success('字典类型已删除')
    if (selectedType.value?.id === row.id) {
      selectedType.value = null
      dataRows.value = []
      dataPage.total = 0
    }
    loadTypes()
  } catch (error) {
    ElMessage.error(error.message || '删除失败')
  }
}

async function removeData(row) {
  try {
    await ElMessageBox.confirm(`确定删除字典数据「${row.dictLabel}」吗？`, '删除字典数据', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消',
    })
  } catch {
    return
  }
  try {
    await deleteDictData(row.id)
    ElMessage.success('字典数据已删除')
    loadData()
  } catch (error) {
    ElMessage.error(error.message || '删除失败')
  }
}

onMounted(loadTypes)
</script>

<template>
  <div class="pb-4">
    <PageHeader title="字典管理" description="字典类型与字典数据维护">
      <template #actions>
        <el-button size="small" :icon="Refresh" :loading="typeLoading" @click="loadTypes">刷新</el-button>
        <el-button v-perm="'system:dict:add'" size="small" type="primary" :icon="Plus" @click="openTypeCreate">
          新增类型
        </el-button>
      </template>
    </PageHeader>

    <div class="rg-page">
      <div class="grid grid-cols-1 items-start gap-2 xl:grid-cols-5">
        <section class="rg-card min-w-0 xl:col-span-2">
          <div class="rg-card-head">
            <span class="rg-section-title">字典类型</span>
            <span class="font-mono text-[10px] text-[#94a3b8]">{{ typePage.total }} 项</span>
          </div>

          <div class="flex flex-wrap items-end gap-2 border-b border-[#e2e8f0] px-3 py-2">
            <el-input v-model="typeFilters.dictName" size="small" placeholder="名称" clearable style="width: 96px" @keyup.enter="searchTypes" />
            <el-input v-model="typeFilters.dictType" size="small" placeholder="类型" clearable style="width: 110px" class="font-mono" @keyup.enter="searchTypes" />
            <el-select v-model="typeFilters.status" size="small" style="width: 84px">
              <el-option label="全部" value="" />
              <el-option label="启用" :value="1" />
              <el-option label="禁用" :value="0" />
            </el-select>
            <el-button size="small" type="primary" :icon="Search" @click="searchTypes">查询</el-button>
            <el-button link size="small" @click="resetTypeFilters">重置</el-button>
          </div>

          <div class="rg-scroll-x">
            <el-table
              v-loading="typeLoading"
              :data="types"
              size="small"
              border
              stripe
              row-key="id"
              highlight-current-row
              empty-text="暂无字典类型"
              @current-change="selectType"
            >
              <el-table-column label="名称" min-width="130">
                <template #default="{ row }">
                  <p class="truncate text-[11px] font-medium text-[#1e293b]">{{ row.dictName }}</p>
                  <p class="truncate font-mono text-[10px] text-[#94a3b8]">{{ row.dictType }}</p>
                </template>
              </el-table-column>
              <el-table-column label="状态" width="58" align="center">
                <template #default="{ row }">
                  <el-switch
                    v-perm="'system:dict:edit'"
                    size="small"
                    :model-value="row.status === 1"
                    @change="(value) => toggleTypeStatus(row, value)"
                  />
                </template>
              </el-table-column>
              <el-table-column label="操作" width="92" fixed="right">
                <template #default="{ row }">
                  <el-button v-perm="'system:dict:edit'" link type="primary" size="small" @click.stop="openTypeEdit(row)">
                    编辑
                  </el-button>
                  <el-button v-perm="'system:dict:delete'" link type="danger" size="small" @click.stop="removeType(row)">
                    删除
                  </el-button>
                </template>
              </el-table-column>
            </el-table>
          </div>

          <div class="flex items-center justify-between border-t border-[#e2e8f0] px-3 py-1.5">
            <span class="font-mono text-[10px] text-[#94a3b8]">点击行查看字典数据</span>
            <el-pagination
              v-model:current-page="typePage.pageNum"
              :page-size="typePage.pageSize"
              :total="typePage.total"
              layout="prev, pager, next"
              size="small"
              @current-change="loadTypes"
            />
          </div>
        </section>

        <section class="rg-card min-w-0 xl:col-span-3">
          <div class="rg-card-head">
            <div class="flex min-w-0 items-center gap-2">
              <span class="rg-section-title">字典数据</span>
              <span v-if="selectedType" class="truncate font-mono text-[10px] text-[#a8435f]">
                {{ selectedType.dictType }}
              </span>
            </div>
            <div class="flex shrink-0 items-center gap-1">
              <el-input v-model="dataFilters.dictLabel" size="small" placeholder="标签" clearable style="width: 96px" @keyup.enter="searchData" />
              <el-select v-model="dataFilters.status" size="small" style="width: 84px">
                <el-option label="全部" value="" />
                <el-option label="启用" :value="1" />
                <el-option label="禁用" :value="0" />
              </el-select>
              <el-button size="small" @click="searchData">查询</el-button>
              <el-button v-perm="'system:dict:add'" size="small" type="primary" :icon="Plus" @click="openDataCreate">
                新增
              </el-button>
            </div>
          </div>

          <div v-if="!selectedType" class="px-3 py-6 text-center text-xs text-[#94a3b8]">
            请选择左侧字典类型
          </div>
          <template v-else>
            <div class="rg-scroll-x">
              <el-table v-loading="dataLoading" :data="dataRows" size="small" border stripe row-key="id" empty-text="该类型下暂无数据">
                <el-table-column label="标签" min-width="100">
                  <template #default="{ row }">
                    <span class="truncate text-[11px] text-[#1e293b]">{{ row.dictLabel }}</span>
                  </template>
                </el-table-column>
                <el-table-column label="值" width="72">
                  <template #default="{ row }">
                    <span class="font-mono text-[11px]">{{ row.dictValue }}</span>
                  </template>
                </el-table-column>
                <el-table-column label="排序" width="52" align="right">
                  <template #default="{ row }">
                    <span class="font-mono text-[11px]">{{ row.sort }}</span>
                  </template>
                </el-table-column>
                <el-table-column label="默认" width="56" align="center">
                  <template #default="{ row }">
                    <span v-if="row.isDefault === 1" class="rounded bg-[#fdf1f5] px-1.5 py-0.5 text-[10px] text-[#a8435f]">默认</span>
                    <span v-else class="text-[10px] text-[#94a3b8]">—</span>
                  </template>
                </el-table-column>
                <el-table-column label="状态" width="58" align="center">
                  <template #default="{ row }">
                    <el-switch
                      v-perm="'system:dict:edit'"
                      size="small"
                      :model-value="row.status === 1"
                      @change="(value) => toggleDataStatus(row, value)"
                    />
                  </template>
                </el-table-column>
                <el-table-column label="操作" width="170" fixed="right">
                  <template #default="{ row }">
                    <div class="flex items-center gap-0.5">
                      <el-button v-perm="'system:dict:edit'" link type="primary" size="small" @click="openDataEdit(row)">编辑</el-button>
                      <el-button v-perm="'system:dict:edit'" link size="small" :disabled="row.isDefault === 1" @click="setDefault(row)">设默认</el-button>
                      <el-button v-perm="'system:dict:delete'" link type="danger" size="small" @click="removeData(row)">删除</el-button>
                    </div>
                  </template>
                </el-table-column>
              </el-table>
            </div>
            <div class="flex items-center justify-between border-t border-[#e2e8f0] px-3 py-1.5">
              <span class="font-mono text-[10px] text-[#94a3b8]">共 {{ dataPage.total }} 条</span>
              <el-pagination
                v-model:current-page="dataPage.pageNum"
                :page-size="dataPage.pageSize"
                :total="dataPage.total"
                layout="prev, pager, next"
                size="small"
                @current-change="loadData"
              />
            </div>
          </template>
        </section>
      </div>
    </div>

    <DictTypeDrawer v-model="typeDrawer" :mode="typeMode" :dict-type-id="typeId" @saved="loadTypes" />
    <DictDataDrawer
      v-model="dataDrawer"
      :mode="dataMode"
      :dict-data-id="dataId"
      :dict-type="selectedType?.dictType || ''"
      @saved="loadData"
    />
  </div>
</template>
