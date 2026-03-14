import * as React from "react"
import { cn } from "@/lib/utils"

interface FormSectionCardProps extends Omit<React.HTMLAttributes<HTMLDivElement>, "title"> {
  title?: React.ReactNode
  description?: React.ReactNode
  children: React.ReactNode
}

export function FormSectionCard({
  title,
  description,
  children,
  className,
  ...props
}: FormSectionCardProps) {
  return (
    <div
      className={cn(
        "rounded-xl border bg-card text-card-foreground shadow-sm",
        className
      )}
      {...props}
    >
      {(title || description) && (
        <div className="flex flex-col space-y-1.5 p-6 pb-4">
          {title && <h3 className="font-semibold leading-none tracking-tight">{title}</h3>}
          {description && (
            <p className="text-sm text-muted-foreground">{description}</p>
          )}
        </div>
      )}
      <div className="p-6 pt-0">{children}</div>
    </div>
  )
}
