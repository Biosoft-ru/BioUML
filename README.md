[![Tests in main branch](https://github.com/Biosoft-ru/BioUML/actions/workflows/run_tests_on_commit.yaml/badge.svg)](https://github.com/Biosoft-ru/BioUML/actions/workflows/run_tests_on_commit.yaml)

# To Build

Ensure that you have at least Java 21

BioUML uses a **dual build system** (Maven + Ant). Both must compile successfully.

Manually install JARs missing in Maven Central

```sh
./install_all_old_jars.sh 
```

**Maven** (modern, multi-module):

```sh
mvn package -DskipTests
```

**Ant** (legacy):

```sh
cd src && ant compile
```

Verify both after any change:

```sh
mvn package -DskipTests && cd src && ant compile
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

