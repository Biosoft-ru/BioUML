[![Tests in main branch](https://github.com/Biosoft-ru/BioUML/actions/workflows/run_tests_on_commit.yaml/badge.svg)](https://github.com/Biosoft-ru/BioUML/actions/workflows/run_tests_on_commit.yaml)

# To Build

Ensure that you have at least Java 11

Manually install JARs as decribed in [install_old_jars.md](install_old_jars.md)

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

Use your browser to open it at http://localhost:8080/bioumlweb/


# To Run tests

On Linux install required packages
```sh
sudo apt install r-base r-base-dev
```

Ensure that you have at least Java 11

```sh
mvn -pl src test 
```
or simply

```sh
mvn test 
```

