# Decisions

One file per decision that was not obvious and that somebody might otherwise undo by
accident. Each one records what we chose, what we rejected and why, so the reasoning
survives after the conversation that produced it.

Keep each decision in its own numbered file. `docs/architecture.md` describes how the
app is built and links here for the decisions behind it; writing them in separate files
keeps two people working on different decisions out of each other's way.

| Decision | Subject |
| --- | --- |
| [0001](0001-tmdb-api-key-in-the-client.md) | The TMDB API key lives in the client, and builds are not published publicly |
