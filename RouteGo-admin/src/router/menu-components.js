export const menuComponents = {
  'system/user/index': () => import('@/views/system/user/UserView.vue'),
  'system/role/index': () => import('@/views/system/role/RoleView.vue'),
  'system/menu/index': () => import('@/views/system/menu/MenuView.vue'),
  'system/permission/index': () => import('@/views/system/permission/PermissionView.vue'),
  'system/dict/index': () => import('@/views/system/dict/DictView.vue'),
  'system/config/index': () => import('@/views/system/config/ConfigView.vue'),
  'system/notice/index': () => import('@/views/system/notice/NoticeView.vue'),
  'system/file/index': () => import('@/views/system/file/FileView.vue'),
  'system/operlog/index': () => import('@/views/system/operlog/OperLogView.vue'),
  'system/loginlog/index': () => import('@/views/system/loginlog/LoginLogView.vue'),

  /* 线路管理 */
  'catalog/route/index': () => import('@/views/catalog/RouteView.vue'),
  'catalog/category/index': () => import('@/views/catalog/RouteCategoryView.vue'),
  'catalog/tag/index': () => import('@/views/catalog/RouteTagView.vue'),
  'catalog/departure/index': () => import('@/views/catalog/RouteDepartureView.vue'),
  'catalog/review/index': () => import('@/views/catalog/RouteReviewView.vue'),

  /* 订单管理 */
  'trade/order/index': () => import('@/views/trade/OrderView.vue'),
  'trade/refund/index': () => import('@/views/trade/RefundView.vue'),
  'trade/payment/index': () => import('@/views/trade/PaymentView.vue'),

  /* 会员管理 */
  'customer/customer/index': () => import('@/views/customer/CustomerView.vue'),
  'customer/coupon-template/index': () => import('@/views/customer/CouponTemplateView.vue'),
  'customer/coupon/index': () => import('@/views/customer/CouponView.vue'),

  /* 运营中心 */
  'operations/ticket/index': () => import('@/views/operations/CsTicketView.vue'),
  'operations/requirement/index': () => import('@/views/operations/CustomRequirementView.vue'),
  'operations/quote/index': () => import('@/views/operations/CustomQuoteView.vue'),
  'operations/stat/index': () => import('@/views/operations/StatDailyView.vue'),
}

export function resolveMenuComponent(component) {
  return component ? menuComponents[component] || null : null
}
