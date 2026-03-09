 "use client"
 
 import Link from "next/link"
 import { useEffect, useState } from "react"
 import { useRouter } from "next/navigation"
 import { Button } from "@/components/ui/button"
 import { PageHeader } from "@/components/ui/page-header"
 import { ItemForm } from "@/components/masters/ItemForm"
 import { getItem, ItemInput, updateItem } from "@/lib/items"
 
 export default function EditItemPage({
   params,
 }: {
   params: { id: string }
 }) {
   const router = useRouter()
   const [loading, setLoading] = useState(true)
   const [error, setError] = useState<string | null>(null)
   const [initialValue, setInitialValue] = useState<ItemInput | null>(null)
 
   useEffect(() => {
     async function load() {
       setLoading(true)
       setError(null)
       try {
         const it = await getItem(params.id)
         setInitialValue({
           name: it.name,
           code: it.code,
           hsnCode: it.hsnCode ?? null,
           taxRate: it.taxRate,
           unitPrice: it.unitPrice,
           uom: it.uom,
        })
      } catch (err) {
        setError(err instanceof Error ? err.message : "Failed to load item")
      } finally {
        setLoading(false)
       }
     }
     load()
   }, [params.id])
 
   async function onSubmit(input: ItemInput) {
     await updateItem(params.id, input)
     router.push("/masters/items")
     router.refresh()
   }
 
   return (
     <div className="space-y-6">
       <PageHeader
         title="Edit Item"
         description="Update item details."
         actions={
           <Link href="/masters/items">
             <Button variant="outline">Back</Button>
           </Link>
         }
       />
 
       {error ? <div className="text-sm text-destructive">{error}</div> : null}
 
       {loading ? (
         <div className="text-sm text-muted-foreground">Loading...</div>
       ) : initialValue ? (
         <ItemForm
           initialValue={initialValue}
           submitLabel="Save Changes"
           onSubmit={onSubmit}
         />
       ) : null}
     </div>
   )
 }
 
