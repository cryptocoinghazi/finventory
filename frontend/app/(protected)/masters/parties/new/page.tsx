"use client"

import Link from "next/link"
import { useRouter } from "next/navigation"
import { PartyForm } from "@/components/masters/PartyForm"
import { PageHeader } from "@/components/ui/page-header"
import { Button } from "@/components/ui/button"
import { createParty, PartyInput } from "@/lib/parties"

export default function NewPartyPage() {
  const router = useRouter()

  async function onSubmit(input: PartyInput) {
    await createParty(input)
    router.push("/masters/parties")
    router.refresh()
  }

  return (
    <div className="space-y-6">
      <PageHeader
        title="New Party"
        description="Add a customer or vendor."
        actions={
          <Link href="/masters/parties">
            <Button variant="outline">Back</Button>
          </Link>
        }
      />
      <PartyForm submitLabel="Create Party" onSubmit={onSubmit} />
    </div>
  )
}

