"use client"

import { useEffect, useState } from "react"
import { PageHeader } from "@/components/ui/page-header"
import { DataTablePro } from "@/components/ui-kit/DataTablePro"
import { listUsers, deleteUser, User } from "@/lib/users"
import { UserDialog } from "./UserDialog"
import { ConfirmDialog } from "@/components/ui-kit/ConfirmDialog"
import { Trash2 } from "lucide-react"
import { Button } from "@/components/ui/button"

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

  function renderActions(user: User) {
    return (
      <ConfirmDialog
        title={`Delete ${user.username}?`}
        description="This action cannot be undone."
        confirmText="Delete"
        onConfirm={async () => {
          try {
            await deleteUser(user.id)
            setData((prev) => prev.filter((u) => u.id !== user.id))
          } catch (err) {
            setError(err instanceof Error ? err.message : "Failed to delete user")
          }
        }}
      >
        <Button variant="ghost" size="icon" className="h-8 w-8 text-destructive">
          <Trash2 className="h-4 w-4" />
        </Button>
      </ConfirmDialog>
    )
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
    {
      key: "actions",
      header: "",
      cell: renderActions,
    },
  ]

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <PageHeader
          title="Users"
          description="Manage system users and roles"
        />
        <UserDialog onSuccess={loadData} />
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
