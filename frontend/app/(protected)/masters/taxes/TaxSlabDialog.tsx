"use client"

import { useEffect, useState } from "react"
import { useForm } from "react-hook-form"
import { Plus } from "lucide-react"

import { Button } from "@/components/ui/button"
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from "@/components/ui/dialog"
import { Input } from "@/components/ui/input"
import { createTaxSlab, updateTaxSlab, TaxSlab } from "@/lib/tax-slabs"

interface TaxSlabFormValues {
  rate: string
  description: string
}

interface TaxSlabDialogProps {
  onSuccess: () => void
  initialData?: TaxSlab | null
  open?: boolean
  onOpenChange?: (open: boolean) => void
  trigger?: React.ReactNode
}

export function TaxSlabDialog({ 
  onSuccess, 
  initialData, 
  open: controlledOpen, 
  onOpenChange: setControlledOpen,
  trigger 
}: TaxSlabDialogProps) {
  const [internalOpen, setInternalOpen] = useState(false)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const isControlled = controlledOpen !== undefined
  const open = isControlled ? controlledOpen : internalOpen
  const setOpen = isControlled ? setControlledOpen! : setInternalOpen

  const {
    register,
    handleSubmit,
    reset,
    setValue,
    formState: { errors },
  } = useForm<TaxSlabFormValues>({
    defaultValues: {
      rate: "",
      description: "",
    },
  })

  useEffect(() => {
    if (open) {
      if (initialData) {
        setValue("rate", String(initialData.rate))
        setValue("description", initialData.description)
      } else {
        reset({ rate: "", description: "" })
      }
      setError(null)
    }
  }, [open, initialData, setValue, reset])

  async function onSubmit(values: TaxSlabFormValues) {
    try {
      setLoading(true)
      setError(null)
      
      const payload = {
        rate: Number(values.rate),
        description: values.description,
      }

      if (initialData) {
        await updateTaxSlab(initialData.id, payload)
      } else {
        await createTaxSlab(payload)
      }
      
      setOpen(false)
      reset()
      onSuccess()
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to save tax slab")
    } finally {
      setLoading(false)
    }
  }

  return (
    <Dialog open={open} onOpenChange={setOpen}>
      {trigger && <DialogTrigger asChild>{trigger}</DialogTrigger>}
      {!trigger && !isControlled && (
         <DialogTrigger asChild>
          <Button className="gap-2">
            <Plus className="h-4 w-4" />
            New Tax Slab
          </Button>
        </DialogTrigger>
      )}
      <DialogContent className="sm:max-w-[425px]">
        <DialogHeader>
          <DialogTitle>{initialData ? "Edit Tax Slab" : "New Tax Slab"}</DialogTitle>
          <DialogDescription>
            {initialData ? "Update tax slab details." : "Create a new tax slab for GST calculation."}
          </DialogDescription>
        </DialogHeader>
        <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
          {error && (
            <div className="p-3 rounded-md bg-destructive/10 text-destructive text-sm">
              {error}
            </div>
          )}
          <div className="space-y-2">
            <label htmlFor="rate" className="text-sm font-medium">
              Rate (%)
            </label>
            <Input
              id="rate"
              placeholder="e.g. 18"
              {...register("rate", {
                required: "Rate is required",
                validate: (val) =>
                  (!isNaN(Number(val)) && Number(val) >= 0) ||
                  "Rate must be a positive number",
              })}
              disabled={loading}
            />
            {errors.rate && (
              <p className="text-sm text-destructive">{errors.rate.message}</p>
            )}
          </div>
          <div className="space-y-2">
            <label htmlFor="description" className="text-sm font-medium">
              Description
            </label>
            <Input
              id="description"
              placeholder="e.g. GST 18%"
              {...register("description", {
                required: "Description is required",
              })}
              disabled={loading}
            />
            {errors.description && (
              <p className="text-sm text-destructive">
                {errors.description.message}
              </p>
            )}
          </div>
          <DialogFooter>
            <Button type="submit" disabled={loading}>
              {loading ? "Creating..." : "Create"}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  )
}
