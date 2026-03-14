 "use client"
 
 import * as React from "react"
 import { useRouter } from "next/navigation"
 import { Button } from "@/components/ui/button"
 import {
   DropdownMenu,
   DropdownMenuContent,
   DropdownMenuItem,
   DropdownMenuLabel,
   DropdownMenuSeparator,
   DropdownMenuTrigger,
 } from "@/components/ui/dropdown-menu"
 import { FileText, Plus, Store, Users } from "lucide-react"
 
 export function QuickCreateMenu() {
   const router = useRouter()
   return (
     <DropdownMenu>
       <DropdownMenuTrigger asChild>
         <Button
           aria-label="Quick create"
           variant="default"
           className="gap-2 shadow-soft"
         >
           <Plus className="h-4 w-4" />
           New
         </Button>
       </DropdownMenuTrigger>
       <DropdownMenuContent align="end" className="min-w-[220px]">
         <DropdownMenuLabel>Create</DropdownMenuLabel>
         <DropdownMenuSeparator />
         <DropdownMenuItem
           onSelect={() => router.push("/sales/invoices?new=1")}
           className="gap-2"
         >
           <FileText className="h-4 w-4 text-muted-foreground" />
           Sales Invoice
         </DropdownMenuItem>
         <DropdownMenuItem
           onSelect={() => router.push("/purchase/invoices?new=1")}
           className="gap-2"
         >
           <FileText className="h-4 w-4 text-muted-foreground" />
           Purchase Invoice
         </DropdownMenuItem>
         <DropdownMenuSeparator />
         <DropdownMenuItem
           onSelect={() => router.push("/masters/parties?new=1")}
           className="gap-2"
         >
           <Users className="h-4 w-4 text-muted-foreground" />
           Party
         </DropdownMenuItem>
         <DropdownMenuItem
           onSelect={() => router.push("/masters/items?new=1")}
           className="gap-2"
         >
           <Store className="h-4 w-4 text-muted-foreground" />
           Item
         </DropdownMenuItem>
       </DropdownMenuContent>
     </DropdownMenu>
   )
 }
 
