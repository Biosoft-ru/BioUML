# Transport decision (2026-09-10, phase 1 spike)

## Chosen: MCP Java SDK `io.modelcontextprotocol.sdk:mcp:0.11.2` — hybrid usage

- SDK **resolves** from the configured Nexus (verified `mvn dependency:get`, jar in `~/.m2`).
- Java 17+ API; we run Java 21. ✓
- Deps (all additive, no version conflicts in repo): `jackson-databind 2.17.0` (repo only has `jackson-core 2.11.2` — no databind), `reactor-core 3.7.0`, `slf4j-api 2.0.16` (repo pins 1.7.25 — **excluded**, use repo's 1.7.25), `json-schema-validator 1.5.7`, `jakarta.servlet-api` (provided).
- **The `mcp-json-jackson2` / `mcp-json-jackson3` JSON-mapper modules do NOT exist for 0.11.2** (404 on Central; 404 via configured Nexus). Consequence: the SDK cannot serialize protocol messages by itself — its `McpServerTransportProviderBase` receives/produces typed `McpSchema` objects and expects the transport layer to do JSON.
- **Decision:**
  1. **Server side = SDK.** `McpServer.sync(provider).serverInfo(...).capabilities(...).tools(...).build()` → `McpSyncServer`. Tool callbacks (`SyncToolSpecification.Builder.callHandler`) route to our support classes. initialize/tools-list/tool-registration/protocol-version negotiation come from the SDK for free.
  2. **Transport side = in-repo bridge.** A custom `McpServerTransportProvider` (`McpServletTransportProvider` in `biouml.plugins.mcp.web`) implements the 4-method interface (`setSessionFactory`, `notifyClients`, `closeGracefully`, `protocolVersions`) + `McpServerSession$Factory` (using the public `McpServerSession` constructor). Message (de)serialization uses `jackson-databind` (the SDK's own declared dep) with plain-JavaBean DTOs mirroring the MCP JSON-RPC wire format (`jsonrpc/id/method/params` + MCP result types: InitializeResult, ToolsListResult, CallToolResult...). The `McpServerSession` constructor's `McpRequestHandler`/`McpNotificationHandler` maps are built from `McpSyncServer`'s tool specs (tools/call → CallToolResult via our callHandler; tools/list → ToolsListResult).
  3. **HTTP side (phase 5):** `McpServlet` (jakarta.servlet or javax, matched to Tomcat) — POST JSON-RPC, optional SSE for streaming; GET 405; auth via BioUML session → 401.
  4. **In-process (phase 5):** `McpInProcessTransport` implements the same provider interface over in-memory queues — same `McpSyncServer` instance shape, no HTTP.
- **Fallback (NOT triggered):** hand-rolled JSON-RPC if the SDK proved unusable — the session-constructor approach above keeps that fallback small (the bridge IS the JSON layer).

## Spike evidence
- `McpServer.sync(McpServerTransportProvider)` → `SingleSessionSyncSpecification` (javap-verified).
- `McpServerFeatures.SyncToolSpecification.Builder().tool(new McpSchema.Tool(name, desc, inputSchemaString)).callHandler((ex, req) -> new CallToolResult(List.of(new TextContent(json)), false))` (javap-verified).
- `StdioServerTransportProvider(ObjectMapper)` exists → spike test round-trips initialize/tools/list/tools/call over in-memory stdio (ObjectMapper provided explicitly, so the missing json-mapper module is irrelevant for stdio).
- `McpServerSession(String, Duration, McpServerTransport, McpInitRequestHandler, Map<String,McpRequestHandler<?>>, Map<String,McpNotificationHandler>)` public → custom-transport bridge is constructible.

## Risks noted for phase 5
- `McpInitRequestHandler`/`McpRequestHandler`/`McpNotificationHandler` interface signatures must be javap-verified when building the bridge (expected: functional-style, return `Mono`).
- reactor `Mono` blocking calls (`.block()`) on the servlet thread are acceptable (request-scoped) but must be bounded (timeout).
- slf4j 2.x vs 1.7.25: SDK calls slf4j-api 2.0.16 classes? If it uses 2.x-only APIs (e.g. `org.slf4j.spi.LoggingEvent`), exclude-and-downgrade will break at runtime — verify in phase 5 with a live servlet request; mitigation: add `slf4j-1.7.25` + `slf4j-jdk14` bridge, or bump slf4j to 2.0.16 repo-wide if compatible.
