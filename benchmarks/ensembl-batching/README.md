# Ensembl batching: equivalence check and benchmark

Compares the per-id SQL of `EnsemblTranscriptsProvider` and `InternalEnsemblHub` (before PR #64) with their batched
versions on a synthetic, Ensembl-shaped MySQL database:

- **verify** dumps every result of every scenario; the old and new dumps must be identical.
- **bench** prints the SQL statements sent (MySQL's session `Questions` counter) and the median time of 5 runs.

It is standalone: it is not part of the Maven or Ant build, and needs a MySQL server it may drop and create a database on.

## Data

20,000 genes, ~100,800 transcripts, ~170,000 exons, ~60,000 translations. Edge cases in the data: transcripts without
exons, transcripts with two translations, 33 stable ids shared by two transcripts (different region and location),
and a seq_region outside the rank-1 coordinate system. `ANALYZE TABLE` runs after loading, so the old and the new run
get the same query plans (queries without `ORDER BY` return rows in plan order).

## Scenarios

- Hub: genes→transcripts (100, 2,000, 10,000 ids), transcripts→genes, proteins→genes, proteins→transcripts,
  transcripts→proteins, genes→proteins (5,000 ids each), empty input, an id with a collation-equal variant in the same
  batch (control character, trailing space, other case, accent), and cancellation (terminated from the start and after
  a few status checks).
- The input ids include versioned ids (`ENSG….3`, which need the retry without version), unknown ids, lowercase ids,
  duplicates, two spellings of one id, and ids with a trailing space, a tab, a control character, an accented or
  full-width letter, as well as the empty string.
- Provider: the subset from each genes→transcripts result plus unknown, lowercase, duplicated and non-unique stable
  ids, with `loadCDS` on and off; an empty subset; all transcripts, with and without `onlyProteinCoding`.

Hub output values are sorted per id: neither the old nor the new SQL defines their order. Provider output is compared
in result order. A scenario that throws is recorded with its error, which must also be the same for old and new (on a
latin1 database the full-width-letter id makes MySQL reject the comparison, in the old code as well).

## Run

Build the classes once (from the repository root) for the old and the new code, e.g. by compiling `src` at the commit
before the change and at the change into two directories, and collect the dependency classpath:

```sh
mvn -q -pl src dependency:build-classpath -Dmdep.outputFile=/tmp/src-deps.cp
```

Start a throwaway MySQL (any version; the database's charset and collation are set per run):

```sh
sudo docker run -d --name ens57 -p 127.0.0.1:3307:3306 -e MYSQL_ROOT_PASSWORD=bench mysql:5.7
```

Then:

```sh
OLD=/path/to/old/classes NEW=/path/to/new/classes DEPS="$(cat /tmp/src-deps.cp)" \
JDBC=/path/to/mysql-connector-j-8.4.0.jar ./run.sh 3307 utf8mb4 utf8mb4_general_ci
```

`run.sh` compiles the harness, loads the data, runs `verify` with the old and the new classes, compares the outputs
and runs `bench` for both.
