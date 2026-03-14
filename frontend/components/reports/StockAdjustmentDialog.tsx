import { ReactNode, useMemo, useState } from "react"
import { Button } from "@/components/ui/button"
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from "@/components/ui/dialog"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Textarea } from "@/components/ui/textarea"
import { createStockAdjustment } from "@/lib/stock-adjustments"
import { useToast } from "@/components/ui/use-toast"
import { Settings2 } from "lucide-react"
import { SmartSelect } from "@/components/ui-kit/SmartSelect"
import { Item } from "@/lib/items"
import { Warehouse } from "@/lib/warehouses"

interface Props {
  itemId?: string
  itemName?: string
  warehouseId?: string
  warehouseName?: string
  items?: Item[]
  warehouses?: Warehouse[]
  onSuccess: () => void
  trigger?: ReactNode
}

export function StockAdjustmentDialog({
  itemId,
  itemName,
  warehouseId,
  warehouseName,
  items,
  warehouses,
  onSuccess,
  trigger,
}: Props) {
  const [open, setOpen] = useState(false)
  const [qty, setQty] = useState("")
  const [reason, setReason] = useState("")
  const [loading, setLoading] = useState(false)
  const [selectedItemId, setSelectedItemId] = useState(itemId ?? "")
  const [selectedWarehouseId, setSelectedWarehouseId] = useState(warehouseId ?? "")
  const { toast } = useToast()

  const selectedItem = useMemo(() => {
    if (!items?.length || !selectedItemId) return null
    return items.find((i) => i.id === selectedItemId) ?? null
  }, [items, selectedItemId])

  const selectedWarehouse = useMemo(() => {
    if (!warehouses?.length || !selectedWarehouseId) return null
    return warehouses.find((w) => w.id === selectedWarehouseId) ?? null
  }, [selectedWarehouseId, warehouses])

  async function handleSubmit() {
    const effectiveItemId = itemId ?? selectedItemId
    const effectiveWarehouseId = warehouseId ?? selectedWarehouseId

    if (!effectiveItemId) {
      toast({ variant: "destructive", title: "Select an item" })
      return
    }
    if (!effectiveWarehouseId) {
      toast({ variant: "destructive", title: "Select a warehouse" })
      return
    }

    const quantity = parseFloat(qty)
    if (isNaN(quantity) || quantity === 0) {
      toast({ variant: "destructive", title: "Invalid Quantity" })
      return
    }

    setLoading(true)
    try {
      await createStockAdjustment({
        itemId: effectiveItemId,
        warehouseId: effectiveWarehouseId,
        quantity,
        reason,
        adjustmentDate: new Date().toISOString().split("T")[0],
      })
      toast({ title: "Success", description: "Stock adjusted" })
      onSuccess()
      setOpen(false)
      setQty("")
      setReason("")
    } catch (err) {
      toast({
        variant: "destructive",
        title: "Error",
        description: err instanceof Error ? err.message : "Failed",
      })
    } finally {
      setLoading(false)
    }
  }

  return (
    <Dialog open={open} onOpenChange={setOpen}>
      <DialogTrigger asChild>
        {trigger ? (
          trigger
        ) : (
          <Button variant="ghost" size="icon" title="Adjust Stock">
            <Settings2 className="h-4 w-4" />
          </Button>
        )}
      </DialogTrigger>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Adjust Stock</DialogTitle>
        </DialogHeader>
        <div className="grid gap-4 py-4">
          <div className="grid grid-cols-4 items-center gap-4">
            <Label className="text-right">Item</Label>
            <div className="col-span-3 font-medium">
              {itemId ? (
                itemName
              ) : (
                <SmartSelect<Item>
                  value={selectedItemId}
                  onSelect={(id) => setSelectedItemId(id === "__all__" ? "" : id)}
                  placeholder="Select item"
                  searchPlaceholder="Search item..."
                  options={items ?? []}
                  labelKey="name"
                  valueKey="id"
                  renderOption={(it) => (
                    <div className="flex flex-col">
                      <span className="text-sm">
                        {it.code} - {it.name}
                      </span>
                      <span className="text-xs text-muted-foreground">
                        Price: ₹{Number(it.unitPrice || 0).toLocaleString("en-IN")}
                      </span>
                    </div>
                  )}
                  renderValue={(it) => (
                    <span className="truncate">
                      {it.code} - {it.name}
                    </span>
                  )}
                  filterOption={(it, q) => {
                    const hay = `${it.name} ${it.code} ${it.hsnCode ?? ""} ${it.barcode ?? ""}`.toLowerCase()
                    return hay.includes(q)
                  }}
                  initialLabel={selectedItem ? `${selectedItem.code} - ${selectedItem.name}` : undefined}
                />
              )}
            </div>
          </div>
          <div className="grid grid-cols-4 items-center gap-4">
            <Label className="text-right">Warehouse</Label>
            <div className="col-span-3 font-medium">
              {warehouseId ? (
                warehouseName
              ) : (
                <SmartSelect<Warehouse>
                  value={selectedWarehouseId}
                  onSelect={(id) => setSelectedWarehouseId(id === "__all__" ? "" : id)}
                  placeholder="Select warehouse"
                  searchPlaceholder="Search warehouse..."
                  options={warehouses ?? []}
                  labelKey="name"
                  valueKey="id"
                  renderValue={(w) => <span className="truncate">{w.name}</span>}
                  filterOption={(w, q) => w.name.toLowerCase().includes(q)}
                  initialLabel={selectedWarehouse ? selectedWarehouse.name : undefined}
                />
              )}
            </div>
          </div>
          <div className="grid grid-cols-4 items-center gap-4">
            <Label htmlFor="qty" className="text-right">
              Quantity (+/-)
            </Label>
            <Input
              id="qty"
              type="number"
              value={qty}
              onChange={(e) => setQty(e.target.value)}
              className="col-span-3"
              placeholder="e.g. 10 or -5"
            />
          </div>
          <div className="grid grid-cols-4 items-center gap-4">
            <Label htmlFor="reason" className="text-right">
              Reason
            </Label>
            <Textarea
              id="reason"
              value={reason}
              onChange={(e) => setReason(e.target.value)}
              className="col-span-3"
            />
          </div>
        </div>
        <Button onClick={handleSubmit} disabled={loading}>
          {loading ? "Saving..." : "Save Adjustment"}
        </Button>
      </DialogContent>
    </Dialog>
  )
}
