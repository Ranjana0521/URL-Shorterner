#!/bin/sh
# ----------------------------------------------------------------------------
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.
# ----------------------------------------------------------------------------

# ----------------------------------------------------------------------------
# Maven Start Up CLI script
# ----------------------------------------------------------------------------

# Resolve links - $0 may be a softlink
PRG="$0"

while [ -h "$PRG" ] ; do
  ls=`ls -ld "$PRG"`
  link=`expr "$ls" : '.*-> \(.*\)$'`
  if expr "$link" : '/.*' > /dev/null; then
    PRG="$link"
  else
    PRG=`dirname "$PRG"`/"$link"
  fi
done

saveddir=`pwd`

VM_PROJECT_ROOT=`dirname "$PRG"`
cd "$VM_PROJECT_ROOT"
VM_PROJECT_ROOT=`pwd`
cd "$saveddir"

WRAPPER_JAR="$VM_PROJECT_ROOT/.mvn/wrapper/maven-wrapper.jar"
WRAPPER_PROPERTIES="$VM_PROJECT_ROOT/.mvn/wrapper/maven-wrapper.properties"
LAUNCHER_CLASS="org.apache.maven.wrapper.MavenWrapperMain"

# Download the wrapper jar if not exists
if [ ! -f "$WRAPPER_JAR" ]; then
    echo "Downloading Maven Wrapper Jar..."
    mkdir -p "$VM_PROJECT_ROOT/.mvn/wrapper"
    curl -sfL https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.2.0/maven-wrapper-3.2.0.jar -o "$WRAPPER_JAR"
fi

if [ -n "$JAVA_HOME" ] ; then
  JAVACMD="$JAVA_HOME/bin/java"
else
  JAVACMD="java"
fi

exec "$JAVACMD" \
  $MAVEN_OPTS \
  -classpath "$WRAPPER_JAR" \
  "-Dmaven.multiModuleProjectDirectory=$VM_PROJECT_ROOT" \
  $LAUNCHER_CLASS \
  "$@"
