"use client"

import Link from "next/link"
import { useParams } from "next/navigation"
import { useEffect, useState } from "react"
import { Button } from "@/components/ui/button"
import { PageHeader } from "@/components/ui/page-header"
import {
  cancelSalesInvoice,
  getSalesInvoice,
  InvoicePaymentStatus,
  SalesInvoice,
  updateSalesInvoicePaymentStatus,
} from "@/lib/sales-invoices"
import { getOrganizationProfile, OrganizationProfile } from "@/lib/settings"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Separator } from "@/components/ui/separator"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table"
import { ConfirmDialog } from "@/components/ui-kit/ConfirmDialog"
import { Trash2 } from "lucide-react"
import { useToast } from "@/components/ui/use-toast"
import { getCurrentUser } from "@/lib/users"

export default function SalesInvoiceDetailPage() {
  const { toast } = useToast()
  const params = useParams()
  const id = params.id as string
  const [invoice, setInvoice] = useState<SalesInvoice | null>(null)
  const [organization, setOrganization] = useState<OrganizationProfile | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [savingStatus, setSavingStatus] = useState(false)
  const [isAdmin, setIsAdmin] = useState<boolean | null>(null)

  useEffect(() => {
    async function load() {
      try {
        setLoading(true)
        const data = await getSalesInvoice(id)
        setInvoice(data)
      } catch (err) {
        setError(err instanceof Error ? err.message : "Failed to load invoice")
      } finally {
        setLoading(false)
      }
    }
    load()
  }, [id])

  useEffect(() => {
    ;(async () => {
      try {
        const user = await getCurrentUser()
        setIsAdmin(user.role === "ADMIN")
      } catch {
        setIsAdmin(null)
      }
    })()
  }, [])

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

  if (loading) {
    return <div className="p-8 text-center">Loading invoice...</div>
  }

  if (error || !invoice) {
    return (
      <div className="p-8 text-center text-destructive">
        Error: {error || "Invoice not found"}
        <div className="mt-4">
          <Link href="/sales/invoices">
            <Button variant="outline">Back to List</Button>
          </Link>
        </div>
      </div>
    )
  }

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
          title={`Invoice ${invoice.invoiceNumber || "Draft"}`}
          description="View invoice details"
          actions={
            <div className="flex gap-2">
              <Link href="/sales/invoices">
                <Button variant="outline">Back</Button>
              </Link>
              <Button variant="secondary" onClick={() => window.print()}>
                Print (A4)
              </Button>
              {isAdmin ? (
                <ConfirmDialog
                  title="Cancel invoice?"
                  description="This will reverse stock and accounting entries."
                  confirmText="Cancel invoice"
                  onConfirm={async () => {
                    try {
                      const updated = await cancelSalesInvoice(invoice.id)
                      setInvoice(updated)
                      toast({ title: "Invoice cancelled" })
                    } catch (err) {
                      toast({
                        variant: "destructive",
                        title: "Cancel failed",
                        description: err instanceof Error ? err.message : "Request failed",
                      })
                    }
                  }}
                  disabled={!!invoice.deletedAt}
                >
                  <Button variant="destructive" disabled={!!invoice.deletedAt}>
                    <Trash2 className="h-4 w-4 mr-2" />
                    Cancel
                  </Button>
                </ConfirmDialog>
              ) : null}
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
            <CardTitle>Invoice Details</CardTitle>
          </CardHeader>
          <CardContent className="space-y-2 text-sm">
            <div className="flex justify-between">
              <span className="text-muted-foreground">Date:</span>
              <span className="font-medium">{invoice.invoiceDate}</span>
            </div>
            <div className="flex justify-between">
              <span className="text-muted-foreground">Number:</span>
              <span className="font-medium">{invoice.invoiceNumber || "N/A"}</span>
            </div>
            <div className="flex justify-between items-center gap-3">
              <span className="text-muted-foreground">Status:</span>
              {invoice.deletedAt ? (
                <span className="font-medium">CANCELLED</span>
              ) : (
                <>
                  <div className="w-[180px] print:hidden">
                    <Select
                      value={invoice.paymentStatus || "PENDING"}
                      onValueChange={async (v) => {
                        const next = v as InvoicePaymentStatus
                        if (invoice.paymentStatus === next) return
                        setSavingStatus(true)
                        try {
                          const updated = await updateSalesInvoicePaymentStatus(invoice.id, next)
                          setInvoice(updated)
                        } catch (err) {
                          setError(err instanceof Error ? err.message : "Failed to update status")
                        } finally {
                          setSavingStatus(false)
                        }
                      }}
                      disabled={savingStatus}
                    >
                      <SelectTrigger>
                        <SelectValue />
                      </SelectTrigger>
                      <SelectContent>
                        <SelectItem value="PENDING">Pending</SelectItem>
                        <SelectItem value="PARTIAL">Partial</SelectItem>
                        <SelectItem value="PAID">Paid</SelectItem>
                      </SelectContent>
                    </Select>
                  </div>
                  <span className="font-medium hidden print:inline">
                    {invoice.paymentStatus || "PENDING"}
                  </span>
                </>
              )}
            </div>
            <div className="flex justify-between">
              <span className="text-muted-foreground">Warehouse:</span>
              <span className="font-medium">{invoice.warehouseName}</span>
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle>Customer Details</CardTitle>
          </CardHeader>
          <CardContent className="space-y-2 text-sm">
            <div className="flex justify-between">
              <span className="text-muted-foreground">Name:</span>
              <span className="font-medium">{invoice.partyName}</span>
            </div>
          </CardContent>
        </Card>
      </div>

      <Card>
        <CardHeader>
          <CardTitle>Line Items</CardTitle>
        </CardHeader>
        <CardContent>
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Item</TableHead>
                <TableHead className="text-right">Qty</TableHead>
                <TableHead className="text-right">Price</TableHead>
                <TableHead className="text-right">Tax</TableHead>
                <TableHead className="text-right">Total</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {invoice.lines.map((line) => (
                <TableRow key={line.id}>
                  <TableCell>
                    <div className="font-medium">{line.itemName}</div>
                    <div className="text-xs text-muted-foreground">
                      {line.itemCode}
                    </div>
                  </TableCell>
                  <TableCell className="text-right">{line.quantity}</TableCell>
                  <TableCell className="text-right">
                    {line.unitPrice.toFixed(2)}
                  </TableCell>
                  <TableCell className="text-right">
                    {(line.taxAmount || 0).toFixed(2)}
                    {line.taxRate ? (
                      <span className="ml-1 text-xs text-muted-foreground">
                        ({line.taxRate}%)
                      </span>
                    ) : null}
                  </TableCell>
                  <TableCell className="text-right">
                    {(line.lineTotal || 0).toFixed(2)}
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>

          <div className="mt-6 flex justify-end">
            <div className="w-80 space-y-2">
              <div className="flex justify-between text-sm">
                <span className="text-muted-foreground">Taxable Amount:</span>
                <span>{(invoice.totalTaxableAmount || 0).toFixed(2)}</span>
              </div>
              <div className="flex justify-between text-sm">
                <span className="text-muted-foreground">Total Tax:</span>
                <span>{(invoice.totalTaxAmount || 0).toFixed(2)}</span>
              </div>
              <Separator />
              <div className="flex justify-between pt-2 text-lg font-bold">
                <span>Grand Total:</span>
                <span>{(invoice.grandTotal || 0).toFixed(2)}</span>
              </div>
            </div>
          </div>
        </CardContent>
      </Card>
      </div>
    </div>
  )
}
