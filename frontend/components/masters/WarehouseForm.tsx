"use client"

import { useForm } from "react-hook-form"
import { zodResolver } from "@hookform/resolvers/zod"
import { z } from "zod"
import { useRouter } from "next/navigation"
import { useState } from "react"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import {
  Form,
  FormControl,
  FormField,
  FormItem,
  FormLabel,
  FormMessage,
} from "@/components/ui/form"
import { FormLayout } from "@/components/ui-kit/FormLayout"
import { FormSectionCard } from "@/components/ui-kit/FormSectionCard"
import { WarehouseInput } from "@/lib/warehouses"

const warehouseSchema = z.object({
  name: z.string().min(1, "Name is required"),
  stateCode: z
    .string()
    .regex(/^\d{2}$/, "State code must be 2 digits")
    .optional()
    .or(z.literal("")),
  location: z.string().optional(),
})

type FormValues = z.infer<typeof warehouseSchema>

export function WarehouseForm({
  initialValue,
  onSubmit,
  submitLabel,
}: {
  initialValue?: WarehouseInput
  submitLabel: string
  onSubmit: (input: WarehouseInput) => Promise<void>
}) {
  const router = useRouter()
  const [serverError, setServerError] = useState<string | null>(null)

  const form = useForm<FormValues>({
    resolver: zodResolver(warehouseSchema),
    defaultValues: {
      name: initialValue?.name ?? "",
      stateCode: initialValue?.stateCode ?? "",
      location: initialValue?.location ?? "",
    },
  })

  async function handleSubmit(data: FormValues) {
    setServerError(null)
    try {
      await onSubmit({
        name: data.name,
        stateCode: data.stateCode || null,
        location: data.location || null,
      })
    } catch (err) {
      setServerError(err instanceof Error ? err.message : "An error occurred")
    }
  }

  return (
    <Form {...form}>
      <form onSubmit={form.handleSubmit(handleSubmit)}>
        <FormLayout
          stickyFooter={
            <>
              <Button
                type="button"
                variant="outline"
                onClick={() => router.back()}
              >
                Cancel
              </Button>
              <Button type="submit" disabled={form.formState.isSubmitting}>
                {form.formState.isSubmitting ? "Saving..." : submitLabel}
              </Button>
            </>
          }
        >
          {serverError && (
            <div className="bg-destructive/15 text-destructive px-4 py-2 rounded-md">
              {serverError}
            </div>
          )}

          <FormSectionCard title="Warehouse Details">
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <FormField
                control={form.control}
                name="name"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Name</FormLabel>
                    <FormControl>
                      <Input placeholder="Main Warehouse" {...field} />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />

              <FormField
                control={form.control}
                name="stateCode"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>State Code</FormLabel>
                    <FormControl>
                      <Input placeholder="22" maxLength={2} {...field} />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />

              <FormField
                control={form.control}
                name="location"
                render={({ field }) => (
                  <FormItem className="md:col-span-2">
                    <FormLabel>Location</FormLabel>
                    <FormControl>
                      <Input placeholder="City/Address" {...field} />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />
            </div>
          </FormSectionCard>
        </FormLayout>
      </form>
    </Form>
  )
}
