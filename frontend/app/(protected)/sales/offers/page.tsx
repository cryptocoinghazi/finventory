"use client"

import Link from "next/link"
import { useCallback, useEffect, useMemo, useState } from "react"
import { Button } from "@/components/ui/button"
import { PageHeader } from "@/components/ui/page-header"
import { DataTablePro } from "@/components/ui-kit/DataTablePro"
import { ConfirmDialog } from "@/components/ui-kit/ConfirmDialog"
import { MoneyText } from "@/components/ui-kit/MoneyText"
import { deleteOffer, listOffers, Offer } from "@/lib/offers"
import { useDebounce } from "@/hooks/use-debounce"

export default function OffersListPage() {
  const [rows, setRows] = useState<Offer[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [query, setQuery] = useState("")
  const [activeOnly, setActiveOnly] = useState(false)
  const [role, setRole] = useState<string | null>(null)
  const debouncedQuery = useDebounce(query, 250)

  const isAdmin = role === "ADMIN"

  const load = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      const data = await listOffers(activeOnly ? { active: true } : undefined)
      setRows(data)
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to load offers")
    } finally {
      setLoading(false)
    }
  }, [activeOnly])

  useEffect(() => {
    setRole(window.localStorage.getItem("role"))
  }, [])

  useEffect(() => {
    load()
  }, [load])

  const filtered = useMemo(() => {
    const q = debouncedQuery.trim().toLowerCase()
    return rows.filter((o) => {
      if (!q) return true
      return (
        o.name.toLowerCase().includes(q) ||
        (o.code ?? "").toLowerCase().includes(q) ||
        o.scope.toLowerCase().includes(q) ||
        o.discountType.toLowerCase().includes(q)
      )
    })
  }, [debouncedQuery, rows])

  function formatDiscount(o: Offer) {
    if (o.discountType === "PERCENT") return `${o.discountValue}%`
    return <MoneyText value={o.discountValue} />
  }

  function renderActions(o: Offer) {
    if (!isAdmin) return null
    return (
      <div className="flex items-center gap-2">
        <Link href={`/sales/offers/${o.id}/edit`}>
          <Button size="sm" variant="secondary">
            Edit
          </Button>
        </Link>
        <ConfirmDialog
          title={`Delete ${o.name}?`}
          description="This action cannot be undone."
          confirmText="Delete"
          onConfirm={async () => {
            try {
              await deleteOffer(o.id)
              setRows((prev) => prev.filter((x) => x.id !== o.id))
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
        title="Offers & Coupons"
        description="Manage discounts that can be applied in Quick POS."
        actions={
          <div className="flex items-center gap-2">
            <Button variant="outline" onClick={load} disabled={loading}>
              Refresh
            </Button>
            <Link href="/sales/offers/new">
              <Button disabled={!isAdmin}>New Offer</Button>
            </Link>
          </div>
        }
      />

      {error ? <div className="text-sm text-destructive">{error}</div> : null}
      {!isAdmin ? (
        <div className="text-xs text-muted-foreground">Only admins can create/edit offers.</div>
      ) : null}

      <DataTablePro
        columns={[
          { key: "name", header: "Name" },
          { key: "code", header: "Code", cell: (r: Offer) => r.code ?? "-" },
          { key: "scope", header: "Scope", cell: (r: Offer) => (r.scope === "CART" ? "Cart" : "Item") },
          {
            key: "discount",
            header: "Discount",
            cell: (r: Offer) => formatDiscount(r),
            className: "text-right",
          },
          {
            key: "active",
            header: "Active",
            cell: (r: Offer) => (r.active ? "Yes" : "No"),
            className: "w-[90px]",
          },
          {
            key: "validity",
            header: "Validity",
            cell: (r: Offer) => {
              const start = r.startDate ?? ""
              const end = r.endDate ?? ""
              if (!start && !end) return "-"
              if (start && end) return `${start} → ${end}`
              if (start) return `From ${start}`
              return `Until ${end}`
            },
          },
          {
            key: "usage",
            header: "Usage",
            cell: (r: Offer) =>
              r.usageLimit ? `${r.usedCount ?? 0}/${r.usageLimit}` : r.usedCount ? String(r.usedCount) : "-",
            className: "text-right w-[110px]",
          },
          { key: "actions", header: "Actions", cell: renderActions, className: "w-[150px]" },
        ]}
        data={filtered}
        loading={loading}
        filters={
          <div className="flex flex-wrap items-center gap-2">
            <input
              className="w-full max-w-xs px-3 py-2 rounded-md border border-input bg-background"
              placeholder="Search name/code/type"
              value={query}
              onChange={(e) => setQuery(e.target.value)}
            />
            <label className="flex items-center gap-2 text-sm text-muted-foreground">
              <input
                type="checkbox"
                checked={activeOnly}
                onChange={(e) => setActiveOnly(e.target.checked)}
              />
              Active only
            </label>
          </div>
        }
        actions={null}
        empty={{
          title: "No offers found",
          description: isAdmin ? "Create your first offer" : "Ask an admin to create offers",
          onAdd: isAdmin ? () => (window.location.href = "/sales/offers/new") : undefined,
        }}
      />
    </div>
  )
}
