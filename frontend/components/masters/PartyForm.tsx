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
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select"
import { FormLayout } from "@/components/ui-kit/FormLayout"
import { FormSectionCard } from "@/components/ui-kit/FormSectionCard"
import { PartyInput, PartyType } from "@/lib/parties"

const GSTIN_REGEX = /^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z][1-9A-Z]Z[0-9A-Z]$/

const partySchema = z
  .object({
    name: z.string().min(1, "Name is required"),
    type: z.enum(["CUSTOMER", "VENDOR"], {
      required_error: "Type is required",
    }),
    gstin: z
      .string()
      .regex(GSTIN_REGEX, "Invalid GSTIN format")
      .optional()
      .or(z.literal("")),
    stateCode: z
      .string()
      .regex(/^\d{2}$/, "State code must be 2 digits")
      .optional()
      .or(z.literal("")),
    address: z.string().optional(),
    phone: z.string().optional(),
    email: z.string().email("Invalid email format").optional().or(z.literal("")),
  })
  .refine(
    (data) => {
      if (data.gstin && data.gstin.length > 0 && !data.stateCode) {
        return false
      }
      return true
    },
    {
      message: "State code is required when GSTIN is provided",
      path: ["stateCode"],
    }
  )

type FormValues = z.infer<typeof partySchema>

export function PartyForm({
  initialValue,
  onSubmit,
  submitLabel,
}: {
  initialValue?: PartyInput
  submitLabel: string
  onSubmit: (input: PartyInput) => Promise<void>
}) {
  const router = useRouter()
  const [serverError, setServerError] = useState<string | null>(null)

  const form = useForm<FormValues>({
    resolver: zodResolver(partySchema),
    defaultValues: {
      name: initialValue?.name ?? "",
      type: initialValue?.type ?? "CUSTOMER",
      gstin: initialValue?.gstin ?? "",
      stateCode: initialValue?.stateCode ?? "",
      address: initialValue?.address ?? "",
      phone: initialValue?.phone ?? "",
      email: initialValue?.email ?? "",
    },
  })

  async function handleSubmit(data: FormValues) {
    setServerError(null)
    try {
      await onSubmit({
        name: data.name,
        type: data.type as PartyType,
        gstin: data.gstin || null,
        stateCode: data.stateCode || null,
        address: data.address || null,
        phone: data.phone || null,
        email: data.email || null,
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

          <FormSectionCard title="Details">
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <FormField
                control={form.control}
                name="name"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Name</FormLabel>
                    <FormControl>
                      <Input placeholder="Acme Traders" {...field} />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />

              <FormField
                control={form.control}
                name="type"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Type</FormLabel>
                    <Select
                      onValueChange={field.onChange}
                      defaultValue={field.value}
                    >
                      <FormControl>
                        <SelectTrigger>
                          <SelectValue placeholder="Select type" />
                        </SelectTrigger>
                      </FormControl>
                      <SelectContent>
                        <SelectItem value="CUSTOMER">Customer</SelectItem>
                        <SelectItem value="VENDOR">Vendor</SelectItem>
                      </SelectContent>
                    </Select>
                    <FormMessage />
                  </FormItem>
                )}
              />

              <FormField
                control={form.control}
                name="gstin"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>GSTIN</FormLabel>
                    <FormControl>
                      <Input placeholder="22ABCDE1234F1Z5" {...field} />
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
                name="email"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Email</FormLabel>
                    <FormControl>
                      <Input
                        type="email"
                        placeholder="contact@example.com"
                        {...field}
                      />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />

              <FormField
                control={form.control}
                name="phone"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Phone</FormLabel>
                    <FormControl>
                      <Input placeholder="9876543210" {...field} />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />

              <FormField
                control={form.control}
                name="address"
                render={({ field }) => (
                  <FormItem className="md:col-span-2">
                    <FormLabel>Address</FormLabel>
                    <FormControl>
                      <Input placeholder="Full Address" {...field} />
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
