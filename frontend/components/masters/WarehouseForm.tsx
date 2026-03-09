 "use client"
 
 import { Button } from "@/components/ui/button"
 import { FormLayout } from "@/components/ui-kit/FormLayout"
 import { FormSectionCard } from "@/components/ui-kit/FormSectionCard"
 import { WarehouseInput } from "@/lib/warehouses"
 import { useMemo, useState } from "react"
 
 export function WarehouseForm({
   initialValue,
   onSubmit,
   submitLabel,
 }: {
   initialValue?: WarehouseInput
   submitLabel: string
   onSubmit: (input: WarehouseInput) => Promise<void>
 }) {
   const [name, setName] = useState(initialValue?.name ?? "")
   const [stateCode, setStateCode] = useState(initialValue?.stateCode ?? "")
   const [location, setLocation] = useState(initialValue?.location ?? "")
   const [submitting, setSubmitting] = useState(false)
   const [error, setError] = useState<string | null>(null)
 
   const normalizedInput = useMemo<WarehouseInput>(() => {
     return {
       name: name.trim(),
       stateCode: stateCode.trim() ? stateCode.trim() : null,
       location: location.trim() ? location.trim() : null,
     }
   }, [name, stateCode, location])
 
   const validationError = useMemo(() => {
     if (!normalizedInput.name) return "Name is required"
     if (normalizedInput.stateCode && !/^\d{2}$/.test(normalizedInput.stateCode)) {
       return "State code must be 2 digits"
     }
     return null
   }, [normalizedInput])
 
   async function handleSubmit(e: React.FormEvent) {
     e.preventDefault()
     setError(null)
     if (validationError) {
       setError(validationError)
       return
     }
     setSubmitting(true)
     try {
      await onSubmit(normalizedInput)
    } catch (err) {
      setError(err instanceof Error ? err.message : "Save failed")
    } finally {
      setSubmitting(false)
     }
   }
 
   return (
     <form onSubmit={handleSubmit} className="space-y-6">
       <FormLayout>
         <FormSectionCard title="Warehouse Details">
           <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
             <Field label="Name">
               <input
                 className="w-full px-3 py-2 rounded-md border border-input bg-background"
                 value={name}
                 onChange={(e) => setName(e.target.value)}
                 placeholder="Main Warehouse"
                 autoFocus
               />
             </Field>
             <Field label="State Code">
               <input
                 className="w-full px-3 py-2 rounded-md border border-input bg-background"
                 value={stateCode ?? ""}
                 onChange={(e) => setStateCode(e.target.value)}
                 placeholder="22"
                 inputMode="numeric"
               />
             </Field>
             <Field label="Location">
               <input
                 className="w-full px-3 py-2 rounded-md border border-input bg-background"
                 value={location ?? ""}
                 onChange={(e) => setLocation(e.target.value)}
                 placeholder="City/Address"
               />
             </Field>
           </div>
           {error ? <div className="text-sm text-destructive">{error}</div> : null}
         </FormSectionCard>
       </FormLayout>
 
       <div className="flex items-center gap-2 sticky bottom-0 bg-background/80 backdrop-blur p-3 border-t border-border">
         <Button disabled={submitting || Boolean(validationError)}>
           {submitting ? "Saving..." : submitLabel}
         </Button>
       </div>
     </form>
   )
 }
 
 function Field({
   label,
   children,
 }: {
   label: string
   children: React.ReactNode
 }) {
   return (
     <div className="space-y-2">
       <div className="text-sm">{label}</div>
       {children}
     </div>
   )
 }
 
