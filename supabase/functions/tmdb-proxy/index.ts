// TMDB Proxy Edge Function
// Routes all TMDB API requests through Supabase to:
// 1. Cache responses (reduce TMDB API calls by 80%+)
// 2. Keep API key secure (never exposed in APK)
// 3. Add rate-limit handling with proper back-off
// 4. Validate callers using the Supabase anon key

import { serve } from "https://deno.land/std@0.168.0/http/server.ts"

const TMDB_API_KEY = Deno.env.get('TMDB_API_KEY')!
// SUPABASE_ANON_KEY is injected automatically by Supabase into every edge function
const SUPABASE_ANON_KEY = Deno.env.get('SUPABASE_ANON_KEY') ?? Deno.env.get('ANON_KEY') ?? ''
const TMDB_BASE_URL = 'https://api.themoviedb.org/3'

// In-memory cache (persists for lifetime of isolate — typically hours)
const cache = new Map<string, { data: unknown; timestamp: number }>()
const CACHE_TTL_MS = 1000 * 60 * 60 * 24 // 24 hours

const corsHeaders = {
  'Access-Control-Allow-Origin': '*',
  'Access-Control-Allow-Methods': 'GET, OPTIONS',
  'Access-Control-Allow-Headers': 'Content-Type, Authorization',
}

serve(async (req) => {
  if (req.method === 'OPTIONS') {
    return new Response(null, { headers: corsHeaders })
  }

  // Verify the caller is your app (must send the Supabase anon key)
  const authHeader = req.headers.get('Authorization') ?? ''
  const token = authHeader.replace('Bearer ', '').trim()
  if (token !== SUPABASE_ANON_KEY) {
    return new Response(
      JSON.stringify({ error: 'Unauthorized' }),
      { status: 401, headers: { ...corsHeaders, 'Content-Type': 'application/json' } }
    )
  }

  try {
    const url = new URL(req.url)
    const endpoint = url.searchParams.get('endpoint')

    if (!endpoint) {
      return new Response(
        JSON.stringify({ error: 'Missing endpoint parameter' }),
        { status: 400, headers: { ...corsHeaders, 'Content-Type': 'application/json' } }
      )
    }

    // Build cache key from endpoint + all query params except endpoint itself
    const queryParams = new URLSearchParams(url.search)
    queryParams.delete('endpoint')
    const cacheKey = `${endpoint}?${queryParams.toString()}`

    // Check in-memory cache first
    const cached = cache.get(cacheKey)
    if (cached && (Date.now() - cached.timestamp) < CACHE_TTL_MS) {
      console.log(`Cache HIT: ${cacheKey}`)
      return new Response(
        JSON.stringify(cached.data),
        {
          status: 200,
          headers: { ...corsHeaders, 'Content-Type': 'application/json', 'X-Cache': 'HIT' }
        }
      )
    }

    console.log(`Cache MISS — fetching from TMDB: ${cacheKey}`)

    // Build TMDB URL — key stays server-side
    const tmdbUrl = new URL(`${TMDB_BASE_URL}/${endpoint}`)
    queryParams.forEach((value, key) => {
      tmdbUrl.searchParams.set(key, value)
    })

    const isJWT = TMDB_API_KEY.startsWith('eyJ')
    const tmdbHeaders: Record<string, string> = { 'Accept': 'application/json' }
    if (isJWT) {
      tmdbHeaders['Authorization'] = `Bearer ${TMDB_API_KEY}`
    } else {
      tmdbUrl.searchParams.set('api_key', TMDB_API_KEY)
    }

    // Retry loop: immediate → 1s → 3s
    const retryDelays = [0, 1000, 3000]
    let lastError: Error | null = null

    for (let i = 0; i < retryDelays.length; i++) {
      if (retryDelays[i] > 0) {
        await new Promise(r => setTimeout(r, retryDelays[i]))
      }

      try {
        const response = await fetch(tmdbUrl.toString(), { headers: tmdbHeaders })

        if (response.status === 429) {
          const retryAfter = response.headers.get('Retry-After')
          const waitMs = retryAfter ? parseInt(retryAfter) * 1000 : 2000
          console.warn(`TMDB rate limited, waiting ${waitMs}ms`)
          await new Promise(r => setTimeout(r, waitMs))
          continue
        }

        if (response.status >= 500 && i < retryDelays.length - 1) {
          console.warn(`TMDB server error ${response.status}, retrying...`)
          continue
        }

        if (!response.ok) {
          return new Response(
            JSON.stringify({ error: `TMDB error ${response.status}` }),
            { status: response.status, headers: { ...corsHeaders, 'Content-Type': 'application/json' } }
          )
        }

        const data = await response.json()

        // Cache successful response
        cache.set(cacheKey, { data, timestamp: Date.now() })

        // Simple LRU: drop oldest entry when cache exceeds 2000 items
        if (cache.size > 2000) {
          const oldestKey = cache.keys().next().value
          if (oldestKey) cache.delete(oldestKey)
        }

        return new Response(
          JSON.stringify(data),
          {
            status: 200,
            headers: { ...corsHeaders, 'Content-Type': 'application/json', 'X-Cache': 'MISS' }
          }
        )
      } catch (err) {
        lastError = err as Error
        console.error(`Attempt ${i + 1} failed:`, err)
      }
    }

    throw lastError ?? new Error('All retries failed')
  } catch (err) {
    console.error('Edge function error:', err)
    return new Response(
      JSON.stringify({ error: (err as Error).message }),
      { status: 500, headers: { ...corsHeaders, 'Content-Type': 'application/json' } }
    )
  }
})

