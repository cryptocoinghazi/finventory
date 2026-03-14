 "use client"
 
 import * as React from "react"
 import { Bell } from "lucide-react"
 import {
   DropdownMenu,
   DropdownMenuContent,
   DropdownMenuItem,
   DropdownMenuLabel,
   DropdownMenuSeparator,
   DropdownMenuTrigger,
 } from "@/components/ui/dropdown-menu"
 import { Button } from "@/components/ui/button"
 
 export function NotificationBell() {
   return (
     <DropdownMenu>
       <DropdownMenuTrigger asChild>
         <Button aria-label="Notifications" variant="outline" size="icon" className="shadow-soft">
           <Bell className="h-4 w-4" />
         </Button>
       </DropdownMenuTrigger>
       <DropdownMenuContent align="end" className="min-w-[240px]">
         <DropdownMenuLabel>Notifications</DropdownMenuLabel>
         <DropdownMenuSeparator />
         <DropdownMenuItem className="text-sm text-muted-foreground">
           No new notifications
         </DropdownMenuItem>
       </DropdownMenuContent>
     </DropdownMenu>
   )
 }
 
