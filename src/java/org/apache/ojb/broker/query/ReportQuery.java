package org.apache.ojb.broker.query;

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

import java.util.Map;

/**
 * Interface for ReportQueries
 * 
 * @author <a href="mailto:jbraeuchi@gmx.ch">Jakob Braeuchi</a>
 * @version $Id: ReportQuery.java,v 1.1 2007-08-24 22:17:36 ewestfal Exp $
 */

public interface ReportQuery
{
	/**
	 * Gets the columns used for the Report.
	 * @return Returns a String[]
     * @deprecated use getAttributes()
	 */
	String[] getColumns();

    /**
     * Gets the attributes used for the Report.
     * @return Returns a String[]
     */
    String[] getAttributes();

    /**
     * Gets the Jdbc-Types of the columns used for the Report.
     * If null the Jdbc-Type is taken from the ResultSet
     * @return Returns an int[] of Jdbc-Types
     * @see java.sql.Types
     */
    int[] getJdbcTypes();  
    
    /**
     * Gets the additional attributes used for building the Join.
     * These Attributes are not appended to the select-clause.
     * @return Returns a String[]
     */
    String[] getJoinAttributes();
    
    /**
     * Returns a Map with FieldDescriptors identified by Attribute
     * @return Map
     */
    Map getAttributeFieldDescriptors();
}
