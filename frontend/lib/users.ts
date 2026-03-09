import { API_BASE } from "./api"

export interface User {
  id: string
  username: string
  email: string
  role: string
  password?: string
}

export type UserInput = Omit<User, "id">

export async function listUsers(): Promise<User[]> {
  const res = await fetch(`${API_BASE}/api/v1/users`, {
    headers: {
      Authorization: `Bearer ${window.localStorage.getItem("token")}`,
    },
  })
  if (!res.ok) throw new Error("Failed to fetch users")
  return res.json()
}

export async function createUser(data: UserInput): Promise<User> {
  const res = await fetch(`${API_BASE}/api/v1/users`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${window.localStorage.getItem("token")}`,
    },
    body: JSON.stringify(data),
  })
  if (!res.ok) throw new Error("Failed to create user")
  return res.json()
}

export async function deleteUser(id: string): Promise<void> {
  const res = await fetch(`${API_BASE}/api/v1/users/${id}`, {
    method: "DELETE",
    headers: {
      Authorization: `Bearer ${window.localStorage.getItem("token")}`,
    },
  })
  if (!res.ok) throw new Error("Failed to delete user")
}
