"use client"

import { Button } from "@/components/ui/button"
import { PageHeader } from "@/components/ui/page-header"
import Link from "next/link"
import { SectionCard } from "@/components/ui-kit/SectionCard"
import { StatCard } from "@/components/ui-kit/StatCard"
import { SkeletonCard } from "@/components/ui-kit/SkeletonCard"
import { ClipboardList, Store, Users, Warehouse } from "lucide-react"
import { useEffect, useState } from "react"
import { getDashboardStats, DashboardStats } from "@/lib/reports"

export default function DashboardPage() {
  const [stats, setStats] = useState<DashboardStats | null>(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    getDashboardStats()
      .then(setStats)
      .catch(console.error)
      .finally(() => setLoading(false))
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
        <SkeletonCard title="Activity" lines={6} />
        <SkeletonCard title="System Status" lines={6} />
      </section>
    </div>
  )
}
