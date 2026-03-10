"use client"

import { useEffect, useState } from "react"
import Link from "next/link"
import { Button } from "@/components/ui/button"
import { PageHeader } from "@/components/ui/page-header"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { getPurchaseReturn, PurchaseReturn } from "@/lib/purchase-returns"
import { getOrganizationProfile, OrganizationProfile } from "@/lib/settings"
import { format } from "date-fns"

export default function PurchaseReturnDetailPage({
  params,
}: {
  params: { id: string }
}) {
  const [returnObj, setReturnObj] = useState<PurchaseReturn | null>(null)
  const [organization, setOrganization] = useState<OrganizationProfile | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    async function load() {
      try {
        const data = await getPurchaseReturn(params.id)
        setReturnObj(data)
      } catch (err) {
        setError("Failed to load return details")
        console.error(err)
      } finally {
        setLoading(false)
      }
    }
    load()
  }, [params.id])

  useEffect(() => {
    document.body.classList.add("a4-print-scope")
    return () => {
      document.body.classList.remove("a4-print-scope")
    }
  }, [])

  useEffect(() => {
    async function loadOrg() {
      try {
        const org = await getOrganizationProfile()
        setOrganization(org)
      } catch {
        setOrganization(null)
      }
    }
    loadOrg()
  }, [])

  if (loading) return <div>Loading...</div>
  if (error || !returnObj) return <div className="text-destructive">{error || "Not found"}</div>

  return (
    <div className="space-y-6">
      <style jsx global>{`
        @media print {
          @page {
            size: A4;
            margin: 12mm;
          }

          html,
          body {
            background: white !important;
          }

          body.a4-print-scope aside,
          body.a4-print-scope header,
          body.a4-print-scope nav,
          body.a4-print-scope .print\\:hidden,
          body.a4-print-scope [role="dialog"] {
            display: none !important;
          }

          body.a4-print-scope main,
          body.a4-print-scope main > div,
          body.a4-print-scope main > div > div {
            max-width: none !important;
          }

          body.a4-print-scope .a4-print-root {
            padding: 0 !important;
            margin: 0 !important;
          }
        }
      `}</style>

      <div className="print:hidden">
        <PageHeader
          title={`Purchase Return ${returnObj.returnNumber || "Draft"}`}
          description="View return details"
          actions={
            <div className="flex gap-2">
              <Link href="/purchase/returns">
                <Button variant="outline">Back</Button>
              </Link>
              <Button variant="secondary" onClick={() => window.print()}>
                Print (A4)
              </Button>
            </div>
          }
        />
      </div>

      <div className="a4-print-root space-y-6">
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

        <div className="grid gap-6 md:grid-cols-2">
        <Card>
          <CardHeader>
            <CardTitle>Return Information</CardTitle>
          </CardHeader>
          <CardContent className="space-y-2">
            <div className="flex justify-between">
              <span className="text-muted-foreground">Return Date:</span>
              <span>{format(new Date(returnObj.returnDate), "dd MMM yyyy")}</span>
            </div>
            <div className="flex justify-between">
              <span className="text-muted-foreground">Vendor:</span>
              <span>{returnObj.partyName}</span>
            </div>
            <div className="flex justify-between">
              <span className="text-muted-foreground">Warehouse:</span>
              <span>{returnObj.warehouseName}</span>
            </div>
            {returnObj.purchaseInvoiceId && (
              <div className="flex justify-between">
                <span className="text-muted-foreground">Original Invoice ID:</span>
                <span>{returnObj.purchaseInvoiceId}</span>
              </div>
            )}
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle>Tax & Totals</CardTitle>
          </CardHeader>
          <CardContent className="space-y-2">
            <div className="flex justify-between">
              <span className="text-muted-foreground">Taxable Amount:</span>
              <span>₹{(returnObj.totalTaxableAmount || 0).toFixed(2)}</span>
            </div>
            <div className="flex justify-between">
              <span className="text-muted-foreground">Total Tax:</span>
              <span>₹{(returnObj.totalTaxAmount || 0).toFixed(2)}</span>
            </div>
            <div className="flex justify-between font-bold text-lg border-t pt-2">
              <span>Grand Total:</span>
              <span>₹{(returnObj.grandTotal || 0).toFixed(2)}</span>
            </div>
          </CardContent>
        </Card>
      </div>

      <Card>
        <CardHeader>
          <CardTitle>Items</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="relative w-full overflow-auto">
            <table className="w-full caption-bottom text-sm">
              <thead className="[&_tr]:border-b">
                <tr className="border-b transition-colors hover:bg-muted/50 data-[state=selected]:bg-muted">
                  <th className="h-12 px-4 text-left align-middle font-medium text-muted-foreground">Item</th>
                  <th className="h-12 px-4 text-right align-middle font-medium text-muted-foreground">Qty</th>
                  <th className="h-12 px-4 text-right align-middle font-medium text-muted-foreground">Price</th>
                  <th className="h-12 px-4 text-right align-middle font-medium text-muted-foreground">Total</th>
                </tr>
              </thead>
              <tbody className="[&_tr:last-child]:border-0">
                {returnObj.lines.map((line) => (
                  <tr key={line.id} className="border-b transition-colors hover:bg-muted/50 data-[state=selected]:bg-muted">
                    <td className="p-4 align-middle">{line.itemName} ({line.itemCode})</td>
                    <td className="p-4 align-middle text-right">{line.quantity}</td>
                    <td className="p-4 align-middle text-right">₹{(line.unitPrice || 0).toFixed(2)}</td>
                    <td className="p-4 align-middle text-right">₹{((line.quantity || 0) * (line.unitPrice || 0)).toFixed(2)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </CardContent>
      </Card>
      </div>
    </div>
  )
}
