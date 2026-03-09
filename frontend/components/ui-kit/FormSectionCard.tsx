import * as React from "react"

export function FormSectionCard({
  title,
  children,
}: {
  title?: React.ReactNode
  children: React.ReactNode
}) {
  return (
    <section className="rounded-xl border border-border p-4 bg-muted/10 space-y-4">
      {title ? <div className="text-sm font-medium">{title}</div> : null}
      {children}
    </section>
  )
}

