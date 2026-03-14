"use client"

import { useEffect } from "react"
import {
  Toast,
  ToastClose,
  ToastDescription,
  ToastProvider,
  ToastTitle,
  ToastViewport,
} from "@/components/ui/toast"
import { useToast } from "@/components/ui/use-toast"

export function Toaster() {
  const { toasts } = useToast()

  useEffect(() => {
    const shouldReload = (value: unknown) => {
      const msg =
        value instanceof Error
          ? value.message
          : typeof value === "string"
            ? value
            : value && typeof value === "object" && "message" in value
              ? String((value as { message?: unknown }).message)
              : ""

      return (
        msg.includes("ChunkLoadError") ||
        msg.includes("Loading chunk") ||
        msg.includes("Failed to fetch dynamically imported module")
      )
    }

    const reloadOnce = () => {
      try {
        const k = "finventory:chunk-reload-at"
        const now = Date.now()
        const last = Number(sessionStorage.getItem(k) ?? "0")
        if (now - last < 15_000) return
        sessionStorage.setItem(k, String(now))
      } catch {
      }
      window.location.reload()
    }

    const onError = (event: ErrorEvent) => {
      if (!shouldReload(event.error ?? event.message)) return
      reloadOnce()
    }

    const onUnhandledRejection = (event: PromiseRejectionEvent) => {
      if (!shouldReload(event.reason)) return
      reloadOnce()
    }

    window.addEventListener("error", onError)
    window.addEventListener("unhandledrejection", onUnhandledRejection)
    return () => {
      window.removeEventListener("error", onError)
      window.removeEventListener("unhandledrejection", onUnhandledRejection)
    }
  }, [])

  return (
    <ToastProvider>
      {toasts.map(function ({ id, title, description, action, ...props }) {
        return (
          <Toast key={id} {...props}>
            <div className="grid gap-1">
              {title && <ToastTitle>{title}</ToastTitle>}
              {description && (
                <ToastDescription>{description}</ToastDescription>
              )}
            </div>
            {action}
            <ToastClose />
          </Toast>
        )
      })}
      <ToastViewport />
    </ToastProvider>
  )
}
