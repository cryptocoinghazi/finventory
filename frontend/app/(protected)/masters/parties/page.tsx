"use client"

import Link from "next/link"
import { useEffect, useMemo, useState } from "react"
import { Button } from "@/components/ui/button"
import { PageHeader } from "@/components/ui/page-header"
import { deleteParty, listParties, Party } from "@/lib/parties"
import { DataTablePro } from "@/components/ui-kit/DataTablePro"
import { ConfirmDialog } from "@/components/ui-kit/ConfirmDialog"

export default function PartiesListPage() {
  const [rows, setRows] = useState<Party[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [query, setQuery] = useState("")
  const [type, setType] = useState<"" | "CUSTOMER" | "VENDOR">("")
  const [stateCode, setStateCode] = useState("")

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

  const filtered = useMemo(() => {
    const q = query.trim().toLowerCase()
    return rows.filter((p) => {
      const matchesText =
        !q ||
        p.name.toLowerCase().includes(q) ||
        p.type.toLowerCase().includes(q) ||
        (p.gstin ?? "").toLowerCase().includes(q) ||
        (p.phone ?? "").toLowerCase().includes(q) ||
        (p.email ?? "").toLowerCase().includes(q)
      const matchesType = !type || p.type === type
      const matchesState =
        !stateCode || (p.stateCode ?? "").toLowerCase() === stateCode.toLowerCase()
      return matchesText && matchesType && matchesState
    })
  }, [query, rows, type, stateCode])

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
          description="This action cannot be undone."
          confirmText="Delete"
          onConfirm={async () => {
            try {
              await deleteParty(p.id)
              setRows((prev) => prev.filter((x) => x.id !== p.id))
            } catch (err) {
              setError(err instanceof Error ? err.message : "Delete failed")
            }
          }}
        />
      </div>
    )
  }

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

      <DataTablePro
        columns={[
          { key: "name", header: "Name" },
          { key: "type", header: "Type" },
          { key: "gstin", header: "GSTIN" },
          { key: "stateCode", header: "State" },
          { key: "phone", header: "Phone" },
          { key: "email", header: "Email" },
          { key: "actions", header: "Actions", cell: renderActions },
        ]}
        data={filtered}
        loading={loading}
        filters={
          <div className="flex flex-wrap items-center gap-2">
            <input
              className="w-full max-w-xs px-3 py-2 rounded-md border border-input bg-background"
              placeholder="Search by name, GSTIN, email"
              value={query}
              onChange={(e) => setQuery(e.target.value)}
            />
            <select
              className="px-3 py-2 rounded-md border border-input bg-background"
              value={type}
              onChange={(e) =>
                setType(e.target.value as "" | "CUSTOMER" | "VENDOR")
              }
            >
              <option value="">All Types</option>
              <option value="CUSTOMER">Customer</option>
              <option value="VENDOR">Vendor</option>
            </select>
            <input
              className="w-[120px] px-3 py-2 rounded-md border border-input bg-background"
              placeholder="State"
              value={stateCode}
              onChange={(e) => setStateCode(e.target.value)}
            />
            <Button variant="outline" onClick={load} disabled={loading}>
              Refresh
            </Button>
          </div>
        }
        actions={null}
        empty={{
          title: "No parties found",
          description: "Create your first party",
          onAdd: () => {
            window.location.href = "/masters/parties/new"
          },
        }}
      />
    </div>
  )
}

