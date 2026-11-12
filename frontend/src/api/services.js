import api from './client.js';

export const authApi = {
  login: (username, password) => api.post('/auth/login', { username, password }),
  register: (data) => api.post('/auth/register', data),
  logout: () => api.post('/auth/logout'),
  me: () => api.get('/auth/me'),
};

export const productApi = {
  list: (params) => api.get('/products', { params }),
  categories: () => api.get('/products/categories'),
  get: (id) => api.get(`/products/${id}`),
  create: (data) => api.post('/products', data),
  update: (id, data) => api.put(`/products/${id}`, data),
  remove: (id) => api.delete(`/products/${id}`),
};

export const orderApi = {
  // Only product ids + quantities are sent. Prices are decided by the server.
  place: (items) => api.post('/orders', { items }),
  mine: () => api.get('/orders/my'),
  all: () => api.get('/orders'),
  cancel: (id) => api.put(`/orders/${id}/cancel`),
};

export const adminUserApi = {
  list: () => api.get('/auth/admin/users'),
  setRoles: (id, roles) => api.put(`/auth/admin/users/${id}/roles`, { roles }),
  setEnabled: (id, enabled) => api.put(`/auth/admin/users/${id}/status`, { enabled }),
  unlock: (id) => api.put(`/auth/admin/users/${id}/unlock`),
};

export const notificationApi = {
  mine: () => api.get('/notifications/my'),
};
