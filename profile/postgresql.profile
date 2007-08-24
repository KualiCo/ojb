#<!--
#/* Copyright 2002-2004 The Apache Software Foundation
# *
# * Licensed under the Apache License, Version 2.0 (the "License");
# * you may not use this file except in compliance with the License.
# * You may obtain a copy of the License at
# *
# *     http://www.apache.org/licenses/LICENSE-2.0
# *
# * Unless required by applicable law or agreed to in writing, software
# * distributed under the License is distributed on an "AS IS" BASIS,
# * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# * See the License for the specific language governing permissions and
# * limitations under the License.
# */
#-->
# location of jar that you will be using for testing
# so that it can be copied into the dist/lib directory.

torque.testDatabaseJar = ${lib.repo}/postgres.jar
torque.idMethod = idbroker
torque.defaultDatabase = ojbtest

# -------------------------------------------------------------------
#
# T O R Q U E  C O N F I G U R A T I O N  F I L E
#
# -------------------------------------------------------------------

# -------------------------------------------------------------------
#
#  T A R G E T  D A T A B A S E
#
# -------------------------------------------------------------------

torque.database = postgresql

# -------------------------------------------------------------------
#
#  D A T A B A S E  S E T T I N G S
#
# -------------------------------------------------------------------
# JDBC connection settings. This is used by the JDBCToXML task that
# will create an XML database schema from JDBC metadata. These
# settings are also used by the SQL Ant task to initialize your
# Turbine system with the generated SQL.
# -------------------------------------------------------------------

dbmsName = PostgreSQL
jdbcLevel = 3.0
urlProtocol = jdbc
urlSubprotocol = postgresql
urlDbalias = ${torque.project}


#
# settings for torque 3.1.1
#
torque.database.host = 127.0.0.1
torque.database.user = ojb
torque.database.password = ojb

# For PostgreSQL we have to specify a specific database when we want to
# create a new database via JDBC
torque.database.createUrl = ${urlProtocol}:${urlSubprotocol}://${torque.database.host}/template1

torque.database.buildUrl = ${urlProtocol}:${urlSubprotocol}://${torque.database.host}/${urlDbalias}
torque.database.url = ${urlProtocol}:${urlSubprotocol}://${torque.database.host}/${urlDbalias}
torque.database.driver = org.postgresql.Driver

# Tells JDBC task that javaName attribute for the tables and columns
# should be made same as SQL name.
sameJavaName=false

# -------------------------------------------------------------------
#
#  D O C U M E N T A T I O N   S E T T I N G S
#
# -------------------------------------------------------------------
# These settings will allow you to customize the way your schema
# documentation is created.
# Valid formats are: html, anakia (for use with jakarta-site2)
# -------------------------------------------------------------------
documentationFormat=html

# -------------------------------------------------------------------
# You should NOT have to edit anything below here.
# -------------------------------------------------------------------

# -------------------------------------------------------------------
#
#  T E M P L A T E  P A T H
#
# -------------------------------------------------------------------

templatePath = ../templates

# -------------------------------------------------------------------
#
#  C O N T R O L  T E M P L A T E S
#
# -------------------------------------------------------------------

SQLControlTemplate = sql/base/Control.vm
OMControlTemplate = om/Control.vm
idTableControlTemplate = sql/id-table/Control.vm
DataDTDControlTemplate = data/Control.vm
DataDumpControlTemplate = data/dump/Control.vm
DataSQLControlTemplate = sql/load/Control.vm
DocControlTemplate = doc/Control.vm

# -------------------------------------------------------------------
#
#  O U T P U T  D I R E C T O R Y
#
# -------------------------------------------------------------------

outputDirectory=src

# -------------------------------------------------------------------
#
#  S C H E M A  D I R E C T O R Y
#
# -------------------------------------------------------------------

schemaDirectory=schema
