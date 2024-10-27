#!/usr/bin/env bash
export GPG_TTY=$(tty)
./mvnw -B release:prepare release:perform -Dmaven.javadoc.skip=true -Pno-test
