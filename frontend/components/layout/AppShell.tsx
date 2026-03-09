import Link from "next/link"
import { ReactNode } from "react"

export function AppShell({ children }: { children: ReactNode }) {
  return (
    <div className="min-h-screen bg-background text-foreground">
      <div className="grid grid-cols-[240px_1fr]">
        <aside className="border-r border-border h-screen sticky top-0 p-4">
          <div className="text-lg font-semibold mb-6">Finventory</div>
          <nav className="space-y-2">
            <Section title="Masters">
              <NavLink href="/masters/parties" label="Parties" />
              <NavLink href="/masters/items" label="Items" />
              <NavLink href="/masters/warehouses" label="Warehouses" />
              <NavLink href="/masters/taxes" label="Tax Slabs" />
            </Section>
            <Section title="Sales">
              <NavLink href="/sales/invoices" label="Invoices" />
              <NavLink href="/sales/returns" label="Returns" />
            </Section>
            <Section title="Purchase">
              <NavLink href="/purchase/invoices" label="Invoices" />
              <NavLink href="/purchase/returns" label="Returns" />
            </Section>
            <Section title="Reports">
              <NavLink href="/reports/stock-summary" label="Stock Summary" />
              <NavLink href="/reports/outstanding" label="Party Outstanding" />
              <NavLink href="/reports/gst-registers" label="GST Registers" />
            </Section>
            <Section title="Admin">
              <NavLink href="/admin/users" label="Users" />
            </Section>
          </nav>
        </aside>
        <div>
          <header className="border-b border-border p-4 flex items-center justify-between">
            <div className="text-sm text-muted-foreground">Dashboard</div>
            <div className="flex items-center gap-3">
              <Link href="/dashboard" className="text-sm hover:underline">
                Home
              </Link>
              <Link href="/login" className="text-sm hover:underline">
                Login
              </Link>
            </div>
          </header>
          <main className="p-6 max-w-[1280px] mx-auto">{children}</main>
        </div>
      </div>
    </div>
  )
}

function Section({ title, children }: { title: string; children: ReactNode }) {
  return (
    <div>
      <div className="text-xs uppercase tracking-wide text-muted-foreground mb-1">
        {title}
      </div>
      <div className="grid gap-1">{children}</div>
    </div>
  )
}

function NavLink({ href, label }: { href: string; label: string }) {
  return (
    <Link
      href={href}
      className="block rounded-md px-2 py-1 hover:bg-muted hover:text-foreground text-sm"
    >
      {label}
    </Link>
  )
}

