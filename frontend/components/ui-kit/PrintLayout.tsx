import * as React from "react"
import { cn } from "@/lib/utils"

export function PrintLayout({ children, className, ...props }: React.HTMLAttributes<HTMLDivElement>) {
  return (
    <div
      className={cn(
        "hidden print:block print:w-full print:bg-white print:text-black",
        className
      )}
      {...props}
    >
      {children}
    </div>
  )
}
