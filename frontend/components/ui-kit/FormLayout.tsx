"use client"

import * as React from "react"
import { cn } from "@/lib/utils"

interface FormLayoutProps extends React.HTMLAttributes<HTMLDivElement> {
  children: React.ReactNode
  stickyFooter?: React.ReactNode
}

export function FormLayout({ children, stickyFooter, className, ...props }: FormLayoutProps) {
  return (
    <div className={cn("relative flex flex-col gap-8 pb-24", className)} {...props}>
      {children}
      {stickyFooter && (
        <div className="fixed bottom-0 left-0 right-0 z-50 border-t bg-background/80 px-6 py-4 backdrop-blur-sm sm:left-64">
          <div className="mx-auto flex max-w-5xl items-center justify-end gap-4">
            {stickyFooter}
          </div>
        </div>
      )}
    </div>
  )
}
