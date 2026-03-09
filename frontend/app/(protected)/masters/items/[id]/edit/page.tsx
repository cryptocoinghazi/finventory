"use client"

import { useEffect, useState } from "react"
import { useParams, useRouter } from "next/navigation"
import { ItemForm } from "@/components/masters/ItemForm"
import { PageHeader } from "@/components/ui/page-header"
import { Button } from "@/components/ui/button"
import { getItem, updateItem, Item, ItemInput } from "@/lib/items"
import Link from "next/link"

export default function EditItemPage() {
  const params = useParams()
  const router = useRouter()
  const id = params.id as string
  
  const [item, setItem] = useState<Item | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    async function load() {
      try {
        setLoading(true)
        const data = await getItem(id)
        setItem(data)
      } catch (err) {
        setError(err instanceof Error ? err.message : "Failed to load item")
      } finally {
        setLoading(false)
      }
    }
    load()
  }, [id])

  async function onSubmit(input: ItemInput) {
    await updateItem(id, input)
    router.push("/masters/items")
    router.refresh()
  }

  if (loading) {
    return <div className="p-8 text-center">Loading...</div>
  }

  if (error || !item) {
    return (
      <div className="p-8 text-center text-destructive">
        Error: {error || "Item not found"}
        <div className="mt-4">
          <Link href="/masters/items">
            <Button variant="outline">Back to List</Button>
          </Link>
        </div>
      </div>
    )
  }

  return (
    <div className="space-y-6">
      <PageHeader
        title="Edit Item"
        description={`Edit details for ${item.name}`}
        actions={
          <Link href="/masters/items">
            <Button variant="outline">Back</Button>
          </Link>
        }
      />
      <ItemForm 
        submitLabel="Update Item" 
        initialValue={item}
        onSubmit={onSubmit} 
      />
    </div>
  )
}
