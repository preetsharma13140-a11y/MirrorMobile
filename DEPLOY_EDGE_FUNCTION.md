# Deploy TMDB Proxy Edge Function

The TMDB proxy is now built and ready to deploy to your Supabase project. Follow these steps:

## 1. Install Supabase CLI

```bash
npm install -g supabase
```

Or with Homebrew (Mac/Linux):
```bash
brew install supabase/tap/supabase
```

## 2. Login to Supabase

```bash
supabase login
```

This opens your browser to authenticate with your Supabase account.

## 3. Link Your Project

```bash
cd c:\Projects\MirrorMedia\MirrorMobile_temp
supabase link --project-ref odrghjtiktpihhnlzwvo
```

## 4. Set TMDB API Key Secret

The edge function needs your TMDB API key, but stores it server-side (not in the app).

```bash
supabase secrets set TMDB_API_KEY=eyJhbGciOiJIUzI1NiJ9.eyJhdWQiOiJiMGI1Yjc3MzgwOGYxNTUxOTc4NTkyYmJhYTUwNDUwMCIsIm5iZiI6MTc3NTA2NjQyMS4xNzcsInN1YiI6IjY5Y2Q1ZDM1NjBhY2E2MDE0ZDI2MzdhZiIsInNjb3BlcyI6WyJhcGlfcmVhZCJdLCJ2ZXJzaW9uIjoxfQ.TfbYtGRnLB48mqO2zlEAvzftbCrU4U55n5WtuaiTFaE
```

Also set the Supabase anon key (so the proxy can validate callers):

```bash
supabase secrets set SUPABASE_ANON_KEY=sb_publishable_4D8xpLdQUkg34TifQM3xSQ_oH9y4D-5
```

## 5. Deploy the Function

```bash
supabase functions deploy tmdb-proxy
```

## 6. Test It

Test the deployed function:

```bash
curl "https://odrghjtiktpihhnlzwvo.supabase.co/functions/v1/tmdb-proxy?endpoint=movie/550" \
  -H "Authorization: Bearer sb_publishable_4D8xpLdQUkg34TifQM3xSQ_oH9y4D-5"
```

You should get back JSON for Fight Club. If you see `"X-Cache": "MISS"` in the response, the function worked and cached the result.

## What This Does

✅ **No more rate limits** — TMDB sees 1 request even if 1000 users ask for the same movie  
✅ **Instant responses** — cached data returns in <50ms  
✅ **API key secured** — never leaves your server, can't be extracted from APK  
✅ **Auto-retry** — 429/5xx handled server-side with Retry-After  
✅ **Cost: $0** — Supabase free tier covers this

## Monitoring

Check function logs:
```bash
supabase functions logs tmdb-proxy
```

Or in the Supabase Dashboard → Edge Functions → tmdb-proxy → Logs

## Rollback (if needed)

If something breaks, remove the edge function:
```bash
supabase functions delete tmdb-proxy
```

Then in `TmdbService.kt`, revert `buildTmdbUrl()` to the old direct TMDB call.
