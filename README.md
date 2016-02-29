# AEM Maven Repository
This OSGi Bundle turn AEM into a maven repository and provides developers access to all OSGi bundles running in Apache Felix inside the Adobe Experience Manager.

## Compatible With

- AEM 6.1

Even when this plugin isn't tested on AEM 6.0 or AEM 5.x, it's quite possible that it works fine with these versions. Please open a ticket on GitHub when you tested it, so I can extend the compatibility list.

## HowTo Deploy

- Download the latest aem-maven-repository jar file from https://github.com/sbrinkmann/aem-maven-repository/releases
- Go to the Apache Felix Console http://<aem-hostname>:<4502|4503>/system/console/bundles
- Select _Install or Update_
- Select the just downloaded aem-maven-repository jar file with the file picker
- Tick the _Start Bundle_ checkbox
- Hit _Install or Update_ button to finish the deployment

## Access

The AEM Maven Repository provides two web interfaces. The first one generates a POM file which represents all OSGi bundles running inside Apache Felix. The second provides access as a Maven repository.
 
##### Access Maven POM file

- Quick Access<br>
  http://localhost:4502/bin/maven/dependencies.xml
- Access the List of Dependencies Only<br>
  http://localhost:4502/bin/maven/dependencies.xml?dependenciesOnly=true
- Adjust group ID, artifact ID and version number (e.g. my.group.id:aem-base:6.1)<br>
  http://localhost:4502/bin/maven/dependencies.xml?groupId=my.group.id&artifactId=aem-base&version=6.1

##### Access Maven Repository

Add the following part to your POM file. Adjust the _<url>_ in case your instance doesn't run on localhost or another port.
```
<repositories>
        <repository>
            <id>aem-repository</id>
            <name>AEM Maven Repository</name>
            <url>http://localhost:4502/bin/maven/repository</url>
            <releases>
                <enabled>true</enabled>
            </releases>
            <snapshots>
                <enabled>true</enabled>
            </snapshots>
        </repository>
 </repositories>
 <pluginRepositories>
        <pluginRepository>
            <id>aem-repository</id>
            <name>AEM Maven Repository</name>
            <url>http://localhost:4502/bin/maven/repository</url>
            <releases>
                <enabled>true</enabled>
            </releases>
            <snapshots>
                <enabled>true</enabled>
            </snapshots>
        </pluginRepository>
 </pluginRepositories>
```

##### Set AEM Admin Credentials in _.m2/settings.xml_

Adjust the password, in case changed the admin password
```
<server>
      <id>aem-repository</id>
      <username>admin</username>
      <password>admin</password>
</server>
```

## Build and Deploy with Maven

*Build*
```
mvn clean install
```

*Build and Deploy*
```
mvn -Pinstall-bundle clean install
```

*Build and Deploy to another instance then localhost:4502 with the credentials admin:admin*
```
mvn -Pinstall-bundle -Dcq.host=localhost -Dcq.port=4502 -Dcq.user=admin -Dcq.password=admin clean install
```
