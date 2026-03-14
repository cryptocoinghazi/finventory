import * as React from "react"
import { cn } from "@/lib/utils"

export function Kbd({ className, ...props }: React.HTMLAttributes<HTMLElement>) {
  return (
    <kbd
      className={cn(
        "pointer-events-none inline-flex h-6 select-none items-center gap-1 rounded-md border border-border bg-muted px-2 font-mono text-[11px] text-muted-foreground",
        className
      )}
      {...props}
    />
  )
}

