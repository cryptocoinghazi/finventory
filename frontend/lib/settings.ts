import { apiFetch } from "./api"

export type OrganizationProfile = {
  companyName: string
  addressLine1: string
  addressLine2: string
  city: string
  state: string
  pincode: string
  email: string
  phone: string
  gstin: string
  website: string
  logoUrl: string
}

export async function getOrganizationProfile(): Promise<OrganizationProfile> {
  const res = await apiFetch("/api/v1/settings/organization")
  if (!res.ok) throw new Error("Failed to fetch organization profile")
  return res.json()
}

export async function updateOrganizationProfile(data: OrganizationProfile): Promise<OrganizationProfile> {
  const res = await apiFetch("/api/v1/settings/organization", {
    method: "PUT",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(data),
  })
  if (!res.ok) throw new Error("Failed to update organization profile")
  return res.json()
}
