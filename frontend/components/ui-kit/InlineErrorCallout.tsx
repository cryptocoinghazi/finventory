import * as React from "react"

export function InlineErrorCallout({
  message,
}: {
  message: React.ReactNode
}) {
  return (
    <div className="rounded-md border border-destructive/40 bg-destructive/10 text-destructive px-3 py-2 text-sm">
      {message}
    </div>
  )
}

