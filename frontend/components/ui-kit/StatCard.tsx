import * as React from "react"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Badge } from "@/components/ui/badge"
import { cn } from "@/lib/utils"
import { ArrowDown, ArrowUp } from "lucide-react"

export function StatCard({
  title,
  value,
  description,
  loading,
  delta,
  positive,
  icon,
  className,
}: {
  title: React.ReactNode
  value?: React.ReactNode
  description?: React.ReactNode
  loading?: boolean
  delta?: React.ReactNode
  positive?: boolean
  icon?: React.ReactNode
  className?: string
}) {
  return (
    <Card
      className={cn(
        "rounded-2xl transition-all hover:shadow-soft-lg hover:-translate-y-[1px]",
        className
      )}
    >
      <CardHeader className="flex flex-row items-center justify-between">
        <CardTitle className="text-sm text-muted-foreground">{title}</CardTitle>
        {icon ? <div className="h-6 w-6 text-muted-foreground">{icon}</div> : null}
      </CardHeader>
      <CardContent className="pt-2">
        {loading ? (
          <div className="h-8 w-28 rounded-md bg-muted animate-pulse" />
        ) : value ? (
          <div className="text-2xl font-semibold">{value}</div>
        ) : (
          <Badge variant="secondary">Coming Soon</Badge>
        )}
        <div className="mt-2 flex items-center gap-2">
          {delta ? (
            <span
              className={cn(
                "inline-flex items-center gap-1 rounded-full px-2 py-0.5 text-xs",
                positive
                  ? "bg-green-500/10 text-green-600"
                  : "bg-red-500/10 text-red-600"
              )}
            >
              {positive ? (
                <ArrowUp className="h-3.5 w-3.5" />
              ) : (
                <ArrowDown className="h-3.5 w-3.5" />
              )}
              {delta}
            </span>
          ) : null}
          {description ? (
            <div className="text-xs text-muted-foreground">{description}</div>
          ) : null}
        </div>
      </CardContent>
    </Card>
  )
}
