"use client"

import Link from "next/link"
import { useRouter } from "next/navigation"
import { Button } from "@/components/ui/button"
import { PageHeader } from "@/components/ui/page-header"
import { createSalesInvoice, SalesInvoiceInput } from "@/lib/sales-invoices"
import { listItems, Item } from "@/lib/items"
import { listParties, Party } from "@/lib/parties"
import { listWarehouses, Warehouse } from "@/lib/warehouses"
import { useEffect, useState } from "react"
import { SalesInvoiceForm } from "@/components/sales/SalesInvoiceForm"
import { getOrganizationProfile, OrganizationProfile } from "@/lib/settings"

export default function NewSalesInvoicePage() {
  const router = useRouter()
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const [parties, setParties] = useState<Party[]>([])
  const [warehouses, setWarehouses] = useState<Warehouse[]>([])
  const [items, setItems] = useState<Item[]>([])
  const [organization, setOrganization] = useState<OrganizationProfile | null>(null)

  useEffect(() => {
    async function loadMasters() {
      setLoading(true)
      setError(null)
      try {
        const [p, w, i] = await Promise.all([
          listParties(),
          listWarehouses(),
          listItems(),
        ])
        setParties(p)
        setWarehouses(w)
        setItems(i)

        try {
          const org = await getOrganizationProfile()
          setOrganization(org)
        } catch {
          setOrganization(null)
        }
      } catch (err) {
        setError(err instanceof Error ? err.message : "Failed to load masters")
      } finally {
        setLoading(false)
      }
    }
    loadMasters()
  }, [])

  async function onSubmit(data: SalesInvoiceInput) {
    try {
      // Ensure numeric types are correct
      const payload: SalesInvoiceInput = {
        ...data,
        invoiceNumber: null, // Auto-generated
        lines: data.lines.map((l) => ({
          itemId: l.itemId,
          quantity: Number(l.quantity),
          unitPrice: Number(l.unitPrice),
        })),
      }
      await createSalesInvoice(payload)
      router.push("/sales/invoices")
      router.refresh()
    } catch (err) {
      setError(err instanceof Error ? err.message : "Create failed")
    }
  }

  if (loading) {
    return <div className="p-8 text-center">Loading masters...</div>
  }

  if (error) {
    return (
      <div className="p-8 text-center text-destructive">
        Error: {error}
        <div className="mt-4">
          <Button variant="outline" onClick={() => window.location.reload()}>
            Retry
          </Button>
        </div>
      </div>
    )
  }

  return (
    <div className="space-y-6">
      <PageHeader
        title="New Sales Invoice"
        description="Create a new sales invoice"
        actions={
          <Link href="/sales/invoices">
            <Button variant="outline">Back</Button>
          </Link>
        }
      />
      {organization ? (
        <div className="rounded-lg border bg-card p-4">
          <div className="flex flex-col gap-1">
            <div className="text-base font-semibold">{organization.companyName}</div>
            <div className="text-sm text-muted-foreground">
              {[
                organization.addressLine1,
                organization.addressLine2,
                [organization.city, organization.state, organization.pincode].filter(Boolean).join(" "),
              ]
                .filter(Boolean)
                .join(", ")}
            </div>
            <div className="text-sm text-muted-foreground">
              {[organization.phone, organization.email, organization.gstin ? `GSTIN: ${organization.gstin}` : ""]
                .filter(Boolean)
                .join(" • ")}
            </div>
          </div>
        </div>
      ) : null}
      <SalesInvoiceForm
        items={items}
        parties={parties}
        warehouses={warehouses}
        onSubmit={onSubmit}
      />
    </div>
  )
}
