<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  Key,
  Menu as MenuIcon,
  Monitor,
  Refresh,
  User,
  UserFilled,
  Warning,
} from '@element-plus/icons-vue'
import { getDashboardOverview } from '@/api/system'
import { formatDateTime } from '@/utils/format'
import PageHeader from '@/components/PageHeader.vue'
import KpiCard from '@/components/KpiCard.vue'
import StatusTag from '@/components/StatusTag.vue'
import TrendChart from '@/components/TrendChart.vue'
import DonutChart from '@/components/DonutChart.vue'

const router = useRouter()
const loading = ref(true)
const errorMessage = ref('')
const data = ref({
  kpis: [],
  trend: [],
  moduleUsage: [],
  recentOperations: [],
  loginFailures: [],
  systemStatus: [],
})

/** KPI 图标与配色（按 key 匹配，未匹配则不显示图标） */
const kpiMeta = {
  logins: { icon: User, color: '#a8435f' },
  members: { icon: Monitor, color: '#3b82f6' },
  admins: { icon: UserFilled, color: '#15803d' },
  abnormal: { icon: Warning, color: '#b45309' },
}

const greeting = computed(() => {
  const hour = new Date().getHours()
  if (hour < 6) return '夜深了'
  if (hour < 12) return '上午好'
  if (hour < 18) return '下午好'
  return '晚上好'
})

const todayText = computed(() => {
  const now = new Date()
  const week = ['日', '一', '二', '三', '四', '五', '六'][now.getDay()]
  return `${now.getFullYear()} 年 ${now.getMonth() + 1} 月 ${now.getDate()} 日 · 星期${week}`
})

const trendTotal = computed(() =>
  data.value.trend.reduce(
    (sum, item) => ({ logins: sum.logins + (item.logins || 0), operations: sum.operations + (item.operations || 0) }),
    { logins: 0, operations: 0 }
  )
)

const quickActions = [
  {
    label: '用户管理',
    description: '账号与状态维护',
    icon: User,
    to: '/system/user',
    perm: 'admin:SysUser:page',
  },
  {
    label: '角色管理',
    description: '调整角色与菜单',
    icon: UserFilled,
    to: '/system/role',
    perm: 'admin:SysRole:page',
  },
  {
    label: '菜单配置',
    description: '维护路由与权限标识',
    icon: MenuIcon,
    to: '/system/menu',
    perm: 'admin:SysMenu:tree',
  },
  {
    label: '权限点',
    description: '按模块查看权限码',
    icon: Key,
    to: '/system/permission',
    perm: 'admin:SysPermission:page',
  },
]

async function load() {
  loading.value = true
  errorMessage.value = ''
  try {
    data.value = await getDashboardOverview()
  } catch (error) {
    errorMessage.value = error.message || '仪表盘加载失败'
  } finally {
    loading.value = false
  }
}

function openOperation(row) {
  ElMessage.info(`操作日志 ${row.id} 的详情将在后续批次开放`)
}

onMounted(load)
</script>

<template>
  <div class="pb-4">
    <PageHeader title="仪表盘" description="系统运行概览 · 登录与操作日志实时聚合">
      <template #actions>
        <el-button size="small" :icon="Refresh" :loading="loading" @click="load">刷新</el-button>
      </template>
    </PageHeader>

    <div class="rg-page space-y-2">
      <el-alert
        v-if="errorMessage"
        :title="errorMessage"
        type="error"
        :closable="false"
        show-icon
      />

      <!-- 欢迎横幅 -->
      <section
        class="rg-card relative overflow-hidden px-4 py-3.5"
        style="background: linear-gradient(112deg, #fdf1f5 0%, #ffffff 46%, #eff6ff 100%)"
      >
        <div class="flex flex-wrap items-center justify-between gap-3">
          <div>
            <p class="text-[15px] font-semibold tracking-tight text-[#121212]">
              {{ greeting }}，欢迎回到 RouteGo 管理后台
            </p>
            <p class="mt-0.5 font-mono text-[11px] text-[#64748b]">{{ todayText }}</p>
          </div>
          <div class="flex flex-wrap items-center gap-4">
            <div class="text-right">
              <p class="rg-label">30 日登录</p>
              <p class="font-mono text-[15px] font-semibold text-[#a8435f]">
                {{ trendTotal.logins }}
              </p>
            </div>
            <div class="h-8 w-px bg-[#e2e8f0]"></div>
            <div class="text-right">
              <p class="rg-label">30 日操作</p>
              <p class="font-mono text-[15px] font-semibold text-[#3b82f6]">
                {{ trendTotal.operations }}
              </p>
            </div>
          </div>
        </div>
        <span
          aria-hidden="true"
          class="pointer-events-none absolute -right-6 -top-8 h-28 w-28 rounded-full bg-[#a8435f]/8"
        ></span>
        <span
          aria-hidden="true"
          class="pointer-events-none absolute right-24 -bottom-10 h-20 w-20 rounded-full bg-[#3b82f6]/8"
        ></span>
      </section>

      <!-- KPI 卡片 -->
      <div class="grid grid-cols-1 gap-2 sm:grid-cols-2 xl:grid-cols-4">
        <KpiCard
          v-for="kpi in data.kpis"
          :key="kpi.key"
          :label="kpi.label"
          :value="kpi.value"
          :unit="kpi.unit"
          :delta="kpi.delta"
          :hint="kpi.hint"
          :invert="Boolean(kpi.invert)"
          :icon="kpiMeta[kpi.key]?.icon"
          :spark="kpi.spark || []"
          :spark-color="kpi.sparkColor || kpiMeta[kpi.key]?.color || '#a8435f'"
        />
      </div>

      <!-- 趋势图 + 模块分布 -->
      <div class="grid grid-cols-1 items-stretch gap-2 xl:grid-cols-3">
        <section class="rg-card min-w-0 xl:col-span-2">
          <div class="rg-card-head">
            <span class="rg-section-title">近 30 日活跃趋势</span>
            <div class="flex items-center gap-3 text-[10px] text-[#64748b]">
              <span class="flex items-center gap-1">
                <span class="h-0.5 w-4 rounded bg-[#a8435f]"></span>登录
              </span>
              <span class="flex items-center gap-1">
                <span class="h-0.5 w-4 rounded bg-[#3b82f6]" style="background-image: linear-gradient(90deg, #3b82f6 60%, transparent 60%); background-size: 6px 2px"></span>后台操作
              </span>
            </div>
          </div>
          <div class="px-2 py-2">
            <TrendChart
              v-loading="loading"
              :data="data.trend"
              name-a="登录"
              name-b="后台操作"
              element-loading-background="rgba(255,255,255,0.7)"
            />
          </div>
        </section>

        <section class="rg-card min-w-0">
          <div class="rg-card-head">
            <span class="rg-section-title">模块访问分布</span>
            <span class="text-[10px] text-[#94a3b8]">近 30 日 · 页面 PV</span>
          </div>
          <DonutChart
            v-loading="loading"
            :items="data.moduleUsage"
            total-label="累计访问"
            element-loading-background="rgba(255,255,255,0.7)"
          />
        </section>
      </div>

      <div class="grid grid-cols-1 items-start gap-2 xl:grid-cols-3">
        <section class="rg-card min-w-0 xl:col-span-2">
          <div class="rg-card-head">
            <div class="flex items-center gap-2">
              <span class="rg-section-title">最近操作</span>
              <span class="font-mono text-[10px] text-[#94a3b8]">
                {{ data.recentOperations.length }} 条
              </span>
            </div>
            <span class="text-[10px] text-[#94a3b8]">异步写入 · 参数已脱敏</span>
          </div>
          <el-table
            v-loading="loading"
            :data="data.recentOperations"
            size="small"
            border
            :show-header="true"
          >
            <el-table-column label="时间" min-width="132">
              <template #default="{ row }">
                <span class="font-mono text-[11px] text-[#64748b]">
                  {{ formatDateTime(row.operTime) }}
                </span>
              </template>
            </el-table-column>
            <el-table-column prop="operName" label="操作人" min-width="88">
              <template #default="{ row }">
                <span class="font-mono text-[11px]">{{ row.operName }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="title" label="动作" min-width="88" />
            <el-table-column label="结果" width="72">
              <template #default="{ row }">
                <StatusTag :value="row.status" type="result" />
              </template>
            </el-table-column>
            <el-table-column label="耗时" width="72" align="right">
              <template #default="{ row }">
                <span class="font-mono text-[11px]">{{ row.costTime }}ms</span>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="56" align="right">
              <template #default="{ row }">
                <el-button link type="primary" size="small" @click="openOperation(row)">
                  查看
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </section>

        <div class="space-y-2">
          <section class="rg-card">
            <div class="rg-card-head">
              <span class="rg-section-title">异常登录</span>
              <StatusTag :value="0" type="result" :labels="['需关注', '']" tone="warning" />
            </div>
            <ul class="divide-y divide-[#e2e8f0]">
              <li
                v-for="item in data.loginFailures"
                :key="item.id"
                class="flex items-start gap-2 px-3 py-2"
              >
                <div class="min-w-0 flex-1">
                  <div class="flex items-center gap-1">
                    <span class="font-mono text-[11px] font-medium text-[#121212]">
                      {{ item.username }}
                    </span>
                    <span class="font-mono text-[10px] text-[#94a3b8]">{{ item.ip }}</span>
                  </div>
                  <p class="mt-0.5 text-[11px] text-[#64748b]">{{ item.reason }}</p>
                  <p class="font-mono text-[10px] text-[#94a3b8]">
                    {{ formatDateTime(item.time) }}
                  </p>
                </div>
                <span
                  class="rounded bg-[#fef2f2] px-1.5 py-0.5 font-mono text-[10px] text-[#b91c1c]"
                >
                  ×{{ item.count }}
                </span>
              </li>
            </ul>
          </section>

          <section class="rg-card">
            <div class="rg-card-head">
              <span class="rg-section-title">系统状态</span>
              <span class="font-mono text-[10px] text-[#94a3b8]">实时</span>
            </div>
            <ul class="divide-y divide-[#e2e8f0]">
              <li
                v-for="item in data.systemStatus"
                :key="item.key"
                class="flex items-center gap-2 px-3 py-2"
              >
                <span
                  class="h-1.5 w-1.5 shrink-0 rounded"
                  :class="{
                    'bg-[#15803d]': item.status === 'success',
                    'bg-[#b45309]': item.status === 'warning',
                    'bg-[#b91c1c]': item.status === 'danger',
                    'bg-[#1d4ed8]': item.status === 'info',
                  }"
                ></span>
                <span class="w-20 shrink-0 text-[11px] text-[#64748b]">{{ item.label }}</span>
                <span class="min-w-0 flex-1 truncate font-mono text-[11px] text-[#1e293b]">
                  {{ item.value }}
                </span>
                <span class="shrink-0 text-[10px] text-[#94a3b8]">{{ item.detail }}</span>
              </li>
            </ul>
          </section>
        </div>
      </div>

      <section class="rg-card">
        <div class="rg-card-head">
          <span class="rg-section-title">快捷入口</span>
          <span class="text-[10px] text-[#94a3b8]">按权限显示</span>
        </div>
        <div class="grid grid-cols-2 gap-2 px-3 py-2.5 sm:grid-cols-4">
          <router-link
            v-for="action in quickActions"
            :key="action.to"
            v-perm="action.perm"
            :to="action.to"
            class="flex items-center gap-2 rounded border border-[#e2e8f0] bg-white px-2.5 py-2 transition-colors duration-150 hover:border-[#e89aaf] hover:bg-[#fdf1f5] focus-visible:ring-1 focus-visible:ring-[#3b82f6]"
          >
            <span
              class="flex h-6 w-6 shrink-0 items-center justify-center rounded bg-[#f8fafc] text-[#a8435f]"
            >
              <el-icon><component :is="action.icon" /></el-icon>
            </span>
            <span class="min-w-0">
              <span class="block truncate text-[11px] font-medium text-[#1e293b]">
                {{ action.label }}
              </span>
              <span class="block truncate text-[10px] text-[#94a3b8]">
                {{ action.description }}
              </span>
            </span>
          </router-link>
        </div>
      </section>
    </div>
  </div>
</template>
