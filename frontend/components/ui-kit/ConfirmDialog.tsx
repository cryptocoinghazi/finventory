import * as React from "react"
import { Button } from "@/components/ui/button"

export function ConfirmDialog({
  title,
  description,
  confirmText = "Confirm",
  cancelText = "Cancel",
  onConfirm,
  onCancel,
  disabled,
  children,
}: {
  title: React.ReactNode
  description?: React.ReactNode
  confirmText?: string
  cancelText?: string
  onConfirm: () => void
  onCancel?: () => void
  disabled?: boolean
  children?: React.ReactNode
}) {
  const [open, setOpen] = React.useState(false)
  return (
    <>
      <div
        onClick={() => {
          if (disabled) return
          setOpen(true)
        }}
        className={
          "inline-block " + (disabled ? "cursor-not-allowed opacity-50" : "cursor-pointer")
        }
      >
        {children || (
          <Button variant="destructive">{confirmText}</Button>
        )}
      </div>
      {open ? (
        <div className="fixed inset-0 z-50 flex items-center justify-center">
          <div className="absolute inset-0 bg-black/30" onClick={() => setOpen(false)} />
          <div className="relative z-10 w-full max-w-sm rounded-xl border border-border bg-background p-6 shadow-lg">
            <div className="text-lg font-semibold">{title}</div>
            {description ? (
              <div className="mt-1 text-sm text-muted-foreground">{description}</div>
            ) : null}
            <div className="mt-4 flex items-center justify-end gap-2">
              <Button
                variant="outline"
                onClick={() => {
                  setOpen(false)
                  onCancel?.()
                }}
              >
                {cancelText}
              </Button>
              <Button
                variant="destructive"
                onClick={() => {
                  setOpen(false)
                  onConfirm()
                }}
              >
                {confirmText}
              </Button>
            </div>
          </div>
        </div>
      ) : null}
    </>
  )
}
