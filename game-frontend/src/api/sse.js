import { getAccessToken, clearLogin } from '../utils/auth'
import { refreshLogin } from './http'

// SSE 不能用 EventSource（不支持 PUT + Authorization 头），
// 用 fetch + ReadableStream 手动解析服务端事件流
const BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'

/**
 * 发送一次对话请求并以 SSE 流式接收 AI 回复。
 * @param {object}   options
 * @param {string}   options.memoryId 会话ID
 * @param {string}   options.message  玩家消息
 * @param {function} options.onToken  收到增量文本片段时回调
 * @param {function} options.onDone   生成结束时回调
 * @param {function} options.onError  出错时回调（Promise 正常 resolve，不抛异常）
 */
export async function streamChat({ memoryId, message, onToken, onDone, onError }) {
  const handlers = { onToken, onDone, onError }
  try {
    await doRequest(memoryId, message, getAccessToken(), handlers)
  } catch (err) {
    if (!err.auth) {
      onError(err instanceof Error ? err : new Error('网络错误，请检查连接后重试'))
      return
    }
    // access token 过期: 走与 axios 拦截器同一套刷新逻辑（带并发锁），成功后重试一次
    try {
      await refreshLogin()
      await doRequest(memoryId, message, getAccessToken(), handlers)
    } catch (e) {
      clearLogin()
      onError(new Error('登录已过期，请重新登录'))
    }
  }
}

async function doRequest(memoryId, message, accessToken, { onToken, onDone, onError }) {
  const response = await fetch(`${BASE_URL}/chat`, {
    method: 'PUT',
    headers: {
      'Content-Type': 'application/json',
      Authorization: `Bearer ${accessToken}`
    },
    body: JSON.stringify({ memoryId, message })
  })

  if (response.status === 401) {
    const err = new Error('未登录')
    err.auth = true
    throw err
  }

  const contentType = response.headers.get('content-type') || ''
  // 非 SSE 响应（业务错误如限流 42900、参数错误）: 解析统一 Result JSON 取提示
  if (!contentType.includes('text/event-stream')) {
    let msg = '服务异常'
    try {
      const result = await response.json()
      msg = result.message || msg
    } catch (e) {
      // 忽略非 JSON 错误体
    }
    throw new Error(msg)
  }

  await readStream(response.body, { onToken, onDone, onError })
}

async function readStream(body, { onToken, onDone, onError }) {
  const reader = body.getReader()
  const decoder = new TextDecoder('utf-8')
  let buffer = ''
  while (true) {
    const { done, value } = await reader.read()
    if (done) break
    buffer += decoder.decode(value, { stream: true })
    // SSE 事件以空行分隔，逐条处理
    let boundary
    while ((boundary = buffer.indexOf('\n\n')) >= 0) {
      const eventBlock = buffer.slice(0, boundary)
      buffer = buffer.slice(boundary + 2)
      for (const line of eventBlock.split('\n')) {
        if (!line.startsWith('data:')) continue
        const event = JSON.parse(line.slice(5).trim())
        if (event.type === 'token') onToken(event.text)
        else if (event.type === 'done') onDone()
        else if (event.type === 'error') onError(new Error(event.message || 'AI 生成失败'))
      }
    }
  }
}
