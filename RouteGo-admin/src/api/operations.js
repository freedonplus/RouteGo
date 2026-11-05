import { http } from './request'

/* 客服工单 */
export function pageTickets(params) {
  return http.get('/operations/cs-ticket/page', { params })
}

export function getTicket(id) {
  return http.get(`/operations/cs-ticket/${id}`)
}

export function createTicket(data) {
  return http.post('/operations/cs-ticket', data)
}

export function updateTicket(id, data) {
  return http.put(`/operations/cs-ticket/${id}`, data)
}

export function assignTicket(id, handlerId) {
  return http.put(`/operations/cs-ticket/${id}/assign`, null, { params: { handlerId } })
}

export function setTicketPriority(id, priority) {
  return http.put(`/operations/cs-ticket/${id}/priority`, null, { params: { priority } })
}

export function setTicketStatus(id, status, remark) {
  return http.put(`/operations/cs-ticket/${id}/status`, null, { params: { status, remark } })
}

export function deleteTicket(id) {
  return http.delete(`/operations/cs-ticket/${id}`)
}

export function getTicketStatistics() {
  return http.get('/operations/cs-ticket/statistics')
}

/* 工单消息 */
export function pageMessages(params) {
  return http.get('/operations/cs-message/page', { params })
}

export function listMessages(ticketId) {
  return http.get('/operations/cs-message/list', { params: { ticketId } })
}

export function sendMessage(data) {
  return http.post('/operations/cs-message', data)
}

/* 定制需求 */
export function pageRequirements(params) {
  return http.get('/operations/custom-requirement/page', { params })
}

export function getRequirement(id) {
  return http.get(`/operations/custom-requirement/${id}`)
}

export function createRequirement(data) {
  return http.post('/operations/custom-requirement', data)
}

export function updateRequirement(id, data) {
  return http.put(`/operations/custom-requirement/${id}`, data)
}

export function assignRequirement(id, handlerId) {
  return http.put(`/operations/custom-requirement/${id}/assign`, null, { params: { handlerId } })
}

export function setRequirementStatus(id, status, remark) {
  return http.put(`/operations/custom-requirement/${id}/status`, null, { params: { status, remark } })
}

export function deleteRequirement(id) {
  return http.delete(`/operations/custom-requirement/${id}`)
}

/* 定制报价 */
export function pageQuotes(params) {
  return http.get('/operations/custom-quote/page', { params })
}

export function getQuote(id) {
  return http.get(`/operations/custom-quote/${id}`)
}

export function listQuotes(requirementId) {
  return http.get('/operations/custom-quote/list', { params: { requirementId } })
}

export function createQuote(data) {
  return http.post('/operations/custom-quote', data)
}

export function updateQuote(id, data) {
  return http.put(`/operations/custom-quote/${id}`, data)
}

export function acceptQuote(id) {
  return http.put(`/operations/custom-quote/${id}/accept`)
}

export function invalidateQuote(id) {
  return http.put(`/operations/custom-quote/${id}/invalidate`)
}

export function deleteQuote(id) {
  return http.delete(`/operations/custom-quote/${id}`)
}

/* 每日经营统计 */
export function pageStats(params) {
  return http.get('/operations/stat-daily/page', { params })
}

export function getStat(date) {
  return http.get(`/operations/stat-daily/${date}`)
}

export function getStatSummary(params) {
  return http.get('/operations/stat-daily/summary', { params })
}

export function getStatTrend(params) {
  return http.get('/operations/stat-daily/trend', { params })
}

export function rebuildStats(data) {
  return http.post('/operations/stat-daily/rebuild', data)
}
