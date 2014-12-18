@echo off
rem ---------------------------------------------------------------------------
rem  Licensed to the Apache Software Foundation (ASF) under one
rem  or more contributor license agreements.  See the NOTICE file
rem  distributed with this work for additional information
rem  regarding copyright ownership.  The ASF licenses this file
rem  to you under the Apache License, Version 2.0 (the
rem  "License"); you may not use this file except in compliance
rem  with the License.  You may obtain a copy of the License at
rem
rem      http://www.apache.org/licenses/LICENSE-2.0
rem
rem  Unless required by applicable law or agreed to in writing,
rem  software distributed under the License is distributed on an
rem  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
rem  KIND, either express or implied.  See the License for the
rem  specific language governing permissions and limitations
rem  under the License.
rem ---------------------------------------------------------------------------
rem  Main Script for Apache Stratos CLI
rem
rem  Environment Variable Prequisites
rem
rem   STRATOS_CLI_HOME   Home of Stratos CLI Tool
rem
rem   STRATOS_URL        The URL of the Stratos Controller
rem ---------------------------------------------------------------------------

rem ----- Only set STRATOS_CLI_HOME if not already set ----------------------------

if "%STRATOS_CLI_HOME%"=="" set STRATOS_CLI_HOME=%CD%

cd %STRATOS_CLI_HOME%

java -jar "org.apache.stratos.cli-4.0.0-SNAPSHOT.jar" %*

