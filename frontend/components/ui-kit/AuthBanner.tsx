 "use client"
 
 import * as React from "react"
 import Link from "next/link"
 import { Button } from "@/components/ui/button"
 
 export function AuthBanner() {
   const [show, setShow] = React.useState(false)
 
   React.useEffect(() => {
     try {
       const v = window.localStorage.getItem("authMissing")
       setShow(v === "1")
     } catch {}
   }, [])
 
   if (!show) return null
 
   return (
     <div className="mb-4 rounded-xl border border-destructive bg-destructive/10 text-destructive px-3 py-2 text-sm flex items-center justify-between">
       <span>Authorization missing. Please sign in again.</span>
       <div className="flex items-center gap-2">
         <Link href="/login">
           <Button size="sm" variant="destructive">Sign in</Button>
         </Link>
         <Button
           size="sm"
           variant="outline"
           onClick={() => {
             try {
               window.localStorage.removeItem("authMissing")
             } catch {}
             setShow(false)
           }}
         >
           Dismiss
         </Button>
       </div>
     </div>
   )
 }
 
