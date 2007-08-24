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

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.ojb.broker.Identity;
import org.apache.ojb.broker.PersistenceBrokerException;
import org.apache.ojb.broker.PersistenceBrokerSQLException;
import org.apache.ojb.broker.core.ValueContainer;
import org.apache.ojb.broker.metadata.ClassDescriptor;
import org.apache.ojb.broker.query.Query;

/**
 * @version $Id: StatementManagerIF.java,v 1.1 2007-08-24 22:17:30 ewestfal Exp $
 */
public interface StatementManagerIF
{

    /** fetchSize hint marking that setting fetch size for current statement is N/A. */
    int FETCH_SIZE_NOT_APPLICABLE = -1;
    /** fetchSize hint marking that there is no statement-level explicit override. */
    int FETCH_SIZE_NOT_EXPLICITLY_SET = 0;

    /**
     * Binds the Identities Primary key values to the statement.
     * @param stmt
     * @param oid
     * @param cld ClassDescriptor for the Object, if <i>null</i> will be lookup automatic
     */
    void bindDelete(PreparedStatement stmt, Identity oid, ClassDescriptor cld) throws java.sql.SQLException;
    /**
     * binds the objects primary key and locking values to the statement, BRJ
     */
    void bindDelete(PreparedStatement stmt, ClassDescriptor cld, Object obj)
        throws java.sql.SQLException;

    /**
     * bind a Query based Select Statement
     */
    int bindStatement(PreparedStatement stmt, Query query, ClassDescriptor cld, int param)
        throws SQLException;

    /**
     * binds the values of the object obj to the statements parameters
     */
    void bindInsert(PreparedStatement stmt, ClassDescriptor cld, Object obj)
		throws SQLException;
    /**
     * binds the Identities Primary key values to the statement
     * @param stmt
     * @param oid
     * @param cld ClassDescriptor for the Object, if <i>null</i> will be lookup automatic
     * @param callableStmt Indicate if the specified {@link java.sql.PreparedStatement}
     * is a {@link java.sql.CallableStatement} supporting stored procedures.
     */
    void bindSelect(PreparedStatement stmt, Identity oid, ClassDescriptor cld, boolean callableStmt) throws SQLException;
    /**
     * binds the values of the object obj to the statements parameters
     */
    void bindUpdate(PreparedStatement stmt, ClassDescriptor cld, Object obj)
        throws SQLException;

    /**
     * binds the given array of values (if not null) starting from the given
     * parameter index
     * @return the next parameter index
     */
    int bindValues(PreparedStatement stmt, ValueContainer[] valueContainer, int index) throws SQLException;

    /**
     * return a prepared DELETE Statement fitting for the given ClassDescriptor
     */
    PreparedStatement getDeleteStatement(ClassDescriptor cds)
        throws PersistenceBrokerSQLException;
    /**
     * return a generic Statement for the given ClassDescriptor
     */
    Statement getGenericStatement(ClassDescriptor cds, boolean scrollable) throws PersistenceBrokerException;
    /**
     * return a prepared Insert Statement fitting for the given ClassDescriptor
     */
    PreparedStatement getInsertStatement(ClassDescriptor cds)
        throws PersistenceBrokerSQLException;
    /**
     * Return a PreparedStatement for selecting against the given ClassDescriptor.
     */
    PreparedStatement getPreparedStatement(ClassDescriptor cds, String sql,
                                           boolean scrollable, int explicitFetchSizeHint, boolean callableStmt)
        throws PersistenceBrokerException;
    /**
     * return a prepared Select Statement for the given ClassDescriptor
     */
    PreparedStatement getSelectByPKStatement(ClassDescriptor cds)
        throws PersistenceBrokerSQLException;
    /**
     * return a prepared Update Statement fitting to the given ClassDescriptor
     */
    PreparedStatement getUpdateStatement(ClassDescriptor cds)
        throws PersistenceBrokerSQLException;

    public void closeResources(Statement stmt, ResultSet rs);

}
