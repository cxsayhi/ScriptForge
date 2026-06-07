import axios, { AxiosError, type AxiosResponse } from 'axios'
import { ElMessage } from 'element-plus'
import type { ApiResponse } from './types'

export const API_BASE = import.meta.env.VITE_API_BASE_URL || '/api'

const http = axios.create({
  baseURL: API_BASE,
  timeout: 20000
})

function resolveHttpErrorMessage(error: AxiosError<Partial<ApiResponse<unknown>>>): string {
  const status = error.response?.status
  const serverMessage = error.response?.data?.message

  if (error.code === 'ECONNABORTED') {
    return '请求超时，AI 生成耗时较长，请稍后查看生成状态或重试'
  }

  if (serverMessage) {
    return serverMessage
  }

  if (status === 400) {
    return '请求参数有误，请检查输入内容'
  }

  if (status === 404) {
    return '请求的资源不存在'
  }

  if (status && status >= 500) {
    return '服务器暂时无法处理请求'
  }

  return error.message || '请求失败'
}

http.interceptors.response.use(
  (response) => response,
  (error: AxiosError<Partial<ApiResponse<unknown>>>) => {
    const message = resolveHttpErrorMessage(error)
    ElMessage.error(message)
    return Promise.reject(error)
  }
)

export async function unwrap<T>(request: Promise<AxiosResponse<ApiResponse<T>>>): Promise<T> {
  const response = await request
  if (response.data.code !== 200) {
    const message = response.data.message || '请求失败'
    ElMessage.error(message)
    throw new Error(message)
  }
  return response.data.data
}

export default http
