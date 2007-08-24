package org.apache.ojb.broker.util;

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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.iterators.ArrayIterator;
import org.apache.commons.collections.map.ReferenceIdentityMap;
import org.apache.ojb.broker.Identity;
import org.apache.ojb.broker.ManageableCollection;
import org.apache.ojb.broker.MtoNImplementor;
import org.apache.ojb.broker.OJBRuntimeException;
import org.apache.ojb.broker.PBKey;
import org.apache.ojb.broker.PersistenceBrokerException;
import org.apache.ojb.broker.accesslayer.StatementManagerIF;
import org.apache.ojb.broker.accesslayer.sql.SqlExistStatement;
import org.apache.ojb.broker.core.PersistenceBrokerImpl;
import org.apache.ojb.broker.core.ValueContainer;
import org.apache.ojb.broker.core.proxy.IndirectionHandler;
import org.apache.ojb.broker.core.proxy.ProxyHelper;
import org.apache.ojb.broker.metadata.ClassDescriptor;
import org.apache.ojb.broker.metadata.CollectionDescriptor;
import org.apache.ojb.broker.metadata.FieldDescriptor;
import org.apache.ojb.broker.metadata.FieldHelper;
import org.apache.ojb.broker.metadata.MetadataException;
import org.apache.ojb.broker.metadata.MetadataManager;
import org.apache.ojb.broker.metadata.ObjectReferenceDescriptor;
import org.apache.ojb.broker.metadata.fieldaccess.PersistentField;
import org.apache.ojb.broker.platforms.Platform;
import org.apache.ojb.broker.query.Criteria;
import org.apache.ojb.broker.query.MtoNQuery;
import org.apache.ojb.broker.query.Query;
import org.apache.ojb.broker.query.QueryByCriteria;
import org.apache.ojb.broker.query.QueryBySQL;
import org.apache.ojb.broker.query.ReportQueryByCriteria;
import org.apache.ojb.broker.query.ReportQueryByMtoNCriteria;
import org.apache.ojb.broker.util.logging.LoggerFactory;
import org.apache.ojb.broker.util.sequence.SequenceManagerException;

/**
 * This class contains helper methods primarily used by the {@link org.apache.ojb.broker.PersistenceBroker}
 * implementation (e.g. contains methods to assign the the values of 'autoincrement' fields).
 * <br/>
 * Furthermore it was used to introduce new features related to {@link org.apache.ojb.broker.PersistenceBroker} - these
 * new features and services (if they stand the test of time) will be moved to separate services in future.
 *
 * @author <a href="mailto:armin@codeAuLait.de">Armin Waibel</a>
 * @version $Id: BrokerHelper.java,v 1.1 2007-08-24 22:17:36 ewestfal Exp $
 */
public class BrokerHelper
{
    public static final String REPOSITORY_NAME_SEPARATOR = "#";
    private PersistenceBrokerImpl m_broker;

    public BrokerHelper(PersistenceBrokerImpl broker)
    {
        this.m_broker = broker;
    }

    /**
     * splits up the name string and extract db url,
     * user name and password and build a new PBKey
     * instance - the token '#' is used to separate
     * the substrings.
     * @throws PersistenceBrokerException if given name was <code>null</code>
     */
    public static PBKey extractAllTokens(String name)
    {
        if(name == null)
        {
            throw new PersistenceBrokerException("Could not extract PBKey, given argument is 'null'");
        }
        String user = null;
        String passwd = null;
        StringTokenizer tok = new StringTokenizer(name, REPOSITORY_NAME_SEPARATOR);
        String dbName = tok.nextToken();
        if(tok.hasMoreTokens())
        {
            user = tok.nextToken();
            if(user != null && user.trim().equals(""))
            {
                user = null;
            }
        }
        if(tok.hasMoreTokens())
        {
            if(user != null)
                passwd = tok.nextToken();
        }
        if(user != null && passwd == null)
        {
            passwd = "";
        }
        return new PBKey(dbName, user, passwd);
    }

    /**
     * Check if the user of the given PBKey was <code>null</code>, if so we try to
     * get user/password from the jdbc-connection-descriptor matching the given
     * PBKey.getAlias().
     */
    public static PBKey crossCheckPBKey(PBKey key)
    {
        if(key.getUser() == null)
        {
            PBKey defKey = MetadataManager.getInstance().connectionRepository().getStandardPBKeyForJcdAlias(key.getAlias());
            if(defKey != null)
            {
                return defKey;
            }
        }
        return key;
    }

    /**
     * Answer the real ClassDescriptor for anObj
     * ie. aCld may be an Interface of anObj, so the cld for anObj is returned
     */
    private ClassDescriptor getRealClassDescriptor(ClassDescriptor aCld, Object anObj)
    {
        ClassDescriptor result;

        if(aCld.getClassOfObject() == ProxyHelper.getRealClass(anObj))
        {
            result = aCld;
        }
        else
        {
            result = aCld.getRepository().getDescriptorFor(anObj.getClass());
        }

        return result;
    }

    /**
     * Returns an Array with an Objects PK VALUES if convertToSql is true, any
     * associated java-to-sql conversions are applied. If the Object is a Proxy
     * or a VirtualProxy NO conversion is necessary.
     *
     * @param objectOrProxy
     * @param convertToSql
     * @return Object[]
     * @throws PersistenceBrokerException
     */
    public ValueContainer[] getKeyValues(ClassDescriptor cld, Object objectOrProxy, boolean convertToSql) throws PersistenceBrokerException
    {
        IndirectionHandler handler = ProxyHelper.getIndirectionHandler(objectOrProxy);

        if(handler != null)
        {
            return getKeyValues(cld, handler.getIdentity(), convertToSql);  //BRJ: convert Identity
        }
        else
        {
            ClassDescriptor realCld = getRealClassDescriptor(cld, objectOrProxy);
            return getValuesForObject(realCld.getPkFields(), objectOrProxy, convertToSql);
        }
    }

    /**
     * Return primary key values of given Identity object.
     *
     * @param cld
     * @param oid
     * @return Object[]
     * @throws PersistenceBrokerException
     */
    public ValueContainer[] getKeyValues(ClassDescriptor cld, Identity oid) throws PersistenceBrokerException
    {
        return getKeyValues(cld, oid, true);
    }

    /**
     * Return key Values of an Identity
     * @param cld
     * @param oid
     * @param convertToSql
     * @return Object[]
     * @throws PersistenceBrokerException
     */
    public ValueContainer[] getKeyValues(ClassDescriptor cld, Identity oid, boolean convertToSql) throws PersistenceBrokerException
    {
        FieldDescriptor[] pkFields = cld.getPkFields();
        ValueContainer[] result = new ValueContainer[pkFields.length];
        Object[] pkValues = oid.getPrimaryKeyValues();

        try
        {
            for(int i = 0; i < result.length; i++)
            {
                FieldDescriptor fd = pkFields[i];
                Object cv = pkValues[i];
                if(convertToSql)
                {
                    // BRJ : apply type and value mapping
                    cv = fd.getFieldConversion().javaToSql(cv);
                }
                result[i] = new ValueContainer(cv, fd.getJdbcType());
            }
        }
        catch(Exception e)
        {
            throw new PersistenceBrokerException("Can't generate primary key values for given Identity " + oid, e);
        }
        return result;
    }

    /**
     * returns an Array with an Objects PK VALUES, with any java-to-sql
     * FieldConversion applied. If the Object is a Proxy or a VirtualProxy NO
     * conversion is necessary.
     *
     * @param objectOrProxy
     * @return Object[]
     * @throws PersistenceBrokerException
     */
    public ValueContainer[] getKeyValues(ClassDescriptor cld, Object objectOrProxy) throws PersistenceBrokerException
    {
        return getKeyValues(cld, objectOrProxy, true);
    }

    /**
     * Decide if the given object value represents 'null'.<br/>
     *
     * - If given value is 'null' itself, true will be returned<br/>
     *
     * - If given value is instance of Number with value 0 and the field-descriptor
     * represents a primitive field, true will be returned<br/>
     *
     * - If given value is instance of String with length 0 and the field-descriptor
     * is a primary key, true will be returned<br/>
     */
    public boolean representsNull(FieldDescriptor fld, Object aValue)
    {
        if(aValue == null) return true;

        boolean result = false;
        if(((aValue instanceof Number) && (((Number) aValue).longValue() == 0)))
        {
            Class type = fld.getPersistentField().getType();
            /*
            AnonymousPersistentFields will *always* have a null type according to the
            javadoc comments in AnonymousPersistentField.getType() and never represents
            a primitve java field with value 0, thus we return always 'false' in this case.
            (If the value object is null, the first check above return true)
            */
            if(type != null)
            {
                result = type.isPrimitive();
            }
        }
        // TODO: Do we need this check?? String could be nullified, why should we assume
        // it's 'null' on empty string?
        else if((aValue instanceof String) && (((String) aValue).length() == 0))
        {
            result = fld.isPrimaryKey();
        }
        return result;
    }

    /**
     * Detect if the given object has a PK field represents a 'null' value.
     */
    public boolean hasNullPKField(ClassDescriptor cld, Object obj)
    {
        FieldDescriptor[] fields = cld.getPkFields();
        boolean hasNull = false;
        // an unmaterialized proxy object can never have nullified PK's
        IndirectionHandler handler = ProxyHelper.getIndirectionHandler(obj);
        if(handler == null || handler.alreadyMaterialized())
        {
            if(handler != null) obj = handler.getRealSubject();
            FieldDescriptor fld;
            for(int i = 0; i < fields.length; i++)
            {
                fld = fields[i];
                hasNull = representsNull(fld, fld.getPersistentField().get(obj));
                if(hasNull) break;
            }
        }
        return hasNull;
    }

    /**
     * Set an autoincremented value in given object field that has already
     * had a field conversion run on it, if an value for the given field is
     * already set, it will be overridden - no further checks are done.
     * <p>
     * The data type of the value that is returned by this method is
     * compatible with the java-world.  The return value has <b>NOT</b>
     * been run through a field conversion and converted to a corresponding
     * sql-type.
     *
     * @return the autoincremented value set on given object
     * @throws PersistenceBrokerException if there is an erros accessing obj field values
     */
    private Object setAutoIncrementValue(FieldDescriptor fd, Object obj)
    {
        PersistentField f = fd.getPersistentField();
        try
        {
            // lookup SeqMan for a value matching db column an
            Object result = m_broker.serviceSequenceManager().getUniqueValue(fd);
            // reflect autoincrement value back into object
            f.set(obj, result);
            return result;
        }
        catch(MetadataException e)
        {
            throw new PersistenceBrokerException(
                    "Error while trying to autoincrement field " + f.getDeclaringClass() + "#" + f.getName(),
                    e);
        }
        catch(SequenceManagerException e)
        {
            throw new PersistenceBrokerException("Could not get key value", e);
        }
    }

    /**
     * Get the values of the fields for an obj
     * Autoincrement values are automatically set.
     * @param fields
     * @param obj
     * @throws PersistenceBrokerException
     */
    public ValueContainer[] getValuesForObject(FieldDescriptor[] fields, Object obj, boolean convertToSql, boolean assignAutoincrement) throws PersistenceBrokerException
    {
        ValueContainer[] result = new ValueContainer[fields.length];

        for(int i = 0; i < fields.length; i++)
        {
            FieldDescriptor fd = fields[i];
            Object cv = fd.getPersistentField().get(obj);

            /*
            handle autoincrement attributes if
            - is a autoincrement field
            - field represents a 'null' value, is nullified
            and generate a new value
            */
            if(assignAutoincrement && fd.isAutoIncrement() && representsNull(fd, cv))
            {
                /*
                setAutoIncrementValue returns a value that is
                properly typed for the java-world.  This value
                needs to be converted to it's corresponding
                sql type so that the entire result array contains
                objects that are properly typed for sql.
                */
                cv = setAutoIncrementValue(fd, obj);
            }
            if(convertToSql)
            {
                // apply type and value conversion
                cv = fd.getFieldConversion().javaToSql(cv);
            }
            // create ValueContainer
            result[i] = new ValueContainer(cv, fd.getJdbcType());
        }
        return result;
    }

    public ValueContainer[] getValuesForObject(FieldDescriptor[] fields, Object obj, boolean convertToSql) throws PersistenceBrokerException
    {
        return getValuesForObject(fields, obj, convertToSql, false);
    }

    /**
     * Returns an array containing values for all non PK field READ/WRITE attributes of the object
     * based on the specified {@link org.apache.ojb.broker.metadata.ClassDescriptor}.
     * <br/>
     * NOTE: This method doesn't do any checks on the specified {@link org.apache.ojb.broker.metadata.ClassDescriptor}
     * the caller is reponsible to pass a valid descriptor.
     *
     * @param cld The {@link org.apache.ojb.broker.metadata.ClassDescriptor} to extract the RW-fields
     * @param obj The object with target fields to extract.
     * @throws MetadataException if there is an erros accessing obj field values
     */
    public ValueContainer[] getNonKeyRwValues(ClassDescriptor cld, Object obj) throws PersistenceBrokerException
    {
        return getValuesForObject(cld.getNonPkRwFields(), obj, true);
    }

    /**
     * Returns an array containing values for all READ/WRITE attributes of the object
     * based on the specified {@link org.apache.ojb.broker.metadata.ClassDescriptor}.
     * <br/>
     * NOTE: This method doesn't do any checks on the specified {@link org.apache.ojb.broker.metadata.ClassDescriptor}
     * the caller is reponsible to pass a valid descriptor.
     *
     * @param cld The {@link org.apache.ojb.broker.metadata.ClassDescriptor} to extract the RW-fields
     * @param obj The object with target fields to extract.
     * @throws MetadataException if there is an erros accessing obj field values
     */
    public ValueContainer[] getAllRwValues(ClassDescriptor cld, Object obj) throws PersistenceBrokerException
    {
        return getValuesForObject(cld.getAllRwFields(), obj, true);
    }

    /**
     * Extract an value array of the given {@link ValueContainer} array.
     * @param containers
     * @return An object array
     */
    public Object[] extractValueArray(ValueContainer[] containers)
    {
        Object[] result = new Object[containers.length];
        for(int i = 0; i < containers.length; i++)
        {
            result[i] = containers[i].getValue();
        }
        return result;
    }

    /**
     * returns true if the primary key fields are valid for store, else false.
     * PK fields are valid if each of them is either an OJB managed
     * attribute (autoincrement or locking) or if it contains
     * a valid non-null value
     * @param fieldDescriptors the array of PK fielddescriptors
     * @param pkValues the array of PK values
     * @return boolean
     */
    public boolean assertValidPksForStore(FieldDescriptor[] fieldDescriptors, Object[] pkValues)
    {
        int fieldDescriptorSize = fieldDescriptors.length;
        for(int i = 0; i < fieldDescriptorSize; i++)
        {
            FieldDescriptor fld = fieldDescriptors[i];
            /**
             * a pk field is valid if it is either managed by OJB
             * (autoincrement or locking) or if it does contain a
             * valid non-null value.
             */
            if(!(fld.isAutoIncrement()
                    || fld.isLocking()
                    || !representsNull(fld, pkValues[i])))
            {
                return false;
            }
        }
        return true;
    }

    /**
     * returns true if the primary key fields are valid for delete, else false.
     * PK fields are valid if each of them contains a valid non-null value
     * @param cld the ClassDescriptor
     * @param obj the object
     * @return boolean
     */
    public boolean assertValidPkForDelete(ClassDescriptor cld, Object obj)
    {
        if(!ProxyHelper.isProxy(obj))
        {
            FieldDescriptor fieldDescriptors[] = cld.getPkFields();
            int fieldDescriptorSize = fieldDescriptors.length;
            for(int i = 0; i < fieldDescriptorSize; i++)
            {
                FieldDescriptor fd = fieldDescriptors[i];
                Object pkValue = fd.getPersistentField().get(obj);
                if (representsNull(fd, pkValue))
                {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Build a Count-Query based on aQuery
     * @param aQuery
     * @return The count query
     */
    public Query getCountQuery(Query aQuery)
    {
        if(aQuery instanceof QueryBySQL)
        {
            return getQueryBySqlCount((QueryBySQL) aQuery);
        }
        else if(aQuery instanceof ReportQueryByCriteria)
        {
            return getReportQueryByCriteriaCount((ReportQueryByCriteria) aQuery);
        }
        else
        {
            return getQueryByCriteriaCount((QueryByCriteria) aQuery);
        }
    }

    /**
     * Create a Count-Query for QueryBySQL
     *
     * @param aQuery
     * @return The count query
     */
    private Query getQueryBySqlCount(QueryBySQL aQuery)
    {
        String countSql = aQuery.getSql();

        int fromPos = countSql.toUpperCase().indexOf(" FROM ");
        if(fromPos >= 0)
        {
            countSql = "select count(*)" + countSql.substring(fromPos);
        }

        int orderPos = countSql.toUpperCase().indexOf(" ORDER BY ");
        if(orderPos >= 0)
        {
            countSql = countSql.substring(0, orderPos);
        }

        return new QueryBySQL(aQuery.getSearchClass(), countSql);
    }

    /**
     * Create a Count-Query for QueryByCriteria
     */
    private Query getQueryByCriteriaCount(QueryByCriteria aQuery)
    {
        Class                 searchClass = aQuery.getSearchClass();
        ReportQueryByCriteria countQuery  = null;
        Criteria              countCrit   = null;
        String[]              columns     = new String[1];

        // BRJ: copied Criteria without groupby, orderby, and prefetched relationships
        if (aQuery.getCriteria() != null)
        {
            countCrit = aQuery.getCriteria().copy(false, false, false);
        }

        if (aQuery.isDistinct())
        {
            // BRJ: Count distinct is dbms dependent
            // hsql/sapdb: select count (distinct(person_id || project_id)) from person_project
            // mysql: select count (distinct person_id,project_id) from person_project
            // [tomdz]
            // Some databases have no support for multi-column count distinct (e.g. Derby)
            // Here we use a SELECT count(*) FROM (SELECT DISTINCT ...) instead 
            //
            // concatenation of pk-columns is a simple way to obtain a single column
            // but concatenation is also dbms dependent:
            //
            // SELECT count(distinct concat(row1, row2, row3)) mysql
            // SELECT count(distinct (row1 || row2 || row3)) ansi
            // SELECT count(distinct (row1 + row2 + row3)) ms sql-server

            FieldDescriptor[] pkFields   = m_broker.getClassDescriptor(searchClass).getPkFields();
            String[]          keyColumns = new String[pkFields.length];

            if (pkFields.length > 1)
            {
                // TODO: Use ColumnName. This is a temporary solution because
                // we cannot yet resolve multiple columns in the same attribute.
                for (int idx = 0; idx < pkFields.length; idx++)
                {
                    keyColumns[idx] = pkFields[idx].getColumnName();
                }
            }
            else
            {
                for (int idx = 0; idx < pkFields.length; idx++)
                {
                    keyColumns[idx] = pkFields[idx].getAttributeName();
                }
            }
            // [tomdz]
            // TODO: Add support for databases that do not support COUNT DISTINCT over multiple columns
//            if (getPlatform().supportsMultiColumnCountDistinct())
//            {
//                columns[0] = "count(distinct " + getPlatform().concatenate(keyColumns) + ")";
//            }
//            else
//            {
//                columns = keyColumns;
//            }

            columns[0] = "count(distinct " + getPlatform().concatenate(keyColumns) + ")";
        }
        else
        {
            columns[0] = "count(*)";
        }

        // BRJ: we have to preserve indirection table !
        if (aQuery instanceof MtoNQuery)
        {
            MtoNQuery                 mnQuery       = (MtoNQuery)aQuery;
            ReportQueryByMtoNCriteria mnReportQuery = new ReportQueryByMtoNCriteria(searchClass, columns, countCrit);

            mnReportQuery.setIndirectionTable(mnQuery.getIndirectionTable());
            countQuery = mnReportQuery;
        }
        else
        {
            countQuery = new ReportQueryByCriteria(searchClass, columns, countCrit);
        }

        // BRJ: we have to preserve outer-join-settings (by André Markwalder)
        for (Iterator outerJoinPath = aQuery.getOuterJoinPaths().iterator(); outerJoinPath.hasNext();)
        {
            String path = (String) outerJoinPath.next();

            if (aQuery.isPathOuterJoin(path))
            {
                countQuery.setPathOuterJoin(path);
            }
        }

        //BRJ: add orderBy Columns asJoinAttributes
        List orderBy = aQuery.getOrderBy();

        if ((orderBy != null) && !orderBy.isEmpty())
        {
            String[] joinAttributes = new String[orderBy.size()];

            for (int idx = 0; idx < orderBy.size(); idx++)
            {
                joinAttributes[idx] = ((FieldHelper)orderBy.get(idx)).name;
            }
            countQuery.setJoinAttributes(joinAttributes);
        }

        // [tomdz]
        // TODO:
        // For those databases that do not support COUNT DISTINCT over multiple columns
        // we wrap the normal SELECT DISTINCT that we just created, into a SELECT count(*)
        // For this however we need a report query that gets its data from a sub query instead
        // of a table (target class)
//        if (aQuery.isDistinct() && !getPlatform().supportsMultiColumnCountDistinct())
//        {
//        }

        return countQuery;
    }

    /**
     * Create a Count-Query for ReportQueryByCriteria
     */
    private Query getReportQueryByCriteriaCount(ReportQueryByCriteria aQuery)
    {
        ReportQueryByCriteria countQuery = (ReportQueryByCriteria) getQueryByCriteriaCount(aQuery);

        // BRJ: keep the original columns to build the Join
        countQuery.setJoinAttributes(aQuery.getAttributes());

        // BRJ: we have to preserve groupby information
        Iterator iter = aQuery.getGroupBy().iterator();
        while(iter.hasNext())
        {
            countQuery.addGroupBy((FieldHelper) iter.next());
        }

        return countQuery;
    }

    /**
     * answer the platform
     *
     * @return the platform
     */
    private Platform getPlatform()
    {
        return m_broker.serviceSqlGenerator().getPlatform();
    }


    /*
    NOTE: use weak key references to allow reclaiming
    of no longer used ClassDescriptor instances
    */
    private Map sqlSelectMap = new ReferenceIdentityMap(ReferenceIdentityMap.WEAK, ReferenceIdentityMap.HARD);
    /**
     * TODO: This method should be moved to {@link org.apache.ojb.broker.accesslayer.JdbcAccess}
     * before 1.1 release.
     *
     * This method checks if the requested object can be
     * found in database (without object materialization).
     *
     * @param cld The {@link org.apache.ojb.broker.metadata.ClassDescriptor} of the
     * object/{@link org.apache.ojb.broker.Identity} to check.
     * @param obj The <em>object</em> to check.
     * @param oid The associated {@link org.apache.ojb.broker.Identity}.
     * {@link org.apache.ojb.broker.Identity} of the object
     * @return Return <em>true</em> if the object is already persisted, <em>false</em> if the object is transient.
     */
    public boolean doesExist(ClassDescriptor cld, Identity oid, Object obj)
    {
        boolean result = false;
        String sql = (String) sqlSelectMap.get(cld);
        if(sql == null)
        {
            sql = new SqlExistStatement(cld, LoggerFactory.getDefaultLogger()).getStatement();
            sqlSelectMap.put(cld, sql);
        }
        ValueContainer[] pkValues;
        if(oid == null)
        {
            pkValues = getKeyValues(cld, obj, true);
        }
        else
        {
            pkValues = getKeyValues(cld, oid);
        }
        StatementManagerIF sm = m_broker.serviceStatementManager();
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try
        {
            stmt = sm.getPreparedStatement(cld, sql, false, 1, false);
            sm.bindValues(stmt, pkValues, 1);
            rs = stmt.executeQuery();
            result = rs.next();
        }
        catch(SQLException e)
        {
            throw ExceptionHelper.generateException("[BrokerHelper#doesExist] Can't check if specified" +
                    " object is already persisted", e, sql, cld, pkValues, null, obj);
        }
        finally
        {
            sm.closeResources(stmt, rs);
        }

        return result;
    }

    /**
     * This method concatenate the main object with all reference
     * objects (1:1, 1:n and m:n) by hand. This method is needed when
     * in the reference metadata definitions the auto-xxx setting was disabled.
     * More info see OJB doc.
     */
    public void link(Object obj, boolean insert)
    {
        linkOrUnlink(true, obj, insert);
    }

    /**
     * Unlink all references from this object.
     * More info see OJB doc.
     * @param obj Object with reference
     */
    public void unlink(Object obj)
    {
        linkOrUnlink(false, obj, false);
    }

    private void linkOrUnlink(boolean doLink, Object obj, boolean insert)
    {
        ClassDescriptor cld = m_broker.getDescriptorRepository().getDescriptorFor(obj.getClass());

        if (cld.getObjectReferenceDescriptors().size() > 0)
        {
            // never returns null, thus we can direct call iterator
            Iterator descriptors = cld.getObjectReferenceDescriptors().iterator();
            while (descriptors.hasNext())
            {
                ObjectReferenceDescriptor ord = (ObjectReferenceDescriptor) descriptors.next();
                linkOrUnlinkOneToOne(doLink, obj, ord, insert);
            }
        }
        if (cld.getCollectionDescriptors().size() > 0)
        {
            // never returns null, thus we can direct call iterator
            Iterator descriptors = cld.getCollectionDescriptors().iterator();
            while (descriptors.hasNext())
            {
                CollectionDescriptor cod = (CollectionDescriptor) descriptors.next();
                linkOrUnlinkXToMany(doLink, obj, cod, insert);
            }
        }
    }

    /**
     * This method concatenate the main object and the specified reference
     * object (1:1 reference a referenced object, 1:n and m:n reference a
     * collection of referenced objects) by hand. This method is needed when
     * in the reference metadata definitions the auto-xxx setting was disabled.
     * More info see OJB doc.
     *
     * @param obj Object with reference
     * @param ord the ObjectReferenceDescriptor of the reference
     * @param insert flag signals insert operation
     */
    public void link(Object obj, ObjectReferenceDescriptor ord, boolean insert)
    {
       linkOrUnlink(true, obj, ord, insert);
    }

    /**
     * This method concatenate the main object and the specified reference
     * object (1:1 reference a referenced object, 1:n and m:n reference a
     * collection of referenced objects) by hand. This method is needed when
     * in the reference metadata definitions the auto-xxx setting was disabled.
     * More info see OJB doc.
     *
     * @param obj Object with reference
     * @param attributeName field name of the reference
     * @param insert flag signals insert operation
     * @return true if the specified reference was found and linking was successful
     */
    public boolean link(Object obj, String attributeName, boolean insert)
    {
       return linkOrUnlink(true, obj, attributeName, insert);
    }

    /**
     * This method concatenate the main object and the specified reference
     * object (1:1 reference a referenced object, 1:n and m:n reference a
     * collection of referenced objects) by hand. This method is needed when
     * in the reference metadata definitions the auto-xxx setting was disabled.
     * More info see OJB doc.
     *
     * @param obj Object with reference
     * @param attributeName field name of the reference
     * @param reference The referenced object
     * @param insert flag signals insert operation
     * @return true if the specified reference was found and linking was successful
     */
    public boolean link(Object obj, String attributeName, Object reference, boolean insert)
    {
        ClassDescriptor cld = m_broker.getDescriptorRepository().getDescriptorFor(ProxyHelper.getRealClass(obj));
        ObjectReferenceDescriptor ord;
        boolean match = false;
        // first look for reference then for collection
        ord = cld.getObjectReferenceDescriptorByName(attributeName);
        if (ord != null)
        {
            linkOrUnlinkOneToOne(true, obj, ord, insert);
            match = true;
        }
        else
        {
            CollectionDescriptor cod = cld.getCollectionDescriptorByName(attributeName);
            if (cod != null)
            {
                linkOrUnlinkXToMany(true, obj, cod, insert);
                match = true;
            }
        }
        return match;
    }

    /**
     * Unlink the specified reference object.
     * More info see OJB doc.
     * @param source The source object with the specified reference field.
     * @param attributeName The field name of the reference to unlink.
     * @param target The referenced object to unlink.
     */
    public boolean unlink(Object source, String attributeName, Object target)
    {
        return linkOrUnlink(false, source, attributeName, false);
    }

    /**
     * Unlink all referenced objects of the specified field.
     * More info see OJB doc.
     * @param source The source object with the specified reference.
     * @param attributeName The field name of the reference to unlink.
     */
    public boolean unlink(Object source, String attributeName)
    {
        return linkOrUnlink(false, source, attributeName, false);
    }

    /**
     * Unlink the specified reference from this object.
     * More info see OJB doc.
     *
     * @param obj Object with reference
     * @param ord the ObjectReferenceDescriptor of the reference
     * @param insert flag signals insert operation
     */
    public void unlink(Object obj, ObjectReferenceDescriptor ord, boolean insert)
    {
       linkOrUnlink(false, obj, ord, insert);
    }

    private boolean linkOrUnlink(boolean doLink, Object obj, String attributeName, boolean insert)
    {
        boolean match = false;
        ClassDescriptor cld = m_broker.getDescriptorRepository().getDescriptorFor(ProxyHelper.getRealClass(obj));
        ObjectReferenceDescriptor ord;

        // first look for reference then for collection
        ord = cld.getObjectReferenceDescriptorByName(attributeName);
        if (ord != null)
        {
            linkOrUnlinkOneToOne(doLink, obj, ord, insert);
            match = true;
        }
        else
        {
            CollectionDescriptor cod = cld.getCollectionDescriptorByName(attributeName);
            if (cod != null)
            {
                linkOrUnlinkXToMany(doLink, obj, cod, insert);
                match = true;
            }
        }

        return match;
    }

    private void linkOrUnlink(boolean doLink, Object obj, ObjectReferenceDescriptor ord, boolean insert)
    {
        if (ord instanceof CollectionDescriptor)
        {
            linkOrUnlinkXToMany(doLink, obj, (CollectionDescriptor) ord, insert);
        }
        else
        {
            linkOrUnlinkOneToOne(doLink, obj, ord, insert);
        }
    }

    private void linkOrUnlinkXToMany(boolean doLink, Object obj, CollectionDescriptor cod, boolean insert)
    {
        if (doLink)
        {
            if (cod.isMtoNRelation())
            {
                m_broker.linkMtoN(obj, cod, insert);
            }
            else
            {
                m_broker.linkOneToMany(obj, cod, insert);
            }
        }
        else
        {
            m_broker.unlinkXtoN(obj, cod);
        }
    }

    private void linkOrUnlinkOneToOne(boolean doLink, Object obj, ObjectReferenceDescriptor ord, boolean insert)
    {
        /*
        arminw: we need the class-descriptor where the reference is declared, thus we ask the
        reference-descriptor for this, instead of using the class-descriptor of the specified
        object. If the reference was declared within an interface (should never happen) we
        only can use the descriptor of the real class.
        */
        ClassDescriptor cld = ord.getClassDescriptor();
        if(cld.isInterface())
        {
            cld = m_broker.getDescriptorRepository().getDescriptorFor(ProxyHelper.getRealClass(obj));
        }

        if (doLink)
        {
            m_broker.linkOneToOne(obj, cld, ord, insert);
        }
        else
        {
            m_broker.unlinkFK(obj, cld, ord);
            // in 1:1 relation we have to set relation to null
            ord.getPersistentField().set(obj, null);
        }
    }

    /**
     * Unlink a bunch of 1:n or m:n objects.
     *
     * @param source The source object with reference.
     * @param cds The {@link org.apache.ojb.broker.metadata.CollectionDescriptor} of the relation.
     * @param referencesToUnlink List of referenced objects to unlink.
     */
    public void unlink(Object source, CollectionDescriptor cds, List referencesToUnlink)
    {
        for(int i = 0; i < referencesToUnlink.size(); i++)
        {
            unlink(source, cds, referencesToUnlink.get(i));
        }
    }

    /**
     * Unlink a single 1:n or m:n object.
     *
     * @param source The source object with reference.
     * @param cds The {@link org.apache.ojb.broker.metadata.CollectionDescriptor} of the relation.
     * @param referenceToUnlink The referenced object to link.
     */
    public void unlink(Object source, CollectionDescriptor cds, Object referenceToUnlink)
    {
        if(cds.isMtoNRelation())
        {
            m_broker.deleteMtoNImplementor(new MtoNImplementor(cds, source, referenceToUnlink));
        }
        else
        {
            ClassDescriptor cld = m_broker.getClassDescriptor(referenceToUnlink.getClass());
            m_broker.unlinkFK(referenceToUnlink, cld, cds);
        }
    }

    /**
     * Link a bunch of 1:n or m:n objects.
     *
     * @param source The source object with reference.
     * @param cds The {@link org.apache.ojb.broker.metadata.CollectionDescriptor} of the relation.
     * @param referencesToLink List of referenced objects to link.
     */
    public void link(Object source, CollectionDescriptor cds, List referencesToLink)
    {
        for(int i = 0; i < referencesToLink.size(); i++)
        {
            link(source, cds, referencesToLink.get(i));
        }
    }

    /**
     * Link a single 1:n or m:n object.
     *
     * @param source The source object with the declared reference.
     * @param cds The {@link org.apache.ojb.broker.metadata.CollectionDescriptor} of the relation declared in source object.
     * @param referenceToLink The referenced object to link.
     */
    public void link(Object source, CollectionDescriptor cds, Object referenceToLink)
    {
        if(cds.isMtoNRelation())
        {
            m_broker.addMtoNImplementor(new MtoNImplementor(cds, source, referenceToLink));
        }
        else
        {
            ClassDescriptor cld = m_broker.getClassDescriptor(referenceToLink.getClass());
            m_broker.link(referenceToLink, cld, cds, source, false);
        }
    }

    /**
     * Returns an Iterator instance for {@link java.util.Collection}, object Array or
     * {@link org.apache.ojb.broker.ManageableCollection} instances.
     *
     * @param collectionOrArray a none <em>null</em> object of type {@link java.util.Collection},
     * Array or {@link org.apache.ojb.broker.ManageableCollection}.
     * @return Iterator able to handle given collection object
     */
    public static Iterator getCollectionIterator(Object collectionOrArray)
    {
        Iterator colIterator;
        if (collectionOrArray instanceof ManageableCollection)
        {
            colIterator = ((ManageableCollection) collectionOrArray).ojbIterator();
        }
        else if (collectionOrArray instanceof Collection)
        {
            colIterator = ((Collection) collectionOrArray).iterator();
        }
        else if (collectionOrArray.getClass().isArray())
        {
            colIterator = new ArrayIterator(collectionOrArray);
        }
        else
        {
            throw new OJBRuntimeException( "Given object collection of type '"
                    + (collectionOrArray != null ? collectionOrArray.getClass().toString() : "null")
                + "' can not be managed by OJB. Use Array, Collection or ManageableCollection instead!");
        }
        return colIterator;
    }

    /**
     * Returns an object array for {@link java.util.Collection}, array or
     * {@link org.apache.ojb.broker.ManageableCollection} instances.
     *
     * @param collectionOrArray a none <em>null</em> object of type {@link java.util.Collection},
     * Array or {@link org.apache.ojb.broker.ManageableCollection}.
     * @return Object array able to handle given collection or array object
     */
    public static Object[] getCollectionArray(Object collectionOrArray)
    {
        Object[] result;
        if (collectionOrArray instanceof Collection)
        {
            result = ((Collection) collectionOrArray).toArray();
        }
        else if (collectionOrArray instanceof ManageableCollection)
        {
            Collection newCol = new ArrayList();
            CollectionUtils.addAll(newCol, ((ManageableCollection) collectionOrArray).ojbIterator());
            result = newCol.toArray();
        }
        else if (collectionOrArray.getClass().isArray())
        {
            result = (Object[]) collectionOrArray;
        }
        else
        {
            throw new OJBRuntimeException( "Given object collection of type '"
                    + (collectionOrArray != null ? collectionOrArray.getClass().toString() : "null")
                + "' can not be managed by OJB. Use Array, Collection or ManageableCollection instead!");
        }
        return result;
    }

    /**
     * Returns <em>true</em> if one or more anonymous FK fields are used.
     * @param cld The {@link org.apache.ojb.broker.metadata.ClassDescriptor} of the main object.
     * @param rds The {@link org.apache.ojb.broker.metadata.ObjectReferenceDescriptor} of the referenced object.
     * @return <em>true</em> if one or more anonymous FK fields are used for specified reference.
     */
    public static boolean hasAnonymousKeyReference(ClassDescriptor cld, ObjectReferenceDescriptor rds)
    {
        boolean result = false;
        FieldDescriptor[] fkFields = rds.getForeignKeyFieldDescriptors(cld);
        for(int i = 0; i < fkFields.length; i++)
        {
            FieldDescriptor fkField = fkFields[i];
            if(fkField.isAnonymous())
            {
                result = true;
                break;
            }
        }
        return result;
    }

//    /**
//     * Use this method to extract the {@link org.apache.ojb.broker.metadata.ClassDescriptor} where
//     * the {@link org.apache.ojb.broker.metadata.ObjectReferenceDescriptor reference} is declared.
//     * It's possible that the reference is declared in a super-class.
//     * @param broker
//     * @param reference
//     * @param source
//     * @return
//     */
//    public static ClassDescriptor extractDescriptorForReference(PersistenceBroker broker, ObjectReferenceDescriptor reference, Object source)
//    {
//        /*
//        arminw: we need the class-descriptor where the reference is declared, thus we ask the
//        reference-descriptor for this, instead of using the class-descriptor of the specified
//        object. If the reference was declared within an interface (should never happen) we
//        only can use the descriptor of the real class.
//        */
//        ClassDescriptor cld = reference.getClassDescriptor();
//        if(cld.isInterface())
//        {
//            cld = broker.getDescriptorRepository().getDescriptorFor(ProxyHelper.getRealClass(source));
//        }
//        return cld;
//    }

//    /**
//     * Returns a {@link java.util.List} instance of the specified object in method argument,
//     * in which the argument must be of type {@link java.util.Collection}, array or
//     * {@link org.apache.ojb.broker.ManageableCollection}.
//     *
//     * @param collectionOrArray a none <em>null</em> object of type {@link java.util.Collection},
//     * Array or {@link org.apache.ojb.broker.ManageableCollection}.
//     * @return Object array able to handle given collection or array object
//     */
//    public static List getCollectionList(Object collectionOrArray)
//    {
//        List result = null;
//        if (collectionOrArray instanceof Collection)
//        {
//            result = ((Collection) collectionOrArray).toArray();
//        }
//        else if (collectionOrArray instanceof ManageableCollection)
//        {
//            Collection newCol = new ArrayList();
//            CollectionUtils.addAll(newCol, ((ManageableCollection) collectionOrArray).ojbIterator());
//            result = newCol.toArray();
//        }
//        else if (collectionOrArray.getClass().isArray())
//        {
//            result = (Object[]) collectionOrArray;
//        }
//        else
//        {
//            throw new OJBRuntimeException( "Given object collection of type '"
//                    + (collectionOrArray != null ? collectionOrArray.getClass().toString() : "null")
//                + "' can not be managed by OJB. Use Array, Collection or ManageableCollection instead!");
//        }
//        return result;
//    }
}
