[![Tests in main branch](https://github.com/Biosoft-ru/BioUML/actions/workflows/run_tests_on_commit.yaml/badge.svg)](https://github.com/Biosoft-ru/BioUML/actions/workflows/run_tests_on_commit.yaml)

# To Build

Ensure that you have at least Java 21

Manually install JARs missing in Maven Central

```sh
./install_all_old_jars.sh 
```

Now build BioUML

```sh
mvn package -DskipTests
```

# To Run

First, start mysql server. The command below is for docker but you can easiliy adopt it to your situation

```sh
docker run --name mysql-biouml2 \
   -p 3306:3306 \
   -v ./dumps/bioumlsupport2.dump.sql:/docker-entrypoint-initdb.d/bioumlsupport2.dump.sql \
   -e MYSQL_ROOT_PASSWORD=biouml \
   -e MYSQL_DATABASE=bioumlsupport2 \
   -e MYSQL_USER=bioumlsupport2 \
   -e MYSQL_PASSWORD=bioumlsupport2 \
   -d mysql:5
```

Alternatively, you can launch mysql server via [docker-compose](docker-compose.yaml)  

Then launch BioUML Web edition.

```sh
mvn -pl tomcat-embedded exec:java
```

For Java versions newer than 11 you can use experimental forked launch mode

```sh
mvn -pl tomcat-embedded exec:exec@run-forked
```

Use your browser to open it at http://localhost:8080/bioumlweb/


## Configure MCP OAuth recovery after a restart

Set `BIOUML_MCP_OAUTH_KEY` in the environment of the process running BioUML to enable
silent recovery for new MCP OAuth grants issued through BioStore password sign-in.
The key encrypts the BioStore login token saved in MySQL. Keep the same key across
restarts and use a separate key per deployment.

For a systemd-managed `tomcat9` service:

1. Generate a key once with `openssl rand -base64 32`. Store it in a root-owned
   file such as `/etc/tomcat9/biouml-mcp-oauth.env` with permissions `0600`.
   The file should contain `BIOUML_MCP_OAUTH_KEY=<generated value>`.
2. Run `sudo systemctl edit tomcat9` and add this drop-in:

   ```ini
   [Service]
   EnvironmentFile=/etc/tomcat9/biouml-mcp-oauth.env
   ```

3. Run `sudo systemctl daemon-reload` and `sudo systemctl restart tomcat9`.
   Authorize the MCP connector once after the server starts to create a recoverable grant.

The grant can recover across restarts within its seven-day refresh lifetime. Without
the key, BioUML still preserves OAuth client registrations so users can sign in again
through the same connector. See the [MCP OAuth deployment notes](plugconfig/biouml.plugins.mcp/README.md#oauth-across-server-restarts).

## Recover a pending project deletion

Use **Retry project deletion** on the account page for an interrupted deletion.
A pending record reserves the name while cleanup is incomplete.

If the BioStore step cannot finish after local files are gone, an authenticated
server administrator can send a POST to `support/recoverProjectDeletion` with
`project` and the usual support authentication parameters. Add `dc` if the
project was in a repository other than the default user projects collection.
The endpoint checks that there is a pending BioStore deletion and that the
repository entry and directory are gone. If the provider reports that the
group still exists, remove the old group before recovering the record. If the
provider does not support a reliable absence check, verify in BioStore that the
old group is gone and send `confirmGroupHandled=true`. Recovery releases the
name once the remaining local cleanup has succeeded.

BioUML logs each completed archive path under `BackupFolder`. A retry can take
another full copy while any project files remain. Keep the copies for your
deployment's backup retention period, and preserve the original archive until
you have verified that it can be restored. BioUML leaves archive expiration to
the deployment. For MySQL backup connections, `localhost` without a port uses
the client's default local socket and an explicit port uses TCP.

# To Run tests

On Linux install required packages
```sh
sudo apt install r-base r-base-dev
```

Ensure that you have at least Java 21

```sh
mvn -pl src test 
```
or simply

```sh
mvn test 
```

