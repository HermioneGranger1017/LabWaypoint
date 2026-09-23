const API_BASE = import.meta.env.VITE_API_BASE || '/api'

export async function request(path, options = {}) {
  const token = localStorage.getItem('labwaypoint_token')
  const headers = new Headers(options.headers || {})
  if (token) headers.set('Authorization', token)
  if (options.body && !(options.body instanceof FormData) && !(options.body instanceof URLSearchParams) && !headers.has('Content-Type')) {
    headers.set('Content-Type', 'application/json')
  }

  let response
  try {
    response = await fetch(`${API_BASE}${path}`, { ...options, headers })
  } catch {
    throw new Error('无法连接服务器，请检查后端服务是否运行。')
  }
  if (response.status === 401) {
    localStorage.removeItem('labwaypoint_token')
    window.dispatchEvent(new Event('labwaypoint:unauthorized'))
    throw new Error('登录已失效，请重新登录。')
  }
  if (response.status === 403) throw new Error('当前账号没有操作权限。')
  const raw = await response.text()
  let payload
  try { payload = raw ? JSON.parse(raw) : null } catch { throw new Error(`服务器返回了无法识别的内容（${response.status}）。`) }
  if (!response.ok) throw new Error(payload?.message || `请求失败（${response.status}）。`)
  if (payload?.code !== 0) throw new Error(payload?.message || '操作失败。')
  return payload.data
}

export const api = {
  get: (path) => request(path),
  post: (path, body) => request(path, { method: 'POST', body: JSON.stringify(body) }),
  put: (path, body) => request(path, { method: 'PUT', body: body === undefined ? undefined : JSON.stringify(body) }),
  patch: (path, body) => request(path, { method: 'PATCH', body: JSON.stringify(body) }),
  delete: (path) => request(path, { method: 'DELETE' }),
}

export function upload(file, scene = 'equipment') {
  const data = new FormData()
  data.append('file', file)
  data.append('scene', scene)
  return request('/file/upload', { method: 'POST', body: data })
}

export function assetUrl(value) {
  if (!value) return ''
  if (/^https?:\/\//i.test(value)) return value
  return value.startsWith('/') ? value : `/${value}`
}
