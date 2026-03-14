 "use client"
 
 import Link from "next/link"
 import { useRouter } from "next/navigation"
 import { ItemForm } from "@/components/masters/ItemForm"
 import { PageHeader } from "@/components/ui/page-header"
 import { Button } from "@/components/ui/button"
 import { createItem, ItemInput, uploadItemImage } from "@/lib/items"
 
 export default function NewItemPage() {
   const router = useRouter()
 
  async function onSubmit(input: ItemInput, imageFile: File | null) {
    const created = await createItem(input)
    if (imageFile) {
      await uploadItemImage(created.id, imageFile)
    }
     router.push("/masters/items")
     router.refresh()
   }
 
   return (
     <div className="space-y-6">
       <PageHeader
         title="New Item"
         description="Add a new inventory item."
         actions={
           <Link href="/masters/items">
             <Button variant="outline">Back</Button>
           </Link>
         }
       />
      <ItemForm submitLabel="Create Item" onSubmit={onSubmit} />
     </div>
   )
 }
 
