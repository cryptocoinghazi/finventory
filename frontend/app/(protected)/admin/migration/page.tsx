"use client"

import { useCallback, useEffect, useMemo, useRef, useState } from "react"
import { PageHeader } from "@/components/ui/page-header"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { useToast } from "@/components/ui/use-toast"
import { InlineErrorCallout } from "@/components/ui-kit/InlineErrorCallout"
import { ConfirmDialog } from "@/components/ui-kit/ConfirmDialog"
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs"
import { getCurrentUser } from "@/lib/users"
import {
  cancelFullSafePipeline,
  createMigrationRun,
  executeMigrationStage,
  getFullSafePipelineProgress,
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

export default function AdminMigrationPage() {
  const { toast } = useToast()

  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [isAdmin, setIsAdmin] = useState<boolean | null>(null)

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

  const [truncateLoading, setTruncateLoading] = useState(false)
  const [truncateKeepUsers, setTruncateKeepUsers] = useState(true)
  const [truncateConfirmText, setTruncateConfirmText] = useState("")

  const selectedStats = useMemo(() => {
    const analyze = stageExecutions.find((s) => s.stageKey === "ANALYZE_SOURCE")
    return prettyJson(analyze?.statsJson ?? null)
  }, [stageExecutions])

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

  return (
    <div className="space-y-6">
      <PageHeader
        title="Migration"
        description="Admin-only: run a safe staged migration (dry-run first)."
      />

      {isAdmin === false ? (
        <InlineErrorCallout message="Your account is not an ADMIN. This screen will not work for non-admin users." />
      ) : null}

      {error ? <InlineErrorCallout message={error} /> : null}

      <Card>
        <CardHeader>
          <CardTitle>Step-by-step</CardTitle>
        </CardHeader>
        <CardContent className="space-y-2 text-sm text-muted-foreground">
          <div>1) Create a run (dry-run enabled).</div>
          <div>2) Execute ANALYZE_SOURCE and review stats + logs.</div>
          <div>3) When ready, execute FINALIZE to close the run.</div>
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle>Create Run</CardTitle>
        </CardHeader>
        <CardContent className="grid gap-4">
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
        </CardContent>
      </Card>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <Card className="overflow-hidden">
          <CardHeader>
            <CardTitle>Runs</CardTitle>
          </CardHeader>
          <CardContent className="space-y-3">
            {runs.length === 0 ? (
              <div className="text-sm text-muted-foreground">No runs yet.</div>
            ) : (
              <div className="space-y-2">
                {runs
                  .slice()
                  .sort((a, b) => (a.startedAt < b.startedAt ? 1 : -1))
                  .map((r) => (
                    <button
                      key={r.id}
                      type="button"
                      className={
                        "w-full text-left rounded-lg border px-3 py-2 " +
                        (selectedRunId === r.id ? "border-primary" : "border-border")
                      }
                      onClick={() => setSelectedRunId(r.id)}
                    >
                      <div className="flex items-center justify-between gap-2">
                        <div className="font-medium truncate">{r.id}</div>
                        <div className="text-xs text-muted-foreground">
                          {r.status} • {r.dryRun ? "dry-run" : "write"}
                        </div>
                      </div>
                      <div className="text-xs text-muted-foreground truncate">
                        {r.sourceReference}
                      </div>
                      <div className="text-xs text-muted-foreground">
                        Started: {formatDateTime(r.startedAt)}
                      </div>
                    </button>
                  ))}
              </div>
            )}
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle>Selected Run</CardTitle>
          </CardHeader>
          <CardContent className="space-y-4">
            {!selectedRunId ? (
              <div className="text-sm text-muted-foreground">Select a run.</div>
            ) : (
              <>
                <div className="grid gap-1 text-sm">
                  <div>
                    <span className="text-muted-foreground">Run ID: </span>
                    <span className="font-mono">{selectedRunId}</span>
                  </div>
                  <div>
                    <span className="text-muted-foreground">Status: </span>
                    <span>{selectedRun?.status ?? "-"}</span>
                  </div>
                  <div>
                    <span className="text-muted-foreground">Dry-run: </span>
                    <span>{selectedRun?.dryRun ? "Yes" : "No"}</span>
                  </div>
                  <div>
                    <span className="text-muted-foreground">Started: </span>
                    <span>{formatDateTime(selectedRun?.startedAt ?? null)}</span>
                  </div>
                  <div>
                    <span className="text-muted-foreground">Finished: </span>
                    <span>{formatDateTime(selectedRun?.finishedAt ?? null)}</span>
                  </div>
                </div>

                <div className="rounded-lg border border-border p-3">
                  <div className="flex items-center justify-between gap-2">
                    <div className="font-medium">Full Safe Pipeline</div>
                    <div className="text-xs text-muted-foreground">
                      {pipelineProgress?.summary ?? "-"}
                      {pipelineProgress?.active ? " • running" : ""}
                    </div>
                  </div>
                  <div className="mt-2 grid gap-1 text-sm">
                    <div>
                      <span className="text-muted-foreground">Preset: </span>
                      <span>{selectedPipelinePreset ?? "-"}</span>
                    </div>
                    <div>
                      <span className="text-muted-foreground">Current stage: </span>
                      <span>{pipelineProgress?.currentStage ?? "-"}</span>
                    </div>
                    <div>
                      <span className="text-muted-foreground">Completed: </span>
                      <span>
                        {pipelineProgress?.completedStages?.length ?? 0}/
                        {pipelineProgress?.plannedStages?.length ?? 0}
                      </span>
                    </div>
                    <div>
                      <span className="text-muted-foreground">Warnings: </span>
                      <span>{pipelineProgress?.warningsCount ?? 0}</span>
                      <span className="text-muted-foreground"> • Errors: </span>
                      <span>{pipelineProgress?.errorsCount ?? 0}</span>
                    </div>
                    {pipelineProgress?.failedStage ? (
                      <div className="text-destructive">Failed: {pipelineProgress.failedStage}</div>
                    ) : null}
                  </div>
                  <div className="mt-3 flex flex-wrap gap-2">
                    {selectedRun?.dryRun ? (
                      <Button
                        onClick={() => onRunFullPipeline(false)}
                        disabled={runPipelineButtonDisabled}
                      >
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
                    )}
                    <Button
                      variant="outline"
                      disabled={pipelineLoading || !selectedRunId || pipelineProgress?.active}
                      onClick={onResumePipeline}
                    >
                      Resume
                    </Button>
                    <Button
                      variant="outline"
                      disabled={
                        pipelineLoading ||
                        !selectedRunId ||
                        pipelineProgress?.active ||
                        !pipelineProgress?.failedStage
                      }
                      onClick={onRetryFailedStage}
                    >
                      Retry Failed Stage
                    </Button>
                    <Button
                      variant="outline"
                      disabled={pipelineLoading || !selectedRunId || !pipelineProgress?.active}
                      onClick={onCancelPipeline}
                    >
                      Cancel
                    </Button>
                    <Button
                      variant="outline"
                      disabled={pipelineLoading || !selectedRunId}
                      onClick={() => selectedRunId && refreshPipeline(selectedRunId)}
                    >
                      Refresh Progress
                    </Button>
                  </div>
                </div>

                <div className="flex flex-wrap gap-2">
                  {stages.map((stage) => {
                    return (
                      <Button
                        key={stage}
                        variant={stage === "ANALYZE_SOURCE" ? "default" : "outline"}
                        disabled={loading}
                        onClick={() => onExecuteStage(stage)}
                        title={`Execute ${stage}`}
                      >
                        Execute {stage}
                      </Button>
                    )
                  })}
                </div>

                <Tabs defaultValue="results">
                  <TabsList>
                    <TabsTrigger value="results">Results</TabsTrigger>
                    <TabsTrigger value="logs">Logs</TabsTrigger>
                  </TabsList>
                  <TabsContent value="results" className="space-y-2">
                    <div className="text-sm font-medium">Stage Results</div>
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

                          return (
                            <div
                              key={s.id}
                              className="rounded-lg border border-border px-3 py-2 text-sm"
                            >
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
                  </TabsContent>
                  <TabsContent value="logs" className="space-y-3">
                    <div className="flex gap-2">
                      <Button
                        variant="outline"
                        disabled={loading || !selectedRunId}
                        onClick={() => selectedRunId && loadRun(selectedRunId)}
                      >
                        Refresh Logs
                      </Button>
                    </div>
                    {logs.length === 0 ? (
                      <div className="text-sm text-muted-foreground">No logs yet.</div>
                    ) : (
                      <div className="space-y-2 max-h-[420px] overflow-auto">
                        {logs.map((l) => (
                          <div
                            key={l.id}
                            className="rounded-lg border border-border px-3 py-2 text-sm"
                          >
                            <div className="flex items-center justify-between gap-2">
                              <div className="font-medium">
                                {l.level}
                                {l.stageKey ? ` • ${l.stageKey}` : ""}
                              </div>
                              <div className="text-xs text-muted-foreground">
                                {formatDateTime(l.createdAt)}
                              </div>
                            </div>
                            <div className="text-sm">{l.message}</div>
                            {l.details ? (
                              <pre className="text-xs whitespace-pre-wrap text-muted-foreground mt-1">
                                {l.details}
                              </pre>
                            ) : null}
                          </div>
                        ))}
                      </div>
                    )}
                  </TabsContent>
                </Tabs>
              </>
            )}
          </CardContent>
        </Card>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <Card>
          <CardHeader>
            <CardTitle>ANALYZE_SOURCE Output</CardTitle>
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
            <CardTitle>Most Recent Stage JSON</CardTitle>
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

      <Card>
        <CardHeader className="flex flex-row items-center justify-between gap-2">
          <CardTitle>Database</CardTitle>
          <Button variant="outline" onClick={loadDbTables} disabled={dbTablesLoading || !isAdmin}>
            Refresh Tables
          </Button>
        </CardHeader>
        <CardContent className="space-y-4">
          {dbTablesError ? <InlineErrorCallout message={dbTablesError} /> : null}

          <div className="space-y-2">
            <div className="text-sm font-medium">Migration Tables</div>
            {dbTablesLoading ? (
              <div className="text-sm text-muted-foreground">Loading…</div>
            ) : dbTables.length === 0 ? (
              <div className="text-sm text-muted-foreground">No migration tables found.</div>
            ) : (
              <div className="rounded-lg border border-border overflow-hidden">
                <div className="grid grid-cols-2 gap-0 border-b border-border bg-muted/20 text-xs font-medium">
                  <div className="px-3 py-2">Table</div>
                  <div className="px-3 py-2">Rows</div>
                </div>
                {dbTables.map((t) => (
                  <div key={t.table} className="grid grid-cols-2 gap-0 border-b border-border text-sm">
                    <div className="px-3 py-2 font-mono">{t.table}</div>
                    <div className="px-3 py-2">{t.rowCount}</div>
                  </div>
                ))}
              </div>
            )}
          </div>

          <div className="space-y-3">
            <div className="text-sm font-medium text-destructive">Truncate All Data</div>
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
    </div>
  )
}
