"use client"

import { Button } from "@/components/ui/button"
import { FormLayout } from "@/components/ui-kit/FormLayout"
import { FormSectionCard } from "@/components/ui-kit/FormSectionCard"
import { PartyInput, PartyType } from "@/lib/parties"
import { useMemo, useState } from "react"

const GSTIN_REGEX =
  /^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z][1-9A-Z]Z[0-9A-Z]$/

export function PartyForm({
  initialValue,
  onSubmit,
  submitLabel,
}: {
  initialValue?: PartyInput
  submitLabel: string
  onSubmit: (input: PartyInput) => Promise<void>
}) {
  const [name, setName] = useState(initialValue?.name ?? "")
  const [type, setType] = useState<PartyType | "">(
    initialValue?.type ?? ""
  )
  const [gstin, setGstin] = useState(initialValue?.gstin ?? "")
  const [stateCode, setStateCode] = useState(initialValue?.stateCode ?? "")
  const [address, setAddress] = useState(initialValue?.address ?? "")
  const [phone, setPhone] = useState(initialValue?.phone ?? "")
  const [email, setEmail] = useState(initialValue?.email ?? "")
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const normalizedInput = useMemo<PartyInput>(() => {
    const cleanedGstin = gstin.trim().toUpperCase()
    return {
      name: name.trim(),
      type: type as PartyType,
      gstin: cleanedGstin ? cleanedGstin : null,
      stateCode: stateCode.trim() ? stateCode.trim() : null,
      address: address.trim() ? address.trim() : null,
      phone: phone.trim() ? phone.trim() : null,
      email: email.trim() ? email.trim() : null,
    }
  }, [address, email, gstin, name, phone, stateCode, type])

  const validationError = useMemo(() => {
    if (!normalizedInput.name) return "Name is required"
    if (!type) return "Type is required"
    if (normalizedInput.gstin && !GSTIN_REGEX.test(normalizedInput.gstin)) {
      return "GSTIN format is invalid"
    }
    if (normalizedInput.gstin && !normalizedInput.stateCode) {
      return "State code is required when GSTIN is provided"
    }
    if (
      normalizedInput.email &&
      !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(normalizedInput.email)
    ) {
      return "Email format is invalid"
    }
    if (normalizedInput.stateCode && !/^\d{2}$/.test(normalizedInput.stateCode)) {
      return "State code must be 2 digits"
    }
    return null
  }, [normalizedInput, type])

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    setError(null)
    if (validationError) {
      setError(validationError)
      return
    }
    setSubmitting(true)
    try {
      await onSubmit(normalizedInput)
    } catch (err: any) {
      setError(err?.message || "Save failed")
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <form onSubmit={handleSubmit} className="space-y-6">
      <FormLayout>
        <FormSectionCard title="Details">
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <Field label="Name">
            <input
              className="w-full px-3 py-2 rounded-md border border-input bg-background"
              value={name}
              onChange={(e) => setName(e.target.value)}
              placeholder="Acme Traders"
              autoFocus
            />
          </Field>
          <Field label="Type">
            <select
              className="w-full px-3 py-2 rounded-md border border-input bg-background"
              value={type}
              onChange={(e) => setType(e.target.value as PartyType | "")}
            >
              <option value="">Select type</option>
              <option value="CUSTOMER">Customer</option>
              <option value="VENDOR">Vendor</option>
            </select>
          </Field>
          <Field label="GSTIN">
            <input
              className="w-full px-3 py-2 rounded-md border border-input bg-background"
              value={gstin ?? ""}
              onChange={(e) => setGstin(e.target.value)}
              placeholder="22AAAAA0000A1Z5"
            />
          </Field>
          <Field label="State Code">
            <input
              className="w-full px-3 py-2 rounded-md border border-input bg-background"
              value={stateCode ?? ""}
              onChange={(e) => setStateCode(e.target.value)}
              placeholder="22"
              inputMode="numeric"
            />
          </Field>
          <Field label="Phone">
            <input
              className="w-full px-3 py-2 rounded-md border border-input bg-background"
              value={phone ?? ""}
              onChange={(e) => setPhone(e.target.value)}
              placeholder="+91..."
            />
          </Field>
          <Field label="Email">
            <input
              className="w-full px-3 py-2 rounded-md border border-input bg-background"
              value={email ?? ""}
              onChange={(e) => setEmail(e.target.value)}
              placeholder="accounts@acme.com"
            />
          </Field>
        </div>
        <Field label="Address">
          <textarea
            className="w-full px-3 py-2 rounded-md border border-input bg-background min-h-[96px]"
            value={address ?? ""}
            onChange={(e) => setAddress(e.target.value)}
            placeholder="Street, City, State"
          />
        </Field>
        {error ? <div className="text-sm text-destructive">{error}</div> : null}
        </FormSectionCard>
      </FormLayout>

      <div className="flex items-center gap-2 sticky bottom-0 bg-background/80 backdrop-blur p-3 border-t border-border">
        <Button disabled={submitting || Boolean(validationError)}>
          {submitting ? "Saving..." : submitLabel}
        </Button>
      </div>
    </form>
  )
}

function Field({
  label,
  children,
}: {
  label: string
  children: React.ReactNode
}) {
  return (
    <div className="space-y-2">
      <div className="text-sm">{label}</div>
      {children}
    </div>
  )
}
