import { apiFetch } from "./api"

export interface User {
  id: string
  username: string
  email: string
  role: string
  password?: string
}

export type UserInput = Omit<User, "id">

export async function listUsers(): Promise<User[]> {
  const res = await apiFetch("/api/v1/users")
  if (!res.ok) throw new Error("Failed to fetch users")
  return res.json()
}

export async function createUser(data: UserInput): Promise<User> {
  const res = await apiFetch("/api/v1/users", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(data),
  })
  if (!res.ok) throw new Error("Failed to create user")
  return res.json()
}

export async function deleteUser(id: string): Promise<void> {
  const res = await apiFetch(`/api/v1/users/${id}`, {
    method: "DELETE",
  })
  if (!res.ok) throw new Error("Failed to delete user")
}

export async function getCurrentUser(): Promise<User> {
  const res = await apiFetch("/api/v1/users/me")
  if (!res.ok) throw new Error("Failed to fetch current user")
  return res.json()
}

export async function updatePassword(data: any): Promise<void> {
  const res = await apiFetch("/api/v1/users/me/password", {
    method: "PUT",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(data),
  })
  if (!res.ok) {
    const text = await res.text()
    throw new Error(text || "Failed to update password")
  }
}
