package org.apache.ojb.broker.accesslayer.sql;

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

import org.apache.ojb.broker.metadata.FieldDescriptor;
import org.apache.ojb.broker.query.Query;

/**
 * This class
 *
 * @author <a href="mailto:arminw@apache.org">Armin Waibel</a>
 * @version $Id: SelectStatement.java,v 1.1 2007-08-24 22:17:39 ewestfal Exp $
 */
public interface SelectStatement extends SqlStatement
{   
    /**
     * Returns the {@link org.apache.ojb.broker.query.Query} instance
     * this statement based on.
     * @return The {@link org.apache.ojb.broker.query.Query} instance or <em>null</em>
     * if no query is used to generate the select string.
     */
    public Query getQueryInstance();

    /**
     * Returns the column index of the specified {@link org.apache.ojb.broker.metadata.FieldDescriptor}
     * in the {@link java.sql.ResultSet} after performing the query.
     *
     * @param fld The {@link org.apache.ojb.broker.metadata.FieldDescriptor}.
     * @return The column index or {@link org.apache.ojb.broker.metadata.JdbcType#MIN_INT} if
     * column index is not supported.
     */
    public int getColumnIndex(FieldDescriptor fld);
}
