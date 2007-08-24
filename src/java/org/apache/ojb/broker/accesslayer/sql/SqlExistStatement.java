package org.apache.ojb.broker.accesslayer.sql;

/* Copyright 2004-2005 The Apache Software Foundation
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

import org.apache.ojb.broker.OJBRuntimeException;
import org.apache.ojb.broker.metadata.ClassDescriptor;
import org.apache.ojb.broker.metadata.FieldDescriptor;
import org.apache.ojb.broker.util.logging.Logger;

/**
 * Generate a select to check existence of an object.
 * Something like "SELECT id_1 FROM myTable where id_1 = 123 and id_2 = 'kjngzt'".
 *
 * @author <a href="mailto:arminw@apache.org">Armin Waibel</a>
 * @version $Id: SqlExistStatement.java,v 1.1 2007-08-24 22:17:39 ewestfal Exp $
 */
public class SqlExistStatement extends SqlPkStatement
{
    private static final String SELECT = "SELECT ";
    private static final String FROM = " FROM ";

    private String sql;

    public SqlExistStatement(ClassDescriptor aCld, Logger aLogger)
    {
        super(aCld, aLogger);
    }

    /** Return SELECT clause for object existence call */
    public String getStatement()
    {
        if(sql == null)
        {
            StringBuffer stmt = new StringBuffer(128);
            ClassDescriptor cld = getClassDescriptor();

            FieldDescriptor[] fieldDescriptors = cld.getPkFields();
            if(fieldDescriptors == null || fieldDescriptors.length == 0)
            {
                throw new OJBRuntimeException("No PK fields defined in metadata for " + cld.getClassNameOfObject());
            }
            FieldDescriptor field = fieldDescriptors[0];

            stmt.append(SELECT);
            stmt.append(field.getColumnName());
            stmt.append(FROM);
            stmt.append(cld.getFullTableName());
            appendWhereClause(cld, false, stmt);

            sql = stmt.toString();
        }
        return sql;
    }
}