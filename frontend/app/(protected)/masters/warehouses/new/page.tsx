 "use client"
 
 import Link from "next/link"
 import { useRouter } from "next/navigation"
 import { WarehouseForm } from "@/components/masters/WarehouseForm"
 import { PageHeader } from "@/components/ui/page-header"
 import { Button } from "@/components/ui/button"
 import { createWarehouse, WarehouseInput } from "@/lib/warehouses"
 
 export default function NewWarehousePage() {
   const router = useRouter()
 
   async function onSubmit(input: WarehouseInput) {
     await createWarehouse(input)
     router.push("/masters/warehouses")
     router.refresh()
   }
 
   return (
     <div className="space-y-6">
       <PageHeader
         title="New Warehouse"
         description="Add a new storage location."
         actions={
           <Link href="/masters/warehouses">
             <Button variant="outline">Back</Button>
           </Link>
         }
       />
       <WarehouseForm submitLabel="Create Warehouse" onSubmit={onSubmit} />
     </div>
   )
 }
 
