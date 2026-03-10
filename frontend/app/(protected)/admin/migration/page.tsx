"use client"

import { useCallback, useEffect, useMemo, useState } from "react"
import { PageHeader } from "@/components/ui/page-header"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { useToast } from "@/components/ui/use-toast"
import { InlineErrorCallout } from "@/components/ui-kit/InlineErrorCallout"
import { getCurrentUser } from "@/lib/users"
import {
  createMigrationRun,
  executeMigrationStage,
  listMigrationLogsForRun,
  listMigrationRuns,
  listMigrationStages,
  listMigrationStagesForRun,
  MigrationLogEntry,
  MigrationRun,
  MigrationStageExecution,
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

  const selectedStats = useMemo(() => {
    const analyze = stageExecutions.find((s) => s.stageKey === "ANALYZE_SOURCE")
    return prettyJson(analyze?.statsJson ?? null)
  }, [stageExecutions])

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
    if (!selectedRunId) return
    loadRun(selectedRunId)
  }, [selectedRunId])

  function parseOptionalNumber(value: string) {
    const trimmed = value.trim()
    if (!trimmed) return null
    const n = Number(trimmed)
    if (!Number.isFinite(n) || n < 0) return null
    return n
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

                <div className="flex flex-wrap gap-2">
                  {stages.map((stage) => {
                    const disabled = stage === "IMPORT_ITEMS"
                    return (
                      <Button
                        key={stage}
                        variant={stage === "ANALYZE_SOURCE" ? "default" : "outline"}
                        disabled={loading || disabled}
                        onClick={() => onExecuteStage(stage)}
                        title={disabled ? "Not implemented yet" : `Execute ${stage}`}
                      >
                        Execute {stage}
                      </Button>
                    )
                  })}
                </div>

                <div className="space-y-2">
                  <div className="text-sm font-medium">Stages</div>
                  {stageExecutions.length === 0 ? (
                    <div className="text-sm text-muted-foreground">
                      No stages executed yet.
                    </div>
                  ) : (
                    <div className="space-y-2">
                      {stageExecutions.map((s) => (
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
                          {s.errorMessage ? (
                            <div className="text-xs text-destructive mt-1">{s.errorMessage}</div>
                          ) : null}
                        </div>
                      ))}
                    </div>
                  )}
                </div>
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
            <CardTitle>Logs</CardTitle>
          </CardHeader>
          <CardContent className="space-y-3">
            <div className="flex gap-2">
              <Button
                variant="outline"
                disabled={loading || !selectedRunId}
                onClick={() => selectedRunId && loadRun(selectedRunId)}
              >
                Refresh Logs
              </Button>
            </div>
            {!selectedRunId ? (
              <div className="text-sm text-muted-foreground">Select a run.</div>
            ) : logs.length === 0 ? (
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
          </CardContent>
        </Card>
      </div>
    </div>
  )
}
