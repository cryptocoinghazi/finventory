"use client"

import Link from "next/link"
import { useEffect, useState } from "react"
import { useParams, useRouter } from "next/navigation"

import { OfferForm } from "@/components/offers/OfferForm"
import { Button } from "@/components/ui/button"
import { PageHeader } from "@/components/ui/page-header"
import { getOffer, Offer, OfferInput, updateOffer } from "@/lib/offers"

export default function EditOfferPage() {
  const params = useParams()
  const router = useRouter()
  const id = params.id as string

  const [offer, setOffer] = useState<Offer | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    async function load() {
      try {
        setLoading(true)
        setError(null)
        const data = await getOffer(id)
        setOffer(data)
      } catch (err) {
        setError(err instanceof Error ? err.message : "Failed to load offer")
      } finally {
        setLoading(false)
      }
    }
    load()
  }, [id])

  async function onSubmit(input: OfferInput) {
    await updateOffer(id, input)
    router.push("/sales/offers")
    router.refresh()
  }

  if (loading) {
    return <div className="p-8 text-center">Loading...</div>
  }

  if (error || !offer) {
    return (
      <div className="p-8 text-center text-destructive">
        Error: {error || "Offer not found"}
        <div className="mt-4">
          <Link href="/sales/offers">
            <Button variant="outline">Back to List</Button>
          </Link>
        </div>
      </div>
    )
  }

  return (
    <div className="space-y-6">
      <PageHeader
        title="Edit Offer"
        description={`Edit details for ${offer.name}`}
        actions={
          <Link href="/sales/offers">
            <Button variant="outline">Back</Button>
          </Link>
        }
      />
      <OfferForm submitLabel="Update Offer" initialValue={offer} onSubmit={onSubmit} />
    </div>
  )
}

