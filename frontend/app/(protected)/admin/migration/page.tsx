"use client"

import { useCallback, useEffect, useMemo, useRef, useState, type ReactNode } from "react"
import { PageHeader } from "@/components/ui/page-header"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Button } from "@/components/ui/button"
import { Badge } from "@/components/ui/badge"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Separator } from "@/components/ui/separator"
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table"
import { useToast } from "@/components/ui/use-toast"
import { InlineErrorCallout } from "@/components/ui-kit/InlineErrorCallout"
import { ConfirmDialog } from "@/components/ui-kit/ConfirmDialog"
import { ChevronDown, ChevronRight } from "lucide-react"
import { getCurrentUser } from "@/lib/users"
import {
  cancelFullSafePipeline,
  createMigrationRun,
  createDatabaseBackup,
  DatabaseBackup,
  downloadDatabaseBackup,
  executeMigrationStage,
  getFullSafePipelineProgress,
  listDatabaseBackups,
  listMigrationDbTables,
  listMigrationLogsForRun,
  listMigrationRuns,
  listMigrationStages,
  listMigrationStagesForRun,
  MigrationDbTable,
  MigrationLogEntry,
  MigrationPipelinePreset,
  MigrationPipelineProgress,
  MigrationRun,
  MigrationStageExecution,
  resumeFullSafePipeline,
  retryFullSafeStage,
  startFullSafePipeline,
  truncateDatabase,
} from "@/lib/migration"

function formatDateTime(value?: string | null) {
  if (!value) return "-"
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  return date.toLocaleString("en-IN", {
    year: "numeric",
    month: "short",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
  })
}

function prettyJson(value?: string | null) {
  if (!value) return null
  try {
    return JSON.stringify(JSON.parse(value), null, 2)
  } catch {
    return value
  }
}

function formatBytes(value?: number | null) {
  if (!value || value <= 0) return "-"
  const units = ["B", "KB", "MB", "GB", "TB"]
  let n = value
  let idx = 0
  while (n >= 1024 && idx < units.length - 1) {
    n = n / 1024
    idx += 1
  }
  const decimals = idx === 0 ? 0 : 1
  return `${n.toFixed(decimals)} ${units[idx]}`
}

type StageStats = {
  reconciliation?: {
    expectedTotal?: number | string | null
    paymentsTotal?: number | string | null
    difference?: number | string | null
    mismatchedOrders?: number | string | null
  } | null
  fallbackUsage?: {
    fallbackPartyUsed?: number | null
    fallbackPartyCreated?: number | null
    fallbackWarehouseCreated?: number | null
  } | null
  vendorsCreatedFromCategories?: number | null
  vendorsLinkedExisting?: number | null
  vendorsAlreadyMapped?: number | null
  vendors?: {
    created?: number | null
    linkedExisting?: number | null
    alreadyMapped?: number | null
  } | null
  created?: number | null
  updated?: number | null
  wouldCreate?: number | null
  wouldUpdate?: number | null
  skippedOutOfScope?: number | null
  skippedOverLimit?: number | null
  warnings?: number | null
  errors?: number | null
}

function parseJsonObject(value?: string | null): StageStats | null {
  if (!value) return null
  try {
    const parsed: unknown = JSON.parse(value)
    if (typeof parsed !== "object" || parsed === null) return null
    return parsed as StageStats
  } catch {
    return null
  }
}

function CollapsibleCard({
  title,
  subtitle,
  open,
  onOpenChange,
  actions,
  children,
}: {
  title: string
  subtitle?: string
  open: boolean
  onOpenChange: (open: boolean) => void
  actions?: ReactNode
  children: ReactNode
}) {
  return (
    <Card>
      <CardHeader className="flex flex-row items-center justify-between gap-3">
        <button
          type="button"
          className="flex min-w-0 items-center gap-2 text-left"
          onClick={() => onOpenChange(!open)}
          aria-expanded={open}
        >
          <span className="flex h-8 w-8 items-center justify-center rounded-md border border-border bg-muted/20">
            {open ? <ChevronDown className="h-4 w-4" /> : <ChevronRight className="h-4 w-4" />}
          </span>
          <div className="min-w-0">
            <CardTitle className="text-base">{title}</CardTitle>
            {subtitle ? <div className="text-xs text-muted-foreground">{subtitle}</div> : null}
          </div>
        </button>
        {actions ? <div className="flex items-center gap-2">{actions}</div> : null}
      </CardHeader>
      {open ? <CardContent className="space-y-4">{children}</CardContent> : null}
    </Card>
  )
}

export default function AdminMigrationPage() {
  const { toast } = useToast()

  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [isAdmin, setIsAdmin] = useState<boolean | null>(null)
  const [logsOpen, setLogsOpen] = useState(false)
  const [runHistoryOpen, setRunHistoryOpen] = useState(false)
  const [advancedOpen, setAdvancedOpen] = useState(false)
  const [logLevelFilter, setLogLevelFilter] = useState<"ALL" | "INFO" | "WARN" | "ERROR">(
    "ALL"
  )

  const [stages, setStages] = useState<string[]>([])
  const [runs, setRuns] = useState<MigrationRun[]>([])
  const [selectedRunId, setSelectedRunId] = useState<string | null>(null)
  const [selectedRun, setSelectedRun] = useState<MigrationRun | null>(null)
  const [stageExecutions, setStageExecutions] = useState<MigrationStageExecution[]>([])
  const [logs, setLogs] = useState<MigrationLogEntry[]>([])

  const [sourceReference, setSourceReference] = useState("../docs/nexo.sql")
  const [dryRun, setDryRun] = useState(true)
  const [sourceIdMin, setSourceIdMin] = useState("")
  const [sourceIdMax, setSourceIdMax] = useState("")
  const [limit, setLimit] = useState("")

  const [pipelineLoading, setPipelineLoading] = useState(false)
  const [pipelineProgress, setPipelineProgress] = useState<MigrationPipelineProgress | null>(
    null
  )
  const lastCompletedStagesRef = useRef<string[]>([])
  const lastFailedStageRef = useRef<string | null>(null)
  const lastActiveRef = useRef<boolean>(false)

  const [dbTablesLoading, setDbTablesLoading] = useState(false)
  const [dbTablesError, setDbTablesError] = useState<string | null>(null)
  const [dbTables, setDbTables] = useState<MigrationDbTable[]>([])

  const [backupsLoading, setBackupsLoading] = useState(false)
  const [backupsError, setBackupsError] = useState<string | null>(null)
  const [backups, setBackups] = useState<DatabaseBackup[]>([])
  const [backupCreating, setBackupCreating] = useState(false)

  const [truncateLoading, setTruncateLoading] = useState(false)
  const [truncateKeepUsers, setTruncateKeepUsers] = useState(true)
  const [truncateConfirmText, setTruncateConfirmText] = useState("")

  const selectedStats = useMemo(() => {
    const analyze = stageExecutions.find((s) => s.stageKey === "ANALYZE_SOURCE")
    return prettyJson(analyze?.statsJson ?? null)
  }, [stageExecutions])

  const filteredLogs = useMemo(() => {
    if (logLevelFilter === "ALL") return logs
    return logs.filter((l) => l.level === logLevelFilter)
  }, [logLevelFilter, logs])

  const selectedPipelinePreset: MigrationPipelinePreset | null = useMemo(() => {
    if (!selectedRun) return null
    return selectedRun.dryRun ? "DRY_RUN_FULL_SAFE" : "REAL_PILOT_FULL_SAFE"
  }, [selectedRun])

  const loadAll = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      const [stagesRes, runsRes] = await Promise.all([
        listMigrationStages(),
        listMigrationRuns(),
      ])
      setStages(stagesRes)
      setRuns(runsRes)
      setSelectedRunId((prev) => prev ?? (runsRes[0]?.id ?? null))
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to load migration data")
    } finally {
      setLoading(false)
    }
  }, [])

  const loadDbTables = useCallback(async () => {
    setDbTablesLoading(true)
    setDbTablesError(null)
    try {
      const res = await listMigrationDbTables()
      setDbTables(res)
    } catch (err) {
      setDbTablesError(err instanceof Error ? err.message : "Failed to load DB tables")
    } finally {
      setDbTablesLoading(false)
    }
  }, [])

  const loadBackups = useCallback(async () => {
    setBackupsLoading(true)
    setBackupsError(null)
    try {
      const res = await listDatabaseBackups()
      setBackups(res)
    } catch (err) {
      setBackupsError(err instanceof Error ? err.message : "Failed to load backups")
    } finally {
      setBackupsLoading(false)
    }
  }, [])

  async function loadRun(runId: string) {
    setLoading(true)
    setError(null)
    try {
      const [runsRes, stagesRes, logsRes] = await Promise.all([
        listMigrationRuns(),
        listMigrationStagesForRun(runId),
        listMigrationLogsForRun(runId, 200),
      ])
      setRuns(runsRes)
      const run = runsRes.find((r) => r.id === runId) ?? null
      setSelectedRun(run)
      setStageExecutions(stagesRes)
      setLogs(logsRes)
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to load run")
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    ;(async () => {
      try {
        const user = await getCurrentUser()
        setIsAdmin(user.role === "ADMIN")
      } catch {
        setIsAdmin(null)
      }
    })()
  }, [])

  useEffect(() => {
    loadAll()
  }, [loadAll])

  useEffect(() => {
    if (!isAdmin) return
    loadDbTables()
  }, [isAdmin, loadDbTables])

  useEffect(() => {
    if (!isAdmin) return
    loadBackups()
  }, [isAdmin, loadBackups])

  useEffect(() => {
    if (!selectedRunId) return
    loadRun(selectedRunId)
  }, [selectedRunId])

  const refreshPipeline = useCallback(
    async (runId: string) => {
      if (!selectedPipelinePreset) return
      try {
        const progress = await getFullSafePipelineProgress(runId, selectedPipelinePreset)
        setPipelineProgress(progress)

        const prevCompleted = new Set(lastCompletedStagesRef.current)
        for (const s of progress.completedStages || []) {
          if (!prevCompleted.has(s)) {
            toast({ title: "Stage completed", description: s })
          }
        }
        lastCompletedStagesRef.current = progress.completedStages || []

        const prevFailed = lastFailedStageRef.current
        if (progress.failedStage && progress.failedStage !== prevFailed) {
          toast({
            variant: "destructive",
            title: "Stage failed",
            description: progress.failedStage,
          })
        }
        lastFailedStageRef.current = progress.failedStage || null

        const prevActive = lastActiveRef.current
        if (prevActive && !progress.active && progress.summary === "Finished") {
          toast({ title: "Pipeline finished" })
        }
        lastActiveRef.current = progress.active
      } catch {
        setPipelineProgress((prev) => prev)
      }
    },
    [selectedPipelinePreset, toast]
  )

  useEffect(() => {
    if (!selectedRunId) return
    refreshPipeline(selectedRunId)
  }, [refreshPipeline, selectedRunId])

  useEffect(() => {
    if (!selectedRunId) return
    if (!pipelineProgress?.active) return
    const handle = setInterval(() => {
      refreshPipeline(selectedRunId)
      loadRun(selectedRunId)
    }, 2000)
    return () => clearInterval(handle)
  }, [pipelineProgress?.active, refreshPipeline, selectedRunId])

  function parseOptionalNumber(value: string) {
    const trimmed = value.trim()
    if (!trimmed) return null
    const n = Number(trimmed)
    if (!Number.isFinite(n) || n < 0) return null
    return n
  }

  const truncateExpected = useMemo(() => {
    return truncateKeepUsers ? "TRUNCATE_DATA" : "TRUNCATE_ALL"
  }, [truncateKeepUsers])

  const truncateEnabled = useMemo(() => {
    return truncateConfirmText.trim().toUpperCase() === truncateExpected
  }, [truncateConfirmText, truncateExpected])

  async function onTruncateDatabase() {
    setTruncateLoading(true)
    try {
      const res = await truncateDatabase({
        keepUsers: truncateKeepUsers,
        confirmText: truncateConfirmText,
      })
      toast({
        title: "Database truncated",
        description: `Truncated ${res.truncatedTables.length} tables`,
      })
      setTruncateConfirmText("")
      loadDbTables()
      if (selectedRunId) {
        loadRun(selectedRunId)
      }
    } catch (err) {
      toast({
        variant: "destructive",
        title: "Truncate failed",
        description: err instanceof Error ? err.message : "Failed to truncate database",
      })
    } finally {
      setTruncateLoading(false)
    }
  }

  async function onCreateBackup() {
    setBackupCreating(true)
    try {
      const created = await createDatabaseBackup()
      toast({
        title: "Backup created",
        description: created.fileName ? `Saved as ${created.fileName}` : "Database backup completed",
      })
      loadBackups()
    } catch (err) {
      toast({
        variant: "destructive",
        title: "Backup failed",
        description: err instanceof Error ? err.message : "Failed to create backup",
      })
    } finally {
      setBackupCreating(false)
    }
  }

  async function onCreateRun() {
    setLoading(true)
    setError(null)
    try {
      const min = parseOptionalNumber(sourceIdMin)
      const max = parseOptionalNumber(sourceIdMax)
      const lim = parseOptionalNumber(limit)

      const created = await createMigrationRun({
        sourceSystem: "NEXOPOS",
        sourceReference: sourceReference || undefined,
        dryRun,
        sourceIdMin: min ?? undefined,
        sourceIdMax: max ?? undefined,
        limit: lim ?? undefined,
      })
      toast({ title: "Migration run created" })
      setSelectedRunId(created.id)
      await loadAll()
    } catch (err) {
      toast({
        variant: "destructive",
        title: "Create run failed",
        description: err instanceof Error ? err.message : "Request failed",
      })
    } finally {
      setLoading(false)
    }
  }

  async function onExecuteStage(stageKey: string) {
    if (!selectedRunId) return
    setLoading(true)
    setError(null)
    try {
      await executeMigrationStage(selectedRunId, stageKey)
      toast({ title: `Stage executed: ${stageKey}` })
      await loadRun(selectedRunId)
    } catch (err) {
      toast({
        variant: "destructive",
        title: "Stage execution failed",
        description: err instanceof Error ? err.message : "Request failed",
      })
      await loadRun(selectedRunId)
    } finally {
      setLoading(false)
    }
  }

  async function onRunFullPipeline(confirmed: boolean) {
    if (!selectedRunId || !selectedRun) return
    setPipelineLoading(true)
    setError(null)
    const preset: MigrationPipelinePreset = selectedRun.dryRun
      ? "DRY_RUN_FULL_SAFE"
      : "REAL_PILOT_FULL_SAFE"
    try {
      const res = await startFullSafePipeline(selectedRunId, preset, confirmed)
      setPipelineProgress(res)
      toast({
        title: "Pipeline started",
        description:
          preset === "DRY_RUN_FULL_SAFE"
            ? "Dry-run full safe pipeline"
            : "Real pilot full safe pipeline",
      })
      await loadRun(selectedRunId)
    } catch (err) {
      toast({
        variant: "destructive",
        title: "Pipeline start failed",
        description: err instanceof Error ? err.message : "Request failed",
      })
      await refreshPipeline(selectedRunId)
    } finally {
      setPipelineLoading(false)
    }
  }

  async function onResumePipeline() {
    if (!selectedRunId || !selectedPipelinePreset) return
    setPipelineLoading(true)
    setError(null)
    try {
      const res = await resumeFullSafePipeline(selectedRunId, selectedPipelinePreset)
      setPipelineProgress(res)
      toast({ title: "Pipeline resumed" })
      await loadRun(selectedRunId)
    } catch (err) {
      toast({
        variant: "destructive",
        title: "Resume failed",
        description: err instanceof Error ? err.message : "Request failed",
      })
    } finally {
      setPipelineLoading(false)
    }
  }

  async function onRetryFailedStage() {
    if (!selectedRunId || !pipelineProgress?.failedStage || !selectedPipelinePreset) return
    setPipelineLoading(true)
    setError(null)
    try {
      const res = await retryFullSafeStage(
        selectedRunId,
        pipelineProgress.failedStage,
        selectedPipelinePreset
      )
      setPipelineProgress(res)
      toast({ title: "Retry started", description: pipelineProgress.failedStage })
      await loadRun(selectedRunId)
    } catch (err) {
      toast({
        variant: "destructive",
        title: "Retry failed",
        description: err instanceof Error ? err.message : "Request failed",
      })
    } finally {
      setPipelineLoading(false)
    }
  }

  async function onCancelPipeline() {
    if (!selectedRunId) return
    setPipelineLoading(true)
    setError(null)
    try {
      const res = await cancelFullSafePipeline(selectedRunId)
      setPipelineProgress(res)
      toast({ title: "Cancel requested" })
      await loadRun(selectedRunId)
    } catch (err) {
      toast({
        variant: "destructive",
        title: "Cancel failed",
        description: err instanceof Error ? err.message : "Request failed",
      })
    } finally {
      setPipelineLoading(false)
    }
  }

  const runPipelineButtonDisabled =
    loading || pipelineLoading || !selectedRunId || !!pipelineProgress?.active

  const currentRunStatus = pipelineProgress?.runStatus ?? selectedRun?.status ?? null
  const currentRunStage = pipelineProgress?.currentStage ?? pipelineProgress?.failedStage ?? null
  const completedCount = pipelineProgress?.completedStages?.length ?? 0
  const totalCount = pipelineProgress?.plannedStages?.length ?? 0

  const statusBadgeVariant = useMemo(() => {
    if (!currentRunStatus) return "outline" as const
    if (currentRunStatus === "FAILED") return "destructive" as const
    if (currentRunStatus === "RUNNING") return "secondary" as const
    if (currentRunStatus === "COMPLETED") return "default" as const
    return "outline" as const
  }, [currentRunStatus])

  const runTypeBadgeVariant = selectedRun?.dryRun ? ("secondary" as const) : ("destructive" as const)
  const runTypeLabel = selectedRun?.dryRun ? "Dry-run" : "Real-run"

  const canRunPipeline = !!selectedRunId && !!selectedRun
  const retryRelevant = !!pipelineProgress?.failedStage
  const cancelRelevant = !!pipelineProgress?.active

  const runVendorMappingDisabled =
    loading || pipelineLoading || !selectedRunId || !!pipelineProgress?.active

  const runVendorMappingButton = !selectedRunId || !selectedRun ? (
    <Button disabled>Map Vendors (Categories → Vendors)</Button>
  ) : selectedRun?.dryRun ? (
    <Button
      variant="outline"
      disabled={runVendorMappingDisabled}
      onClick={() => onExecuteStage("IMPORT_PARTIES")}
      title="Runs IMPORT_PARTIES (includes vendor mapping from categories)"
    >
      Map Vendors (Categories → Vendors)
    </Button>
  ) : (
    <ConfirmDialog
      title="Run vendor mapping (write to DB)?"
      description="This executes IMPORT_PARTIES and will create/link vendor parties from categories."
      confirmText="Run Vendor Mapping"
      cancelText="Cancel"
      onConfirm={() => onExecuteStage("IMPORT_PARTIES")}
      disabled={runVendorMappingDisabled}
    >
      <Button
        variant="outline"
        disabled={runVendorMappingDisabled}
        title="Runs IMPORT_PARTIES (includes vendor mapping from categories)"
      >
        Map Vendors (Categories → Vendors)
      </Button>
    </ConfirmDialog>
  )

  const runFullSafePipelineButton = !canRunPipeline ? (
    <Button disabled>Run Full Safe Pipeline</Button>
  ) : selectedRun.dryRun ? (
    <Button onClick={() => onRunFullPipeline(false)} disabled={runPipelineButtonDisabled}>
      Run Full Safe Pipeline
    </Button>
  ) : (
    <ConfirmDialog
      title="Run real pilot pipeline?"
      description="This will write master data and sales pilot into the database."
      confirmText="Run Real Pipeline"
      cancelText="Cancel"
      onConfirm={() => onRunFullPipeline(true)}
      disabled={runPipelineButtonDisabled}
    >
      <Button disabled={runPipelineButtonDisabled} variant="destructive">
        Run Full Safe Pipeline
      </Button>
    </ConfirmDialog>
  )

  return (
    <div className="space-y-6">
      <PageHeader
        title="Migration Center"
        description="Run safe migration, monitor progress, and manage migration data"
        actions={
          <>
            {runFullSafePipelineButton}
            {runVendorMappingButton}
            <Button
              variant="outline"
              disabled={pipelineLoading || !selectedRunId}
              onClick={() => selectedRunId && refreshPipeline(selectedRunId)}
            >
              Refresh Progress
            </Button>
          </>
        }
      />

      {isAdmin === false ? (
        <InlineErrorCallout message="Your account is not an ADMIN. This screen will not work for non-admin users." />
      ) : null}

      {error ? <InlineErrorCallout message={error} /> : null}

      <Card>
        <CardHeader>
          <CardTitle>Current Migration Run</CardTitle>
        </CardHeader>
        <CardContent className="space-y-4">
          {!selectedRun ? (
            <div className="text-sm text-muted-foreground">
              No migration run selected yet. Open <span className="font-medium">Run History</span>{" "}
              to pick a run, or use <span className="font-medium">Advanced Details</span> to
              create one.
            </div>
          ) : (
            <>
              <div className="flex flex-wrap items-center justify-between gap-3">
                <div className="min-w-0">
                  <div className="text-xs text-muted-foreground">Run ID</div>
                  <div className="font-mono text-sm break-all">{selectedRun.id}</div>
                </div>
                <div className="flex flex-wrap items-center gap-2">
                  <Badge variant={statusBadgeVariant}>{currentRunStatus ?? "-"}</Badge>
                  <Badge variant={runTypeBadgeVariant}>{runTypeLabel}</Badge>
                </div>
              </div>

              <Separator />

              <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                <div>
                  <div className="text-xs text-muted-foreground">Current stage</div>
                  <div className="text-sm">{currentRunStage ?? "-"}</div>
                </div>
                <div>
                  <div className="text-xs text-muted-foreground">Progress</div>
                  <div className="text-sm">
                    {totalCount > 0 ? `${completedCount} / ${totalCount} stages` : "-"}
                  </div>
                </div>
                <div>
                  <div className="text-xs text-muted-foreground">Summary</div>
                  <div className="text-sm">{pipelineProgress?.summary ?? "-"}</div>
                </div>
                <div>
                  <div className="text-xs text-muted-foreground">Warnings</div>
                  <div className="text-sm">{pipelineProgress?.warningsCount ?? 0}</div>
                </div>
                <div>
                  <div className="text-xs text-muted-foreground">Errors</div>
                  <div className="text-sm">{pipelineProgress?.errorsCount ?? 0}</div>
                </div>
                <div>
                  <div className="text-xs text-muted-foreground">Started / finished</div>
                  <div className="text-sm">
                    {formatDateTime(pipelineProgress?.startedAt ?? selectedRun.startedAt)} •{" "}
                    {formatDateTime(pipelineProgress?.finishedAt ?? selectedRun.finishedAt ?? null)}
                  </div>
                </div>
              </div>

              <div className="flex flex-wrap gap-2 pt-1">
                {runFullSafePipelineButton}
                <Button
                  variant="outline"
                  disabled={pipelineLoading || !selectedRunId || pipelineProgress?.active}
                  onClick={onResumePipeline}
                >
                  Resume
                </Button>
                {retryRelevant ? (
                  <Button
                    variant="outline"
                    disabled={pipelineLoading || !selectedRunId || pipelineProgress?.active}
                    onClick={onRetryFailedStage}
                  >
                    Retry Failed Stage
                  </Button>
                ) : null}
                {cancelRelevant ? (
                  <Button
                    variant="outline"
                    disabled={pipelineLoading || !selectedRunId}
                    onClick={onCancelPipeline}
                  >
                    Cancel
                  </Button>
                ) : null}
              </div>
            </>
          )}
        </CardContent>
      </Card>

      <Card>
        <CardHeader className="flex flex-row items-center justify-between gap-2">
          <CardTitle>Database Tools</CardTitle>
          <Button variant="outline" onClick={loadDbTables} disabled={dbTablesLoading || !isAdmin}>
            Refresh Tables
          </Button>
        </CardHeader>
        <CardContent className="space-y-4">
          {dbTablesError ? <InlineErrorCallout message={dbTablesError} /> : null}

          <div className="space-y-2">
            <div className="flex flex-wrap items-center justify-between gap-2">
              <div className="text-sm font-medium">Database Backups</div>
              <div className="flex items-center gap-2">
                <Button variant="outline" onClick={loadBackups} disabled={backupsLoading || !isAdmin}>
                  Refresh Backups
                </Button>
                <Button onClick={onCreateBackup} disabled={backupCreating || !isAdmin}>
                  {backupCreating ? "Creating…" : "Create Backup"}
                </Button>
              </div>
            </div>
            {backupsError ? <InlineErrorCallout message={backupsError} /> : null}
            {backupsLoading ? (
              <div className="text-sm text-muted-foreground">Loading…</div>
            ) : backups.length === 0 ? (
              <div className="text-sm text-muted-foreground">No backups yet.</div>
            ) : (
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead className="w-[160px]">Created</TableHead>
                    <TableHead className="w-[110px]">Status</TableHead>
                    <TableHead>File</TableHead>
                    <TableHead className="w-[110px]">Size</TableHead>
                    <TableHead className="w-[160px]">Requested by</TableHead>
                    <TableHead className="w-[120px] text-right">Action</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {backups.map((b) => (
                    <TableRow key={b.id}>
                      <TableCell className="text-xs text-muted-foreground">
                        {formatDateTime(b.createdAt)}
                      </TableCell>
                      <TableCell>
                        <Badge
                          variant={
                            b.status === "FAILED"
                              ? "destructive"
                              : b.status === "RUNNING"
                                ? "secondary"
                                : "default"
                          }
                        >
                          {b.status}
                        </Badge>
                      </TableCell>
                      <TableCell className="text-xs">
                        <div className="font-mono">{b.fileName ?? "-"}</div>
                        {b.errorMessage ? (
                          <div className="text-destructive mt-1">{b.errorMessage}</div>
                        ) : null}
                      </TableCell>
                      <TableCell className="text-xs">{formatBytes(b.fileSize ?? null)}</TableCell>
                      <TableCell className="text-xs text-muted-foreground">
                        {b.requestedBy ?? "-"}
                      </TableCell>
                      <TableCell className="text-right">
                        <Button
                          size="sm"
                          variant="outline"
                          disabled={!b.fileName || b.status !== "SUCCESS"}
                          onClick={async () => {
                            try {
                              await downloadDatabaseBackup(b)
                            } catch (err) {
                              toast({
                                variant: "destructive",
                                title: "Download failed",
                                description:
                                  err instanceof Error ? err.message : "Failed to download backup",
                              })
                            }
                          }}
                        >
                          Download
                        </Button>
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            )}
          </div>

          <div className="space-y-2">
            <div className="text-sm font-medium">Migration Tables</div>
            {dbTablesLoading ? (
              <div className="text-sm text-muted-foreground">Loading…</div>
            ) : dbTables.length === 0 ? (
              <div className="text-sm text-muted-foreground">No migration tables found.</div>
            ) : (
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>Table</TableHead>
                    <TableHead className="text-right">Rows</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {dbTables.map((t) => (
                    <TableRow key={t.table}>
                      <TableCell className="font-mono">{t.table}</TableCell>
                      <TableCell className="text-right">{t.rowCount}</TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            )}
          </div>

          <div className="rounded-lg border border-destructive/30 bg-destructive/5 p-4 space-y-3">
            <div className="space-y-1">
              <div className="text-sm font-medium text-destructive">Danger zone</div>
              <div className="text-xs text-muted-foreground">
                Truncate permanently deletes data from your database. Use only when you are sure.
              </div>
            </div>
            <div className="flex items-center gap-3">
              <input
                id="truncateKeepUsers"
                type="checkbox"
                className="h-4 w-4"
                checked={truncateKeepUsers}
                onChange={(e) => setTruncateKeepUsers(e.target.checked)}
              />
              <Label htmlFor="truncateKeepUsers">Keep users (recommended)</Label>
            </div>
            <div className="grid gap-2">
              <Label htmlFor="truncateConfirm">
                Type <span className="font-mono">{truncateExpected}</span> to enable
              </Label>
              <Input
                id="truncateConfirm"
                value={truncateConfirmText}
                onChange={(e) => setTruncateConfirmText(e.target.value)}
                placeholder={truncateExpected}
              />
            </div>
            <Button
              variant="destructive"
              disabled={!isAdmin || truncateLoading || !truncateEnabled}
              onClick={onTruncateDatabase}
            >
              {truncateLoading ? "Truncating…" : "Truncate Database"}
            </Button>
          </div>
        </CardContent>
      </Card>

      <CollapsibleCard
        title="Logs"
        subtitle="Open only when you need detailed troubleshooting information."
        open={logsOpen}
        onOpenChange={setLogsOpen}
        actions={
          <Button
            variant="outline"
            disabled={loading || !selectedRunId}
            onClick={() => selectedRunId && loadRun(selectedRunId)}
          >
            Refresh Logs
          </Button>
        }
      >
        {!selectedRunId ? (
          <div className="text-sm text-muted-foreground">Select a run to view logs.</div>
        ) : (
          <>
            <div className="flex flex-wrap items-center gap-2">
              <Button
                size="sm"
                variant={logLevelFilter === "ALL" ? "default" : "outline"}
                onClick={() => setLogLevelFilter("ALL")}
              >
                All
              </Button>
              <Button
                size="sm"
                variant={logLevelFilter === "INFO" ? "default" : "outline"}
                onClick={() => setLogLevelFilter("INFO")}
              >
                INFO
              </Button>
              <Button
                size="sm"
                variant={logLevelFilter === "WARN" ? "default" : "outline"}
                onClick={() => setLogLevelFilter("WARN")}
              >
                WARN
              </Button>
              <Button
                size="sm"
                variant={logLevelFilter === "ERROR" ? "default" : "outline"}
                onClick={() => setLogLevelFilter("ERROR")}
              >
                ERROR
              </Button>
              <div className="text-xs text-muted-foreground ml-auto">
                Showing {filteredLogs.length} / {logs.length}
              </div>
            </div>

            {filteredLogs.length === 0 ? (
              <div className="text-sm text-muted-foreground">No logs for this filter.</div>
            ) : (
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead className="w-[140px]">Time</TableHead>
                    <TableHead className="w-[90px]">Level</TableHead>
                    <TableHead className="w-[180px]">Stage</TableHead>
                    <TableHead>Message</TableHead>
                    <TableHead className="w-[140px]">Details</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {filteredLogs.map((l) => (
                    <TableRow key={l.id}>
                      <TableCell className="text-xs text-muted-foreground">
                        {formatDateTime(l.createdAt)}
                      </TableCell>
                      <TableCell>
                        <Badge
                          variant={
                            l.level === "ERROR"
                              ? "destructive"
                              : l.level === "WARN"
                                ? "secondary"
                                : "outline"
                          }
                        >
                          {l.level}
                        </Badge>
                      </TableCell>
                      <TableCell className="text-xs">{l.stageKey ?? "-"}</TableCell>
                      <TableCell className="text-sm">{l.message}</TableCell>
                      <TableCell className="text-sm">
                        {l.details ? (
                          <details className="text-xs">
                            <summary className="cursor-pointer text-muted-foreground hover:text-foreground">
                              View
                            </summary>
                            <pre className="mt-2 whitespace-pre-wrap rounded-lg border border-border bg-muted/20 p-2 text-xs text-muted-foreground">
                              {l.details}
                            </pre>
                          </details>
                        ) : (
                          <span className="text-xs text-muted-foreground">-</span>
                        )}
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            )}
          </>
        )}
      </CollapsibleCard>

      <CollapsibleCard
        title="Run History"
        subtitle="Pick a previous run when needed."
        open={runHistoryOpen}
        onOpenChange={setRunHistoryOpen}
        actions={
          <Button variant="outline" onClick={loadAll} disabled={loading}>
            Refresh
          </Button>
        }
      >
        {runs.length === 0 ? (
          <div className="text-sm text-muted-foreground">No runs yet.</div>
        ) : (
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Run</TableHead>
                <TableHead>Status</TableHead>
                <TableHead>Started</TableHead>
                <TableHead>Type</TableHead>
                <TableHead className="text-right">Action</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {runs
                .slice()
                .sort((a, b) => (a.startedAt < b.startedAt ? 1 : -1))
                .map((r) => (
                  <TableRow key={r.id} className={selectedRunId === r.id ? "bg-muted/20" : ""}>
                    <TableCell className="font-mono text-xs break-all">{r.id}</TableCell>
                    <TableCell>
                      <Badge
                        variant={
                          r.status === "FAILED"
                            ? "destructive"
                            : r.status === "RUNNING"
                              ? "secondary"
                              : r.status === "COMPLETED"
                                ? "default"
                                : "outline"
                        }
                      >
                        {r.status}
                      </Badge>
                    </TableCell>
                    <TableCell className="text-xs text-muted-foreground">
                      {formatDateTime(r.startedAt)}
                    </TableCell>
                    <TableCell>
                      <Badge variant={r.dryRun ? "secondary" : "destructive"}>
                        {r.dryRun ? "Dry-run" : "Real-run"}
                      </Badge>
                    </TableCell>
                    <TableCell className="text-right">
                      <Button
                        size="sm"
                        variant="outline"
                        onClick={() => setSelectedRunId(r.id)}
                        disabled={loading}
                      >
                        Open
                      </Button>
                    </TableCell>
                  </TableRow>
                ))}
            </TableBody>
          </Table>
        )}
      </CollapsibleCard>

      <CollapsibleCard
        title="Advanced Details"
        subtitle="Technical tools and detailed outputs (optional)."
        open={advancedOpen}
        onOpenChange={setAdvancedOpen}
      >
        <div className="space-y-4">
          <div className="grid gap-3">
            <div className="text-sm font-medium">Create Run</div>
            <div className="grid gap-2">
              <Label htmlFor="sourceReference">Source dump path</Label>
              <Input
                id="sourceReference"
                value={sourceReference}
                onChange={(e) => setSourceReference(e.target.value)}
                placeholder="../docs/nexo.sql"
              />
            </div>

            <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
              <div className="grid gap-2">
                <Label htmlFor="sourceIdMin">Source ID min</Label>
                <Input
                  id="sourceIdMin"
                  value={sourceIdMin}
                  onChange={(e) => setSourceIdMin(e.target.value)}
                  placeholder="(optional)"
                />
              </div>
              <div className="grid gap-2">
                <Label htmlFor="sourceIdMax">Source ID max</Label>
                <Input
                  id="sourceIdMax"
                  value={sourceIdMax}
                  onChange={(e) => setSourceIdMax(e.target.value)}
                  placeholder="(optional)"
                />
              </div>
              <div className="grid gap-2">
                <Label htmlFor="limit">Limit</Label>
                <Input
                  id="limit"
                  value={limit}
                  onChange={(e) => setLimit(e.target.value)}
                  placeholder="(optional)"
                />
              </div>
            </div>

            <div className="flex items-center gap-3">
              <input
                id="dryRun"
                type="checkbox"
                className="h-4 w-4"
                checked={dryRun}
                onChange={(e) => setDryRun(e.target.checked)}
              />
              <Label htmlFor="dryRun">Dry-run (no writes)</Label>
            </div>

            <div className="flex flex-wrap gap-2">
              <Button onClick={onCreateRun} disabled={loading}>
                Create Run
              </Button>
              <Button variant="outline" onClick={loadAll} disabled={loading}>
                Refresh
              </Button>
            </div>
          </div>

          <Separator />

          <div className="space-y-2">
            <div className="text-sm font-medium">Manual Stage Execution</div>
            {!selectedRunId ? (
              <div className="text-sm text-muted-foreground">Select a run first.</div>
            ) : (
              <div className="flex flex-wrap gap-2">
                {stages.map((stage) => (
                  <Button
                    key={stage}
                    variant="outline"
                    disabled={loading}
                    onClick={() => onExecuteStage(stage)}
                    title={`Execute ${stage}`}
                  >
                    Execute {stage}
                  </Button>
                ))}
              </div>
            )}
          </div>

          <Separator />

          <div className="space-y-2">
            <div className="text-sm font-medium">Stage-by-stage Results</div>
            {stageExecutions.length === 0 ? (
              <div className="text-sm text-muted-foreground">No stages executed yet.</div>
            ) : (
              <div className="space-y-2">
                {stageExecutions.map((s) => {
                  const statsObj = parseJsonObject(s.statsJson ?? null)
                  const reconciliation = statsObj?.reconciliation ?? null
                  const fallbackUsage = statsObj?.fallbackUsage ?? null
                  const created = statsObj?.created ?? statsObj?.wouldCreate ?? null
                  const updated = statsObj?.updated ?? statsObj?.wouldUpdate ?? null
                  const skipped =
                    (statsObj?.skippedOutOfScope ?? 0) + (statsObj?.skippedOverLimit ?? 0)
                  const warnings = statsObj?.warnings ?? 0
                  const errors = statsObj?.errors ?? 0
                  const vendorsCreatedFromCategories =
                    statsObj?.vendorsCreatedFromCategories ?? statsObj?.vendors?.created ?? null
                  const vendorsLinkedExisting =
                    statsObj?.vendorsLinkedExisting ?? statsObj?.vendors?.linkedExisting ?? null
                  const vendorsAlreadyMapped =
                    statsObj?.vendorsAlreadyMapped ?? statsObj?.vendors?.alreadyMapped ?? null

                  return (
                    <div key={s.id} className="rounded-lg border border-border px-3 py-2 text-sm">
                      <div className="flex items-center justify-between gap-2">
                        <div className="font-medium">{s.stageKey}</div>
                        <div className="text-xs text-muted-foreground">{s.status}</div>
                      </div>
                      <div className="text-xs text-muted-foreground">
                        Started: {formatDateTime(s.startedAt)} • Finished:{" "}
                        {formatDateTime(s.finishedAt ?? null)}
                      </div>
                      <div className="mt-1 text-xs text-muted-foreground">
                        created={created ?? "-"} • updated={updated ?? "-"} • skipped={skipped} •
                        warnings={warnings} • errors={errors}
                      </div>
                      {(s.stageKey === "IMPORT_PARTIES" || s.stageKey === "IMPORT_ITEMS") &&
                      (vendorsCreatedFromCategories !== null ||
                        vendorsLinkedExisting !== null ||
                        vendorsAlreadyMapped !== null) ? (
                        <div className="mt-1 text-xs text-muted-foreground">
                          vendors created={String(vendorsCreatedFromCategories ?? "-")} • linkedExisting=
                          {String(vendorsLinkedExisting ?? "-")} • alreadyMapped=
                          {String(vendorsAlreadyMapped ?? "-")}
                        </div>
                      ) : null}
                      {reconciliation ? (
                        <div className="mt-1 text-xs text-muted-foreground">
                          reconciliation expectedTotal={String(reconciliation.expectedTotal ?? "-")} •
                          paymentsTotal={String(reconciliation.paymentsTotal ?? "-")} • difference=
                          {String(reconciliation.difference ?? "-")} • mismatchedOrders=
                          {String(reconciliation.mismatchedOrders ?? "-")}
                        </div>
                      ) : null}
                      {fallbackUsage ? (
                        <div className="mt-1 text-xs text-muted-foreground">
                          fallback partyUsed={String(fallbackUsage.fallbackPartyUsed ?? 0)} • partyCreated=
                          {String(fallbackUsage.fallbackPartyCreated ?? 0)} • warehouseCreated=
                          {String(fallbackUsage.fallbackWarehouseCreated ?? 0)}
                        </div>
                      ) : null}
                      {s.errorMessage ? (
                        <div className="text-xs text-destructive mt-1">{s.errorMessage}</div>
                      ) : null}
                    </div>
                  )
                })}
              </div>
            )}
          </div>

          <Separator />

          <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
            <Card>
              <CardHeader>
                <CardTitle className="text-base">Source Analysis</CardTitle>
              </CardHeader>
              <CardContent>
                {!selectedRunId ? (
                  <div className="text-sm text-muted-foreground">Select a run.</div>
                ) : !selectedStats ? (
                  <div className="text-sm text-muted-foreground">
                    Execute ANALYZE_SOURCE to see stats.
                  </div>
                ) : (
                  <pre className="text-xs whitespace-pre-wrap rounded-lg border border-border bg-muted/20 p-3 overflow-auto max-h-[420px]">
                    {selectedStats}
                  </pre>
                )}
              </CardContent>
            </Card>

            <Card>
              <CardHeader>
                <CardTitle className="text-base">Technical Details</CardTitle>
              </CardHeader>
              <CardContent>
                {!selectedRunId ? (
                  <div className="text-sm text-muted-foreground">Select a run.</div>
                ) : stageExecutions.length === 0 ? (
                  <div className="text-sm text-muted-foreground">Execute a stage to see stats.</div>
                ) : (
                  <pre className="text-xs whitespace-pre-wrap rounded-lg border border-border bg-muted/20 p-3 overflow-auto max-h-[420px]">
                    {prettyJson(stageExecutions[stageExecutions.length - 1]?.statsJson ?? null) ??
                      "(no stats)"}
                  </pre>
                )}
              </CardContent>
            </Card>
          </div>
        </div>
      </CollapsibleCard>
    </div>
  )
}
