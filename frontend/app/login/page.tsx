"use client"

import { useState } from "react"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { motion } from "framer-motion"
import { InlineErrorCallout } from "@/components/ui-kit/InlineErrorCallout"
import { Eye, EyeOff, Lock, User } from "lucide-react"

export default function LoginPage() {
  const [username, setUsername] = useState("")
  const [password, setPassword] = useState("")
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [showPassword, setShowPassword] = useState(false)
  const [remember, setRemember] = useState(false)

  async function onSubmit(e: React.FormEvent) {
    e.preventDefault()
    setLoading(true)
    setError(null)
    try {
      const res = await fetch(`/api/auth/login`, {
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
    <div
      className="min-h-screen bg-background relative"
      style={{
        backgroundImage: "url('/globe.svg')",
        backgroundRepeat: "no-repeat",
        backgroundSize: "cover",
        backgroundPosition: "center",
      }}
    >
      <div className="absolute inset-0 bg-gradient-to-b from-background/60 to-background" />
      <header className="relative z-10 flex items-center justify-between px-6 py-4">
        <div className="flex items-center gap-2">
          <div className="h-7 w-7 rounded-md bg-primary/90" />
          <div className="text-lg font-semibold">Finventory</div>
        </div>
      </header>
      <div className="relative z-10 flex items-center justify-center px-4 py-10">
        <motion.form
          onSubmit={onSubmit}
          initial={{ opacity: 0, y: 12 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.18, ease: "easeOut" }}
          className="w-full max-w-md rounded-2xl border border-border bg-background/95 backdrop-blur p-8 shadow-soft space-y-6"
        >
          <div className="space-y-1 text-center">
            <h1 className="text-2xl font-semibold">Welcome back</h1>
            <p className="text-sm text-muted-foreground">Sign in to continue</p>
          </div>
          <div className="space-y-2">
            <label className="text-sm">Username</label>
            <div className="relative">
              <User className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
              <Input
                aria-label="Username"
                className="pl-9"
                value={username}
                onChange={(e) => setUsername(e.target.value)}
                placeholder="demo"
              />
            </div>
          </div>
          <div className="space-y-2">
            <label className="text-sm">Password</label>
            <div className="relative">
              <Lock className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
              <Input
                aria-label="Password"
                type={showPassword ? "text" : "password"}
                className="pl-9 pr-9"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                placeholder="••••••••"
              />
              <button
                type="button"
                aria-label={showPassword ? "Hide password" : "Show password"}
                className="absolute right-2 top-1/2 -translate-y-1/2 rounded-md p-1 text-muted-foreground hover:text-foreground"
                onClick={() => setShowPassword((v) => !v)}
              >
                {showPassword ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
              </button>
            </div>
          </div>
          {error ? <InlineErrorCallout message={error} /> : null}
          <div className="flex items-center justify-between">
            <label className="flex items-center gap-2 text-sm text-muted-foreground">
              <input
                aria-label="Remember me"
                type="checkbox"
                checked={remember}
                onChange={(e) => setRemember(e.target.checked)}
              />
              Remember me
            </label>
            <a href="#" className="text-sm text-muted-foreground hover:underline">
              Forgot your password?
            </a>
          </div>
          <Button disabled={loading} className="w-full">
            {loading ? "Signing in..." : "Sign in"}
          </Button>
        </motion.form>
      </div>
    </div>
  )
}
