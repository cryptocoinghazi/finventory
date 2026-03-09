"use client"

import { useEffect, useState } from "react"
import { useForm } from "react-hook-form"
import { PageHeader } from "@/components/ui/page-header"
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Input } from "@/components/ui/input"
import { Button } from "@/components/ui/button"
import { Label } from "@/components/ui/label"
import { getCurrentUser, updatePassword, User } from "@/lib/users"
import { getOrganizationProfile, updateOrganizationProfile, OrganizationProfile } from "@/lib/settings"
import { InlineErrorCallout } from "@/components/ui-kit/InlineErrorCallout"
import { useToast } from "@/components/ui/use-toast"
import { Loader2 } from "lucide-react"

export default function SettingsPage() {
  const [role, setRole] = useState<string | null>(null)

  useEffect(() => {
    setRole(window.localStorage.getItem("role"))
  }, [])

  return (
    <div className="space-y-6">
      <PageHeader
        title="Settings"
        description="Manage your profile and application settings"
      />

      <Tabs defaultValue="profile" className="space-y-4">
        <TabsList>
          <TabsTrigger value="profile">Profile</TabsTrigger>
          {role === "ADMIN" && <TabsTrigger value="organization">Organization</TabsTrigger>}
        </TabsList>
        <TabsContent value="profile">
          <ProfileTab />
        </TabsContent>
        {role === "ADMIN" && (
          <TabsContent value="organization">
            <OrganizationTab />
          </TabsContent>
        )}
      </Tabs>
    </div>
  )
}

function ProfileTab() {
  const [user, setUser] = useState<User | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [success, setSuccess] = useState<string | null>(null)

  const { register, handleSubmit, reset, formState: { errors, isSubmitting } } = useForm({
    defaultValues: {
      currentPassword: "",
      newPassword: "",
      confirmPassword: ""
    }
  })

  useEffect(() => {
    getCurrentUser()
      .then(setUser)
      .catch((err) => setError("Failed to load profile"))
      .finally(() => setLoading(false))
  }, [])

  const onSubmit = async (data: any) => {
    setError(null)
    setSuccess(null)
    
    if (data.newPassword !== data.confirmPassword) {
      setError("Passwords do not match")
      return
    }

    try {
      await updatePassword({
        currentPassword: data.currentPassword,
        newPassword: data.newPassword,
        confirmPassword: data.confirmPassword
      })
      setSuccess("Password updated successfully")
      reset()
    } catch (err: any) {
      setError(err.message)
    }
  }

  if (loading) return <div>Loading...</div>

  return (
    <div className="grid gap-6">
      <Card>
        <CardHeader>
          <CardTitle>My Profile</CardTitle>
          <CardDescription>View your account details</CardDescription>
        </CardHeader>
        <CardContent className="space-y-4">
          <div className="grid gap-2">
            <Label>Username</Label>
            <Input value={user?.username || ""} disabled />
          </div>
          <div className="grid gap-2">
            <Label>Email</Label>
            <Input value={user?.email || ""} disabled />
          </div>
          <div className="grid gap-2">
            <Label>Role</Label>
            <Input value={user?.role || ""} disabled />
          </div>
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle>Change Password</CardTitle>
          <CardDescription>Update your password to keep your account secure</CardDescription>
        </CardHeader>
        <CardContent>
          <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
            {error && <InlineErrorCallout error={error} />}
            {success && <div className="text-sm text-green-600 font-medium">{success}</div>}
            
            <div className="grid gap-2">
              <Label htmlFor="currentPassword">Current Password</Label>
              <Input
                id="currentPassword"
                type="password"
                {...register("currentPassword", { required: "Current password is required" })}
              />
              {errors.currentPassword && <p className="text-sm text-red-500">{errors.currentPassword.message as string}</p>}
            </div>

            <div className="grid gap-2">
              <Label htmlFor="newPassword">New Password</Label>
              <Input
                id="newPassword"
                type="password"
                {...register("newPassword", { required: "New password is required", minLength: { value: 6, message: "Password must be at least 6 characters" } })}
              />
              {errors.newPassword && <p className="text-sm text-red-500">{errors.newPassword.message as string}</p>}
            </div>

            <div className="grid gap-2">
              <Label htmlFor="confirmPassword">Confirm New Password</Label>
              <Input
                id="confirmPassword"
                type="password"
                {...register("confirmPassword", { required: "Confirm password is required" })}
              />
              {errors.confirmPassword && <p className="text-sm text-red-500">{errors.confirmPassword.message as string}</p>}
            </div>

            <Button type="submit" disabled={isSubmitting}>
              {isSubmitting && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
              Update Password
            </Button>
          </form>
        </CardContent>
      </Card>
    </div>
  )
}

function OrganizationTab() {
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [success, setSuccess] = useState<string | null>(null)

  const { register, handleSubmit, reset, formState: { errors, isSubmitting } } = useForm<OrganizationProfile>()

  useEffect(() => {
    getOrganizationProfile()
      .then((data) => {
        reset(data)
        setLoading(false)
      })
      .catch((err) => {
        setError("Failed to load organization profile")
        setLoading(false)
      })
  }, [reset])

  const onSubmit = async (data: OrganizationProfile) => {
    setError(null)
    setSuccess(null)
    try {
      await updateOrganizationProfile(data)
      setSuccess("Organization profile updated successfully")
    } catch (err: any) {
      setError(err.message)
    }
  }

  if (loading) return <div>Loading...</div>

  return (
    <Card>
      <CardHeader>
        <CardTitle>Organization Profile</CardTitle>
        <CardDescription>Manage your company details for invoices and reports</CardDescription>
      </CardHeader>
      <CardContent>
        <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
          {error && <InlineErrorCallout error={error} />}
          {success && <div className="text-sm text-green-600 font-medium">{success}</div>}

          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div className="grid gap-2">
              <Label htmlFor="companyName">Company Name</Label>
              <Input id="companyName" {...register("companyName", { required: "Company name is required" })} />
              {errors.companyName && <p className="text-sm text-red-500">{errors.companyName.message}</p>}
            </div>
            
            <div className="grid gap-2">
              <Label htmlFor="gstin">GSTIN</Label>
              <Input id="gstin" {...register("gstin")} />
            </div>

            <div className="grid gap-2">
              <Label htmlFor="email">Email</Label>
              <Input id="email" type="email" {...register("email")} />
            </div>

            <div className="grid gap-2">
              <Label htmlFor="phone">Phone</Label>
              <Input id="phone" {...register("phone")} />
            </div>

            <div className="grid gap-2">
              <Label htmlFor="website">Website</Label>
              <Input id="website" {...register("website")} />
            </div>
            
             <div className="grid gap-2">
              <Label htmlFor="logoUrl">Logo URL</Label>
              <Input id="logoUrl" {...register("logoUrl")} />
            </div>
          </div>

          <div className="space-y-4 pt-4">
             <h3 className="text-sm font-medium">Address</h3>
             <div className="grid gap-2">
              <Label htmlFor="addressLine1">Address Line 1</Label>
              <Input id="addressLine1" {...register("addressLine1")} />
            </div>
            <div className="grid gap-2">
              <Label htmlFor="addressLine2">Address Line 2</Label>
              <Input id="addressLine2" {...register("addressLine2")} />
            </div>
            <div className="grid grid-cols-3 gap-4">
                <div className="grid gap-2">
                  <Label htmlFor="city">City</Label>
                  <Input id="city" {...register("city")} />
                </div>
                <div className="grid gap-2">
                  <Label htmlFor="state">State</Label>
                  <Input id="state" {...register("state")} />
                </div>
                <div className="grid gap-2">
                  <Label htmlFor="pincode">Pincode</Label>
                  <Input id="pincode" {...register("pincode")} />
                </div>
            </div>
          </div>

          <Button type="submit" disabled={isSubmitting} className="mt-4">
            {isSubmitting && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
            Save Changes
          </Button>
        </form>
      </CardContent>
    </Card>
  )
}
