export const API_BASE =
  process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080"

export async function apiFetch(
  path: string,
  init: RequestInit = {}
): Promise<Response> {
  const token = getToken()
  const headers = new Headers(init.headers)
  if (token) headers.set("Authorization", `Bearer ${token}`)
  const res = await fetch(`${API_BASE}${path}`, {
    ...init,
    headers,
  })
  if (typeof window !== "undefined" && (res.status === 401 || res.status === 403)) {
    try {
      window.localStorage.setItem("authMissing", "1")
    } catch {}
  }
  if (res.status === 401 && typeof window !== "undefined") {
    try {
      window.location.href = "/login"
    } catch {}
  }
  return res
}

function getToken(): string | null {
  if (typeof window !== "undefined") {
    const fromLocal = window.localStorage.getItem("token")
    if (fromLocal) return fromLocal
    const fromCookie = getCookie("token")
    if (fromCookie) return fromCookie
  }
  return null
}

function getCookie(name: string): string | null {
  if (typeof document === "undefined") return null
  const value = `; ${document.cookie}`
  const parts = value.split(`; ${name}=`)
  if (parts.length === 2) return parts.pop()!.split(";").shift() || null
  return null
}
