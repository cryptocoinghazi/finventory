"use client"

import { useRouter } from "next/navigation"
import { OfferForm } from "@/components/offers/OfferForm"
import { createOffer } from "@/lib/offers"

export default function NewOfferPage() {
  const router = useRouter()

  return (
    <OfferForm
      submitLabel="Create Offer"
      onSubmit={async (input) => {
        await createOffer(input)
        router.push("/sales/offers")
      }}
    />
  )
}

