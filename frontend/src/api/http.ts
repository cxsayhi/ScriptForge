import axios, { AxiosError } from 'axios'
import { ElMessage } from 'element-plus'
import type { ApiResponse } from './types'

export const API_BASE = import.meta.env.VITE_API_BASE_URL || '/api'

const http = axios.create({
  baseURL: API_BASE,
  timeout: 20000
})

http.interceptors.response.use(
  (response) => response,
  (error: AxiosError<ApiResponse<unknown>>) => {
    const message = error.response?.data?.message || error.message || '请求失败'
    ElMessage.error(message)
    return Promise.reject(error)
  }
)

export async function unwrap<T>(request: Promise<{ data: ApiResponse<T> }>): Promise<T> {
  const response = await request
  if (response.data.code !== 200) {
    throw new Error(response.data.message)
  }
  return response.data.data
}

export default http
