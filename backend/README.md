# Movara TMDB proxy

A Cloudflare Worker that forwards the app's TMDB requests and adds the API key to them. The key is stored as a Cloudflare secret, so no build of the app contains it.

There are two environments:

| Environment | Worker | Used by |
| --- | --- | --- |
| `staging` | `movara-tmdb-proxy-staging` | `debug` and `staging` builds |
| `production` | `movara-tmdb-proxy-production` | `release` builds |

## What the proxy forwards

Only `GET` requests for the paths the app uses are forwarded to `https://api.themoviedb.org/3`. Each path accepts only its own query parameters, each at most once, and only with the values the app sends:

| Path | Query parameters |
| --- | --- |
| `/search/movie` | `query`: not blank, at most 100 characters; `page` |
| `/trending/movie/week` | `page` |
| `/movie/{id}/recommendations` | `page` |
| `/discover/movie` | `sort_by`: `popularity.desc`, `vote_count.desc` or `vote_average.desc`; optional `with_genres`: one genre id; `vote_average.gte`: integer 0–10; `vote_count.gte`: integer 0–2147483647; `primary_release_date.lte`: valid `YYYY-MM-DD` date; `page` |
| `/movie/{id}` | `append_to_response`: only `credits` |

`page` is a whole number from 1 to 500. `{id}` and a genre id are whole numbers of 1 to 9 digits without a leading zero.

| Request | Answer |
| --- | --- |
| Allowed path and parameters | TMDB's status, body and `Content-Type` |
| Any other path | `404` |
| Any method other than `GET` | `405` |
| A query parameter the path does not take (including `api_key`), a value outside its rule, or a parameter given twice | `400` |
| The Worker has no `TMDB_API_KEY` secret | `500` |
| TMDB cannot be reached | `502` |

When the app starts using a new endpoint, parameter or value, add it to `ENDPOINTS` in `src/index.ts` together with a test, and update the tables above. The search field in the app has to keep its input within the 100-character limit.

## 1. Install

You need Node.js 22. From the repository root:

```bash
cd backend
npm install
```

## 2. Run the tests

```bash
npm test
npm run typecheck
```

The tests replace `fetch` with a mock, so they never call TMDB and need no key.

## 3. Run the Worker locally

Copy `.dev.vars.example` to `.dev.vars` and put your own TMDB key in it:

```
TMDB_API_KEY=your_key_here
```

`.dev.vars` is listed in `backend/.gitignore`. **Do not remove that line and do not force-add the file.**

```bash
npm run dev
```

The Worker answers on `http://localhost:8787`, for example `http://localhost:8787/trending/movie/week?page=1`.

## 4. Deploy

Deploying needs access to the project's Cloudflare account. Log in once:

```bash
npx wrangler login
```

Then deploy one environment at a time:

```bash
npm run deploy:staging
npm run deploy:production
```

Plain `npx wrangler deploy` without `--env` creates a third Worker called `movara-tmdb-proxy` that nothing uses, so always go through the scripts above.

## 5. Set the API key

Each environment reads its own `TMDB_API_KEY` secret. Use the key of the project's TMDB account, never a personal one.

The simplest way is the Cloudflare dashboard: **Workers & Pages** → the Worker → **Settings** → **Variables and Secrets** → **Add**, type **Secret**, name `TMDB_API_KEY`.

From a terminal, run the command and type the key when it asks for it:

```bash
npx wrangler secret put TMDB_API_KEY --env staging
npx wrangler secret put TMDB_API_KEY --env production
```

Do not pass the key as an argument or pipe it in with `echo`, because it then ends up in your shell history.

## 6. If the key leaks

1. Sign in to the project's TMDB account and regenerate the key at `https://www.themoviedb.org/settings/api`. The old key stops working immediately.
2. Set the new key as the `TMDB_API_KEY` secret in both environments (section 5).

The app needs no update, because no build contains the key.

## Limits

The Workers Free plan allows 100,000 requests per day for the whole Cloudflare account. Past that, Cloudflare answers with error 1027 until 00:00 UTC. Going over the limit does not create a bill; the app just cannot load new TMDB data until the limit resets.
