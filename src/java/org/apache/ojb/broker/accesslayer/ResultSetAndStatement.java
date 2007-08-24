package org.apache.ojb.broker.accesslayer;

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

import java.sql.ResultSet;
import java.sql.Statement;

import org.apache.ojb.broker.accesslayer.sql.SelectStatement;

/**
 * Intern used wrapper for {@link Statement} and {@link ResultSet} instances.
 *
 * @version $Id: ResultSetAndStatement.java,v 1.1 2007-08-24 22:17:30 ewestfal Exp $
 */
public class ResultSetAndStatement
{
	// private static Logger log = LoggerFactory.getLogger(ResultSetAndStatement.class);

	private final StatementManagerIF manager;
    private boolean isClosed;
    /*
    arminw: declare final to avoid stmt/rs leaking in use
    by re-setting these fields.
    */
    public final ResultSet m_rs;
    public final Statement m_stmt;
    public final SelectStatement m_sql;

	public ResultSetAndStatement(StatementManagerIF manager, Statement stmt, ResultSet rs, SelectStatement sql)
    {
		this.manager = manager;
        m_stmt = stmt;
        m_rs = rs;
        m_sql = sql;
        isClosed = false;
    }

    /**
     * do a platform specific resource release.
     * <br/>
     * Note: This method must be called after usage
     * of this class.
     */
    public void close()
    {
        if(!isClosed)
        {
            manager.closeResources(m_stmt, m_rs);
            isClosed = true;
        }
    }

// arminw: This class is internaly used, thus we should take care to close all used
// resources without this check.
//    protected void finalize() throws Throwable
//    {
//        super.finalize();
//        if(!isClosed && (m_stmt != null || m_rs != null))
//        {
//            log.warn("** Associated resources (Statement/ResultSet) not closed!" +
//                    " Try automatic cleanup **");
//            try
//            {
//                close();
//            }
//            catch (Exception ignore)
//            {
//                //ignore it
//            }
//        }
//    }
}
