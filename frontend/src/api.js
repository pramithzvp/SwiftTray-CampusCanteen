let csrf;
export class ApiError extends Error {
  constructor(message, status) { super(message); this.status = status; }
}
export async function api(path, options = {}) {
  const method = options.method || 'GET';
  if (!['GET', 'HEAD'].includes(method) && !csrf) csrf = await api('/auth/csrf');
  let response;
  try {
    response = await fetch(`/api${path}`, {
      credentials: 'same-origin', ...options,
      headers: { 'Content-Type': 'application/json', ...(csrf && method !== 'GET' ? { [csrf.headerName]: csrf.token } : {}), ...options.headers },
      ...(options.body ? { body: JSON.stringify(options.body) } : {}),
      signal: options.signal || AbortSignal.timeout(12000),
    });
  } catch { throw new ApiError('The canteen service is unavailable. Please try again shortly.', 0); }
  if (response.status === 204) return null;
  const data = await response.json().catch(() => null);
  if (!response.ok) {
    if (response.status === 403) csrf = null;
    throw new ApiError(data?.message || 'The canteen service is unavailable. Please try again shortly.', response.status);
  }
  return data;
}
export function clearCsrf() { csrf = null; }
