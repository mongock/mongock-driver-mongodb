#!/usr/bin/env bash
export GPG_TTY=$(tty)
mvn -B release:prepare -Darguments="-Dgpg.passphrase=$MAVEN_CENTRAL_TOKEN -Dmaven.javadoc.skip=true -Pno-test"
mvn -B release:perform -Darguments="-Dgpg.passphrase=$MAVEN_CENTRAL_TOKEN -Dmaven.javadoc.skip=true -Pno-test --non-recursive -f ./pom.xml"
mvn -B release:perform -Darguments="-Dgpg.passphrase=$MAVEN_CENTRAL_TOKEN -Dmaven.javadoc.skip=true -Pno-test --non-recursive -f ./mongock-core/pom.xml"
mvn -B release:perform -Darguments="-Dgpg.passphrase=$MAVEN_CENTRAL_TOKEN -Dmaven.javadoc.skip=true -Pno-test --non-recursive -f ./mongock-core/mongock-utils/pom.xml"
mvn -B release:perform -Darguments="-Dgpg.passphrase=$MAVEN_CENTRAL_TOKEN -Dmaven.javadoc.skip=true -Pno-test --non-recursive -f ./mongock-core/mongock-api/pom.xml"
mvn -B release:perform -Darguments="-Dgpg.passphrase=$MAVEN_CENTRAL_TOKEN -Dmaven.javadoc.skip=true -Pno-test --non-recursive -f ./mongock-core/mongock-test-util/pom.xml"
mvn -B release:perform -Darguments="-Dgpg.passphrase=$MAVEN_CENTRAL_TOKEN -Dmaven.javadoc.skip=true -Pno-test --non-recursive -f ./mongock-core/mongock-driver/pom.xml"
mvn -B release:perform -Darguments="-Dgpg.passphrase=$MAVEN_CENTRAL_TOKEN -Dmaven.javadoc.skip=true -Pno-test --non-recursive -f ./mongock-core/mongock-driver/mongock-driver-api/pom.xml"
mvn -B release:perform -Darguments="-Dgpg.passphrase=$MAVEN_CENTRAL_TOKEN -Dmaven.javadoc.skip=true -Pno-test --non-recursive -f ./mongock-core/mongock-driver/mongock-driver-core/pom.xml"
mvn -B release:perform -Darguments="-Dgpg.passphrase=$MAVEN_CENTRAL_TOKEN -Dmaven.javadoc.skip=true -Pno-test --non-recursive -f ./mongock-core/mongock-runner/pom.xml"
mvn -B release:perform -Darguments="-Dgpg.passphrase=$MAVEN_CENTRAL_TOKEN -Dmaven.javadoc.skip=true -Pno-test --non-recursive -f ./mongock-core/mongock-runner/mongock-runner-core/pom.xml"
mvn -B release:perform -Darguments="-Dgpg.passphrase=$MAVEN_CENTRAL_TOKEN -Dmaven.javadoc.skip=true -Pno-test --non-recursive -f ./mongock-core/mongock-runner/mongock-test-runner/pom.xml"
mvn -B release:perform -Darguments="-Dgpg.passphrase=$MAVEN_CENTRAL_TOKEN -Dmaven.javadoc.skip=true -Pno-test --non-recursive -f ./mongock-core/mongock-runner/spring/pom.xml"
mvn -B release:perform -Darguments="-Dgpg.passphrase=$MAVEN_CENTRAL_TOKEN -Dmaven.javadoc.skip=true -Pno-test --non-recursive -f ./mongock-core/mongock-runner/spring/mongock-spring-base/pom.xml"
mvn -B release:perform -Darguments="-Dgpg.passphrase=$MAVEN_CENTRAL_TOKEN -Dmaven.javadoc.skip=true -Pno-test --non-recursive -f ./mongock-core/mongock-runner/spring/springboot/pom.xml"
mvn -B release:perform -Darguments="-Dgpg.passphrase=$MAVEN_CENTRAL_TOKEN -Dmaven.javadoc.skip=true -Pno-test --non-recursive -f ./mongock-core/mongock-runner/spring/springboot/mongock-springboot-base/pom.xml"
mvn -B release:perform -Darguments="-Dgpg.passphrase=$MAVEN_CENTRAL_TOKEN -Dmaven.javadoc.skip=true -Pno-test --non-recursive -f ./mongock-core/mongock-decorator-util/pom.xml"
mvn -B release:perform -Darguments="-Dgpg.passphrase=$MAVEN_CENTRAL_TOKEN -Dmaven.javadoc.skip=true -Pno-test --non-recursive -f ./drivers/pom.xml"
mvn -B release:perform -Darguments="-Dgpg.passphrase=$MAVEN_CENTRAL_TOKEN -Dmaven.javadoc.skip=true -Pno-test --non-recursive -f ./drivers/mongodb/pom.xml"
mvn -B release:perform -Darguments="-Dgpg.passphrase=$MAVEN_CENTRAL_TOKEN -Dmaven.javadoc.skip=true -Pno-test --non-recursive -f ./drivers/mongodb/mongodb-driver-test-template/pom.xml"
mvn -B release:perform -Darguments="-Dgpg.passphrase=$MAVEN_CENTRAL_TOKEN -Dmaven.javadoc.skip=true -Pno-test --non-recursive -f ./drivers/mongodb/mongodb-sync-v4-driver/pom.xml"
mvn -B release:perform -Darguments="-Dgpg.passphrase=$MAVEN_CENTRAL_TOKEN -Dmaven.javadoc.skip=true -Pno-test --non-recursive -f ./drivers/mongodb/mongodb-springdata-v3-driver/pom.xml"
mvn -B release:perform -Darguments="-Dgpg.passphrase=$MAVEN_CENTRAL_TOKEN -Dmaven.javadoc.skip=true -Pno-test --non-recursive -f ./mongock-core/mongock-runner/spring/springboot/mongock-springboot/pom.xml"
mvn -B release:perform -Darguments="-Dgpg.passphrase=$MAVEN_CENTRAL_TOKEN -Dmaven.javadoc.skip=true -Pno-test --non-recursive -f ./mongock-core/mongock-runner/standalone/pom.xml"
mvn -B release:perform -Darguments="-Dgpg.passphrase=$MAVEN_CENTRAL_TOKEN -Dmaven.javadoc.skip=true -Pno-test --non-recursive -f ./mongock-core/mongock-runner/standalone/mongock-standalone-base/pom.xml"
mvn -B release:perform -Darguments="-Dgpg.passphrase=$MAVEN_CENTRAL_TOKEN -Dmaven.javadoc.skip=true -Pno-test --non-recursive -f ./mongock-core/mongock-runner/standalone/mongock-standalone/pom.xml"
mvn -B release:perform -Darguments="-Dgpg.passphrase=$MAVEN_CENTRAL_TOKEN -Dmaven.javadoc.skip=true -Pno-test --non-recursive -f ./mongock-core/mongock-core-bom/pom.xml"
mvn -B release:perform -Darguments="-Dgpg.passphrase=$MAVEN_CENTRAL_TOKEN -Dmaven.javadoc.skip=true -Pno-test --non-recursive -f ./drivers/mongodb/mongock-driver-mongodb-bom/pom.xml"
mvn -B release:perform -Darguments="-Dgpg.passphrase=$MAVEN_CENTRAL_TOKEN -Dmaven.javadoc.skip=true -Pno-test --non-recursive -f ./drivers/mongodb/mongodb-v3-driver/pom.xml"
mvn -B release:perform -Darguments="-Dgpg.passphrase=$MAVEN_CENTRAL_TOKEN -Dmaven.javadoc.skip=true -Pno-test --non-recursive -f ./drivers/mongodb/mongodb-reactive-driver/pom.xml"
mvn -B release:perform -Darguments="-Dgpg.passphrase=$MAVEN_CENTRAL_TOKEN -Dmaven.javadoc.skip=true -Pno-test --non-recursive -f ./drivers/mongodb/mongodb-springdata-v2-driver/pom.xml"
mvn -B release:perform -Darguments="-Dgpg.passphrase=$MAVEN_CENTRAL_TOKEN -Dmaven.javadoc.skip=true -Pno-test --non-recursive -f ./drivers/dynamodb/pom.xml"
mvn -B release:perform -Darguments="-Dgpg.passphrase=$MAVEN_CENTRAL_TOKEN -Dmaven.javadoc.skip=true -Pno-test --non-recursive -f ./drivers/dynamodb/dynamodb-driver/pom.xml"
mvn -B release:perform -Darguments="-Dgpg.passphrase=$MAVEN_CENTRAL_TOKEN -Dmaven.javadoc.skip=true -Pno-test --non-recursive -f ./drivers/dynamodb/dynamodb-springboot-driver/pom.xml"
mvn -B release:perform -Darguments="-Dgpg.passphrase=$MAVEN_CENTRAL_TOKEN -Dmaven.javadoc.skip=true -Pno-test --non-recursive -f ./drivers/dynamodb/mongock-driver-dynamodb-bom/pom.xml"
mvn -B release:perform -Darguments="-Dgpg.passphrase=$MAVEN_CENTRAL_TOKEN -Dmaven.javadoc.skip=true -Pno-test --non-recursive -f ./drivers/couchbase/pom.xml"
mvn -B release:perform -Darguments="-Dgpg.passphrase=$MAVEN_CENTRAL_TOKEN -Dmaven.javadoc.skip=true -Pno-test --non-recursive -f ./drivers/couchbase/mongock-driver-couchbase-bom/pom.xml"
mvn -B release:perform -Darguments="-Dgpg.passphrase=$MAVEN_CENTRAL_TOKEN -Dmaven.javadoc.skip=true -Pno-test --non-recursive -f ./drivers/couchbase/couchbase-driver/pom.xml"
mvn -B release:perform -Darguments="-Dgpg.passphrase=$MAVEN_CENTRAL_TOKEN -Dmaven.javadoc.skip=true -Pno-test --non-recursive -f ./drivers/couchbase/couchbase-springboot-driver/pom.xml"
mvn -B release:perform -Darguments="-Dgpg.passphrase=$MAVEN_CENTRAL_TOKEN -Dmaven.javadoc.skip=true -Pno-test --non-recursive -f ./mongock-community-bom/pom.xml"
mvn -B release:perform -Darguments="-Dgpg.passphrase=$MAVEN_CENTRAL_TOKEN -Dmaven.javadoc.skip=true -Pno-test --non-recursive -f ./mongock-test/pom.xml"
mvn -B release:perform -Darguments="-Dgpg.passphrase=$MAVEN_CENTRAL_TOKEN -Dmaven.javadoc.skip=true -Pno-test --non-recursive -f ./mongock-test/mongock-test-core/pom.xml"
mvn -B release:perform -Darguments="-Dgpg.passphrase=$MAVEN_CENTRAL_TOKEN -Dmaven.javadoc.skip=true -Pno-test --non-recursive -f ./mongock-test/mongock-springboot-test/pom.xml"
mvn -B release:perform -Darguments="-Dgpg.passphrase=$MAVEN_CENTRAL_TOKEN -Dmaven.javadoc.skip=true -Pno-test --non-recursive -f ./mongock-test/mongock-springboot-junit5/pom.xml"
