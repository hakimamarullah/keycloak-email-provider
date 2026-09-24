#!/bin/bash

set -e

mvn clean package -DskipTests

rm -rf docker/providers/keycloak-email-provider*.jar

cp target/keycloak-email-provider*.jar docker/providers/