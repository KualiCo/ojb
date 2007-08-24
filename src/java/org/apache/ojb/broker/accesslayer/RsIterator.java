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

import java.lang.ref.WeakReference;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Vector;

import org.apache.ojb.broker.Identity;
import org.apache.ojb.broker.OJBRuntimeException;
import org.apache.ojb.broker.PBLifeCycleEvent;
import org.apache.ojb.broker.PBStateEvent;
import org.apache.ojb.broker.PBStateListener;
import org.apache.ojb.broker.PersistenceBrokerException;
import org.apache.ojb.broker.PersistenceBrokerInternal;
import org.apache.ojb.broker.PersistenceBrokerSQLException;
import org.apache.ojb.broker.cache.MaterializationCache;
import org.apache.ojb.broker.cache.ObjectCacheInternal;
import org.apache.ojb.broker.core.PersistenceBrokerImpl;
import org.apache.ojb.broker.metadata.ClassDescriptor;
import org.apache.ojb.broker.metadata.DescriptorRepository;
import org.apache.ojb.broker.metadata.FieldDescriptor;
import org.apache.ojb.broker.metadata.JdbcConnectionDescriptor;
import org.apache.ojb.broker.query.Query;
import org.apache.ojb.broker.query.QueryBySQL;
import org.apache.ojb.broker.util.logging.Logger;
import org.apache.ojb.broker.util.logging.LoggerFactory;

/**
 * RsIterator can be used to iterate over a jdbc ResultSet to retrieve
 * persistent objects step-by-step and not all at once. In fact the
 * PersistenceBroker::getCollectionByQuery(...) method uses a RsIterator
 * internally to build up a Collection of objects
 *
 * <p>
 * NOTE: OJB is very strict in handling <tt>RsIterator</tt> instances. <tt>RsIterator</tt> is
 * bound very closely to the used {@link org.apache.ojb.broker.PersistenceBroker} instance.
 * Thus if you do a
 * <br/> - {@link org.apache.ojb.broker.PersistenceBroker#close}
 * <br/> - {@link org.apache.ojb.broker.PersistenceBroker#commitTransaction}
 * <br/> - {@link org.apache.ojb.broker.PersistenceBroker#abortTransaction}
 * <br/>
 * call, the current <tt>RsIterator</tt> instance resources will be cleaned up automatic
 * and invalidate current instance.
 * </p>
 *
 * <p>
 * NOTE: this code uses features that only JDBC 2.0 compliant Drivers support.
 * It will NOT work with JDBC 1.0 Drivers (e.g. SUN's JdbcOdbcDriver) If you
 * are forced to use such a driver, you can use code from the 0.1.30 release.
 * </p>
 * @author <a href="mailto:thma@apache.org">Thomas Mahler <a>
 * @author <a href="mailto:mattbaird@yahoo.com">Matthew Baird <a>- added the
 *         support for extents mapped to single table - added the .size
 *         functionality - added cursor control
 *
 * @version $Id: RsIterator.java,v 1.1 2007-08-24 22:17:30 ewestfal Exp $
 */
public class RsIterator implements OJBIterator
{
    protected Logger logger = LoggerFactory.getLogger(this.getClass());
    private static final String INFO_MSG = "Resources already cleaned up, recommend to set" +
            " this flag before first use of the iterator";
    /*
	 * arminw: to improve performance we only use this instance to fire events
	 * and set the target object on every use TODO: Find a better solution
	 */
    private PBLifeCycleEvent afterLookupEvent;
    private MaterializationCache m_cache;

    /**
     * reference to the PersistenceBroker
     */
    private PersistenceBrokerImpl m_broker;

    /**
     * the underlying resultset
     */
    private ResultSetAndStatement m_rsAndStmt;

    /**
     * the underlying query object
     */
    private RsQueryObject m_queryObject;

    /**
     * the proxy class to be used or null
     */
    private Class m_itemProxyClass;

    /**
     * the top-level class of the item objects
     */
    private Class m_itemTopLevelClass = null;

    /**
     * this container holds the values of the current ro during materialisation
     */
    private Map m_row = null;

    /**
     * flag that indicates wether hasNext on m_rs has allready been called
     */
    private boolean m_hasCalledCheck = false;

    /**
     * prefetch relationship: inBatchedMode true prevents releasing of
     * DbResources IN_LIMIT defines the max number of values of sql (IN) , -1
     * for no limits
     */
    private boolean m_inBatchedMode = false;

    /**
     * return value of the previously called hasNext from m_rs
     */
    private boolean hasNext = false;

    private boolean advancedJDBCSupport = false;
    private boolean JDBCSupportAssessed = false;
    private int m_current_row = 0;
    /**
     * Tracks whether or not the resources that are used by this class have been released.
     */
    private boolean resourcesAreReleased = false;

    /**
     * Flag that indicates if the automatic resource cleanup should be
     * done or not. Default is <tt>true</tt>.
     */
    private boolean autoRelease = true;
    private ResourceWrapper resourceListener;
    
    /** if true do not fire PBLifeCycleEvent. */
    private boolean disableLifeCycleEvents = false;

    /**
     * RsIterator constructor.
     *
     * @param queryObject query object
     * @param broker the broker we should use.
     */
    public RsIterator(RsQueryObject queryObject, final PersistenceBrokerImpl broker)
    {
        setCache(broker.getInternalCache());
        setRow(new HashMap());
        setBroker(broker);
        setQueryObject(queryObject);

        Class classToPrefetch = broker.getReferenceBroker().getClassToPrefetch();
        if ((classToPrefetch != null) && classToPrefetch.isAssignableFrom(queryObject.getClassDescriptor().getClassOfObject()))
        {
            setItemProxyClass(null);
        }
        else
        {
            setItemProxyClass(queryObject.getClassDescriptor().getProxyClass());
        }

        /*
		 * arminw: to improve performance we only use this instance to fire
		 * events and set the target object on every use TODO: Find a better
		 * solution
		 */
        setAfterLookupEvent(new PBLifeCycleEvent(getBroker(), PBLifeCycleEvent.Type.AFTER_LOOKUP));

        try
        {
            setRsAndStmt(queryObject.performQuery(broker.serviceJdbcAccess()));
            /*
             * TODO: how does prefetchRelationships handle QueryBySQL instances? Is
             * it ok to pass query object?
             */
            prefetchRelationships(queryObject.getQuery());
            if (logger.isDebugEnabled())
            {
                logger.debug("RsIterator[" + queryObject + "] initialized");
            }
        }
        catch (RuntimeException e)
        {
            autoReleaseDbResources();
            throw e;
        }

        /*
        now RsIterator instance is created, we wrap this instance with a
        PBStateListener to make sure that resources of this instance will be
        released. Add this as temporary PBStateListener.
        */
        resourceListener = new ResourceWrapper(this);
        m_broker.addListener(resourceListener);
    }

    protected Class getTopLevelClass()
    {
        if (m_itemTopLevelClass == null)
        {
            m_itemTopLevelClass = getBroker().getTopLevelClass(getQueryObject().getClassDescriptor().getClassOfObject());
        }
        return m_itemTopLevelClass;
    }

    /**
     * returns true if there are still more rows in the underlying ResultSet.
     * Returns false if ResultSet is exhausted.
     */
    public synchronized boolean hasNext()
    {
        try
        {
            if (!isHasCalledCheck())
            {
                setHasCalledCheck(true);
                setHasNext(getRsAndStmt().m_rs.next());
                if (!getHasNext())
                {
                    autoReleaseDbResources();
                }
            }
        }
        catch (Exception ex)
        {
            setHasNext(false);
            autoReleaseDbResources();
            if(ex instanceof ResourceClosedException)
            {
                throw (ResourceClosedException)ex;
            }
            if(ex instanceof SQLException)
            {
                throw new PersistenceBrokerSQLException("Calling ResultSet.next() failed", (SQLException) ex);
            }
            else
            {
               throw new PersistenceBrokerException("Can't get next row from ResultSet", ex);
            }
        }
        if (logger.isDebugEnabled())
            logger.debug("hasNext() -> " + getHasNext());

        return getHasNext();
    }

    /**
     * moves to the next row of the underlying ResultSet and returns the
     * corresponding Object materialized from this row.
     */
    public synchronized Object next() throws NoSuchElementException
    {
        try
        {
            if (!isHasCalledCheck())
            {
                hasNext();
            }
            setHasCalledCheck(false);
            if (getHasNext())
            {
                Object obj = getObjectFromResultSet();
                m_current_row++;

                // Invoke events on PersistenceBrokerAware instances and listeners
                // set target object
                if (!disableLifeCycleEvents)
                {
                    getAfterLookupEvent().setTarget(obj);
                    getBroker().fireBrokerEvent(getAfterLookupEvent());
                    getAfterLookupEvent().setTarget(null);
                }    
                return obj;
            }
            else
            {
                throw new NoSuchElementException("inner hasNext was false");
            }
        }
        catch (ResourceClosedException ex)
        {
            autoReleaseDbResources();
            throw ex;
        }
        catch (NoSuchElementException ex)
        {
            autoReleaseDbResources();
            logger.error("Error while iterate ResultSet for query " + m_queryObject, ex);
            throw new NoSuchElementException("Could not obtain next object: " + ex.getMessage());
        }
    }

    /**
     * removing is not supported
     */
    public void remove()
    {
        throw new UnsupportedOperationException("removing not supported by RsIterator");
    }

    /**
     * read all objects of this iterator. objects will be placed in cache
     */
    private Collection getOwnerObjects()
    {
        Collection owners = new Vector();
        while (hasNext())
        {
            owners.add(next());
        }
        return owners;
    }

    /**
     * prefetch defined relationships requires JDBC level 2.0, does not work
     * with Arrays
     */
    private void prefetchRelationships(Query query)
    {
        List prefetchedRel;
        Collection owners;
        String relName;
        RelationshipPrefetcher[] prefetchers;

        if (query == null || query.getPrefetchedRelationships() == null || query.getPrefetchedRelationships().isEmpty())
        {
            return;
        }

        if (!supportsAdvancedJDBCCursorControl())
        {
            logger.info("prefetching relationships requires JDBC level 2.0");
            return;
        }

        // prevent releasing of DBResources
        setInBatchedMode(true);

        prefetchedRel = query.getPrefetchedRelationships();
        prefetchers = new RelationshipPrefetcher[prefetchedRel.size()];

        // disable auto retrieve for all prefetched relationships
        for (int i = 0; i < prefetchedRel.size(); i++)
        {
            relName = (String) prefetchedRel.get(i);
            prefetchers[i] = getBroker().getRelationshipPrefetcherFactory()
                    .createRelationshipPrefetcher(getQueryObject().getClassDescriptor(), relName);
            prefetchers[i].prepareRelationshipSettings();
        }

        // materialize ALL owners of this Iterator
        owners = getOwnerObjects();

        // prefetch relationships and associate with owners
        for (int i = 0; i < prefetchedRel.size(); i++)
        {
            prefetchers[i].prefetchRelationship(owners);
        }

        // reset auto retrieve for all prefetched relationships
        for (int i = 0; i < prefetchedRel.size(); i++)
        {
            prefetchers[i].restoreRelationshipSettings();
        }

        try
        {
            getRsAndStmt().m_rs.beforeFirst(); // reposition resultset jdbc 2.0
        }
        catch (SQLException e)
        {
            logger.error("beforeFirst failed !", e);
        }

        setInBatchedMode(false);
        setHasCalledCheck(false);
    }

    /**
     * returns an Identity object representing the current resultset row
     */
    protected Identity getIdentityFromResultSet() throws PersistenceBrokerException
    {
        // fill primary key values from Resultset
        FieldDescriptor fld;
        FieldDescriptor[] pkFields = getQueryObject().getClassDescriptor().getPkFields();
        Object[] pkValues = new Object[pkFields.length];

        for (int i = 0; i < pkFields.length; i++)
        {
            fld = pkFields[i];
            pkValues[i] = getRow().get(fld.getColumnName());
        }

        // return identity object build up from primary keys
        return getBroker().serviceIdentity().buildIdentity(
                getQueryObject().getClassDescriptor().getClassOfObject(), getTopLevelClass(), pkValues);
    }

    /**
     * returns a fully materialized Object from the current row of the
     * underlying resultset. Works as follows: - read Identity from the primary
     * key values of current row - check if Object is in cache - return cached
     * object or read it from current row and put it in cache
     */
    protected Object getObjectFromResultSet() throws PersistenceBrokerException
    {
        getRow().clear();
        /**
         * MBAIRD if a proxy is to be used, return a proxy instance and dont
         * perfom a full materialization. NOTE: Potential problem here with
         * multi-mapped table. The itemProxyClass is for the m_cld
         * classdescriptor. The object you are materializing might not be of
         * that type, it could be a subclass. We should get the concrete class
         * type out of the resultset then check the proxy from that.
         * itemProxyClass should NOT be a member variable.
         */

        RowReader rowReader = getQueryObject().getClassDescriptor().getRowReader();
        // in any case we need the PK values of result set row
        // provide m_row with primary key data of current row
        rowReader.readPkValuesFrom(getRsAndStmt(), getRow());

        if (getItemProxyClass() != null)
        {
            // assert: m_row is filled with primary key values from db
            return getProxyFromResultSet();
        }
        else
        {
            // 1.read Identity
            Identity oid = getIdentityFromResultSet();
            Object result;

            // 2. check if Object is in cache. if so return cached version.
            result = getCache().lookup(oid);
            if (result == null)
            {

                // map all field values from the current result set
                rowReader.readObjectArrayFrom(getRsAndStmt(), getRow());
                // 3. If Object is not in cache
                // materialize Object with primitive attributes filled from current row
                result = rowReader.readObjectFrom(getRow());
                // result may still be null!
                if (result != null)
                {
                    /*
					 * synchronize on result so the ODMG-layer can take a
					 * snapshot only of fully cached (i.e. with all references +
					 * collections) objects
					 */
                    synchronized (result)
                    {
                        getCache().enableMaterializationCache();
                        try
                        {
                            getCache().doInternalCache(oid, result, ObjectCacheInternal.TYPE_NEW_MATERIALIZED);
                            /**
                             * MBAIRD if you have multiple classes mapped to a
                             * table, and you query on the base class you could get
                             * back NON base class objects, so we shouldn't pass
                             * m_cld, but rather the class descriptor for the
                             * actual class.
                             */
                            // fill reference and collection attributes
                            ClassDescriptor cld = getBroker().getClassDescriptor(result.getClass());
                            // don't force loading of reference
                            final boolean unforced = false;
                            // Maps ReferenceDescriptors to HashSets of owners
                            getBroker().getReferenceBroker().retrieveReferences(result, cld, unforced);
                            getBroker().getReferenceBroker().retrieveCollections(result, cld, unforced);
                            getCache().disableMaterializationCache();
                        }
                        catch(RuntimeException e)
                        {
                            // catch runtime exc. to guarantee clearing of internal buffer on failure
                            getCache().doLocalClear();
                            throw e;
                        }
                    }
                }
            }
            else // Object is in cache
            {
                ClassDescriptor cld = getBroker().getClassDescriptor(result.getClass());
                // if refresh is required, read all values from the result set and
                // update the cache instance from the db
                if (cld.isAlwaysRefresh())
                {
                    // map all field values from the current result set
                    rowReader.readObjectArrayFrom(getRsAndStmt(), getRow());
                    rowReader.refreshObject(result, getRow());
                }
                getBroker().checkRefreshRelationships(result, oid, cld);
            }

            return result;
        }
    }

    /**
     * Reads primary key information from current RS row and generates a
     *
     * corresponding Identity, and returns a proxy from the Identity.
     *
     * @throws PersistenceBrokerException
     *             if there was an error creating the proxy class
     */
    protected Object getProxyFromResultSet() throws PersistenceBrokerException
    {
        // 1. get Identity of current row:
        Identity oid = getIdentityFromResultSet();

        // 2. return a Proxy instance:
        return getBroker().createProxy(getItemProxyClass(), oid);
    }

    /**
     * with a new batch of JDBC 3.0 drivers coming out we can't just check for
     * begins with 2, we need to check the actual version and see if it's
     * greater than or equal to 2.
     */
    private boolean supportsAdvancedJDBCCursorControl()
    {
        if (!JDBCSupportAssessed)
        {
            if (getConnectionDescriptor().getJdbcLevel() >= 2.0)
                advancedJDBCSupport = true;
            JDBCSupportAssessed = true;
        }
        return advancedJDBCSupport;
    }

    /**
     * Answer the counted size
     *
     * @return int
     */
    protected int countedSize() throws PersistenceBrokerException
    {
        Query countQuery = getBroker().serviceBrokerHelper().getCountQuery(getQueryObject().getQuery());
        ResultSetAndStatement rsStmt;
        ClassDescriptor cld = getQueryObject().getClassDescriptor();
        int count = 0;

        // BRJ: do not use broker.getCount() because it's extent-aware
        // the count we need here must not include extents !
        if (countQuery instanceof QueryBySQL)
        {
            String countSql = ((QueryBySQL) countQuery).getSql();
            rsStmt = getBroker().serviceJdbcAccess().executeSQL(countSql, cld, Query.NOT_SCROLLABLE);
        }
        else
        {
            rsStmt = getBroker().serviceJdbcAccess().executeQuery(countQuery, cld);
        }

        try
        {
            if (rsStmt.m_rs.next())
            {
                count = rsStmt.m_rs.getInt(1);
            }
        }
        catch (SQLException e)
        {
            throw new PersistenceBrokerException(e);
        }
        finally
        {
            rsStmt.close();
        }

        return count;
    }

    /**
     * @return the size of the iterator, aka the number of rows in this
     *         iterator.
     */
    public int size() throws PersistenceBrokerException
    {
        int retval = 0; // default size is 0;
        boolean forwardOnly = true;
        try
        {
            forwardOnly = getRsAndStmt().m_stmt.getResultSetType() == ResultSet.TYPE_FORWARD_ONLY;
        }
        catch (SQLException e)
        {
            //ignore it
        }
        if (!supportsAdvancedJDBCCursorControl()
                || getBroker().serviceConnectionManager().getSupportedPlatform().useCountForResultsetSize()
                || forwardOnly)
        {
            /**
             * MBAIRD: doesn't support the .last .getRow method, use the
             * .getCount on the persistenceBroker which executes a count(*)
             * query.
             */
            if (logger.isDebugEnabled())
                logger.debug("Executing count(*) to get size()");
            retval = countedSize();
        }
        else
        {
            /**
             * Use the .last .getRow method of finding size. The reason for
             * supplying an alternative method is effeciency, some driver/db
             * combos are a lot more efficient at just moving the cursor and
             * returning the row in a real (not -1) number.
             */
            int whereIAm; // first
            try
            {
                if (getRsAndStmt().m_rs != null)
                {
                    whereIAm = getRsAndStmt().m_rs.getRow();
                    if (getRsAndStmt().m_rs.last())
                    {
                        retval = getRsAndStmt().m_rs.getRow();
                    }
                    else
                    {
                        retval = 0;
                    }
                    // go back from whence I came.
                    if (whereIAm > 0)
                    {
                        getRsAndStmt().m_rs.absolute(whereIAm);
                    }
                    else
                    {
                        getRsAndStmt().m_rs.beforeFirst();
                    }
                }
            }
            catch (SQLException se)
            {
                advancedJDBCSupport = false;
            }
        }
        return retval;
    }

    /* (non-Javadoc)
     * @see org.apache.ojb.broker.accesslayer.OJBIterator#fullSize()
     */
    public int fullSize() throws PersistenceBrokerException
    {
        return size();
    }

    /**
     * Moves the cursor to the given row number in the iterator. If the row
     * number is positive, the cursor moves to the given row number with
     * respect to the beginning of the iterator. The first row is row 1, the
     * second is row 2, and so on.
     *
     * @param row  the row to move to in this iterator, by absolute number
     */
    public boolean absolute(int row) throws PersistenceBrokerException
    {
        boolean retval;
        if (supportsAdvancedJDBCCursorControl())
        {
            retval = absoluteAdvanced(row);
        }
        else
        {
            retval = absoluteBasic(row);
        }
        return retval;
    }

    /**
     * absolute for basicJDBCSupport
     * @param row
     */
    private boolean absoluteBasic(int row)
    {
        boolean retval = false;
        
        if (row > m_current_row)
        {
            try
            {
                while (m_current_row < row && getRsAndStmt().m_rs.next())
                {
                    m_current_row++;
                }
                if (m_current_row == row)
                {
                    retval = true;
                }
                else
                {
                    setHasCalledCheck(true);
                    setHasNext(false);
                    retval = false;
                    autoReleaseDbResources();
                }
            }
            catch (Exception ex)
            {
                setHasCalledCheck(true);
                setHasNext(false);
                retval = false;
            }
        }
        else
        {
            logger.info("Your driver does not support advanced JDBC Functionality, " +
                    "you cannot call absolute() with a position < current");
        }
        return retval;
    }

    /**
     * absolute for advancedJDBCSupport
     * @param row
     */
    private boolean absoluteAdvanced(int row)
    {
        boolean retval = false;
        
        try
        {
            if (getRsAndStmt().m_rs != null)
            {
                if (row == 0)
                {
                    getRsAndStmt().m_rs.beforeFirst();
                }
                else
                {
                    retval = getRsAndStmt().m_rs.absolute(row);                        
                }
                m_current_row = row;
                setHasCalledCheck(false);
            }
        }
        catch (SQLException e)
        {
            advancedJDBCSupport = false;
        }
        return retval;
    }

    /**
     * Moves the cursor a relative number of rows, either positive or negative.
     * Attempting to move beyond the first/last row in the iterator positions
     * the cursor before/after the the first/last row. Calling relative(0) is
     * valid, but does not change the cursor position.
     *
     * @param row
     *            the row to move to in this iterator, by relative number
     */
    public boolean relative(int row) throws PersistenceBrokerException
    {
        boolean retval = false;
        if (supportsAdvancedJDBCCursorControl())
        {
            try
            {
                if (getRsAndStmt().m_rs != null)
                {
                    retval = getRsAndStmt().m_rs.relative(row);
                    m_current_row += row;
                }
            }
            catch (SQLException e)
            {
                advancedJDBCSupport = false;
            }
        }
        else
        {
            if (row >= 0)
            {
                return absolute(m_current_row + row);
            }
            else
            {
                logger.info("Your driver does not support advanced JDBC Functionality, you cannot call relative() with a negative value");
            }
        }
        return retval;
    }

    /**
     * Release all internally used Database resources of the iterator. Clients
     * must call this methods explicitely if the iterator is not exhausted by
     * the client application. If the Iterator is exhauseted this method will
     * be called implicitely.
     */
    public void releaseDbResources()
    {
        release(true);
    }

    void release(boolean removeResourceListener)
    {
        if (!isInBatchedMode()) // resources are reused in batched mode
        {
            // If we haven't released resources yet, then do so.
            if (!this.resourcesAreReleased)
            {
                // remove the resource listener
                if(removeResourceListener && resourceListener != null)
                {
                    try
                    {
                        /*
                        when RsIterator is closed, the resource listener
                        was no longer needed to listen on PB events for clean up.
                        */
                        m_broker.removeListener(resourceListener);
                        this.resourceListener = null;
                    }
                    catch(Exception e)
                    {
                        logger.error("Error when try to remove RsIterator resource listener", e);
                    }
                }
                this.resourcesAreReleased = true;
                if (m_rsAndStmt != null)
                {
                    m_rsAndStmt.close();
                    m_rsAndStmt = null;
                }
            }
        }
    }

    /**
     * Internally used by this class to close used resources
     * as soon as possible.
     */
    protected void autoReleaseDbResources()
    {
        if(autoRelease)
        {
            releaseDbResources();
        }
    }

    /**
     * Allows user to switch off/on automatic resource cleanup.
     * Set <tt>false</tt> to take responsibility of resource cleanup
     * for this class, means after use it's mandatory to call
     * {@link #releaseDbResources}.
     * <br/> By default it's <tt>true</tt> and resource cleanup is done
     * automatic.
     */
    public void setAutoRelease(boolean autoRelease)
    {
        /*
        arminw:
        this method should be declared in OJBIterator interface till
        OJB 1.1 and PersistenceBroker interface should only return
        OJBIterator instead of Iterator instances
        */
        if(resourcesAreReleased && !autoRelease)
        {
            logger.info(INFO_MSG);
        }
        this.autoRelease = autoRelease;
    }

    /**
     * Return the DescriptorRepository
     */
    protected DescriptorRepository getDescriptorRepository()
    {
        return getBroker().getDescriptorRepository();
    }

    protected JdbcConnectionDescriptor getConnectionDescriptor()
    {
        return getBroker().serviceConnectionManager().getConnectionDescriptor();
    }

    /**
     * safety just in case someone leaks.
     */
    protected void finalize()
    {
        if (m_rsAndStmt != null)
        {
            logger.info("Found unclosed resources while finalize (causer class: " + this.getClass().getName() + ")" +
                    " Do automatic cleanup");
            releaseDbResources();
        }
        try
        {
            super.finalize();
        }
        catch(Throwable throwable)
        {
            throwable.printStackTrace();
        }
    }

    public String toString()
    {
        return super.toString();
    }

    /**
     * @return Returns the cld.
     */
    public ClassDescriptor getClassDescriptor()
    {
        return getQueryObject().getClassDescriptor();
    }

    protected void setBroker(PersistenceBrokerImpl broker)
    {
        m_broker = broker;
    }

    protected PersistenceBrokerInternal getBroker()
    {
        return m_broker;
    }

    protected void setRsAndStmt(ResultSetAndStatement rsAndStmt)
    {
        if(m_rsAndStmt != null)
        {
            throw new ResourceNotClosedException("Unclosed resources found, please release resources" +
                    " before set new ones");
        }
        resourcesAreReleased = false;
        m_rsAndStmt = rsAndStmt;
    }

    protected ResultSetAndStatement getRsAndStmt()
    {
        if(resourcesAreReleased)
        {
            throw new ResourceClosedException("Resources no longer reachable, RsIterator will be automatic" +
                    " cleaned up on PB.close/.commitTransaction/.abortTransaction");
        }
        return m_rsAndStmt;
    }

    protected void setQueryObject(RsQueryObject queryObject)
    {
        this.m_queryObject = queryObject;
    }

    protected RsQueryObject getQueryObject()
    {
        return m_queryObject;
    }

    protected void setItemProxyClass(Class itemProxyClass)
    {
        this.m_itemProxyClass = itemProxyClass;
    }

    protected Class getItemProxyClass()
    {
        return m_itemProxyClass;
    }

    protected void setRow(Map row)
    {
        m_row = row;
    }

    protected Map getRow()
    {
        return m_row;
    }

    protected void setCache(MaterializationCache cache)
    {
        this.m_cache = cache;
    }

    protected MaterializationCache getCache()
    {
        return m_cache;
    }

    protected void setAfterLookupEvent(PBLifeCycleEvent afterLookupEvent)
    {
        this.afterLookupEvent = afterLookupEvent;
    }

    protected PBLifeCycleEvent getAfterLookupEvent()
    {
        return afterLookupEvent;
    }

    protected void setHasCalledCheck(boolean hasCalledCheck)
    {
        this.m_hasCalledCheck = hasCalledCheck;
    }

    protected boolean isHasCalledCheck()
    {
        return m_hasCalledCheck;
    }

    protected void setHasNext(boolean hasNext)
    {
        this.hasNext = hasNext;
    }

    protected boolean getHasNext()
    {
        return hasNext;
    }

    protected void setInBatchedMode(boolean inBatchedMode)
    {
        this.m_inBatchedMode = inBatchedMode;
    }

    protected boolean isInBatchedMode()
    {
        return m_inBatchedMode;
    }

    //***********************************************************
    // inner classes
    //***********************************************************
    /**
     * Wraps a {@link RsIterator} instance as {@link WeakReference}.
     */
    public static class ResourceWrapper implements PBStateListener
    {
        /*
        arminw:
        we do register a PBStateListener to PB instance
        to make sure that this instance will be cleaned up at PB.close() call.
        If PB was in tx, we cleanup resources on PB.commit/abort, because
        commit/abort close the current used connection and all Statement/ResultSet
        instances will become invalid.
        */
        WeakReference ref;

        public ResourceWrapper(RsIterator rs)
        {
            ref = new WeakReference(rs);
        }

        public void beforeClose(PBStateEvent event)
        {
            if(ref != null)
            {
                RsIterator rs = (RsIterator) ref.get();
                if(rs != null) rs.release(false);
                ref = null;
            }
        }

        public void beforeRollback(PBStateEvent event)
        {
            if(ref != null)
            {
                RsIterator rs = (RsIterator) ref.get();
                if(rs != null) rs.release(false);
                ref = null;
            }
        }

        public void beforeCommit(PBStateEvent event)
        {
            if(ref != null)
            {
                RsIterator rs = (RsIterator) ref.get();
                if(rs != null) rs.release(false);
                ref = null;
            }
        }

        public void afterCommit(PBStateEvent event)
        {
            //do nothing
        }
        public void afterRollback(PBStateEvent event)
        {
            //do nothing
        }
        public void afterBegin(PBStateEvent event)
        {
            //do nothing
        }
        public void beforeBegin(PBStateEvent event)
        {
            //do nothing
        }
        public void afterOpen(PBStateEvent event)
        {
            //do nothing
        }
    }

    public static class ResourceClosedException extends OJBRuntimeException
    {
        public ResourceClosedException(String msg)
        {
            super(msg);
        }

        public ResourceClosedException(String msg, Throwable cause)
        {
            super(msg, cause);
        }
    }

    public static class ResourceNotClosedException extends OJBRuntimeException
    {
        public ResourceNotClosedException(String msg)
        {
            super(msg);
        }

        public ResourceNotClosedException(String msg, Throwable cause)
        {
            super(msg, cause);
        }
    }

    /**
     * @see org.apache.ojb.broker.accesslayer.OJBIterator#disableLifeCycleEvents()
     */
    public void disableLifeCycleEvents()
    {
        disableLifeCycleEvents = true;
    }
}
