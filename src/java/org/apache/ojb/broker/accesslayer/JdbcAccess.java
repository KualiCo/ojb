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

import org.apache.ojb.broker.Identity;
import org.apache.ojb.broker.PersistenceBrokerException;
import org.apache.ojb.broker.core.ValueContainer;
import org.apache.ojb.broker.metadata.ClassDescriptor;
import org.apache.ojb.broker.query.Query;

/**
 * JdbcAccess is responsible for establishing performing
 * SQL Queries against remote Databases.
 * It hides all knowledge about JDBC from the
 * {@link org.apache.ojb.broker.PersistenceBroker}
 *
 * @author <a href="mailto:thma@apache.org">Thomas Mahler</a>
 * @author <a href="mailto:armin@codeAuLait.de">Armin Waibel</a>
 * @version $Id: JdbcAccess.java,v 1.1 2007-08-24 22:17:30 ewestfal Exp $
 */
public interface JdbcAccess
{
    /**
     * performs a DELETE operation against RDBMS.
     * @param cld ClassDescriptor providing mapping information.
     * @param obj The object to be deleted.
     */
    public void executeDelete(ClassDescriptor cld, Object obj) throws PersistenceBrokerException;

    /**
     * performs a DELETE operation based on the given {@link Query} against RDBMS.
     * @param query the query string.
     * @param cld ClassDescriptor providing JDBC information.
     */
    public void executeDelete(Query query, ClassDescriptor cld) throws PersistenceBrokerException;

    /**
     * performs an INSERT operation against RDBMS.
     * @param obj The Object to be inserted as a row of the underlying table.
     * @param cld ClassDescriptor providing mapping information.
     */
    public void executeInsert(ClassDescriptor cld, Object obj) throws PersistenceBrokerException;

    /**
     * performs a SQL SELECT statement against RDBMS.
     * @param sqlStatement the query string.
     * @param cld ClassDescriptor providing meta-information.
	 * @param scrollable Does this resultset need cursor control for operations like last, first and size
     */
    public ResultSetAndStatement executeSQL(String sqlStatement, ClassDescriptor cld, boolean scrollable) throws PersistenceBrokerException;

    /**
     * performs a SQL SELECT statement against RDBMS.
     * @param sqlStatement the query string.
     * @param cld ClassDescriptor providing meta-information.
     * @param values The set of values to bind to the statement (may be null)
	 * @param scrollable Does this resultset need cursor control for operations like last, first and size
     */
    public ResultSetAndStatement executeSQL(String sqlStatement, ClassDescriptor cld, ValueContainer[] values, boolean scrollable) throws PersistenceBrokerException;

    /**
     * performs a SQL UPDTE, INSERT or DELETE statement against RDBMS.
     * @param sqlStatement the query string.
     * @param cld ClassDescriptor providing meta-information.
     * @return int returncode
     */
    public int executeUpdateSQL(String sqlStatement, ClassDescriptor cld)
            throws PersistenceBrokerException;

    /**
     * performs a SQL UPDTE, INSERT or DELETE statement against RDBMS.
     * @param sqlStatement the query string.
     * @param cld ClassDescriptor providing meta-information.
     * @param values1 The first set of values to bind to the statement (may be null)
     * @param values2 The second set of values to bind to the statement (may be null)
     * @return int returncode
     */
    public int executeUpdateSQL(String sqlStatement, ClassDescriptor cld, ValueContainer[] values1, ValueContainer[] values2)
            throws PersistenceBrokerException;
    /**
     * performs an UPDATE operation against RDBMS.
     * @param obj The Object to be updated in the underlying table.
     * @param cld ClassDescriptor providing mapping information.
     */
    public void executeUpdate(ClassDescriptor cld, Object obj) throws PersistenceBrokerException;

    /**
     * performs a primary key lookup operation against RDBMS and materializes
     * an object from the resulting row. Only skalar attributes are filled from
     * the row, references are not resolved.
     * @param oid contains the primary key info.
     * @param cld ClassDescriptor providing mapping information.
     * @return the materialized object, null if no matching row was found or if
     * any error occured.
     */
    public Object materializeObject(ClassDescriptor cld, Identity oid)
            throws PersistenceBrokerException;

    /**
     * performs a SELECT operation against RDBMS.
     * @param query the query string.
     * @param cld ClassDescriptor providing JDBC information.
     */
    public ResultSetAndStatement executeQuery(Query query, ClassDescriptor cld) throws PersistenceBrokerException;
}
