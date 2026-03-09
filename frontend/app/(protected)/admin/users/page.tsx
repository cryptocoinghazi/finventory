"use client"

import { useEffect, useState } from "react"
import { Button } from "@/components/ui/button"
import { PageHeader } from "@/components/ui/page-header"
import { DataTablePro } from "@/components/ui-kit/DataTablePro"
import { listUsers, User } from "@/lib/users"
import { Plus } from "lucide-react"

export default function UsersPage() {
  const [data, setData] = useState<User[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    loadData()
  }, [])

  async function loadData() {
    try {
      setLoading(true)
      const res = await listUsers()
      setData(res)
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to load users")
    } finally {
      setLoading(false)
    }
  }

  const columns = [
    {
      key: "username",
      header: "Username",
    },
    {
      key: "email",
      header: "Email",
    },
    {
      key: "role",
      header: "Role",
    },
  ]

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <PageHeader
          title="Users"
          description="Manage system users and roles"
        />
        <Button className="gap-2">
          <Plus className="h-4 w-4" />
          New User
        </Button>
      </div>

      {error ? (
        <div className="p-4 rounded-lg border border-destructive/50 bg-destructive/10 text-destructive">
          {error}
        </div>
      ) : (
        <DataTablePro
          columns={columns}
          data={data}
          loading={loading}
          searchKey="username"
        />
      )}
    </div>
  )
}
