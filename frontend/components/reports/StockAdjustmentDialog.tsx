import { useState } from "react"
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

interface Props {
  itemId: string
  itemName: string
  warehouseId: string
  warehouseName: string
  onSuccess: () => void
}

export function StockAdjustmentDialog({
  itemId,
  itemName,
  warehouseId,
  warehouseName,
  onSuccess,
}: Props) {
  const [open, setOpen] = useState(false)
  const [qty, setQty] = useState("")
  const [reason, setReason] = useState("")
  const [loading, setLoading] = useState(false)
  const { toast } = useToast()

  async function handleSubmit() {
    const quantity = parseFloat(qty)
    if (isNaN(quantity) || quantity === 0) {
      toast({ variant: "destructive", title: "Invalid Quantity" })
      return
    }

    setLoading(true)
    try {
      await createStockAdjustment({
        itemId,
        warehouseId,
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
        <Button variant="ghost" size="icon" title="Adjust Stock">
          <Settings2 className="h-4 w-4" />
        </Button>
      </DialogTrigger>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Adjust Stock</DialogTitle>
        </DialogHeader>
        <div className="grid gap-4 py-4">
          <div className="grid grid-cols-4 items-center gap-4">
            <Label className="text-right">Item</Label>
            <div className="col-span-3 font-medium">{itemName}</div>
          </div>
          <div className="grid grid-cols-4 items-center gap-4">
            <Label className="text-right">Warehouse</Label>
            <div className="col-span-3 font-medium">{warehouseName}</div>
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
