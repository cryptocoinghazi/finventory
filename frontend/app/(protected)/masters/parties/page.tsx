"use client"

import Link from "next/link"
import { useEffect, useMemo, useState } from "react"
import { Button } from "@/components/ui/button"
import { PageHeader } from "@/components/ui/page-header"
import { deleteParty, listParties, Party } from "@/lib/parties"
import { DataTablePro } from "@/components/ui-kit/DataTablePro"
import { ConfirmDialog } from "@/components/ui-kit/ConfirmDialog"
import { Input } from "@/components/ui/input"
import { ChevronDown, ChevronRight, Search } from "lucide-react"

export default function PartiesListPage() {
  const [rows, setRows] = useState<Party[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [vendorsCollapsed, setVendorsCollapsed] = useState(false)
  const [customersCollapsed, setCustomersCollapsed] = useState(false)
  const [vendorQuery, setVendorQuery] = useState("")
  const [customerQuery, setCustomerQuery] = useState("")

  async function load() {
    setLoading(true)
    setError(null)
    try {
      const data = await listParties()
      setRows(data)
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to load parties")
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    load()
  }, [])

  const vendors = useMemo(() => rows.filter((p) => p.type === "VENDOR"), [rows])
  const customers = useMemo(
    () => rows.filter((p) => p.type === "CUSTOMER"),
    [rows]
  )

  const vendorFiltered = useMemo(() => {
    const q = vendorQuery.trim().toLowerCase()
    return vendors.filter((p) => {
      return (
        !q ||
        p.name.toLowerCase().includes(q) ||
        (p.gstin ?? "").toLowerCase().includes(q) ||
        (p.phone ?? "").toLowerCase().includes(q) ||
        (p.email ?? "").toLowerCase().includes(q)
      )
    })
  }, [vendorQuery, vendors])

  const customerFiltered = useMemo(() => {
    const q = customerQuery.trim().toLowerCase()
    return customers.filter((p) => {
      return (
        !q ||
        p.name.toLowerCase().includes(q) ||
        (p.gstin ?? "").toLowerCase().includes(q) ||
        (p.phone ?? "").toLowerCase().includes(q) ||
        (p.email ?? "").toLowerCase().includes(q)
      )
    })
  }, [customerQuery, customers])

  function renderActions(p: Party) {
    return (
      <div className="flex items-center gap-2">
        <Link href={`/masters/parties/${p.id}/edit`}>
          <Button size="sm" variant="secondary">
            Edit
          </Button>
        </Link>
        <ConfirmDialog
          title={`Delete ${p.name}?`}
          description="This will permanently delete the party and any related invoices/returns/ledger entries."
          confirmText="Delete"
          onConfirm={async () => {
            try {
              await deleteParty(p.id, { force: true })
              setRows((prev) => prev.filter((x) => x.id !== p.id))
            } catch (err) {
              setError(err instanceof Error ? err.message : "Delete failed")
            }
          }}
        />
      </div>
    )
  }

  const columns = [
    { key: "name", header: "Name" },
    { key: "gstin", header: "GSTIN" },
    { key: "stateCode", header: "State" },
    { key: "phone", header: "Phone" },
    { key: "email", header: "Email" },
    { key: "actions", header: "Actions", cell: renderActions },
  ]

  return (
    <div className="space-y-6">
      <PageHeader
        title="Parties"
        description="Create and manage customers and vendors."
        actions={
          <Link href="/masters/parties/new">
            <Button>Add Party</Button>
          </Link>
        }
      />

      {error ? <div className="text-sm text-destructive">{error}</div> : null}

      <div className="space-y-4 border rounded-lg p-4 bg-card">
        <div className="flex items-center justify-between gap-3">
          <button
            type="button"
            className="text-lg font-semibold inline-flex items-center gap-2"
            onClick={() => setVendorsCollapsed((v) => !v)}
          >
            {vendorsCollapsed ? (
              <ChevronRight className="h-5 w-5 text-muted-foreground" />
            ) : (
              <ChevronDown className="h-5 w-5 text-muted-foreground" />
            )}
            <span>Vendors</span>
            <span className="text-sm font-normal text-muted-foreground">
              ({vendorFiltered.length})
            </span>
          </button>
        </div>

        {vendorsCollapsed ? null : (
          <DataTablePro
            columns={columns}
            data={vendorFiltered}
            loading={loading}
            filters={
              <div className="flex flex-wrap items-center gap-2">
                <div className="relative w-full max-w-xs">
                  <Search className="absolute left-2.5 top-2.5 h-4 w-4 text-muted-foreground" />
                  <Input
                    type="search"
                    placeholder="Search vendors..."
                    className="pl-8"
                    value={vendorQuery}
                    onChange={(e) => setVendorQuery(e.target.value)}
                  />
                </div>
                <Button variant="outline" onClick={load} disabled={loading}>
                  Refresh
                </Button>
              </div>
            }
            actions={null}
            empty={{
              title: "No vendors found",
              description: "Create your first vendor",
              onAdd: () => {
                window.location.href = "/masters/parties/new"
              },
            }}
          />
        )}
      </div>

      <div className="space-y-4 border rounded-lg p-4 bg-card">
        <div className="flex items-center justify-between gap-3">
          <button
            type="button"
            className="text-lg font-semibold inline-flex items-center gap-2"
            onClick={() => setCustomersCollapsed((v) => !v)}
          >
            {customersCollapsed ? (
              <ChevronRight className="h-5 w-5 text-muted-foreground" />
            ) : (
              <ChevronDown className="h-5 w-5 text-muted-foreground" />
            )}
            <span>Customers</span>
            <span className="text-sm font-normal text-muted-foreground">
              ({customerFiltered.length})
            </span>
          </button>
        </div>

        {customersCollapsed ? null : (
          <DataTablePro
            columns={columns}
            data={customerFiltered}
            loading={loading}
            filters={
              <div className="flex flex-wrap items-center gap-2">
                <div className="relative w-full max-w-xs">
                  <Search className="absolute left-2.5 top-2.5 h-4 w-4 text-muted-foreground" />
                  <Input
                    type="search"
                    placeholder="Search customers..."
                    className="pl-8"
                    value={customerQuery}
                    onChange={(e) => setCustomerQuery(e.target.value)}
                  />
                </div>
                <Button variant="outline" onClick={load} disabled={loading}>
                  Refresh
                </Button>
              </div>
            }
            actions={null}
            empty={{
              title: "No customers found",
              description: "Create your first customer",
              onAdd: () => {
                window.location.href = "/masters/parties/new"
              },
            }}
          />
        )}
      </div>
    </div>
  )
}

