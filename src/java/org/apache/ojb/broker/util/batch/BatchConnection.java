package org.apache.ojb.broker.util.batch;

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

import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import org.apache.ojb.broker.PersistenceBroker;
import org.apache.ojb.broker.metadata.ClassDescriptor;
import org.apache.ojb.broker.metadata.CollectionDescriptor;
import org.apache.ojb.broker.metadata.DescriptorRepository;
import org.apache.ojb.broker.metadata.JdbcConnectionDescriptor;
import org.apache.ojb.broker.metadata.ObjectReferenceDescriptor;
import org.apache.ojb.broker.util.WrappedConnection;

/**
 * The implementation of {@link java.sql.Connection} which
 * automatically gathers INSERT, UPDATE and DELETE
 * PreparedStatements into batches.
 *
 * @author Oleg Nitz (<a href="mailto:olegnitz@apache.org">olegnitz@apache.org</a>)
 * @version $Id: BatchConnection.java,v 1.1 2007-08-24 22:17:42 ewestfal Exp $
 */
public class BatchConnection extends WrappedConnection
{
    private static final int MAX_COUNT = 100;

    /**
     * Maps PBKey to another HashMap,
     * which maps table name to List of related tables (N:1 or 1:1)
     */
    private static HashMap _pbkeyToFKInfo = new HashMap();

    private boolean _useBatchInserts = true;
    private HashMap _statements = new HashMap();
    private ArrayList _order = new ArrayList();
    private HashMap _fkInfo;
    private HashSet _deleted;
    private HashSet _dontInsert;
    private HashSet _touched = new HashSet();
    private int count = 0;
    private JdbcConnectionDescriptor m_jcd;

    public BatchConnection(Connection conn, PersistenceBroker broker)
    {
        super(conn);
        m_jcd = broker.serviceConnectionManager().getConnectionDescriptor();
        _fkInfo = (HashMap) _pbkeyToFKInfo.get(broker.getPBKey());
        if (_fkInfo != null)
        {
            return;
        }

        DescriptorRepository repos = broker.getDescriptorRepository();
        _fkInfo = new HashMap();
        for (Iterator it = repos.iterator(); it.hasNext();)
        {
            ClassDescriptor desc = (ClassDescriptor) it.next();
            List ordList = desc.getObjectReferenceDescriptors();
            if (!ordList.isEmpty())
            {
                HashSet fkTables = getFKTablesFor(desc.getFullTableName());
                for (Iterator it2 = ordList.iterator(); it2.hasNext();)
                {
                    ObjectReferenceDescriptor ord = (ObjectReferenceDescriptor) it2.next();
                    ClassDescriptor oneDesc = repos.getDescriptorFor(ord.getItemClass());
                    fkTables.addAll(getFullTableNames(oneDesc, repos));
                }
            }

            List codList = desc.getCollectionDescriptors();
            for (Iterator it2 = codList.iterator(); it2.hasNext();)
            {
                CollectionDescriptor cod = (CollectionDescriptor) it2.next();
                ClassDescriptor manyDesc = repos.getDescriptorFor(cod.getItemClass());
                if (cod.isMtoNRelation())
                {
                    HashSet fkTables = getFKTablesFor(cod.getIndirectionTable());
                    fkTables.addAll(getFullTableNames(desc, repos));
                    fkTables.addAll(getFullTableNames(manyDesc, repos));
                }
                else
                {
                    HashSet manyTableNames = getFullTableNames(manyDesc, repos);
                    for (Iterator it3 = manyTableNames.iterator(); it3.hasNext();)
                    {
                        HashSet fkTables = getFKTablesFor((String) it3.next());
                        fkTables.addAll(getFullTableNames(desc, repos));
                    }
                }
            }
        }
        _pbkeyToFKInfo.put(broker.getPBKey(), _fkInfo);
    }

    private HashSet getFKTablesFor(String tableName)
    {
        HashSet fkTables = (HashSet) _fkInfo.get(tableName);

        if (fkTables == null)
        {
            fkTables = new HashSet();
            _fkInfo.put(tableName, fkTables);
        }
        return fkTables;
    }

    private HashSet getFullTableNames(ClassDescriptor desc, DescriptorRepository repos)
    {
        String tableName;
        HashSet tableNamesSet = new HashSet();
        Collection extents = desc.getExtentClasses();

        tableName = desc.getFullTableName();
        if (tableName != null)
        {
            tableNamesSet.add(tableName);
        }
        for (Iterator it = extents.iterator(); it.hasNext();)
        {
            Class extClass = (Class) it.next();
            ClassDescriptor extDesc = repos.getDescriptorFor(extClass);
            tableName = extDesc.getFullTableName();
            if (tableName != null)
            {
                tableNamesSet.add(tableName);
            }
        }
        return tableNamesSet;
    }

    public void setUseBatchInserts(boolean useBatchInserts)
    {
        _useBatchInserts = useBatchInserts;
    }

    /**
     * Remember the order of execution
     */
    void nextExecuted(String sql) throws SQLException
    {
        count++;

        if (_order.contains(sql))
        {
            return;
        }

        String sqlCmd = sql.substring(0, 7);
        String rest = sql.substring(sqlCmd.equals("UPDATE ") ? 7 // "UPDATE "
                : 12); // "INSERT INTO " or "DELETE FROM "
        String tableName = rest.substring(0, rest.indexOf(' '));
        HashSet fkTables = (HashSet) _fkInfo.get(tableName);

        // we should not change order of INSERT/DELETE/UPDATE
        // statements for the same table
        if (_touched.contains(tableName))
        {
            executeBatch();
        }
        if (sqlCmd.equals("INSERT "))
        {
            if (_dontInsert != null && _dontInsert.contains(tableName))
            {
                // one of the previous INSERTs contained a table
                // that references this table.
                // Let's execute that previous INSERT right now so that
                // in the future INSERTs into this table will go first
                // in the _order array.
                executeBatch();
            }
        }
        else
        //if (sqlCmd.equals("DELETE ") || sqlCmd.equals("UPDATE "))
        {
            // We process UPDATEs in the same way as DELETEs
            // because setting FK to NULL in UPDATE is equivalent
            // to DELETE from the referential integrity point of view.

            if (_deleted != null && fkTables != null)
            {
                HashSet intersection = (HashSet) _deleted.clone();

                intersection.retainAll(fkTables);
                if (!intersection.isEmpty())
                {
                    // one of the previous DELETEs contained a table
                    // that is referenced from this table.
                    // Let's execute that previous DELETE right now so that
                    // in the future DELETEs into this table will go first
                    // in the _order array.
                    executeBatch();
                }
            }
        }

        _order.add(sql);

        _touched.add(tableName);
        if (sqlCmd.equals("INSERT "))
        {
            if (fkTables != null)
            {
                if (_dontInsert == null)
                {
                    _dontInsert = new HashSet();
                }
                _dontInsert.addAll(fkTables);
            }
        }
        else if (sqlCmd.equals("DELETE "))
        {
            if (_deleted == null)
            {
                _deleted = new HashSet();
            }
            _deleted.add(tableName);
        }
    }

    /**
     * If UPDATE, INSERT or DELETE, return BatchPreparedStatement,
     * otherwise return null.
     */
    private PreparedStatement prepareBatchStatement(String sql)
    {
        String sqlCmd = sql.substring(0, 7);

        if (sqlCmd.equals("UPDATE ") || sqlCmd.equals("DELETE ") || (_useBatchInserts && sqlCmd.equals("INSERT ")))
        {
            PreparedStatement stmt = (PreparedStatement) _statements.get(sql);
            if (stmt == null)
            {
                // [olegnitz] for JDK 1.2 we need to list both PreparedStatement and Statement
                // interfaces, otherwise proxy.jar works incorrectly
                stmt = (PreparedStatement) Proxy.newProxyInstance(getClass().getClassLoader(), new Class[]{
                        PreparedStatement.class, Statement.class, BatchPreparedStatement.class},
                        new PreparedStatementInvocationHandler(this, sql, m_jcd));
                _statements.put(sql, stmt);
            }
            return stmt;
        }
        else
        {
            return null;
        }
    }

    public PreparedStatement prepareStatement(String sql) throws SQLException
    {
        PreparedStatement stmt = null;
        stmt = prepareBatchStatement(sql);

        if (stmt == null)
        {
            stmt = getDelegate().prepareStatement(sql);
        }
        return stmt;
    }

    public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency)
            throws SQLException
    {
        PreparedStatement stmt = null;
        stmt = prepareBatchStatement(sql);

        if (stmt == null)
        {
            stmt = getDelegate().prepareStatement(sql, resultSetType, resultSetConcurrency);
        }
        return stmt;
    }

    public void executeBatch() throws SQLException
    {
        BatchPreparedStatement batchStmt;
        Connection conn = getDelegate();

        try
        {
            for (Iterator it = _order.iterator(); it.hasNext();)
            {
                batchStmt = (BatchPreparedStatement) _statements.get(it.next());
                batchStmt.doExecute(conn);
            }
        }
        finally
        {
            _order.clear();

            if (_dontInsert != null)
            {
                _dontInsert.clear();
            }

            if (_deleted != null)
            {
                _deleted.clear();
            }
            _touched.clear();
            count = 0;
        }
    }

    public void executeBatchIfNecessary() throws SQLException
    {
        if (count >= MAX_COUNT)
        {
            executeBatch();
        }
    }

    public void clearBatch()
    {
        _order.clear();
        _statements.clear();

        if (_dontInsert != null)
        {
            _dontInsert.clear();
        }

        if (_deleted != null)
        {
            _deleted.clear();
        }
    }

    public void commit() throws SQLException
    {
        executeBatch();
        _statements.clear();
        getDelegate().commit();
    }

    public void rollback() throws SQLException
    {
        clearBatch();
        getDelegate().rollback();
    }
}