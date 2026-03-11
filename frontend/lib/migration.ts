import { apiFetch } from "./api"

export type MigrationRunStatus = "CREATED" | "RUNNING" | "FAILED" | "COMPLETED" | "CANCELLED"

export interface MigrationRun {
  id: string
  sourceSystem: string
  sourceReference: string
  dryRun: boolean
  status: MigrationRunStatus
  startedAt: string
  finishedAt?: string | null
  requestedBy?: string | null
  scopeSourceIdMin?: number | null
  scopeSourceIdMax?: number | null
  scopeLimit?: number | null
}

export type MigrationStageStatus = "CREATED" | "RUNNING" | "FAILED" | "COMPLETED"

export interface MigrationStageExecution {
  id: string
  runId: string
  stageKey: string
  status: MigrationStageStatus
  startedAt: string
  finishedAt?: string | null
  statsJson?: string | null
  errorMessage?: string | null
}

export type MigrationLogLevel = "INFO" | "WARN" | "ERROR"

export interface MigrationLogEntry {
  id: string
  runId: string
  stageKey?: string | null
  level: MigrationLogLevel
  message: string
  details?: string | null
  createdAt: string
}

export type MigrationPipelinePreset = "DRY_RUN_FULL_SAFE" | "REAL_PILOT_FULL_SAFE"

export interface MigrationPipelineProgress {
  runId: string
  runStatus: MigrationRunStatus
  preset: MigrationPipelinePreset
  active: boolean
  startedAt?: string | null
  finishedAt?: string | null
  currentStage?: string | null
  plannedStages: string[]
  completedStages: string[]
  failedStage?: string | null
  warningsCount: number
  errorsCount: number
  summary?: string | null
}

export interface MigrationDbTable {
  table: string
  rowCount: number
}

export interface TruncateDatabaseRequest {
  keepUsers?: boolean
  confirmText: string
}

export interface TruncateDatabaseResponse {
  truncatedTables: string[]
  keepUsers: boolean
}

export interface CreateMigrationRunRequest {
  sourceSystem?: string
  sourceReference?: string
  dryRun?: boolean
  sourceIdMin?: number
  sourceIdMax?: number
  limit?: number
}

export async function listMigrationStages(): Promise<string[]> {
  const res = await apiFetch("/api/v1/admin/migration/stages")
  if (!res.ok) throw new Error("Failed to fetch migration stages")
  return res.json()
}

export async function listMigrationRuns(): Promise<MigrationRun[]> {
  const res = await apiFetch("/api/v1/admin/migration/runs")
  if (!res.ok) throw new Error("Failed to fetch migration runs")
  return res.json()
}

export async function createMigrationRun(
  req: CreateMigrationRunRequest
): Promise<MigrationRun> {
  const res = await apiFetch("/api/v1/admin/migration/runs", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(req),
  })
  if (!res.ok) {
    const text = await res.text()
    throw new Error(text || "Failed to create migration run")
  }
  return res.json()
}

export async function getMigrationRun(id: string): Promise<MigrationRun> {
  const res = await apiFetch(`/api/v1/admin/migration/runs/${id}`)
  if (!res.ok) throw new Error("Failed to fetch migration run")
  return res.json()
}

export async function listMigrationStagesForRun(
  id: string
): Promise<MigrationStageExecution[]> {
  const res = await apiFetch(`/api/v1/admin/migration/runs/${id}/stages`)
  if (!res.ok) throw new Error("Failed to fetch migration stages for run")
  return res.json()
}

export async function listMigrationLogsForRun(
  id: string,
  limit: number = 200
): Promise<MigrationLogEntry[]> {
  const res = await apiFetch(`/api/v1/admin/migration/runs/${id}/logs?limit=${limit}`)
  if (!res.ok) throw new Error("Failed to fetch migration logs")
  return res.json()
}

export async function executeMigrationStage(
  runId: string,
  stageKey: string
): Promise<MigrationStageExecution> {
  const res = await apiFetch(
    `/api/v1/admin/migration/runs/${runId}/stages/${encodeURIComponent(stageKey)}/execute`,
    {
      method: "POST",
    }
  )
  if (!res.ok) {
    const text = await res.text()
    throw new Error(text || "Failed to execute migration stage")
  }
  return res.json()
}

export async function startFullSafePipeline(
  runId: string,
  preset?: MigrationPipelinePreset,
  confirmed?: boolean
): Promise<MigrationPipelineProgress> {
  const res = await apiFetch(`/api/v1/admin/migration/runs/${runId}/pipeline/full-safe/start`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ preset, confirmed }),
  })
  if (!res.ok) {
    const text = await res.text()
    throw new Error(text || "Failed to start pipeline")
  }
  return res.json()
}

export async function getFullSafePipelineProgress(
  runId: string,
  preset?: MigrationPipelinePreset
): Promise<MigrationPipelineProgress> {
  const query = preset ? `?preset=${encodeURIComponent(preset)}` : ""
  const res = await apiFetch(`/api/v1/admin/migration/runs/${runId}/pipeline/full-safe/progress${query}`)
  if (!res.ok) throw new Error("Failed to fetch pipeline progress")
  return res.json()
}

export async function resumeFullSafePipeline(
  runId: string,
  preset?: MigrationPipelinePreset
): Promise<MigrationPipelineProgress> {
  const query = preset ? `?preset=${encodeURIComponent(preset)}` : ""
  const res = await apiFetch(`/api/v1/admin/migration/runs/${runId}/pipeline/full-safe/resume${query}`, {
    method: "POST",
  })
  if (!res.ok) {
    const text = await res.text()
    throw new Error(text || "Failed to resume pipeline")
  }
  return res.json()
}

export async function retryFullSafeStage(
  runId: string,
  stageKey: string,
  preset?: MigrationPipelinePreset
): Promise<MigrationPipelineProgress> {
  const query = preset ? `?preset=${encodeURIComponent(preset)}` : ""
  const res = await apiFetch(
    `/api/v1/admin/migration/runs/${runId}/pipeline/full-safe/retry/${encodeURIComponent(
      stageKey
    )}${query}`,
    { method: "POST" }
  )
  if (!res.ok) {
    const text = await res.text()
    throw new Error(text || "Failed to retry stage")
  }
  return res.json()
}

export async function cancelFullSafePipeline(runId: string): Promise<MigrationPipelineProgress> {
  const res = await apiFetch(`/api/v1/admin/migration/runs/${runId}/pipeline/full-safe/cancel`, {
    method: "POST",
  })
  if (!res.ok) {
    const text = await res.text()
    throw new Error(text || "Failed to cancel pipeline")
  }
  return res.json()
}

export async function listMigrationDbTables(): Promise<MigrationDbTable[]> {
  const res = await apiFetch("/api/v1/admin/migration/db/migration-tables")
  if (!res.ok) throw new Error("Failed to fetch migration tables")
  return res.json()
}

export async function truncateDatabase(
  req: TruncateDatabaseRequest
): Promise<TruncateDatabaseResponse> {
  const res = await apiFetch("/api/v1/admin/migration/db/truncate", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(req),
  })
  if (!res.ok) {
    const text = await res.text()
    throw new Error(text || "Failed to truncate database")
  }
  return res.json()
}
