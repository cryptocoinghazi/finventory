"use client"

import Link from "next/link"
import { useEffect, useState } from "react"
import { useRouter } from "next/navigation"
import { Button } from "@/components/ui/button"
import { PageHeader } from "@/components/ui/page-header"
import { PartyForm } from "@/components/masters/PartyForm"
import { getParty, PartyInput, updateParty } from "@/lib/parties"

export default function EditPartyPage({
  params,
}: {
  params: { id: string }
}) {
  const router = useRouter()
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [initialValue, setInitialValue] = useState<PartyInput | null>(null)

  useEffect(() => {
    async function load() {
      setLoading(true)
      setError(null)
      try {
        const p = await getParty(params.id)
        setInitialValue({
          name: p.name,
          type: p.type,
          gstin: p.gstin ?? null,
          stateCode: p.stateCode ?? null,
          address: p.address ?? null,
          phone: p.phone ?? null,
          email: p.email ?? null,
        })
      } catch (err) {
        setError(err instanceof Error ? err.message : "Failed to load party")
      } finally {
        setLoading(false)
      }
    }
    load()
  }, [params.id])

  async function onSubmit(input: PartyInput) {
    await updateParty(params.id, input)
    router.push("/masters/parties")
    router.refresh()
  }

  return (
    <div className="space-y-6">
      <PageHeader
        title="Edit Party"
        description="Update party details."
        actions={
          <Link href="/masters/parties">
            <Button variant="outline">Back</Button>
          </Link>
        }
      />

      {error ? <div className="text-sm text-destructive">{error}</div> : null}

      {loading ? (
        <div className="text-sm text-muted-foreground">Loading...</div>
      ) : initialValue ? (
        <PartyForm
          initialValue={initialValue}
          submitLabel="Save Changes"
          onSubmit={onSubmit}
        />
      ) : null}
    </div>
  )
}

