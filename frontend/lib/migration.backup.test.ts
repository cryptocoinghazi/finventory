import { beforeEach, describe, expect, it, vi } from "vitest"

vi.mock("./api", () => ({
  apiFetch: vi.fn(),
}))

import { apiFetch } from "./api"
import { createDatabaseBackup, listDatabaseBackups } from "./migration"

describe("database backup API", () => {
  beforeEach(() => {
    vi.resetAllMocks()
  })

  it("lists backups", async () => {
    const payload = [{ id: "1", status: "SUCCESS", createdAt: "2026-01-01T00:00:00Z" }]
    ;(apiFetch as unknown as ReturnType<typeof vi.fn>).mockResolvedValue({
      ok: true,
      json: async () => payload,
    })

    const res = await listDatabaseBackups()
    expect(apiFetch).toHaveBeenCalledWith("/api/v1/admin/migration/backups")
    expect(res).toEqual(payload)
  })

  it("creates backup", async () => {
    const payload = { id: "1", status: "SUCCESS", createdAt: "2026-01-01T00:00:00Z" }
    ;(apiFetch as unknown as ReturnType<typeof vi.fn>).mockResolvedValue({
      ok: true,
      json: async () => payload,
    })

    const res = await createDatabaseBackup()
    expect(apiFetch).toHaveBeenCalledWith("/api/v1/admin/migration/backups", { method: "POST" })
    expect(res).toEqual(payload)
  })
})
