import { API_BASE } from "./api"

export interface User {
  id: string
  username: string
  email: string
  role: string
}

export async function listUsers(): Promise<User[]> {
  const res = await fetch(`${API_BASE}/api/v1/users`, {
    headers: {
      Authorization: `Bearer ${window.localStorage.getItem("token")}`,
    },
  })
  if (!res.ok) throw new Error("Failed to fetch users")
  return res.json()
}
