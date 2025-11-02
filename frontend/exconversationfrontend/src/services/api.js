import axios from 'axios';

const API_BASE_URL = 'http://localhost:8080/api';

const api = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Request interceptor
api.interceptors.request.use(
  (config) => {
    // Add auth token if needed
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// Response interceptor
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response) {
      // Handle errors
      console.error('API Error:', error.response.data);
    }
    return Promise.reject(error);
  }
);

// Pattern API
export const patternAPI = {
  getAll: () => api.get('/patterns'),
  getById: (id) => api.get(`/patterns/${id}`),
  create: (data) => api.post('/patterns', data),
  update: (id, data) => api.put(`/patterns/${id}`, data),
  delete: (id) => api.delete(`/patterns/${id}`),
  test: (id, testText) => api.post(`/patterns/${id}/test`, testText, {
    headers: { 'Content-Type': 'text/plain' },
  }),
};

// Upload API
export const uploadAPI = {
  upload: (file, uploadedByName) => {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('uploadedByName', uploadedByName || 'System');
    return api.post('/upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    });
  },
  getAll: () => api.get('/upload'),
  getById: (id) => api.get(`/upload/${id}`),
  getByStatus: (status) => api.get(`/upload/status/${status}`),
  parse: (id, patternId) => api.post(`/upload/${id}/parse`, null, {
    params: { patternId },
  }),
  delete: (id) => api.delete(`/upload/${id}`),
};

// Question API
export const questionAPI = {
  getAll: (chapterId) => api.get('/questions', { params: { chapterId } }),
  getById: (id) => api.get(`/questions/${id}`),
  update: (id, data) => api.put(`/questions/${id}`, data),
  createVersion: (id, data) => api.post(`/questions/${id}/version`, data),
  delete: (id) => api.delete(`/questions/${id}`),
};

// Chapter API (if needed)
export const chapterAPI = {
  getAll: () => api.get('/chapters').catch(() => ({ data: [] })), // Return empty if endpoint not exists
};

// Blueprint API
export const blueprintAPI = {
  getAll: () => api.get('/blueprints'),
  getById: (id) => api.get(`/blueprints/${id}`),
  create: (data) => api.post('/blueprints', data),
  update: (id, data) => api.put(`/blueprints/${id}`, data),
  delete: (id) => api.delete(`/blueprints/${id}`),
};

// Exam API
export const examAPI = {
  getAll: () => api.get('/exams'),
  getById: (id) => api.get(`/exams/${id}`),
  generate: (blueprintId, examName, createdByName) =>
    api.post('/exams/generate', null, {
      params: { blueprintId, examName, createdByName },
    }),
  delete: (id) => api.delete(`/exams/${id}`),
  export: (id, includeAnswers = false) =>
    api.get(`/exams/${id}/export`, {
      params: { includeAnswers },
      responseType: 'blob',
    }),
  exportWithAnswers: (id) =>
    api.get(`/exams/${id}/export-with-answers`, {
      responseType: 'blob',
    }),
};

export default api;

