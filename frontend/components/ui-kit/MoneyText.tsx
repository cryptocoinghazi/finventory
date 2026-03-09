import * as React from "react"
import { cn } from "@/lib/utils"

interface MoneyTextProps extends React.HTMLAttributes<HTMLSpanElement> {
  value: number
  currency?: string
}

export function MoneyText({ value, currency = "INR", className, ...props }: MoneyTextProps) {
  const formatted = new Intl.NumberFormat("en-IN", {
    style: "currency",
    currency: currency,
    minimumFractionDigits: 2,
  }).format(value)

  return (
    <span className={cn("font-mono", className)} {...props}>
      {formatted}
    </span>
  )
}
