"use client"

import { Button } from "@/components/ui/button"
import { PageHeader } from "@/components/ui/page-header"
import Link from "next/link"
import { SectionCard } from "@/components/ui-kit/SectionCard"
import { StatCard } from "@/components/ui-kit/StatCard"
import { ClipboardList, Store, Users, Warehouse } from "lucide-react"
import { useEffect, useState } from "react"
import {
  getActivityFeed,
  getDashboardStats,
  getSystemStatus,
  ActivityFeedEntry,
  DashboardStats,
  SystemStatus,
} from "@/lib/reports"
import { apiFetch } from "@/lib/api"
import { Badge } from "@/components/ui/badge"

export default function DashboardPage() {
  const [stats, setStats] = useState<DashboardStats | null>(null)
  const [loading, setLoading] = useState(true)
  const [backendUp, setBackendUp] = useState<boolean | null>(null)
  const [backendError, setBackendError] = useState<string | null>(null)
  const [systemStatus, setSystemStatus] = useState<SystemStatus | null>(null)
  const [systemStatusError, setSystemStatusError] = useState<string | null>(null)
  const [activity, setActivity] = useState<ActivityFeedEntry[] | null>(null)
  const [activityError, setActivityError] = useState<string | null>(null)

  useEffect(() => {
    getDashboardStats()
      .then(setStats)
      .catch(console.error)
      .finally(() => setLoading(false))

    apiFetch("/health", { cache: "no-store" })
      .then(async (res) => {
        if (!res.ok) {
          const msg = await res.text()
          throw new Error(msg || `Health check failed (${res.status})`)
        }
        setBackendUp(true)
        setBackendError(null)
      })
      .catch((err) => {
        setBackendUp(false)
        setBackendError(err instanceof Error ? err.message : "Health check failed")
      })

    getSystemStatus()
      .then((data) => {
        setSystemStatus(data)
        setSystemStatusError(null)
      })
      .catch((err) => {
        setSystemStatus(null)
        setSystemStatusError(err instanceof Error ? err.message : "System status failed")
      })

    getActivityFeed(10)
      .then((data) => {
        setActivity(data)
        setActivityError(null)
      })
      .catch((err) => {
        setActivity(null)
        setActivityError(err instanceof Error ? err.message : "Activity feed failed")
      })
  }, [])

  return (
    <div className="space-y-6">
      <PageHeader
        title="Dashboard"
        description="Quick links and system status."
      />
      <section className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
        <StatCard
          title="Sales Today"
          value={stats ? `₹${stats.salesToday.toLocaleString("en-IN")}` : undefined}
          loading={loading}
        />
        <StatCard
          title="Purchase Today"
          value={stats ? `₹${stats.purchaseToday.toLocaleString("en-IN")}` : undefined}
          loading={loading}
        />
        <StatCard
          title="Stock Value"
          value={stats ? `₹${stats.stockValue.toLocaleString("en-IN")}` : undefined}
          loading={loading}
        />
        <StatCard
          title="Outstanding"
          value={stats ? `₹${stats.outstanding.toLocaleString("en-IN")}` : undefined}
          loading={loading}
        />
      </section>
      <section className="grid grid-cols-1 lg:grid-cols-2 gap-4">
        <SectionCard title="Masters" description="Jump to key setup screens">
          <div className="grid grid-cols-2 md:grid-cols-3 gap-2">
            <Link href="/masters/parties">
              <Button variant="secondary" className="w-full gap-2">
                <Users className="h-4 w-4" />
                Parties
              </Button>
            </Link>
            <Link href="/masters/items">
              <Button variant="secondary" className="w-full gap-2">
                <Store className="h-4 w-4" />
                Items
              </Button>
            </Link>
            <Link href="/masters/warehouses">
              <Button variant="secondary" className="w-full gap-2">
                <Warehouse className="h-4 w-4" />
                Warehouses
              </Button>
            </Link>
            <Link href="/masters/taxes">
              <Button variant="secondary" className="w-full gap-2">
                <ClipboardList className="h-4 w-4" />
                Tax Slabs
              </Button>
            </Link>
          </div>
        </SectionCard>
        <SectionCard
          title="Reports"
          description="Frequently used summaries"
        >
          <div className="grid grid-cols-2 gap-2">
            <Link href="/reports/stock-summary">
              <Button variant="secondary" className="w-full gap-2">
                <ClipboardList className="h-4 w-4" />
                Stock Summary
              </Button>
            </Link>
            <Link href="/reports/outstanding">
              <Button variant="secondary" className="w-full gap-2">
                <Users className="h-4 w-4" />
                Party Outstanding
              </Button>
            </Link>
          </div>
        </SectionCard>
      </section>
      <section className="grid grid-cols-1 md:grid-cols-2 gap-4">
        <SectionCard
          title="Activity"
          description="Recent operational activity (invoices, returns, adjustments)"
        >
          {activity === null ? (
            <div className="text-sm text-muted-foreground">
              {activityError ? activityError : "Loading..."}
            </div>
          ) : activity.length === 0 ? (
            <div className="text-sm text-muted-foreground">No recent activity.</div>
          ) : (
            <div className="space-y-2">
              {activity.map((entry) => {
                const dateLabel = entry.date
                  ? new Date(entry.date).toLocaleDateString("en-IN", {
                      year: "numeric",
                      month: "short",
                      day: "2-digit",
                    })
                  : ""
                const content = (
                  <div className="flex items-start justify-between gap-3">
                    <div className="min-w-0">
                      <div className="text-sm font-medium truncate">{entry.title}</div>
                      <div className="text-xs text-muted-foreground truncate">
                        {entry.subtitle}
                      </div>
                    </div>
                    <div className="text-right">
                      <div className="text-xs text-muted-foreground">{dateLabel}</div>
                      {entry.amount != null ? (
                        <div className="text-sm">₹{entry.amount.toLocaleString("en-IN")}</div>
                      ) : null}
                    </div>
                  </div>
                )

                return entry.href ? (
                  <Link
                    key={entry.id}
                    href={entry.href}
                    className="block rounded-lg border bg-card px-3 py-2 hover:bg-accent"
                  >
                    {content}
                  </Link>
                ) : (
                  <div key={entry.id} className="rounded-lg border bg-card px-3 py-2">
                    {content}
                  </div>
                )
              })}
            </div>
          )}
        </SectionCard>
        <SectionCard
          title="System Status"
          description="Connectivity checks for the Finventory backend"
        >
          <div className="grid grid-cols-1 gap-3">
            <div className="flex items-center justify-between gap-3">
              <div className="text-sm">Backend</div>
              {backendUp === null ? (
                <div className="h-5 w-24 rounded-full bg-muted animate-pulse" />
              ) : backendUp ? (
                <Badge>UP</Badge>
              ) : (
                <Badge variant="destructive">DOWN</Badge>
              )}
            </div>
            {backendUp === false && backendError ? (
              <div className="text-xs text-destructive">{backendError}</div>
            ) : null}

            <div className="flex items-center justify-between gap-3">
              <div className="text-sm">Database</div>
              {systemStatus === null ? (
                <div className="h-5 w-24 rounded-full bg-muted animate-pulse" />
              ) : systemStatus.dbUp ? (
                <Badge>UP</Badge>
              ) : (
                <Badge variant="destructive">DOWN</Badge>
              )}
            </div>
            {systemStatus === null && systemStatusError ? (
              <div className="text-xs text-destructive">{systemStatusError}</div>
            ) : null}
            {systemStatus && systemStatus.dbUp === false && systemStatus.dbError ? (
              <div className="text-xs text-destructive">{systemStatus.dbError}</div>
            ) : null}

            {systemStatus ? (
              <div className="grid grid-cols-2 gap-2 text-sm">
                <div className="rounded-lg border bg-card px-3 py-2">
                  <div className="text-xs text-muted-foreground">Docs Today</div>
                  <div className="font-medium">
                    {systemStatus.salesInvoicesToday +
                      systemStatus.purchaseInvoicesToday +
                      systemStatus.salesReturnsToday +
                      systemStatus.purchaseReturnsToday +
                      systemStatus.stockAdjustmentsToday}
                  </div>
                </div>
                <div className="rounded-lg border bg-card px-3 py-2">
                  <div className="text-xs text-muted-foreground">Items</div>
                  <div className="font-medium">{systemStatus.items}</div>
                </div>
                <div className="rounded-lg border bg-card px-3 py-2">
                  <div className="text-xs text-muted-foreground">Parties</div>
                  <div className="font-medium">{systemStatus.parties}</div>
                </div>
                <div className="rounded-lg border bg-card px-3 py-2">
                  <div className="text-xs text-muted-foreground">Warehouses</div>
                  <div className="font-medium">{systemStatus.warehouses}</div>
                </div>
              </div>
            ) : null}
          </div>
        </SectionCard>
      </section>
    </div>
  )
}
