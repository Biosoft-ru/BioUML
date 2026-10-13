# Repo map

_Generated 2026-09-10 09:34:35_

## Top-level layout
- AGENTS.md
- analyses
- attic
- biouml2test-nightly.log
- BioUML.ico
- biouml.lcf
- biouml.png
- biouml.policy
- build
- CLAUDE.md
- configuration
- data
- data_resources
- docker-compose.yaml
- dumps
- install_all_old_jars.bat
- install_all_old_jars.sh
- installer(IzPack)
- install.ini
- jpackage
- libsrc
- LICENSE
- logging.properties
- META-INF
- out
- plugconfig
- plugins
- pom.xml
- preferences_server.xml
- preferences.xml
- README.md
- run.bat
- run_debug.bat
- run_debug.sh
- run.sh
- security.properties
- server_config
- server.lcf
- server_simple.bat
- server_tomcat.bat

## Source directories (depth 2)
### `src/`
- src/target
- src/target/generated-sources
- src/target/maven-status
- src/target/maven-archiver
- src/target/surefire-reports
- src/target/classes
- src/target/generated-test-sources
- src/target/test-classes
- src/com
- src/com/developmentontheedge
- src/ru
- src/ru/biosoft
- src/biouml
- src/biouml/plugins
- src/biouml/standard
- src/biouml/launcher
- src/biouml/workbench
- src/biouml/model
- src/biouml/splash

## File counts (top extensions)
- `.xml`: 10069 files
- `.java`: 6272 files
- `.txt`: 2085 files
- `.csv`: 1834 files
- `.m`: 1828 files
- `.gif`: 1005 files
- `.png`: 352 files
- `.config`: 286 files
- `.jar`: 282 files
- `.json`: 240 files

## Largest source files (top 15 by line count)
- `src/biouml/plugins/sbgn/_test/titles.txt` (133403 lines)
- `src/biouml/plugins/riboseq/comparison_article/resources/ucsc_old_genes.txt` (59122 lines)
- `src/biouml/plugins/gtrd/_test/resources/huTF_classification.html` (49049 lines)
- `src/biouml/plugins/gtrd/_test/resources/huTF_classification.html~` (40167 lines)
- `plugins/groovy-3.0.25.jar` (38146 lines)
- `plugins/org.sbolstandard.core2_4.0/libSBOLj-2.4.0-withDependencies.jar` (33093 lines)
- `plugins/org.apache.poi_3.17/poi-ooxml-schemas-3.17.jar` (31081 lines)
- `plugins/org.openscience.jchempaint_3.1.2/jchempaint-3.1.2.jar` (29103 lines)
- `plugins/org.openscience.jmol_12.2.4.jar` (25949 lines)
- `plugconfig/biouml.plugins.chipmunk/chipmunk.jar` (18260 lines)
- `plugins/org.apache.batik_1.7/batik-all.jar` (17952 lines)
- `src/ru/biosoft/server/servlets/webservices/webfiles/lib/table/jquery.dataTables.js` (15356 lines)
- `plugins/org.apache.lucene_9.11.1/lucene-core-9.11.1.jar` (15262 lines)
- `data_resources/_test/testMatrixLib.lib` (14036 lines)
- `plugins/org.apache.poi_3.17/ooxml-lib/xmlbeans-2.6.0.jar` (13517 lines)

## Test surface
- Directories named `test`: 6
- Directories named `tests`: 7
- Test files (by name pattern): 0

## Notable config / infra
- `docker-compose.yaml`
- `.github/workflows`

## Recent activity (last 10 commits)
- `94415c7d` 2026-09-02 Bug fixing
- `749e7563` 2026-09-02 perf(profiler): optimize hot paths from async-profiler (#37)
- `7ee7121e` 2026-09-02 perf(profiler): reduce allocations in CoordinateMapping.mapInterval (#36)
- `51f8664d` 2026-09-01 fix(simulation): restore the boundary clone in fireSolutionUpdate; keep safe micro-opts
- `3c3fb3a2` 2026-09-01 perf(simulation): optimize hot paths identified by async-profiler
- `44be6dad` 2026-09-01 fix(servermonitor): make the high-water cursor invariant hold and give the concurrency test a real seam
- `e18923e0` 2026-09-01 perf(profiler): integerize heap-sort index math in Util.sortHeap (#34)
- `3f61e324` 2026-09-01 fix(servermonitor): count lock waiters, not just holders, in the per-profile lock registry
- `2df410e5` 2026-09-01 fix(servermonitor): distinguish malformed sidecar from cursor 0, bound lock map, real concurrency test
- `1e73881f` 2026-09-01 fix(servermonitor): serialize per-profile report, guard cursor high-water mark

## Files churned in last 20 commits (top 10)
- `src/biouml/plugins/servermonitor/_test/TestSubProcessLog.java` (11×)
- `src/biouml/plugins/servermonitor/MonitoringService.java` (10×)
- `src/ru/biosoft/server/servlets/support/SupportServlet.java` (6×)
- `src/biouml/standard/simulation/SimulationResult.java` (2×)
- `src/biouml/standard/simulation/MathUtils.java` (2×)
- `src/biouml/plugins/simulation/SimulatorSupport.java` (2×)
- `src/biouml/plugins/simulation/ode/jvode/JVodeDense.java` (2×)
- `src/biouml/plugins/servermonitor/SubProcessMonitor.java` (2×)
- `src/biouml/plugins/servermonitor/ServerMonitorPlugin.java` (2×)
- `src/ru/biosoft/server/servlets/webservices/providers/WebTablesProvider.java` (1×)

_End repo map._
