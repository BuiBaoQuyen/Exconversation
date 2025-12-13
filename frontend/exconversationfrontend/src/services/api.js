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
      console.error('API Error:', error.response.status, error.response.data);
      
      // Handle specific error cases
      if (error.response.status === 500) {
        console.error('Server error:', error.response.data);
      } else if (error.response.status === 404) {
        console.error('Resource not found');
      } else if (error.response.status === 429) {
        console.error('Rate limit exceeded');
      }
    } else if (error.request) {
      // Request was made but no response received
      console.error('No response received:', error.request);
      error.message = 'Network error. Please check your connection.';
    } else {
      // Something else happened
      console.error('Error setting up request:', error.message);
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
  parse: (id) => api.post(`/upload/${id}/parse`), // AI parsing - no patternId needed
  delete: (id) => api.delete(`/upload/${id}`),
};

// Question API
export const questionAPI = {
  getAll: (chapterId, page = 0, size = 20) => 
    api.get('/questions', { params: { chapterId, page, size } }),
  getById: (id) => api.get(`/questions/${id}`),
  update: (id, data) => api.put(`/questions/${id}`, data),
  createVersion: (id, data) => api.post(`/questions/${id}/version`, data),
  delete: (id) => api.delete(`/questions/${id}`),
};

// Chapter API
export const chapterAPI = {
  getAll: () => api.get('/chapters'),
  getById: (id) => api.get(`/chapters/${id}`),
  getByUploadId: (uploadId) => api.get(`/chapters/upload/${uploadId}`),
};

// Exam API
export const examAPI = {
  getAll: () => api.get('/exams'),
  getById: (id) => api.get(`/exams/${id}`),
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
  printRandom: (numberOfQuestions = 40, includeAnswers = false) => {
    console.log('[API] printRandom called with params:', { numberOfQuestions, includeAnswers });
    console.log('[API] Making GET request to /exams/print-random');
    return api.get('/exams/print-random', {
      params: { numberOfQuestions, includeAnswers },
      responseType: 'blob',
    }).then(response => {
      console.log('[API] printRandom response received:', {
        status: response.status,
        statusText: response.statusText,
        headers: response.headers,
        dataType: response.data?.constructor?.name || typeof response.data,
        dataSize: response.data?.size || response.data?.length || 'unknown'
      });
      return response;
    }).catch(error => {
      console.error('[API] printRandom error:', error);
      console.error('[API] Error details:', {
        message: error.message,
        response: error.response,
        status: error.response?.status,
        statusText: error.response?.statusText,
        data: error.response?.data
      });
      throw error;
    });
  },
};

export default api;

