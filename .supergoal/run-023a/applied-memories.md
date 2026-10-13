# Applied memories

- **dual_build_system** — after any change in `src/`, verify `mvn package -DskipTests` AND `cd src && ant compile` both pass. The MCP service code must be Java 21-compatible and buildable under both systems.
- **gx_server_branch_source** — the geneXplain `biouml2_based` branch at /home/zha/projects/java/genexplain runs on this BioUML codebase; the MCP service should be designed to be usable from that deployment context too.
- **gxp_dev branch topology** (context) — user works across divergent branches; work in a dedicated `mcp` branch as requested.
