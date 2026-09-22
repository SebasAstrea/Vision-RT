# 🤖 AGENTS.md — Mandatory instructions for AI agents

> **If you are an AI agent (opencode, Claude Code, Cursor, Cline, etc.) and you
> are about to modify, create, or analyze any file in this repository, read
> this document BEFORE doing anything.**

This project has a **local RAG service** that provides curated context about the
project. **Using it is mandatory** — it saves many tokens and makes answers more
precise.

- **Source of truth:** `docs/REQUIREMENTS.md`, `docs/ARCHITECTURE.md`,
  `docs/AGENTS.md` (full operational methodology), `docs/ROADMAP.md`.
- **RAG service:** `http://localhost:8767` (VisionRT gets its own containers
  `rag-visionrt-ollama` / `rag-visionrt-rag`; ports 8765/8766 are used by other
  projects). Collection: `visionrt_chunks`, project: `VisionRT`.

---

## 1. Check that the RAG service is up

```bash
curl -s http://localhost:8767/health | head -50
```

If it responds, continue. If not, start it (Docker is the primary path for this
project):

```bash
cd <repo-root>
docker compose -f docker-compose.rag.yml up -d      # Ollama + RAG en :8767
# Manual fallback (without Docker):
#   pip install -r rag/requirements.txt
#   python -m rag.server &                          # sirve en :8765
# Espera ~2s a que arranque, luego indexa:
curl -X POST 'http://localhost:8767/reindex'
# y comprueba que cuadra (drift debe ser 0):
curl -s http://localhost:8767/stats | python -m json.tool
```

The first indexing takes 1-5 minutes (depends on chunk size and whether Ollama
has `bge-m3` already in memory). ~332 chunks indexed for this repo's docs.

> **Do not use `?force=true` by default.** It re-embeds the whole project and on
> CPU can take **more than an hour**. Incremental reindex suffices 99% of the
> time: it detects changes by mtime **and** reconciles the manifest against
> Chroma. Reserve `force` for when a normal reindex leaves `drift != 0`.

---

## 2. Before ANY modification, call these endpoints

### 2.1. Read the project "soul" (always)

```bash
curl -s http://localhost:8767/methodology | python -m json.tool | head -120
```

Returns the chunks marked as **methodology**: purpose, architecture, structure,
conventions and contribution guide (from `docs/*.md` and the files listed in
`rag.config.json → methodology.entire_files`). **Do not modify anything without
reading it at least once per session.**

### 2.2. If the project declares categories, request their cards

```bash
curl -s http://localhost:8767/categories | python -m json.tool     # which categories
curl -s http://localhost:8767/category/<id> | python -m json.tool  # one category
```

This project has **no categories** (graph is disabled: it's docs-only, no CSS
themes/modules). If `/categories` returns an empty list, search directly with
`/query`. If future modules add categories (e.g. after M0/Android modules), see
`rag.config.json → graph` and `rag/README.md`.

### 2.3. If you need to find a pattern or understand "how X is done"

```bash
curl -s -X POST http://localhost:8767/query \
  -H 'Content-Type: application/json' \
  -d '{"q": "latency budget for inference and end-to-end alert", "k": 5}'
```

Useful parameters:
- `category`: filter by category (`"category": "<id>"`)
- `file_glob`: filter by file pattern (substring) (`"file_glob": "docs/architecture"`)
- `include_methodology`: `true` by default; injects project context
- `k`: 1-20 chunks (default 5)

### 2.4. To understand graph relationships

```bash
curl -s http://localhost:8767/related/category/<id> | python -m json.tool
curl -s http://localhost:8767/related/file/docs/ARCHITECTURE.md | python -m json.tool
curl -s http://localhost:8767/related/token/<token> | python -m json.tool
```

`/related/{kind}/<id>` with `kind ∈ {category, file, token}` returns the
neighbors at 1 hop.

---

## 3. Behavior rules

1. **Read whole files only when strictly necessary.** If the RAG gives you the
   relevant chunk, read only that chunk + its adjacent context, not the whole
   file.

2. **After modifying a file, re-index (incremental):**
   ```bash
   curl -X POST 'http://localhost:8767/reindex'
   curl -s http://localhost:8767/stats | python -m json.tool   # drift must be 0
   ```
   The indexer detects changes by mtime and re-embeds only what is needed.

   - **One reindex at a time.** If one is already running the endpoint
     responds `409`; check `/stats → reindex` (`running`, `status`,
     `started_at`) and wait. Do not fire another "just in case".
   - **Check `drift`.** In `/stats`, `drift = chroma_chunks - manifest_chunks`
     must be `0`. If not, some declared chunks are missing from Chroma: an
     incremental reindex (no `force`) self-heals via reconciliation.
   - If `/stats → reindex.status` is `"error"`, the pass failed (e.g. Ollama
     down). The index stays **intact** and is retried on the next pass.
   - Do not track progress by `chroma_chunks` mid-`force=true`: during embedding
     it may look like 0 chunks. Use `/stats → reindex.running`.

3. **If you add a new category** (themes, modules, etc.):
   - Declare its detection in `rag.config.json → graph` (block/mention regexes;
     see `rag/README.md`).
   - `curl -X POST 'http://localhost:8767/reindex'` (incremental; no `force`).

4. **If you add a new token** that must be in the graph: the indexer picks it
   up automatically on re-index.

5. **If you modify the README, AGENTS.md or docs/*.md:** sections with
   `methodology=true` are injected automatically into every `/query`. Detection
   is configurable in `rag.config.json → methodology` (`entire_files` for full
   documents, `heading_patterns` for specific sections; if empty it infers).

---

## 4. Quick reference endpoints

| Method | Endpoint | Use |
|---|---|---|
| `GET`  | `/health` | Ollama + collection + graph status |
| `GET`  | `/methodology` | "Soul" chunks (docs + entire_files) |
| `GET`  | `/categories` | Category list with 1-line summary (if any) |
| `GET`  | `/category/{id}` | Full card of a category (tokens + files + chunks) |
| `POST` | `/query` | Semantic search `{q, k, category?, file_glob?}` |
| `GET`  | `/related/{kind}/{id}` | Graph neighbors (`kind ∈ {category, file, token}`) |
| `POST` | `/reindex` | Reindex (incremental + reconciliation; `409` if running) |
| `GET`  | `/stats` | Statistics + `drift` + reindex state |

---

## 5. RAG configuration for this project

Behavior is declared in **`rag.config.json`** (project root). It is
auto-generated on first boot with sensible values and regenerates/repairs
itself. Edit it when you need to:

- `collection` / `project_name` — Chroma collection and project name (currently
  `visionrt_chunks` / `VisionRT`)
- `index_globs` / `exclude_dirs` / `exclude_files` — what is indexed and what is
  not (Kotlin globs `.kt`/`.kts` are already included; `rag/` is excluded)
- `chunkers` — ext → chunker (css, js, py, md, yaml, toml, raw…)
- `methodology` — `entire_files` and `heading_patterns` ("soul" of the project)
- `graph` — category/token regexes and summary file (disabled for now)
- `chunk_sizes` / `chunk_overlap` / `embed_text_max_chars` — budgets

Do not just delete it: regenerate from `rag/config.py` (delete `rag.config.json`
and restart the server) or edit by hand; it reloads when the server restarts
(`docker compose -f docker-compose.rag.yml restart rag`).

Environment variables that override config at runtime:

```bash
export OLLAMA_HOST=http://localhost:11434    # Ollama container URL
export RAG_EMBED_MODEL=bge-m3                # embedding model
export RAG_EMBED_DIM=1024                    # vector dim (bge-m3 default)
export RAG_HOST=0.0.0.0
export RAG_PORT=8765
export RAG_PROJECT=VisionRT                  # override project_name
export RAG_COLLECTION=visionrt_chunks        # override collection
```

If your Ollama is on another host, adjust `OLLAMA_HOST` and restart the RAG.

---

## 6. Why this file exists

- **Before**: a new agent spent ~80-150k input tokens just reading README +
  source files to understand context.
- **After**: with 4-5 HTTP calls (~3-5k tokens of response) it has the same
  knowledge + the project "soul" injected automatically.
- **Bonus**: the graph answers "which files make up category X" or "which
  categories define this token" without parsing it yourself.

If you find ways to improve the RAG, edit `rag/README.md` and this file.