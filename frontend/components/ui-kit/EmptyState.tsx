import * as React from "react"
import { Button } from "@/components/ui/button"

export function EmptyState({
  title,
  description,
  action,
  icon,
}: {
  title: React.ReactNode
  description?: React.ReactNode
  action?: { label: string; onClick: () => void }
  icon?: React.ReactNode
}) {
  return (
    <div className="rounded-2xl border border-border p-10 text-center bg-muted/20">
      {icon ? <div className="mx-auto mb-4 h-10 w-10 text-muted-foreground">{icon}</div> : null}
      <div className="text-lg font-semibold">{title}</div>
      {description ? (
        <div className="mt-1 text-sm text-muted-foreground">{description}</div>
      ) : null}
      {action ? (
        <div className="mt-4">
          <Button onClick={action.onClick}>{action.label}</Button>
        </div>
      ) : null}
    </div>
  )
}
