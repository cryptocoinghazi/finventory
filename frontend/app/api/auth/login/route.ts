export async function POST(req: Request) {
  const { username, password } = await req.json()
  const configured = process.env.NEXT_PUBLIC_API_URL
  const api =
    configured && configured.trim()
      ? configured.trim()
      : process.env.NODE_ENV === "production"
        ? new URL(req.url).origin
        : "http://localhost:8080"
  const res = await fetch(`${api}/api/v1/auth/login`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ username, password }),
  })
  const text = await res.text()
  return new Response(text, {
    status: res.status,
    headers: { "Content-Type": res.headers.get("Content-Type") ?? "application/json" },
  })
}
