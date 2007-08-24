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
import org.apache.ojb.broker.platforms.Platform;
import org.apache.ojb.broker.query.Query;

/**
 * This interface defines the behaviour of an SqlGenartor component 
 * that is responsible for building sql statements.
 *
 * @author <a href="mailto:thma@apache.org">Thomas Mahler<a>
 * @version $Id: SqlGenerator.java,v 1.1 2007-08-24 22:17:39 ewestfal Exp $
 */
public interface SqlGenerator
{
    /**
    * generate an INSERT-Statement for M:N indirection table
     *
     * @param table
     * @param pkColumns1
     * @param pkColumns2
     * @return String
     */
    public String getInsertMNStatement(String table, String[] pkColumns1, String[] pkColumns2);

    /**
     * generate a SELECT-Statement for M:N indirection table
     * @param table the indirection table
     * @param selectColumns selected columns
     * @param columns for where
     */
    public String getSelectMNStatement(String table, String[] selectColumns, String[] columns);

    /**
     * generate a DELETE-Statement for M:N indirection table
     *
     * @param table
     * @param pkColumns1
     * @param pkColumns2
     * @return String
     */
    public String getDeleteMNStatement(String table, String[] pkColumns1, String[] pkColumns2);

    /**
     * generate a select-Statement according to query
     * @param query the Query
     * @param cld the ClassDescriptor
     */
    public SelectStatement getPreparedSelectStatement(Query query, ClassDescriptor cld);

    /**
     * generate a select-Statement according to query
     * @param query the Query
     * @param cld the ClassDescriptor
     */
    public SelectStatement getSelectStatementDep(Query query, ClassDescriptor cld);

    /**
     * generate a prepared DELETE-Statement according to query
     * @param query the Query
     * @param cld the ClassDescriptor
     */
    public SqlStatement getPreparedDeleteStatement(Query query, ClassDescriptor cld);
    
    /**
     * generate a prepared DELETE-Statement for the Class
     * described by cld.
     * @param cld the ClassDescriptor
     */
    public SqlStatement getPreparedDeleteStatement(ClassDescriptor cld);
    
    /**
     * generate a prepared INSERT-Statement for the Class
     * described by mif.
     * @param cld the ClassDescriptor
     */
    public SqlStatement getPreparedInsertStatement(ClassDescriptor cld);
    
    /**
     * generate a prepared SELECT-Statement for the Class
     * described by cld
     * @param cld the ClassDescriptor
     */
    public SelectStatement getPreparedSelectByPkStatement(ClassDescriptor cld);

    /**
     * generate a prepared UPDATE-Statement for the Class
     * described by cld
     * @param cld the ClassDescriptor
     */
    public SqlStatement getPreparedUpdateStatement(ClassDescriptor cld);


    /**
     * Answer the Platform used by the SqlGenerator
     * @return Platform
     */
    public Platform getPlatform();

}
