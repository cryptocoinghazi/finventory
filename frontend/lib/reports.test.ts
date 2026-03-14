import { describe, expect, it } from "vitest"
import { filterPartyOutstandingRows, getPartyOutstandingStatus, PartyOutstanding } from "./reports"

describe("party outstanding status filtering", () => {
  const rows: PartyOutstanding[] = [
    {
      partyId: "p1",
      partyName: "Alice Traders",
      partyType: "CUSTOMER",
      phone: "9999999999",
      gstin: "27ABCDE1234F1Z5",
      totalReceivable: 100,
      totalPayable: 0,
      netBalance: 100,
      age0to30: 100,
      age31to60: 0,
      age61to90: 0,
      age90Plus: 0,
    },
    {
      partyId: "p2",
      partyName: "Bob Suppliers",
      partyType: "VENDOR",
      phone: "8888888888",
      gstin: "29ABCDE1234F1Z5",
      totalReceivable: 0,
      totalPayable: 250,
      netBalance: -250,
      age0to30: 0,
      age31to60: -250,
      age61to90: 0,
      age90Plus: 0,
    },
    {
      partyId: "p3",
      partyName: "Charlie Wholesale",
      partyType: "CUSTOMER",
      phone: "7777777777",
      gstin: null,
      totalReceivable: 40,
      totalPayable: 0,
      netBalance: 40,
      age0to30: 10,
      age31to60: 30,
      age61to90: 0,
      age90Plus: 0,
    },
  ]

  it("detects unpaid vs pending based on ageing buckets", () => {
    expect(getPartyOutstandingStatus(rows[0])).toBe("PENDING")
    expect(getPartyOutstandingStatus(rows[1])).toBe("UNPAID")
    expect(getPartyOutstandingStatus(rows[2])).toBe("UNPAID")
  })

  it("unpaid filter shows only unpaid records", () => {
    const out = filterPartyOutstandingRows(rows, { status: "UNPAID" })
    expect(out.map((r) => r.partyId).sort()).toEqual(["p2", "p3"])
  })

  it("pending filter shows only pending records", () => {
    const out = filterPartyOutstandingRows(rows, { status: "PENDING" })
    expect(out.map((r) => r.partyId)).toEqual(["p1"])
  })

  it("combined filters (search + status) work correctly", () => {
    const out1 = filterPartyOutstandingRows(rows, { query: "alice", status: "UNPAID" })
    expect(out1).toHaveLength(0)

    const out2 = filterPartyOutstandingRows(rows, { query: "bob", status: "UNPAID" })
    expect(out2.map((r) => r.partyId)).toEqual(["p2"])

    const out3 = filterPartyOutstandingRows(rows, { query: "29abcde", status: "UNPAID" })
    expect(out3.map((r) => r.partyId)).toEqual(["p2"])
  })
})
