 "use client"
 
 import { Button } from "@/components/ui/button"
 import { FormLayout } from "@/components/ui-kit/FormLayout"
 import { FormSectionCard } from "@/components/ui-kit/FormSectionCard"
 import { ItemInput } from "@/lib/items"
 import { useMemo, useState } from "react"
 
 export function ItemForm({
   initialValue,
   onSubmit,
   submitLabel,
 }: {
   initialValue?: ItemInput
   submitLabel: string
   onSubmit: (input: ItemInput) => Promise<void>
 }) {
   const [name, setName] = useState(initialValue?.name ?? "")
   const [code, setCode] = useState(initialValue?.code ?? "")
   const [hsnCode, setHsnCode] = useState(initialValue?.hsnCode ?? "")
   const [taxRate, setTaxRate] = useState<string>(
     initialValue ? String(initialValue.taxRate) : ""
   )
   const [unitPrice, setUnitPrice] = useState<string>(
     initialValue ? String(initialValue.unitPrice) : ""
   )
   const [uom, setUom] = useState(initialValue?.uom ?? "")
   const [submitting, setSubmitting] = useState(false)
   const [error, setError] = useState<string | null>(null)
 
   const normalizedInput = useMemo<ItemInput>(() => {
     const rate = Number.parseFloat(taxRate)
     const price = Number.parseFloat(unitPrice)
     return {
       name: name.trim(),
       code: code.trim(),
       hsnCode: hsnCode.trim() ? hsnCode.trim() : null,
       taxRate: Number.isFinite(rate) ? rate : 0,
       unitPrice: Number.isFinite(price) ? price : 0,
       uom: uom.trim(),
     }
   }, [name, code, hsnCode, taxRate, unitPrice, uom])
 
   const validationError = useMemo(() => {
     if (!normalizedInput.name) return "Name is required"
     if (!normalizedInput.code) return "Code is required"
     if (!normalizedInput.uom) return "UOM is required"
     if (normalizedInput.taxRate < 0) return "Tax rate must be >= 0"
     if (normalizedInput.unitPrice < 0) return "Unit price must be >= 0"
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
         <FormSectionCard title="Item Details">
           <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
             <Field label="Name">
               <input
                 className="w-full px-3 py-2 rounded-md border border-input bg-background"
                 value={name}
                 onChange={(e) => setName(e.target.value)}
                 placeholder="Steel Rod"
                 autoFocus
               />
             </Field>
             <Field label="Code">
               <input
                 className="w-full px-3 py-2 rounded-md border border-input bg-background"
                 value={code}
                 onChange={(e) => setCode(e.target.value)}
                 placeholder="STL-ROD-10MM"
               />
             </Field>
             <Field label="HSN Code">
               <input
                 className="w-full px-3 py-2 rounded-md border border-input bg-background"
                 value={hsnCode ?? ""}
                 onChange={(e) => setHsnCode(e.target.value)}
                 placeholder="7301"
               />
             </Field>
             <Field label="Tax Rate (%)">
               <input
                 className="w-full px-3 py-2 rounded-md border border-input bg-background"
                 value={taxRate}
                 inputMode="decimal"
                 onChange={(e) => setTaxRate(e.target.value)}
                 placeholder="18"
               />
             </Field>
             <Field label="Unit Price">
               <input
                 className="w-full px-3 py-2 rounded-md border border-input bg-background"
                 value={unitPrice}
                 inputMode="decimal"
                 onChange={(e) => setUnitPrice(e.target.value)}
                 placeholder="100.00"
               />
             </Field>
             <Field label="UOM">
               <input
                 className="w-full px-3 py-2 rounded-md border border-input bg-background"
                 value={uom}
                 onChange={(e) => setUom(e.target.value)}
                 placeholder="KG"
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
 
