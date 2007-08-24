package org.apache.ojb.broker.core;

/* Copyright 2003-2005 The Apache Software Foundation
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.sql.SQLException;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.ArrayUtils;
import org.apache.ojb.broker.MtoNImplementor;
import org.apache.ojb.broker.OJBRuntimeException;
import org.apache.ojb.broker.PersistenceBrokerException;
import org.apache.ojb.broker.PersistenceBrokerSQLException;
import org.apache.ojb.broker.accesslayer.ResultSetAndStatement;
import org.apache.ojb.broker.core.proxy.ProxyHelper;
import org.apache.ojb.broker.metadata.ClassDescriptor;
import org.apache.ojb.broker.metadata.CollectionDescriptor;
import org.apache.ojb.broker.metadata.DescriptorRepository;
import org.apache.ojb.broker.metadata.FieldDescriptor;
import org.apache.ojb.broker.metadata.JdbcType;
import org.apache.ojb.broker.query.Query;
import org.apache.ojb.broker.util.logging.Logger;
import org.apache.ojb.broker.util.logging.LoggerFactory;

/**
 * Manage all stuff related to non-decomposed M:N association.
 *
 * @author <a href="mailto:thma@apache.org">Thomas Mahler<a>
 * @author <a href="mailto:leandro@ibnetwork.com.br">Leandro Rodrigo Saad Cruz<a>
 * @author <a href="mailto:mattbaird@yahoo.com">Matthew Baird<a>
 * @author <a href="mailto:jbraeuchi@hotmail.com">Jakob Braeuchi</a>
 * @author <a href="mailto:armin@codeAuLait.de">Armin Waibel</a>
 * @version $Id: MtoNBroker.java,v 1.1 2007-08-24 22:17:35 ewestfal Exp $
 */
public class MtoNBroker
{
    private Logger log = LoggerFactory.getLogger(MtoNBroker.class);

    private PersistenceBrokerImpl pb;
    /**
     * Used to store {@link GenericObject} while transaction running, used as
     * workaround for m:n insert problem.
     * TODO: find better solution for m:n handling
     */
    private List tempObjects = new ArrayList();

    public MtoNBroker(final PersistenceBrokerImpl broker)
    {
        this.pb = broker;
    }

    public void reset()
    {
        tempObjects.clear();
    }

    /**
     * Stores new values of a M:N association in a indirection table.
     *
     * @param cod        The {@link org.apache.ojb.broker.metadata.CollectionDescriptor} for the m:n relation
     * @param realObject The real object
     * @param otherObj   The referenced object
     * @param mnKeys     The all {@link org.apache.ojb.broker.core.MtoNBroker.Key} matching the real object
     */
    public void storeMtoNImplementor(CollectionDescriptor cod, Object realObject, Object otherObj, Collection mnKeys)
    {
        ClassDescriptor cld = pb.getDescriptorRepository().getDescriptorFor(realObject.getClass());
        ValueContainer[] pkValues = pb.serviceBrokerHelper().getKeyValues(cld, realObject);
        String[] pkColumns = cod.getFksToThisClass();

        ClassDescriptor otherCld = pb.getDescriptorRepository().getDescriptorFor(ProxyHelper.getRealClass(otherObj));
        ValueContainer[] otherPkValues = pb.serviceBrokerHelper().getKeyValues(otherCld, otherObj);

        String[] otherPkColumns = cod.getFksToItemClass();
        String table = cod.getIndirectionTable();
        MtoNBroker.Key key = new MtoNBroker.Key(otherPkValues);

        if(mnKeys.contains(key))
        {
            return;
        }

        /*
        fix for OJB-76, composite M & N keys that have some fields common
        find the "shared" indirection table columns, values and remove these from m- or n- side
        */
        for(int i = 0; i < otherPkColumns.length; i++)
        {
            int index = ArrayUtils.indexOf(pkColumns, otherPkColumns[i]);
            if(index != -1)
            {
                // shared indirection table column found, remove this column from one side
                pkColumns = (String[]) ArrayUtils.remove(pkColumns, index);
                // remove duplicate value too
                pkValues = (ValueContainer[]) ArrayUtils.remove(pkValues, index);
            }
        }

        String[] cols = mergeColumns(pkColumns, otherPkColumns);
        String insertStmt = pb.serviceSqlGenerator().getInsertMNStatement(table, pkColumns, otherPkColumns);
        ValueContainer[] values = mergeContainer(pkValues, otherPkValues);
        GenericObject gObj = new GenericObject(table, cols, values);
        if(! tempObjects.contains(gObj))
        {
            pb.serviceJdbcAccess().executeUpdateSQL(insertStmt, cld, pkValues, otherPkValues);
            tempObjects.add(gObj);
        }
    }

    /**
     * get a Collection of Keys of already existing m:n rows
     *
     * @param cod
     * @param obj
     * @return Collection of Key
     */
    public List getMtoNImplementor(CollectionDescriptor cod, Object obj)
    {
        ResultSetAndStatement rs = null;
        ArrayList result = new ArrayList();
        ClassDescriptor cld = pb.getDescriptorRepository().getDescriptorFor(obj.getClass());
        ValueContainer[] pkValues = pb.serviceBrokerHelper().getKeyValues(cld, obj);
        String[] pkColumns = cod.getFksToThisClass();
        String[] fkColumns = cod.getFksToItemClass();
        String table = cod.getIndirectionTable();

        String selectStmt = pb.serviceSqlGenerator().getSelectMNStatement(table, fkColumns, pkColumns);

        ClassDescriptor itemCLD = pb.getDescriptorRepository().getDescriptorFor(cod.getItemClass());
        Collection extents = pb.getDescriptorRepository().getAllConcreteSubclassDescriptors(itemCLD);
        if(extents.size() > 0)
        {
            itemCLD = (ClassDescriptor) extents.iterator().next();
        }
        FieldDescriptor[] itemClassPKFields = itemCLD.getPkFields();
        if(itemClassPKFields.length != fkColumns.length)
        {
            throw new PersistenceBrokerException("All pk fields of the element-class need to" +
                    " be declared in the indirection table. Element class is "
                    + itemCLD.getClassNameOfObject() + " with " + itemClassPKFields.length + " pk-fields." +
                    " Declared 'fk-pointing-to-element-class' elements in collection-descriptor are"
                    + fkColumns.length);
        }
        try
        {
            rs = pb.serviceJdbcAccess().executeSQL(selectStmt, cld, pkValues, Query.NOT_SCROLLABLE);
            while(rs.m_rs.next())
            {
                ValueContainer[] row = new ValueContainer[fkColumns.length];
                for(int i = 0; i < row.length; i++)
                {
                    row[i] = new ValueContainer(rs.m_rs.getObject(i + 1), itemClassPKFields[i].getJdbcType());
                }
                result.add(new MtoNBroker.Key(row));
            }
        }
        catch(PersistenceBrokerException e)
        {
            throw e;
        }
        catch(SQLException e)
        {
            throw new PersistenceBrokerSQLException(e);
        }
        finally
        {
            if(rs != null) rs.close();
        }
        return result;
    }

    /**
     * delete all rows from m:n table belonging to obj
     *
     * @param cod
     * @param obj
     */
    public void deleteMtoNImplementor(CollectionDescriptor cod, Object obj)
    {
        ClassDescriptor cld = pb.getDescriptorRepository().getDescriptorFor(obj.getClass());
        ValueContainer[] pkValues = pb.serviceBrokerHelper().getKeyValues(cld, obj);
        String[] pkColumns = cod.getFksToThisClass();
        String table = cod.getIndirectionTable();
        String deleteStmt = pb.serviceSqlGenerator().getDeleteMNStatement(table, pkColumns, null);
        pb.serviceJdbcAccess().executeUpdateSQL(deleteStmt, cld, pkValues, null);
    }

    /**
     * deletes all rows from m:n table that are not used in relatedObjects
     *
     * @param cod
     * @param obj
     * @param collectionIterator
     * @param mnKeys
     */
    public void deleteMtoNImplementor(CollectionDescriptor cod, Object obj, Iterator collectionIterator, Collection mnKeys)
    {
        if(mnKeys.isEmpty() || collectionIterator == null)
        {
            return;
        }
        List workList = new ArrayList(mnKeys);
        MtoNBroker.Key relatedObjKeys;
        ClassDescriptor relatedCld = pb.getDescriptorRepository().getDescriptorFor(cod.getItemClass());
        Object relatedObj;

        // remove keys of relatedObject from the existing m:n rows in workList
        while(collectionIterator.hasNext())
        {
            relatedObj = collectionIterator.next();
            relatedObjKeys = new MtoNBroker.Key(pb.serviceBrokerHelper().getKeyValues(relatedCld, relatedObj, true));
            workList.remove(relatedObjKeys);
        }

        // delete all remaining keys in workList
        ClassDescriptor cld = pb.getDescriptorRepository().getDescriptorFor(obj.getClass());
        ValueContainer[] pkValues = pb.serviceBrokerHelper().getKeyValues(cld, obj);

        String[] pkColumns = cod.getFksToThisClass();
        String[] fkColumns = cod.getFksToItemClass();
        String table = cod.getIndirectionTable();
        String deleteStmt;

        ValueContainer[] fkValues;
        Iterator iter = workList.iterator();
        while(iter.hasNext())
        {
            fkValues = ((MtoNBroker.Key) iter.next()).m_containers;
            deleteStmt = pb.serviceSqlGenerator().getDeleteMNStatement(table, pkColumns, fkColumns);
            pb.serviceJdbcAccess().executeUpdateSQL(deleteStmt, cld, pkValues, fkValues);
        }
    }

    /**
     * @param m2n
     */
    public void storeMtoNImplementor(MtoNImplementor m2n)
    {
        if(log.isDebugEnabled()) log.debug("Storing M2N implementor [" + m2n + "]");
        insertOrDeleteMtoNImplementor(m2n, true);
    }

    /**
     * @param m2n
     */
    public void deleteMtoNImplementor(MtoNImplementor m2n)
    {
        if(log.isDebugEnabled()) log.debug("Deleting M2N implementor [" + m2n + "]");
        insertOrDeleteMtoNImplementor(m2n, false);
    }


    /**
     * @see org.apache.ojb.broker.PersistenceBroker#deleteMtoNImplementor
     */
    private void insertOrDeleteMtoNImplementor(MtoNImplementor m2nImpl, boolean insert)
            throws PersistenceBrokerException
    {
        //look for a collection descriptor on left  such as left.element-class-ref='right'
        DescriptorRepository dr = pb.getDescriptorRepository();

        Object leftObject = m2nImpl.getLeftObject();
        Class leftClass = m2nImpl.getLeftClass();
        Object rightObject = m2nImpl.getRightObject();
        Class rightClass = m2nImpl.getRightClass();

        //are written per class, maybe referencing abstract classes or interfaces
        //so let's look for collection descriptors on the left class and try to
        // handle extents on teh right class
        ClassDescriptor leftCld = dr.getDescriptorFor(leftClass);
        ClassDescriptor rightCld = dr.getDescriptorFor(rightClass);
        //Vector leftColds = leftCld.getCollectionDescriptors();
        CollectionDescriptor wanted = m2nImpl.getLeftDescriptor();

        if(leftObject == null || rightObject == null)
        {
            //TODO: to be implemented, must change MtoNImplementor
            //deleteMtoNImplementor(wanted,leftObject) || deleteMtoNImplementor(wanted,rightObject)
            log.error("Can't handle MtoNImplementor in correct way, found a 'null' object");
        }
        else
        {
            //delete only one row
            ValueContainer[] leftPkValues = pb.serviceBrokerHelper().getKeyValues(leftCld, leftObject);
            ValueContainer[] rightPkValues = pb.serviceBrokerHelper().getKeyValues(rightCld, rightObject);
            String[] pkLeftColumns = wanted.getFksToThisClass();
            String[] pkRightColumns = wanted.getFksToItemClass();
            String table = wanted.getIndirectionTable();
            if(table == null) throw new PersistenceBrokerException("Can't remove MtoN implementor without an indirection table");

            String stmt;
            String[] cols = mergeColumns(pkLeftColumns, pkRightColumns);
            ValueContainer[] values = mergeContainer(leftPkValues, rightPkValues);
            if(insert)
            {
                stmt = pb.serviceSqlGenerator().getInsertMNStatement(table, pkLeftColumns, pkRightColumns);
                GenericObject gObj = new GenericObject(table, cols, values);
                if(!tempObjects.contains(gObj))
                {
                    pb.serviceJdbcAccess().executeUpdateSQL(stmt, leftCld, leftPkValues, rightPkValues);
                    tempObjects.add(gObj);
                }
            }
            else
            {
                stmt = pb.serviceSqlGenerator().getDeleteMNStatement(table, pkLeftColumns, pkRightColumns);
                pb.serviceJdbcAccess().executeUpdateSQL(stmt, leftCld, leftPkValues, rightPkValues);
            }
        }
    }

    private String[] mergeColumns(String[] first, String[] second)
    {
        String[] cols = new String[first.length + second.length];
        System.arraycopy(first, 0, cols, 0, first.length);
        System.arraycopy(second, 0, cols, first.length, second.length);
        return cols;
    }

    private ValueContainer[] mergeContainer(ValueContainer[] first, ValueContainer[] second)
    {
        ValueContainer[] values = new ValueContainer[first.length + second.length];
        System.arraycopy(first, 0, values, 0, first.length);
        System.arraycopy(second, 0, values, first.length, second.length);
        return values;
    }



// ************************************************************************
// inner class
// ************************************************************************

    /**
     * This is a helper class to model a Key of an Object
     */
    private static final class Key
    {
        final ValueContainer[] m_containers;

        Key(final ValueContainer[] containers)
        {
            m_containers = new ValueContainer[containers.length];

            for(int i = 0; i < containers.length; i++)
            {
                Object value = containers[i].getValue();
                JdbcType type = containers[i].getJdbcType();

                // BRJ:
                // convert all Numbers to Long to simplify equals
                // Long(100) is not equal to Integer(100)
                //
                // could lead to problems when Floats are used as key
                // converting to String could be a better alternative
                if(value instanceof Number)
                {
                    value = new Long(((Number) value).longValue());
                }

                m_containers[i] = new ValueContainer(value, type);
            }
        }

        public boolean equals(Object other)
        {
            if(other == this)
            {
                return true;
            }
            if(!(other instanceof Key))
            {
                return false;
            }

            Key otherKey = (Key) other;
            EqualsBuilder eb = new EqualsBuilder();

            eb.append(m_containers, otherKey.m_containers);
            return eb.isEquals();
        }

        public int hashCode()
        {
            HashCodeBuilder hb = new HashCodeBuilder();
            hb.append(m_containers);

            return hb.toHashCode();
        }
    }



    // ************************************************************************
    // inner class
    // ************************************************************************
    private static final class GenericObject
    {
         private String tablename;
        private String[] columnNames;
        private ValueContainer[] values;

        public GenericObject(String tablename, String[] columnNames, ValueContainer[] values)
        {
            this.tablename = tablename;
            this.columnNames = columnNames;
            this.values = values;
            if(values != null && columnNames.length != values.length)
            {
                throw new OJBRuntimeException("Column name array and value array have NOT same length");
            }
        }

        public boolean equals(Object obj)
        {
            if(this == obj)
            {
                return true;
            }
            boolean result = false;
            if(obj instanceof GenericObject)
            {
                GenericObject other = (GenericObject) obj;
                result = (tablename.equalsIgnoreCase(other.tablename)
                        && (columnNames != null)
                        && (other.columnNames != null)
                        && (columnNames.length == other.columnNames.length));

                if(result)
                {
                    for (int i = 0; i < columnNames.length; i++)
                    {
                        int otherIndex = other.indexForColumn(columnNames[i]);
                        if(otherIndex < 0)
                        {
                            result = false;
                            break;
                        }
                        result = values[i].equals(other.values[otherIndex]);
                        if(!result) break;
                    }
                }
            }
            return result;
        }

        int indexForColumn(String name)
        {
            int result = -1;
            for (int i = 0; i < columnNames.length; i++)
            {
                if(columnNames[i].equals(name))
                {
                    result = i;
                    break;
                }
            }
            return result;
        }

        public int hashCode()
        {
            return super.hashCode();
        }

        public ValueContainer getValueFor(String columnName)
        {
            try
            {
                return values[indexForColumn(columnName)];
            }
            catch(Exception e)
            {
                throw new OJBRuntimeException("Can't find value for column " + columnName
                        + (indexForColumn(columnName) < 0 ? ". Column name was not found" : ""), e);
            }
        }

        public String getTablename()
        {
            return tablename;
        }

        public String[] getColumnNames()
        {
            return columnNames;
        }

        public ValueContainer[] getValues()
        {
            return values;
        }

        public void setValues(ValueContainer[] values)
        {
            this.values = values;
        }

        public String toString()
        {
            return new ToStringBuilder(this)
                    .append("tableName", tablename)
                    .append("columnNames", columnNames)
                    .append("values", values)
                    .toString();
        }
    }
}
