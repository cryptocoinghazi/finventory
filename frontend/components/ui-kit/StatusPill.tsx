import * as React from "react"
import { Badge } from "@/components/ui/badge"

interface StatusPillProps extends React.ComponentProps<typeof Badge> {
  status: string
}

const statusMap: Record<string, "default" | "secondary" | "destructive" | "outline"> = {
  DRAFT: "secondary",
  POSTED: "default",
  APPROVED: "default",
  CANCELLED: "destructive",
  PAID: "outline",
  PARTIAL: "secondary",
  OVERDUE: "destructive",
}

export function StatusPill({ status, className, ...props }: StatusPillProps) {
  const variant = statusMap[status.toUpperCase()] || "outline"
  
  return (
    <Badge variant={variant} className={className} {...props}>
      {status}
    </Badge>
  )
}
