import { http } from './request'

/* 会员 */
export function pageCustomers(params) {
  return http.get('/customer/customer/page', { params })
}

export function getCustomer(id) {
  return http.get(`/customer/customer/${id}`)
}

export function createCustomer(data) {
  return http.post('/customer/customer', data)
}

export function updateCustomer(id, data) {
  return http.put(`/customer/customer/${id}`, data)
}

export function updateCustomerPhone(id, phone) {
  return http.put(`/customer/customer/${id}/phone`, { phone })
}

export function resetCustomerPassword(id, password) {
  return http.put(`/customer/customer/${id}/password`, { password })
}

export function setCustomerStatus(id, status) {
  return http.put(`/customer/customer/${id}/status`, null, { params: { status } })
}

export function setCustomerLevel(id, level) {
  return http.put(`/customer/customer/${id}/level`, null, { params: { level } })
}

export function adjustCustomerPoints(id, delta) {
  return http.post(`/customer/customer/${id}/points`, null, { params: { delta } })
}

export function listCustomerCoupons(id, status) {
  return http.get(`/customer/customer/${id}/coupons`, { params: { status } })
}

export function deleteCustomer(id) {
  return http.delete(`/customer/customer/${id}`)
}

/* 券模板 */
export function pageCouponTemplates(params) {
  return http.get('/customer/coupon-template/page', { params })
}

export function listCouponTemplates() {
  return http.get('/customer/coupon-template/list')
}

export function getCouponTemplate(id) {
  return http.get(`/customer/coupon-template/${id}`)
}

export function createCouponTemplate(data) {
  return http.post('/customer/coupon-template', data)
}

export function updateCouponTemplate(id, data) {
  return http.put(`/customer/coupon-template/${id}`, data)
}

export function setCouponTemplateStatus(id, status) {
  return http.put(`/customer/coupon-template/${id}/status`, null, { params: { status } })
}

export function grantCoupons(data) {
  return http.post('/customer/coupon-template/grant', data)
}

export function deleteCouponTemplate(id) {
  return http.delete(`/customer/coupon-template/${id}`)
}

/* 用户优惠券 */
export function pageCoupons(params) {
  return http.get('/customer/coupon/page', { params })
}

export function getCoupon(id) {
  return http.get(`/customer/coupon/${id}`)
}

export function useCoupon(id, orderId) {
  return http.put(`/customer/coupon/${id}/use`, { orderId })
}

export function revokeCoupon(id) {
  return http.put(`/customer/coupon/${id}/revoke`)
}

export function deleteCoupon(id) {
  return http.delete(`/customer/coupon/${id}`)
}

export function refreshExpiredCoupons() {
  return http.post('/customer/coupon/expire-refresh')
}
