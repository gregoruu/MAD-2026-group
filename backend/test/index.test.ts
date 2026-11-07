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
