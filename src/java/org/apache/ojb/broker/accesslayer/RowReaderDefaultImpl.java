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

import java.lang.reflect.Method;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

import org.apache.ojb.broker.PersistenceBrokerException;
import org.apache.ojb.broker.metadata.ClassDescriptor;
import org.apache.ojb.broker.metadata.FieldDescriptor;
import org.apache.ojb.broker.util.ClassHelper;
import org.apache.ojb.broker.util.SqlHelper;

/**
 * Default implementation of the {@link RowReader} interface.
 *
 * @version $Id: RowReaderDefaultImpl.java,v 1.1 2007-08-24 22:17:30 ewestfal Exp $
 */

public class RowReaderDefaultImpl implements RowReader
{
    /**
     * Used as key in result set row map.
     */
    private static final String OJB_CONCRETE_CLASS_KEY = "ojbTemporaryNoneColumnKey";
    /**
     * represents a zero sized parameter array
     */
    private static final Object[] NO_ARGS = {};

    private ClassDescriptor m_cld;

    public RowReaderDefaultImpl(ClassDescriptor cld)
    {
        this.m_cld = cld;
    }

    /**
     * materialize a single object, described by cld,
     * from the first row of the ResultSet rs.
     * There are two possible strategies:
     * 1. The persistent class defines a public constructor with arguments matching the persistent
     * primitive attributes of the class. In this case we build an array args of arguments from rs
     * and call Constructor.newInstance(args) to build an object.
     * 2. The persistent class does not provide such a constructor, but only a public default
     * constructor. In this case we create an empty instance with Class.newInstance().
     * This empty instance is then filled by calling Field::set(obj,getObject(matchingColumn))
     * for each attribute.
     * The second strategy needs n calls to Field::set() which are much more expensive
     * than the filling of the args array in the first strategy.
     * client applications should therefore define adequate constructors to benefit from
     * performance gain of the first strategy.
     *
     * MBAIRD: The rowreader is told what type of object to materialize, so we have to trust
     * it is asked for the right type. It is possible someone marked an extent in the repository,
     * but not in java, or vice versa and this could cause problems in what is returned.
     *
     * we *have* to be able to materialize an object from a row that has a objConcreteClass, as we
     * retrieve ALL rows belonging to that table. The objects using the rowReader will make sure they
     * know what they are asking for, so we don't have to make sure a descriptor is assignable from the
     * selectClassDescriptor. This allows us to map both inherited classes and unrelated classes to the
     * same table.
     *
     */
    public Object readObjectFrom(Map row) throws PersistenceBrokerException
    {
        // allow to select a specific classdescriptor
        ClassDescriptor cld = selectClassDescriptor(row);
        return buildOrRefreshObject(row, cld, null);
    }

    /**
     * @see org.apache.ojb.broker.accesslayer.RowReader#refreshObject(Object, Map)
     */
    public void refreshObject(Object instance, Map row)
    {
        // 1. select target ClassDescriptor
        ClassDescriptor targetClassDescriptor = selectClassDescriptor(row);
        // 2. fill all scalar attributes of the existing object
        buildOrRefreshObject(row, targetClassDescriptor, instance);
    }

    /**
     * Creates an object instance according to clb, and fills its fileds width data provided by row.
     * @param row A {@link Map} contain the Object/Row mapping for the object.
     * @param targetClassDescriptor If the "ojbConcreteClass" feature was used, the target
     * {@link org.apache.ojb.broker.metadata.ClassDescriptor} could differ from the descriptor
     * this class was associated - see {@link #selectClassDescriptor}.
     * @param targetObject If 'null' a new object instance is build, else fields of object will
     * be refreshed.
     * @throws PersistenceBrokerException if there ewas an error creating the new object
     */
    protected Object buildOrRefreshObject(Map row, ClassDescriptor targetClassDescriptor, Object targetObject)
    {
        Object result = targetObject;
        FieldDescriptor fmd;
        FieldDescriptor[] fields = targetClassDescriptor.getFieldDescriptor(true);

        if(targetObject == null)
        {
            // 1. create new object instance if needed
            result = ClassHelper.buildNewObjectInstance(targetClassDescriptor);
        }

        // 2. fill all scalar attributes of the new object
        for (int i = 0; i < fields.length; i++)
        {
            fmd = fields[i];
            fmd.getPersistentField().set(result, row.get(fmd.getColumnName()));
        }

        if(targetObject == null)
        {
            // 3. for new build objects, invoke the initialization method for the class if one is provided
            Method initializationMethod = targetClassDescriptor.getInitializationMethod();
            if (initializationMethod != null)
            {
                try
                {
                    initializationMethod.invoke(result, NO_ARGS);
                }
                catch (Exception ex)
                {
                    throw new PersistenceBrokerException("Unable to invoke initialization method:" + initializationMethod.getName() + " for class:" + m_cld.getClassOfObject(), ex);
                }
            }
        }
        return result;
    }

    /**
     * materialize a single object, described by cld,
     * from the first row of the ResultSet rs.
     * There are two possible strategies:
     * 1. The persistent class defines a public constructor with arguments matching the persistent
     * primitive attributes of the class. In this case we build an array args of arguments from rs
     * and call Constructor.newInstance(args) to build an object.
     * 2. The persistent class does not provide such a constructor, but only a public default
     * constructor. In this case we create an empty instance with Class.newInstance().
     * This empty instance is then filled by calling Field::set(obj,getObject(matchingColumn))
     * for each attribute.
     * The second strategy needs n calls to Field::set() which are much more expensive
     * than the filling of the args array in the first strategy.
     * client applications should therefore define adequate constructors to benefit from
     * performance gain of the first strategy.
     *
     * @throws PersistenceBrokerException if there is an error accessing the access layer
     */
    public void readObjectArrayFrom(ResultSetAndStatement rs_stmt, Map row)
    {
        FieldDescriptor[] fields;
/*
arminw:
TODO: this feature doesn't work, so remove this in future
*/
        if (m_cld.getSuperClass() != null)
        {
            /**
             * treeder
             * append super class fields if exist
             */
            fields = m_cld.getFieldDescriptorsInHeirarchy();
        }
        else
        {
            String ojbConcreteClass = extractOjbConcreteClass(m_cld, rs_stmt.m_rs, row);
            /*
            arminw:
            if multiple classes were mapped to the same table, lookup the concrete
            class and use these fields, attach ojbConcreteClass in row map for later use
            */
            if(ojbConcreteClass != null)
            {
                ClassDescriptor cld = m_cld.getRepository().getDescriptorFor(ojbConcreteClass);
                row.put(OJB_CONCRETE_CLASS_KEY, cld.getClassOfObject());
                fields = cld.getFieldDescriptor(true);
            }
            else
            {
                String ojbClass = SqlHelper.getOjbClassName(rs_stmt.m_rs);
                if (ojbClass != null)
                {
                    ClassDescriptor cld = m_cld.getRepository().getDescriptorFor(ojbClass);
                    row.put(OJB_CONCRETE_CLASS_KEY, cld.getClassOfObject());
                    fields = cld.getFieldDescriptor(true);
                }
                else
                {
                    fields = m_cld.getFieldDescriptor(true);
                }           
            }         
        }
        readValuesFrom(rs_stmt, row, fields);
    }

    /*
     * @see RowReader#readPkValuesFrom(ResultSet, ClassDescriptor, Map)
     * @throws PersistenceBrokerException if there is an error accessing the access layer
     */
    public void readPkValuesFrom(ResultSetAndStatement rs_stmt, Map row)
    {
        String ojbClass = SqlHelper.getOjbClassName(rs_stmt.m_rs);
        ClassDescriptor cld;
        
        if (ojbClass != null)
        {
            cld = m_cld.getRepository().getDescriptorFor(ojbClass);
        }
        else
        {
            cld = m_cld;
        }

        FieldDescriptor[] pkFields = cld.getPkFields();
        readValuesFrom(rs_stmt, row, pkFields);
    }

    protected void readValuesFrom(ResultSetAndStatement rs_stmt, Map row, FieldDescriptor[] fields)
    {
        int size = fields.length;
        Object val;
        FieldDescriptor fld = null;
        try
        {
            for (int j = 0; j < size; j++)
            {
                fld = fields[j];
                if(!row.containsKey(fld.getColumnName()))
                {
                    int idx = rs_stmt.m_sql.getColumnIndex(fld);
                    val = fld.getJdbcType().getObjectFromColumn(rs_stmt.m_rs, null, fld.getColumnName(), idx);
                    row.put(fld.getColumnName(), fld.getFieldConversion().sqlToJava(val));
                }
            }
        }
        catch (SQLException t)
        {
            throw new PersistenceBrokerException("Error reading class '"
                    + (fld != null ? fld.getClassDescriptor().getClassNameOfObject() : m_cld.getClassNameOfObject())
                    + "' from result set, current read field was '"
                    + (fld != null ? fld.getPersistentField().getName() + "'" : null), t);
        }
    }

    protected String extractOjbConcreteClass(ClassDescriptor cld, ResultSet rs, Map row)
    {
        FieldDescriptor fld = m_cld.getOjbConcreteClassField();
        if (fld == null)
        {
            return null;
        }
        try
        {
            Object tmp = fld.getJdbcType().getObjectFromColumn(rs, fld.getColumnName());
            // allow field-conversion for discriminator column too
            String result = (String) fld.getFieldConversion().sqlToJava(tmp);
            result = result != null ? result.trim() : null;
            if (result == null || result.length() == 0)
            {
                throw new PersistenceBrokerException(
                        "ojbConcreteClass field for class " + cld.getClassNameOfObject()
                        + " returned null or 0-length string");
            }
            else
            {
                /*
                arminw: Make sure that we don't read discriminator field twice from the ResultSet.
                */
                row.put(fld.getColumnName(), result);
                return result;
            }
        }
        catch(SQLException e)
        {
            throw new PersistenceBrokerException("Unexpected error while try to read 'ojbConcretClass'" +
                    " field from result set using column name " + fld.getColumnName() + " main class" +
                    " was " + m_cld.getClassNameOfObject(), e);
        }
    }

    /**
     * Check if there is an attribute which tells us which concrete class is to be instantiated.
     */
    protected ClassDescriptor selectClassDescriptor(Map row) throws PersistenceBrokerException
    {
        ClassDescriptor result = m_cld;
        Class ojbConcreteClass = (Class) row.get(OJB_CONCRETE_CLASS_KEY);
        if(ojbConcreteClass != null)
        {
            result = m_cld.getRepository().getDescriptorFor(ojbConcreteClass);
            // if we can't find class-descriptor for concrete class, something wrong with mapping
            if (result == null)
            {
                throw new PersistenceBrokerException("Can't find class-descriptor for ojbConcreteClass '"
                        + ojbConcreteClass + "', the main class was " + m_cld.getClassNameOfObject());
            }
        }
        return result;
    }

    public void setClassDescriptor(ClassDescriptor cld)
    {
        this.m_cld = cld;
    }

    public ClassDescriptor getClassDescriptor()
    {
        return m_cld;
    }
}
