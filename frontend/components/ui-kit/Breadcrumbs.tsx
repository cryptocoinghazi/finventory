 "use client"
 
 import * as React from "react"
 import Link from "next/link"
 import { usePathname } from "next/navigation"
 import { Separator } from "@/components/ui/separator"
 
 export function Breadcrumbs() {
   const pathname = usePathname() || "/"
   const parts = pathname.split("/").filter(Boolean)
   const crumbs = parts.map((p, i) => {
     const href = "/" + parts.slice(0, i + 1).join("/")
     return { label: capitalize(p), href }
   })
 
   return (
     <nav aria-label="Breadcrumb" className="flex items-center gap-2">
       <Link href="/dashboard" className="text-sm text-muted-foreground hover:underline">
         Dashboard
       </Link>
       {crumbs.length ? <Separator orientation="vertical" className="h-5" /> : null}
       {crumbs.map((c, i) => (
         <React.Fragment key={c.href}>
           <Link href={c.href} className="text-sm hover:underline">
             {c.label}
           </Link>
           {i < crumbs.length - 1 ? (
             <Separator orientation="vertical" className="h-5" />
           ) : null}
         </React.Fragment>
       ))}
     </nav>
   )
 }
 
 function capitalize(s: string) {
   if (!s) return s
   return s.slice(0, 1).toUpperCase() + s.slice(1).replaceAll("-", " ")
 }
 
