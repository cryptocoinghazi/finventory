import * as React from "react"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { cn } from "@/lib/utils"

export function SkeletonCard({
  title,
  lines = 3,
  className,
}: {
  title?: React.ReactNode
  lines?: number
  className?: string
}) {
  return (
    <Card className={cn("rounded-2xl", className)}>
      {title ? (
        <CardHeader>
          <CardTitle className="text-sm text-muted-foreground">{title}</CardTitle>
        </CardHeader>
      ) : null}
      <CardContent className="space-y-2">
        {Array.from({ length: lines }).map((_, i) => (
          <div key={i} className="h-4 w-full max-w-[80%] rounded-md bg-muted animate-pulse" />
        ))}
      </CardContent>
    </Card>
  )
}

