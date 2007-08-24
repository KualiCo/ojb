package org.apache.ojb.broker.util;

/* Copyright 2002-2005 The Apache Software Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 *
 * The Interface Version holds OJB versioning information. 
 * The $ ENTRY $ placeholders are replaced by the OJB preprocessor
 * with the settings configured in build.properties.
 *
 * @author Thomas Mahler
 * @version $Id: Version.java,v 1.1 2007-08-24 22:17:36 ewestfal Exp $
 */

public interface Version {
	
	/**
	 * The full qualified OJB release string.
	 * consists of OJB_VERSION_MAJOR . OJB_VERSION_MINOR . OJB_VERSION_BUILD 
	 */
	public static final String OJB_VERSION_FULLQUALIFIED = "1.0.4";

	/**
	 * The OJB major version number
	 */
	public static final String OJB_VERSION_MAJOR = "1";

	/**
	 * The OJB minor version number
	 */
	public static final String OJB_VERSION_MINOR = "0";
	
	/**
	 * The OJB build number
	 */
	public static final String OJB_VERSION_BUILD = "4";

	/**
	 * The timestamp of the current release
	 */
	public static final String OJB_VERSION_DATE = "2005-12-30";
}
