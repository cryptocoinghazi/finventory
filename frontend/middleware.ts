import { NextRequest, NextResponse } from "next/server"

const PUBLIC_PATHS = ["/login", "/_next", "/favicon.ico", "/public"]

export function middleware(req: NextRequest) {
  const { pathname } = req.nextUrl
  const isPublic = PUBLIC_PATHS.some((p) => pathname.startsWith(p))
  if (isPublic) return NextResponse.next()

  const token = req.cookies.get("token")?.value
  const protectedPrefixes = ["/dashboard", "/masters", "/sales", "/purchase", "/reports", "/admin", "/settings"]
  const isProtected = protectedPrefixes.some((p) => pathname.startsWith(p))

  if (isProtected && !token) {
    const loginUrl = new URL("/login", req.url)
    return NextResponse.redirect(loginUrl)
  }
  return NextResponse.next()
}

export const config = {
  matcher: ["/((?!api).*)"],
}

