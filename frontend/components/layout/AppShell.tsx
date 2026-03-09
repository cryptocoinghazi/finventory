"use client"

import Link from "next/link"
import { usePathname, useRouter } from "next/navigation"
import { ReactNode, useEffect, useState } from "react"
import {
  ClipboardList,
  FileText,
  LayoutDashboard,
  PanelLeftClose,
  PanelLeftOpen,
  Search,
  Settings,
  ShoppingCart,
  Store,
  Users,
  Warehouse,
} from "lucide-react"

import { cn } from "@/lib/utils"
import { Button } from "@/components/ui/button"
import {
  CommandDialog,
  CommandEmpty,
  CommandGroup,
  CommandInput,
  CommandItem,
  CommandList,
  CommandSeparator,
} from "@/components/ui/command"
import { Kbd } from "@/components/ui/kbd"
import {
  TooltipProvider,
} from "@/components/ui/tooltip"
import { ThemeToggle } from "@/components/ui/theme-toggle"
import { QuickCreateMenu } from "@/components/ui-kit/QuickCreateMenu"
import { NotificationBell } from "@/components/ui/notifications"
import { Breadcrumbs } from "@/components/ui-kit/Breadcrumbs"
import { SignOutButton } from "@/components/ui/signout-button"
import { AuthBanner } from "@/components/ui-kit/AuthBanner"
import { motion } from "framer-motion"

type NavItem = {
  title: string
  href: string
  icon: React.ComponentType<{ className?: string }>
  keywords?: string
}

type NavSection = {
  title: string
  items: NavItem[]
}

const NAV: NavSection[] = [
  {
    title: "Overview",
    items: [{ title: "Dashboard", href: "/dashboard", icon: LayoutDashboard }],
  },
  {
    title: "Masters",
    items: [
      { title: "Parties", href: "/masters/parties", icon: Users },
      { title: "Items", href: "/masters/items", icon: Store },
      { title: "Warehouses", href: "/masters/warehouses", icon: Warehouse },
      { title: "Tax Slabs", href: "/masters/taxes", icon: ClipboardList },
    ],
  },
  {
    title: "Sales",
    items: [
      { title: "Invoices", href: "/sales/invoices", icon: FileText },
      { title: "Returns", href: "/sales/returns", icon: ShoppingCart },
    ],
  },
  {
    title: "Purchase",
    items: [
      { title: "Invoices", href: "/purchase/invoices", icon: FileText },
      { title: "Returns", href: "/purchase/returns", icon: ShoppingCart },
    ],
  },
  {
    title: "Reports",
    items: [
      { title: "Stock Summary", href: "/reports/stock-summary", icon: ClipboardList },
      { title: "Party Outstanding", href: "/reports/outstanding", icon: Users },
      { title: "GST Registers", href: "/reports/gst-registers", icon: FileText },
    ],
  },
  {
    title: "Admin",
    items: [
      { title: "Users", href: "/admin/users", icon: Users },
    ],
  },
  {
    title: "System",
    items: [
      { title: "Settings", href: "/settings", icon: Settings },
    ],
  },
]

export function AppShell({ children }: { children: ReactNode }) {
  const pathname = usePathname()
  const router = useRouter()
  const [collapsed, setCollapsed] = useState(false)
  const [commandOpen, setCommandOpen] = useState(false)
  const [role, setRole] = useState<string | null>(null)

  useEffect(() => {
    setRole(window.localStorage.getItem("role"))

    function onKeyDown(e: KeyboardEvent) {
      if ((e.ctrlKey || e.metaKey) && e.key.toLowerCase() === "k") {
        e.preventDefault()
        setCommandOpen(true)
      }
    }
    window.addEventListener("keydown", onKeyDown)
    return () => window.removeEventListener("keydown", onKeyDown)
  }, [])

  return (
    <TooltipProvider delayDuration={150}>
      <div className="min-h-screen bg-app-bg text-foreground">
        <div className="flex">
          <motion.aside
            className={cn(
              "h-screen sticky top-0 border-r border-sidebar-border bg-sidebar text-sidebar-foreground"
            )}
            animate={{ width: collapsed ? 76 : 288 }}
            transition={{ duration: 0.15, ease: "easeInOut" }}
          >
            <div
              className={cn(
                "flex items-center gap-3 px-4 py-4",
                collapsed ? "justify-center" : "justify-between"
              )}
            >
              <Link href="/dashboard" className="flex items-center gap-2">
                <div className="h-8 w-8 rounded-xl bg-primary text-primary-foreground grid place-items-center shadow-soft">
                  <span className="text-sm font-semibold">F</span>
                </div>
                {collapsed ? null : (
                  <div className="leading-tight">
                    <div className="text-sm font-semibold">Finventory</div>
                    <div className="text-xs text-muted-foreground">Inventory & Billing</div>
                  </div>
                )}
              </Link>
              {collapsed ? null : (
                <Button
                  variant="ghost"
                  size="icon"
                  onClick={() => setCollapsed(true)}
                  className="text-muted-foreground hover:text-foreground"
                >
                  <PanelLeftClose className="h-4 w-4" />
                </Button>
              )}
              {collapsed ? (
                <Button
                  variant="ghost"
                  size="icon"
                  onClick={() => setCollapsed(false)}
                  className="text-muted-foreground hover:text-foreground"
                >
                  <PanelLeftOpen className="h-4 w-4" />
                </Button>
              ) : null}
            </div>

            <div className="px-3 pb-4">
              <Button
                type="button"
                variant="outline"
                onClick={() => setCommandOpen(true)}
                className={cn(
                  "w-full justify-start gap-3 bg-surface text-foreground shadow-soft border-border-subtle",
                  collapsed ? "px-3" : ""
                )}
              >
                <Search className="h-4 w-4 text-muted-foreground" />
                {collapsed ? null : (
                  <>
                    <span className="text-sm text-muted-foreground">Search…</span>
                    <span className="ml-auto flex items-center gap-1">
                      <Kbd>Ctrl</Kbd>
                      <Kbd>K</Kbd>
                    </span>
                  </>
                )}
              </Button>
            </div>

            <nav className="px-2 pb-6 space-y-1">
              {NAV.filter((section) => section.title !== "Admin" || role === "ADMIN").map((section) => (
                <div key={section.title} className="pt-2">
                  {collapsed ? null : (
                    <div className="px-2 pb-2 text-[11px] font-medium uppercase tracking-wide text-muted-foreground">
                      {section.title}
                    </div>
                  )}
                  {section.items.map((item) => {
                    const isActive = pathname.startsWith(item.href)
                    return (
                      <Link
                        key={item.href}
                        href={item.href}
                        className={cn(
                          "group flex items-center gap-3 rounded-lg px-3 py-2 text-sm font-medium transition-colors hover:bg-sidebar-accent hover:text-sidebar-accent-foreground",
                          isActive ? "bg-sidebar-accent text-sidebar-accent-foreground" : "text-sidebar-foreground/70",
                          collapsed ? "justify-center" : ""
                        )}
                      >
                        <item.icon className="h-4 w-4" />
                        {collapsed ? null : <span>{item.title}</span>}
                      </Link>
                    )
                  })}
                </div>
              ))}
            </nav>
          </motion.aside>

          <div className="min-w-0 flex-1">
            <header className="sticky top-0 z-40 border-b border-topbar-border bg-topbar/80 backdrop-blur">
              <div className="h-14 px-6 flex items-center justify-between">
                <div className="flex items-center gap-3 min-w-0">
                  <Breadcrumbs />
                </div>
                <div className="flex items-center gap-2">
                  <QuickCreateMenu />
                  <NotificationBell />
                  <SignOutButton />
                  <Button
                    variant="outline"
                    className="gap-2 shadow-soft border-border-subtle"
                    onClick={() => setCommandOpen(true)}
                  >
                    <Search className="h-4 w-4 text-muted-foreground" />
                    <span className="hidden sm:inline">Search</span>
                    <span className="hidden md:flex items-center gap-1">
                      <Kbd>Ctrl</Kbd>
                      <Kbd>K</Kbd>
                    </span>
                  </Button>
                  <ThemeToggle />
                </div>
              </div>
            </header>

            <main className="px-6 py-6">
              <div className="max-w-[1320px] mx-auto">
                <AuthBanner />
                {children}
              </div>
            </main>
          </div>
        </div>

        <CommandDialog open={commandOpen} onOpenChange={setCommandOpen}>
          <CommandInput placeholder="Search pages…" />
          <CommandList>
            <CommandEmpty>No results found.</CommandEmpty>
            <CommandGroup heading="Quick Create">
              {[
                { title: "New Party", href: "/masters/parties?new=1", icon: Users },
                { title: "New Item", href: "/masters/items?new=1", icon: Store },
                { title: "New Sales Invoice", href: "/sales/invoices?new=1", icon: FileText },
                { title: "New Purchase Invoice", href: "/purchase/invoices?new=1", icon: FileText },
              ].map((action) => (
                <CommandItem
                  key={action.title}
                  value={action.title}
                  onSelect={() => {
                    setCommandOpen(false)
                    router.push(action.href)
                  }}
                >
                  <action.icon className="mr-2 h-4 w-4 text-muted-foreground" />
                  <span>{action.title}</span>
                </CommandItem>
              ))}
            </CommandGroup>
            <CommandSeparator />
            {NAV.filter((section) => section.title !== "Admin" || role === "ADMIN").map((section) => (
              <CommandGroup key={section.title} heading={section.title}>
                {section.items.map((item) => (
                  <CommandItem
                    key={item.href}
                    value={`${item.title} ${item.keywords || ""} ${item.href}`}
                    onSelect={() => {
                      setCommandOpen(false)
                      router.push(item.href)
                    }}
                  >
                    <item.icon className="mr-2 h-4 w-4 text-muted-foreground" />
                    <span>{item.title}</span>
                    <span className="ml-auto text-xs text-muted-foreground">{item.href}</span>
                  </CommandItem>
                ))}
                <CommandSeparator />
              </CommandGroup>
            ))}
          </CommandList>
        </CommandDialog>
      </div>
    </TooltipProvider>
  )
}


