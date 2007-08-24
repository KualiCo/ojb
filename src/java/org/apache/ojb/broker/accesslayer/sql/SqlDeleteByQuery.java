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

import org.apache.ojb.broker.metadata.ClassDescriptor;
import org.apache.ojb.broker.metadata.FieldDescriptor;
import org.apache.ojb.broker.platforms.Platform;
import org.apache.ojb.broker.query.Criteria;
import org.apache.ojb.broker.query.Query;
import org.apache.ojb.broker.util.SqlHelper.PathInfo;
import org.apache.ojb.broker.util.logging.Logger;

/**
 * Model a DELETE by Query Statement
 *
 * @author Thomas Mahler
 * @version $Id: SqlDeleteByQuery.java,v 1.1 2007-08-24 22:17:39 ewestfal Exp $
 */
public class SqlDeleteByQuery extends SqlQueryStatement
{

	/**
	 * Constructor for SqlDeleteByQuery.
	 * @param cld
	 */
	public SqlDeleteByQuery(Platform pf, ClassDescriptor cld, Query query, Logger logger)
	{
		super(pf, cld, query, logger);
	}

    /**
     * @see org.apache.ojb.broker.accesslayer.sql.SqlQueryStatement#buildStatement()
     */
	protected String buildStatement()
	{
		StringBuffer stmt = new StringBuffer();
		StringBuffer where = new StringBuffer();

		Criteria crit = this.getQuery().getCriteria();

		stmt.append("DELETE FROM ");
		stmt.append(getSearchClassDescriptor().getFullTableName());
		appendWhereClause(where, crit, stmt);

		return stmt.toString();
	}

	/* (non-Javadoc)
	 * @see org.apache.ojb.broker.accesslayer.sql.SqlQueryStatement#getColName(org.apache.ojb.broker.accesslayer.sql.SqlQueryStatement.TableAlias, org.apache.ojb.broker.util.SqlHelper.PathInfo, boolean)
	 */
	protected String getColName(TableAlias aTableAlias, PathInfo aPathInfo, boolean translate)
	{
        FieldDescriptor fld = null;
        String result;

        if (translate)
        {
            fld = getFieldDescriptor(aTableAlias, aPathInfo);
        }

        if (fld != null)
        {
            // BRJ : No alias for delete
            result = fld.getColumnName();
        }
        else
        {
            result = aPathInfo.column;
        }

       return result;
   }

}
