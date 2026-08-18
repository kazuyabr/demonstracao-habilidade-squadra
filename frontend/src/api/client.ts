import axios, { AxiosInstance } from 'axios';
import config from '../config';
import * as oauth from '../auth/oauth';

export const api: AxiosInstance = axios.create({
  baseURL: config.apiGateway
});

// Attach the bearer token to every request
api.interceptors.request.use(async (request) => {
  let token = oauth.getAccessToken();
  if (token && !oauth.isTokenValid()) {
    token = await oauth.refreshAccessToken();
  }
  if (token) {
    request.headers = request.headers || {};
    request.headers.Authorization = `Bearer ${token}`;
  }
  return request;
});

// On 401, attempt a refresh once, then fail
api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const original = error.config;
    if (error.response?.status === 401 && !original?._retry) {
      original._retry = true;
      const newToken = await oauth.refreshAccessToken();
      if (newToken) {
        original.headers = original.headers || {};
        original.headers.Authorization = `Bearer ${newToken}`;
        return api(original);
      }
    }
    return Promise.reject(error);
  }
);
