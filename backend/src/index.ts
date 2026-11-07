export interface Env {
  TMDB_API_KEY: string;
}

const TMDB_BASE_URL = "https://api.themoviedb.org/3";

// Paths the proxy forwards, relative to the TMDB base URL.
const ALLOWED_PATHS: RegExp[] = [
  /^\/search\/movie$/,
  /^\/trending\/movie\/week$/,
  /^\/movie\/\d+\/recommendations$/,
  /^\/discover\/movie$/,
  /^\/movie\/\d+$/,
];

// Query parameters the proxy forwards.
const ALLOWED_QUERY_PARAMETERS: string[] = [
  "query",
  "page",
  "with_genres",
  "sort_by",
  "append_to_response",
];

function isAllowedPath(path: string): boolean {
  for (const pattern of ALLOWED_PATHS) {
    if (pattern.test(path)) {
      return true;
    }
  }
  return false;
}

function errorResponse(status: number, message: string): Response {
  return Response.json({ status_message: message }, { status: status });
}

// Forwards an allowed GET request to TMDB with the API key added.
export async function proxyRequest(request: Request, apiKey: string | undefined): Promise<Response> {
  if (!apiKey) {
    return errorResponse(500, "The proxy is not configured.");
  }

  if (request.method !== "GET") {
    const response = errorResponse(405, "Only GET requests are allowed.");
    response.headers.set("Allow", "GET");
    return response;
  }

  const incomingUrl = new URL(request.url);
  if (!isAllowedPath(incomingUrl.pathname)) {
    return errorResponse(404, "This path is not available through the proxy.");
  }

  const upstreamUrl = new URL(TMDB_BASE_URL + incomingUrl.pathname);
  for (const [name, value] of incomingUrl.searchParams) {
    if (!ALLOWED_QUERY_PARAMETERS.includes(name)) {
      return errorResponse(400, `Query parameter "${name}" is not allowed.`);
    }
    upstreamUrl.searchParams.append(name, value);
  }
  upstreamUrl.searchParams.set("api_key", apiKey);

  let upstreamResponse: Response;
  try {
    upstreamResponse = await fetch(upstreamUrl.toString(), {
      headers: { Accept: "application/json" },
    });
  } catch {
    return errorResponse(502, "TMDB could not be reached.");
  }

  // Only the status, body and content type are passed back.
  const headers = new Headers();
  const contentType = upstreamResponse.headers.get("Content-Type");
  if (contentType !== null) {
    headers.set("Content-Type", contentType);
  }
  return new Response(upstreamResponse.body, {
    status: upstreamResponse.status,
    headers: headers,
  });
}

export default {
  async fetch(request: Request, env: Env): Promise<Response> {
    return proxyRequest(request, env.TMDB_API_KEY);
  },
} satisfies ExportedHandler<Env>;
