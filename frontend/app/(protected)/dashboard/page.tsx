import { Button } from "@/components/ui/button"
import { PageHeader } from "@/components/ui/page-header"
import Link from "next/link"

export default function DashboardPage() {
  return (
    <div className="space-y-6">
      <PageHeader
        title="Dashboard"
        description="Quick links and system status. Real data where possible; else Coming Soon."
      />
      <section className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
        <Card title="Masters">
          <div className="flex gap-2">
            <Link href="/masters/parties">
              <Button variant="secondary">Parties</Button>
            </Link>
            <Link href="/masters/items">
              <Button variant="secondary">Items</Button>
            </Link>
            <Link href="/masters/warehouses">
              <Button variant="secondary">Warehouses</Button>
            </Link>
          </div>
        </Card>
        <Card title="Sales">
          <div className="flex gap-2">
            <Link href="/sales/invoices">
              <Button variant="secondary">Invoices</Button>
            </Link>
          </div>
        </Card>
        <Card title="Purchase">
          <div className="flex gap-2">
            <Link href="/purchase/invoices">
              <Button variant="secondary">Invoices</Button>
            </Link>
          </div>
        </Card>
        <Card title="Reports">
          <div className="flex gap-2">
            <Link href="/reports/stock-summary">
              <Button variant="secondary">Stock Summary</Button>
            </Link>
            <Link href="/reports/outstanding">
              <Button variant="secondary">Party Outstanding</Button>
            </Link>
          </div>
        </Card>
      </section>
      <section className="rounded-lg border border-border p-4">
        <div className="text-sm text-muted-foreground">
          Status panel: Backend endpoints lacking filters will show Coming Soon
          in corresponding screens until available.
        </div>
      </section>
    </div>
  )
}

function Card({ title, children }: { title: string; children: React.ReactNode }) {
  return (
    <div className="rounded-lg border border-border p-4 space-y-2">
      <div className="text-sm font-medium">{title}</div>
      {children}
    </div>
  )
}
