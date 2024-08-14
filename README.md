## Accessing the project

Use `git clone ssh://git@85.118.228.170:7222/isb/biouml2.git` to access project without VPN
using your public SSH key.

On modern Linux distributions it make still ask for password. In this case use `ssh -v -p 7222 git@85.118.228.170` to investigate. If you see in the logs something like
`no mutual signature algorithm` add the following to `.ssh/config`

```
Host *
  Ciphers +3des-cbc
  KexAlgorithms +diffie-hellman-group1-sha1,diffie-hellman-group14-sha1
  HostkeyAlgorithms +ssh-rsa
  PubkeyAcceptedAlgorithms +ssh-rsa
```


## Сборка bioumlweb под Docker (рекомендуется)

```
cd BioUML/src

ant -DDOCKER=true -DDEPLOY_DIR=$HOME/BioUML_Server/webapps -DSERVER_PATH=$HOME/BioUML_Server \
    -DUSE_LARGE_ICONS=true \
    -DDEPLOY_RESOURCES=dev -DDEPLOY_PLUGINS=true \
    clean deploy.server biouml.webserver
```
### **Запуск приложения bioumlweb**

```
cd BioUML/Docker

mkdir -p docker.out/logs && CONTAINER_USER=$(id -u) CONTAINER_GROUP=$(id -g) docker-compose up -d
```

или с удалением логов

```
mkdir -p docker.out/logs && rm docker.out/logs/* && CONTAINER_USER=$(id -u) CONTAINER_GROUP=$(id -g) docker-compose up -d
```

или просто, но логи тогда будут правами root

```
docker-compose up -d
```

# Сборка bioumlweb со своим Томкатом

### **Для сборки bioumlweb требуется Tomcat 8**

```
curl http://mirror.linux-ia64.org/apache/tomcat/tomcat-8/v8.5.56/bin/apache-tomcat-8.5.56.tar.gz | tar xz

ln -s apache-tomcat-8.5.56 tomcat8
```

### **Сборка приложения bioumlweb**

```
ant -DDEPLOY_DIR=$HOME/tomcat8/webapps/biouml -DSERVER_PATH=$HOME/tomcat8/BioUML_Server \
    -DUSE_LARGE_ICONS=true \
    -DDEPLOY_RESOURCES=dev -DDEPLOY_PLUGINS=true \
    clean deploy.server biouml.webserver
```

### **Запуск приложения bioumlweb**

```
CATALINA_HOME=$HOME/tomcat8 \
JAVA_OPTS="-Djava.security.egd=file:/dev/./urandom -Djava.security.manager -Djava.security.policy=$HOME/projects/java/BioUML/biouml.policy" \
   $HOME/tomcat8/bin/startup.sh
```