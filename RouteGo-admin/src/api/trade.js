import { http } from './request'

/* 订单 */
export function pageOrders(params) {
  return http.get('/trade/orders/page', { params })
}

export function getOrder(id) {
  return http.get(`/trade/orders/${id}`)
}

export function createOrder(data) {
  return http.post('/trade/orders', data)
}

export function updateOrder(id, data) {
  return http.put(`/trade/orders/${id}`, data)
}

export function cancelOrder(id, reason) {
  return http.post(`/trade/orders/${id}/cancel`, { reason })
}

export function payOrder(id, data) {
  return http.post(`/trade/orders/${id}/pay`, data || {})
}

export function startOrder(id) {
  return http.post(`/trade/orders/${id}/start`)
}

export function completeOrder(id) {
  return http.post(`/trade/orders/${id}/complete`)
}

export function deleteOrder(id) {
  return http.delete(`/trade/orders/${id}`)
}

/* 出行人 */
export function listTravelers(orderId) {
  return http.get(`/trade/order-traveler/list/${orderId}`)
}

export function createTraveler(orderId, data) {
  return http.post(`/trade/order-traveler/${orderId}`, data)
}

export function updateTraveler(id, data) {
  return http.put(`/trade/order-traveler/${id}`, data)
}

export function deleteTraveler(id) {
  return http.delete(`/trade/order-traveler/${id}`)
}

export function replaceTravelers(orderId, items) {
  return http.put(`/trade/order-traveler/batch/${orderId}`, { items })
}

/* 状态流水 */
export function pageStatusLogs(params) {
  return http.get('/trade/order-status-log/page', { params })
}

export function listStatusLogs(orderId) {
  return http.get(`/trade/order-status-log/list/${orderId}`)
}

/* 支付流水 */
export function pagePayments(params) {
  return http.get('/trade/payment-record/page', { params })
}

export function listPayments(orderId) {
  return http.get(`/trade/payment-record/list/${orderId}`)
}

/* 退款 */
export function pageRefunds(params) {
  return http.get('/trade/refund-record/page', { params })
}

export function getRefund(id) {
  return http.get(`/trade/refund-record/${id}`)
}

export function listRefunds(orderId) {
  return http.get(`/trade/refund-record/list/${orderId}`)
}

export function applyRefund(orderId, data) {
  return http.post(`/trade/refund-record/${orderId}`, data)
}

export function auditRefund(id, data) {
  return http.put(`/trade/refund-record/${id}/audit`, data)
}
