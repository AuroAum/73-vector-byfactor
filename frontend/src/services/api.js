import axios from 'axios';

/**
 * API service: handles all communication with the Spring Boot backend.
 * 
 * We use axios because it:
 * - Supports file upload progress tracking
 * - Handles multipart/form-data automatically
 * - Has clean error handling
 */

const API_BASE = '/api/documents';

/**
 * Uploads a document file and returns the OCR results.
 * 
 * @param {File} file - The file to upload (PDF, PNG, JPG)
 * @param {Function} onProgress - Callback for upload progress (0-100)
 * @returns {Promise<Object>} - The processing results
 */
export const uploadDocument = async (file, onProgress) => {
  // FormData is used for file uploads (multipart/form-data encoding)
  const formData = new FormData();
  formData.append('file', file);

  try {
    const response = await axios.post(`${API_BASE}/upload`, formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
      // Track upload progress for the progress bar
      onUploadProgress: (progressEvent) => {
        if (onProgress && progressEvent.total) {
          const percent = Math.round((progressEvent.loaded * 100) / progressEvent.total);
          onProgress(percent);
        }
      },
      // OCR can take a while, don't timeout too early
      timeout: 120000, // 2 minutes
    });

    return response.data;
  } catch (error) {
    if (error.response) {
      // Server returned an error response
      throw new Error(error.response.data.error || 'Server error occurred');
    } else if (error.request) {
      // No response received (server might be down)
      throw new Error('Cannot connect to server. Is the backend running?');
    } else {
      throw new Error('Failed to upload: ' + error.message);
    }
  }
};

/**
 * Uploads multiple documents and returns per-file processing results.
 *
 * @param {File[]} files - Files to upload
 * @param {Function} onProgress - Callback for upload progress (0-100)
 * @returns {Promise<{results: Object[], errors: Object[], totalFiles: number, processedFiles: number}>}
 */
export const uploadDocuments = async (files, onProgress) => {
  const formData = new FormData();
  files.forEach((file) => formData.append('files', file));

  try {
    const response = await axios.post(`${API_BASE}/upload-multiple`, formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
      onUploadProgress: (progressEvent) => {
        if (onProgress && progressEvent.total) {
          const percent = Math.round((progressEvent.loaded * 100) / progressEvent.total);
          onProgress(percent);
        }
      },
      timeout: 300000, // 5 minutes for multi-file OCR
    });

    return response.data;
  } catch (error) {
    if (error.response) {
      throw new Error(error.response.data.error || 'Server error occurred');
    } else if (error.request) {
      throw new Error('Cannot connect to server. Is the backend running?');
    } else {
      throw new Error('Failed to upload files: ' + error.message);
    }
  }
};

/**
 * Health check: verifies the backend is running.
 */
export const checkHealth = async () => {
  try {
    const response = await axios.get(`${API_BASE}/health`, { timeout: 5000 });
    return response.data;
  } catch {
    return null;
  }
};
