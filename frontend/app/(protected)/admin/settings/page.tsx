"use client"

import { PageHeader } from "@/components/ui/page-header"

export default function SettingsPage() {
  return (
    <div className="space-y-6">
      <PageHeader
        title="Settings"
        description="Manage application settings"
      />
      <div className="flex h-[400px] items-center justify-center rounded-lg border border-dashed">
        <div className="text-center">
          <h3 className="text-lg font-medium">Coming Soon</h3>
          <p className="text-sm text-muted-foreground">
            System settings and configuration will be available here.
          </p>
        </div>
      </div>
    </div>
  )
}
