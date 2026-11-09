import { afterEach, describe, expect, it, vi } from "vitest";
import worker, { proxyRequest } from "../src/index";

const API_KEY = "test-key";
const PROXY_URL = "https://proxy.example";

// Replaces fetch with a mock that answers with the given response.
function mockTmdb(response: Response) {
  const fetchMock = vi.fn<typeof fetch>().mockResolvedValue(response);
  vi.stubGlobal("fetch", fetchMock);
  return fetchMock;
}

// Returns the URL of the first request the proxy sent to TMDB.
function forwardedUrl(fetchMock: ReturnType<typeof mockTmdb>): URL {
  return new URL(fetchMock.mock.calls[0][0] as string);
}

afterEach(() => {
  vi.unstubAllGlobals();
});

describe("proxyRequest", () => {
  it("forwards every request the app makes", async () => {
    const paths = [
      "/search/movie?query=alien&page=1",
      "/trending/movie/week?page=1",
      "/movie/550/recommendations?page=2",
      "/discover/movie?sort_by=popularity.desc&with_genres=28&page=1",
      "/movie/550?append_to_response=credits",
    ];
    for (const path of paths) {
      const fetchMock = mockTmdb(Response.json({}));
      const response = await proxyRequest(new Request(PROXY_URL + path), API_KEY);
      expect(response.status, path).toBe(200);
      expect(fetchMock, path).toHaveBeenCalledTimes(1);
    }
  });

  it("adds the API key and keeps the query parameters", async () => {
    const fetchMock = mockTmdb(Response.json({}));
    await proxyRequest(new Request(PROXY_URL + "/search/movie?query=alien&page=2"), API_KEY);

    const url = forwardedUrl(fetchMock);
    expect(url.origin + url.pathname).toBe("https://api.themoviedb.org/3/search/movie");
    expect(url.searchParams.get("query")).toBe("alien");
    expect(url.searchParams.get("page")).toBe("2");
    expect(url.searchParams.get("api_key")).toBe(API_KEY);
  });

  it("rejects paths the app does not use", async () => {
    const paths = [
      "/",
      "/account",
      "/authentication/token/new",
      "/movie/550/account_states",
      "/movie/abc",
      "/search/tv",
      "/3/search/movie",
    ];
    for (const path of paths) {
      const fetchMock = mockTmdb(Response.json({}));
      const response = await proxyRequest(new Request(PROXY_URL + path), API_KEY);
      expect(response.status, path).toBe(404);
      expect(fetchMock, path).not.toHaveBeenCalled();
    }
  });

  it("rejects methods other than GET", async () => {
    const methods = ["POST", "PUT", "DELETE", "PATCH"];
    for (const method of methods) {
      const fetchMock = mockTmdb(Response.json({}));
      const request = new Request(PROXY_URL + "/search/movie?query=alien", { method: method });
      const response = await proxyRequest(request, API_KEY);
      expect(response.status, method).toBe(405);
      expect(response.headers.get("Allow"), method).toBe("GET");
      expect(fetchMock, method).not.toHaveBeenCalled();
    }
  });

  it("rejects an api_key sent by the client", async () => {
    const fetchMock = mockTmdb(Response.json({}));
    const request = new Request(PROXY_URL + "/search/movie?query=alien&api_key=someone-elses-key");
    const response = await proxyRequest(request, API_KEY);
    expect(response.status).toBe(400);
    expect(fetchMock).not.toHaveBeenCalled();
  });

  it("rejects query parameters the app does not send", async () => {
    const fetchMock = mockTmdb(Response.json({}));
    const request = new Request(PROXY_URL + "/movie/550?session_id=abc");
    const response = await proxyRequest(request, API_KEY);
    expect(response.status).toBe(400);
    expect(fetchMock).not.toHaveBeenCalled();
  });

  it("rejects a parameter on a path that does not take it", async () => {
    const paths = [
      "/search/movie?query=alien&append_to_response=credits",
      "/trending/movie/week?query=alien",
      "/movie/550/recommendations?with_genres=28",
      "/discover/movie?query=alien",
      "/movie/550?page=1",
    ];
    for (const path of paths) {
      const fetchMock = mockTmdb(Response.json({}));
      const response = await proxyRequest(new Request(PROXY_URL + path), API_KEY);
      expect(response.status, path).toBe(400);
      expect(fetchMock, path).not.toHaveBeenCalled();
    }
  });

  it("rejects values the app does not send", async () => {
    const tooLongQuery = "a".repeat(101);
    const paths = [
      "/movie/550?append_to_response=credits,videos",
      "/movie/550?append_to_response=videos",
      "/discover/movie?sort_by=release_date.desc",
      "/discover/movie?with_genres=28,12",
      "/discover/movie?with_genres=abc",
      "/trending/movie/week?page=0",
      "/trending/movie/week?page=501",
      "/trending/movie/week?page=-1",
      "/trending/movie/week?page=1.5",
      "/trending/movie/week?page=",
      "/search/movie?query=",
      "/search/movie?query=%20%20",
      "/search/movie?query=" + tooLongQuery,
    ];
    for (const path of paths) {
      const fetchMock = mockTmdb(Response.json({}));
      const response = await proxyRequest(new Request(PROXY_URL + path), API_KEY);
      expect(response.status, path).toBe(400);
      expect(fetchMock, path).not.toHaveBeenCalled();
    }
  });

  it("forwards discovery sorting and filters with and without a genre", async () => {
    for (const sort of ["popularity.desc", "vote_count.desc", "vote_average.desc"]) {
      for (const genre of ["", "&with_genres=28"]) {
        const fetchMock = mockTmdb(Response.json({}));
        const path = "/discover/movie?sort_by=" + sort + genre +
          "&vote_average.gte=6&vote_count.gte=100&primary_release_date.lte=2026-11-09&page=2";
        const response = await proxyRequest(new Request(PROXY_URL + path), API_KEY);
        expect(response.status).toBe(200);
        const url = forwardedUrl(fetchMock);
        expect(url.searchParams.get("sort_by")).toBe(sort);
        expect(url.searchParams.get("vote_average.gte")).toBe("6");
        expect(url.searchParams.get("vote_count.gte")).toBe("100");
        expect(url.searchParams.get("primary_release_date.lte")).toBe("2026-11-09");
        expect(url.searchParams.get("with_genres")).toBe(genre ? "28" : null);
      }
    }
  });

  it("rejects malformed discovery filters before making an upstream request", async () => {
    const filters = [
      "vote_average.gte=-1", "vote_average.gte=11", "vote_average.gte=6.5",
      "vote_count.gte=-1", "vote_count.gte=1.5", "vote_count.gte=2147483648",
      "vote_count.gte=NaN", "vote_count.gte=1e3",
      "primary_release_date.lte=2026-02-30", "primary_release_date.lte=2026-02-29",
      "primary_release_date.lte=2026-1-01", "primary_release_date.lte=not-a-date",
      "vote_average.gte=6&vote_average.gte=7",
    ];
    for (const filter of filters) {
      const fetchMock = mockTmdb(Response.json({}));
      const response = await proxyRequest(new Request(PROXY_URL + "/discover/movie?" + filter), API_KEY);
      expect(response.status, filter).toBe(400);
      expect(fetchMock, filter).not.toHaveBeenCalled();
    }
  });

  it("accepts valid boundary values and leap dates", async () => {
    for (const filter of [
      "vote_average.gte=0", "vote_average.gte=10",
      "vote_count.gte=0", "vote_count.gte=2147483647",
      "primary_release_date.lte=2024-02-29",
    ]) {
      const fetchMock = mockTmdb(Response.json({}));
      const response = await proxyRequest(new Request(PROXY_URL + "/discover/movie?" + filter), API_KEY);
      expect(response.status, filter).toBe(200);
    }
  });

  it("accepts the largest page and the longest query", async () => {
    const longestQuery = "a".repeat(100);
    const paths = ["/trending/movie/week?page=500", "/search/movie?query=" + longestQuery];
    for (const path of paths) {
      const fetchMock = mockTmdb(Response.json({}));
      const response = await proxyRequest(new Request(PROXY_URL + path), API_KEY);
      expect(response.status, path).toBe(200);
      expect(fetchMock, path).toHaveBeenCalledTimes(1);
    }
  });

  it("rejects a parameter given more than once", async () => {
    const fetchMock = mockTmdb(Response.json({}));
    const request = new Request(PROXY_URL + "/trending/movie/week?page=1&page=2");
    const response = await proxyRequest(request, API_KEY);
    expect(response.status).toBe(400);
    expect(fetchMock).not.toHaveBeenCalled();
  });

  it("rejects parameter names that every object inherits", async () => {
    const names = ["constructor", "toString", "hasOwnProperty", "__proto__"];
    for (const name of names) {
      const fetchMock = mockTmdb(Response.json({}));
      const response = await proxyRequest(new Request(PROXY_URL + "/movie/550?" + name + "=x"), API_KEY);
      expect(response.status, name).toBe(400);
      expect(fetchMock, name).not.toHaveBeenCalled();
    }
  });

  it("rejects movie ids the app does not send", async () => {
    const paths = ["/movie/0", "/movie/0550", "/movie/1234567890", "/movie/1234567890/recommendations"];
    for (const path of paths) {
      const fetchMock = mockTmdb(Response.json({}));
      const response = await proxyRequest(new Request(PROXY_URL + path), API_KEY);
      expect(response.status, path).toBe(404);
      expect(fetchMock, path).not.toHaveBeenCalled();
    }
  });

  it("answers 500 without calling TMDB when the key is missing", async () => {
    const missingKeys = [undefined, ""];
    for (const missingKey of missingKeys) {
      const fetchMock = mockTmdb(Response.json({}));
      const response = await proxyRequest(new Request(PROXY_URL + "/trending/movie/week"), missingKey);
      expect(response.status).toBe(500);
      expect(fetchMock).not.toHaveBeenCalled();
    }
  });

  it("passes the TMDB status and body through", async () => {
    const tmdbBody = { status_code: 34, status_message: "The resource you requested could not be found." };
    mockTmdb(Response.json(tmdbBody, { status: 404 }));
    const response = await proxyRequest(new Request(PROXY_URL + "/movie/999999999"), API_KEY);
    expect(response.status).toBe(404);
    expect(await response.json()).toEqual(tmdbBody);
  });

  it("passes back only the content type header", async () => {
    const tmdbResponse = new Response("{}", {
      headers: { "Content-Type": "application/json;charset=utf-8", Server: "openresty" },
    });
    mockTmdb(tmdbResponse);
    const response = await proxyRequest(new Request(PROXY_URL + "/trending/movie/week"), API_KEY);
    expect(response.headers.get("Content-Type")).toBe("application/json;charset=utf-8");
    expect(response.headers.get("Server")).toBeNull();
  });

  it("answers 502 when TMDB cannot be reached", async () => {
    const fetchMock = vi.fn<typeof fetch>().mockRejectedValue(new TypeError("network error"));
    vi.stubGlobal("fetch", fetchMock);
    const response = await proxyRequest(new Request(PROXY_URL + "/trending/movie/week"), API_KEY);
    expect(response.status).toBe(502);
  });
});

describe("worker", () => {
  it("reads the API key from the environment", async () => {
    const fetchMock = mockTmdb(Response.json({}));
    await worker.fetch(new Request(PROXY_URL + "/trending/movie/week"), { TMDB_API_KEY: API_KEY });
    expect(forwardedUrl(fetchMock).searchParams.get("api_key")).toBe(API_KEY);
  });
});
