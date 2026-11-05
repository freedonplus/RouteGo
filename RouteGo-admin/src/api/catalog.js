import { http } from './request'

/* 线路 */
export function pageRoutes(params) {
  return http.get('/catalog/route/page', { params })
}

export function getRoute(id) {
  return http.get(`/catalog/route/${id}`)
}

export function createRoute(data) {
  return http.post('/catalog/route', data)
}

export function updateRoute(id, data) {
  return http.put(`/catalog/route/${id}`, data)
}

export function setRouteStatus(id, status) {
  return http.put(`/catalog/route/${id}/status`, null, { params: { status } })
}

export function deleteRoute(id) {
  return http.delete(`/catalog/route/${id}`)
}

/* 线路分类 */
export function getCategoryTree() {
  return http.get('/catalog/route-category/tree')
}

export function getCategory(id) {
  return http.get(`/catalog/route-category/${id}`)
}

export function createCategory(data) {
  return http.post('/catalog/route-category', data)
}

export function updateCategory(id, data) {
  return http.put(`/catalog/route-category/${id}`, data)
}

export function setCategoryStatus(id, status) {
  return http.put(`/catalog/route-category/${id}/status`, null, { params: { status } })
}

export function deleteCategory(id) {
  return http.delete(`/catalog/route-category/${id}`)
}

/* 行程 */
export function listItineraries(routeId) {
  return http.get(`/catalog/route-itinerary/list/${routeId}`)
}

export function createItinerary(data) {
  return http.post('/catalog/route-itinerary', data)
}

export function updateItinerary(id, data) {
  return http.put(`/catalog/route-itinerary/${id}`, data)
}

export function deleteItinerary(id) {
  return http.delete(`/catalog/route-itinerary/${id}`)
}

export function saveItineraries(routeId, items) {
  return http.put(`/catalog/route-itinerary/batch/${routeId}`, { items })
}

/* 图片 */
export function listRouteImages(routeId) {
  return http.get(`/catalog/route-image/list/${routeId}`)
}

export function createRouteImage(data) {
  return http.post('/catalog/route-image', data)
}

export function deleteRouteImage(id) {
  return http.delete(`/catalog/route-image/${id}`)
}

/* 标签 */
export function pageTags(params) {
  return http.get('/catalog/route-tag/page', { params })
}

export function listTags() {
  return http.get('/catalog/route-tag/list')
}

export function createTag(data) {
  return http.post('/catalog/route-tag', data)
}

export function updateTag(id, data) {
  return http.put(`/catalog/route-tag/${id}`, data)
}

export function setTagStatus(id, status) {
  return http.put(`/catalog/route-tag/${id}/status`, null, { params: { status } })
}

export function deleteTag(id) {
  return http.delete(`/catalog/route-tag/${id}`)
}

/* 线路 ↔ 标签 */
export function listRouteTags(routeId) {
  return http.get(`/catalog/route-tag-rel/list/${routeId}`)
}

export function assignRouteTags(routeId, tagIds) {
  return http.put(`/catalog/route-tag-rel/${routeId}`, { tagIds })
}

export function removeRouteTag(routeId, tagId) {
  return http.delete(`/catalog/route-tag-rel/${routeId}/${tagId}`)
}

/* 班期 */
export function pageDepartures(params) {
  return http.get('/catalog/route-departure/page', { params })
}

export function listDepartures(routeId) {
  return http.get(`/catalog/route-departure/list/${routeId}`)
}

export function createDeparture(data) {
  return http.post('/catalog/route-departure', data)
}

export function updateDeparture(id, data) {
  return http.put(`/catalog/route-departure/${id}`, data)
}

export function setDepartureStatus(id, status) {
  return http.put(`/catalog/route-departure/${id}/status`, null, { params: { status } })
}

export function deleteDeparture(id) {
  return http.delete(`/catalog/route-departure/${id}`)
}

/* 评价 */
export function pageReviews(params) {
  return http.get('/catalog/route-review/page', { params })
}

export function getReview(id) {
  return http.get(`/catalog/route-review/${id}`)
}

export function replyReview(id, reply) {
  return http.put(`/catalog/route-review/${id}/reply`, { reply })
}

export function setReviewStatus(id, status) {
  return http.put(`/catalog/route-review/${id}/status`, null, { params: { status } })
}

export function deleteReview(id) {
  return http.delete(`/catalog/route-review/${id}`)
}
