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

#
# McKoi is not supported by Torque (at least not by the version that comes with OJB)
# but only by the commons-sql database handling
# Therefore this profile does not contain Torque properties
#

dbmsName = Mckoi
jdbcLevel = 2.0
urlProtocol = jdbc
urlSubprotocol = mckoi
urlDbalias = //localhost/OJB

#
# settings for torque 3.1.1
#
torque.database.createUrl = ${urlProtocol}:${urlSubprotocol}:${urlDbalias}
torque.database.buildUrl = ${urlProtocol}:${urlSubprotocol}:${urlDbalias}
torque.database.url = ${urlProtocol}:${urlSubprotocol}:${urlDbalias}
torque.database.driver = com.mckoi.JDBCDriver
torque.database.user = admin_user
torque.database.password = aupass00
torque.database.host = 127.0.0.1


