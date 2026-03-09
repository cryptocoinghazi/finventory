"use client"

import { useState } from "react"
import { Button } from "@/components/ui/button"

export default function LoginPage() {
  const [username, setUsername] = useState("")
  const [password, setPassword] = useState("")
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  async function onSubmit(e: React.FormEvent) {
    e.preventDefault()
    setLoading(true)
    setError(null)
    try {
      const res = await fetch(`${process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080"}/api/v1/auth/login`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ username, password }),
      })
      if (!res.ok) {
        const msg = await res.text()
        throw new Error(msg || "Login failed")
      }
      const data = await res.json()
      document.cookie = `token=${data.token}; path=/`
      window.location.href = "/dashboard"
    } catch (err: any) {
      setError(err.message || "Login failed")
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="min-h-screen flex items-center justify-center bg-background">
      <form
        onSubmit={onSubmit}
        className="w-full max-w-sm rounded-lg border border-border p-6 space-y-4"
      >
        <div>
          <h1 className="text-xl font-semibold">Login</h1>
          <p className="text-sm text-muted-foreground">
            Enter credentials to continue
          </p>
        </div>
        <div className="space-y-2">
          <label className="text-sm">Username</label>
          <input
            className="w-full px-3 py-2 rounded-md border border-input bg-background"
            value={username}
            onChange={(e) => setUsername(e.target.value)}
            placeholder="user@example.com"
          />
        </div>
        <div className="space-y-2">
          <label className="text-sm">Password</label>
          <input
            type="password"
            className="w-full px-3 py-2 rounded-md border border-input bg-background"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            placeholder="••••••••"
          />
        </div>
        {error ? (
          <div className="text-sm text-destructive">{error}</div>
        ) : null}
        <Button disabled={loading} className="w-full">
          {loading ? "Signing in..." : "Sign in"}
        </Button>
      </form>
    </div>
  )
}

