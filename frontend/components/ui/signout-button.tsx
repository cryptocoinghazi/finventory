 "use client"
 
 import { Button } from "@/components/ui/button"
 
 export function SignOutButton() {
   function signOut() {
     try {
       window.localStorage.removeItem("token")
     } catch {}
     try {
       document.cookie = "token=; path=/; max-age=0; samesite=lax"
     } catch {}
     try {
       window.location.href = "/login"
     } catch {}
   }
 
   return (
     <Button
       aria-label="Sign out"
       variant="outline"
       className="shadow-soft border-border-subtle"
       onClick={signOut}
     >
       Sign out
     </Button>
   )
 }
 
