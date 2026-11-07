export interface Env {
  TMDB_API_KEY: string;
}

const TMDB_BASE_URL = "https://api.themoviedb.org/3";

const MAX_PAGE = 500;
const MAX_QUERY_LENGTH = 100;

type ValueCheck = (value: string) => boolean;

interface Endpoint {
  path: RegExp;
  parameters: Map<string, ValueCheck>;
}

function isPage(value: string): boolean {
  if (!/^[1-9]\d{0,2}$/.test(value)) {
    return false;
  }
  return Number(value) <= MAX_PAGE;
}

function isGenreId(value: string): boolean {
  return /^[1-9]\d{0,8}$/.test(value);
}

function isSearchQuery(value: string): boolean {
  if (value.trim() === "") {
    return false;
  }
  return value.length <= MAX_QUERY_LENGTH;
}

function isPopularityDescending(value: string): boolean {
  return value === "popularity.desc";
}

function isCredits(value: string): boolean {
  return value === "credits";
}

// Paths the proxy forwards, relative to the TMDB base URL, with the query parameters each one accepts.
const ENDPOINTS: Endpoint[] = [
  {
    path: /^\/search\/movie$/,
    parameters: new Map([
      ["query", isSearchQuery],
      ["page", isPage],
    ]),
  },
  {
    path: /^\/trending\/movie\/week$/,
    parameters: new Map([["page", isPage]]),
  },
  {
    path: /^\/movie\/[1-9]\d{0,8}\/recommendations$/,
    parameters: new Map([["page", isPage]]),
  },
  {
    path: /^\/discover\/movie$/,
    parameters: new Map([
      ["sort_by", isPopularityDescending],
      ["with_genres", isGenreId],
      ["page", isPage],
    ]),
  },
  {
    path: /^\/movie\/[1-9]\d{0,8}$/,
    parameters: new Map([["append_to_response", isCredits]]),
  },
];

function findEndpoint(path: string): Endpoint | null {
  for (const endpoint of ENDPOINTS) {
    if (endpoint.path.test(path)) {
      return endpoint;
    }
  }
  return null;
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
  const endpoint = findEndpoint(incomingUrl.pathname);
  if (endpoint === null) {
    return errorResponse(404, "This path is not available through the proxy.");
  }

  const upstreamUrl = new URL(TMDB_BASE_URL + incomingUrl.pathname);
  for (const [name, value] of incomingUrl.searchParams) {
    const check = endpoint.parameters.get(name);
    if (check === undefined) {
      return errorResponse(400, `Query parameter "${name}" is not allowed on this path.`);
    }
    if (upstreamUrl.searchParams.has(name)) {
      return errorResponse(400, `Query parameter "${name}" is given more than once.`);
    }
    if (!check(value)) {
      return errorResponse(400, `Query parameter "${name}" has a value that is not allowed.`);
    }
    upstreamUrl.searchParams.set(name, value);
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
