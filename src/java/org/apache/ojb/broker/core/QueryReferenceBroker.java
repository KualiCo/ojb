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

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.ojb.broker.Identity;
import org.apache.ojb.broker.ManageableCollection;
import org.apache.ojb.broker.PBLifeCycleEvent;
import org.apache.ojb.broker.PersistenceBrokerException;
import org.apache.ojb.broker.accesslayer.OJBIterator;
import org.apache.ojb.broker.accesslayer.PagingIterator;
import org.apache.ojb.broker.accesslayer.PlainPrefetcher;
import org.apache.ojb.broker.accesslayer.RelationshipPrefetcher;
import org.apache.ojb.broker.core.proxy.CollectionProxyDefaultImpl;
import org.apache.ojb.broker.core.proxy.CollectionProxyListener;
import org.apache.ojb.broker.core.proxy.IndirectionHandler;
import org.apache.ojb.broker.core.proxy.MaterializationListener;
import org.apache.ojb.broker.core.proxy.ProxyHelper;
import org.apache.ojb.broker.metadata.ClassDescriptor;
import org.apache.ojb.broker.metadata.ClassNotPersistenceCapableException;
import org.apache.ojb.broker.metadata.CollectionDescriptor;
import org.apache.ojb.broker.metadata.FieldDescriptor;
import org.apache.ojb.broker.metadata.FieldHelper;
import org.apache.ojb.broker.metadata.MetadataException;
import org.apache.ojb.broker.metadata.ObjectReferenceDescriptor;
import org.apache.ojb.broker.metadata.fieldaccess.PersistentField;
import org.apache.ojb.broker.query.Criteria;
import org.apache.ojb.broker.query.Query;
import org.apache.ojb.broker.query.QueryByCriteria;
import org.apache.ojb.broker.query.QueryFactory;
import org.apache.ojb.broker.util.BrokerHelper;
import org.apache.ojb.broker.util.collections.ManageableArrayList;
import org.apache.ojb.broker.util.collections.ManageableHashSet;
import org.apache.ojb.broker.util.collections.RemovalAwareCollection;
import org.apache.ojb.broker.util.collections.RemovalAwareList;
import org.apache.ojb.broker.util.collections.RemovalAwareSet;
import org.apache.ojb.broker.util.logging.Logger;
import org.apache.ojb.broker.util.logging.LoggerFactory;

/**
 * Encapsulates 1:1 and 1:n references and collection references stuff.
 *
 * TODO: Should we made this class independend from PB implementation class
 * and only use PB interface methods?
 *
 * @author <a href="mailto:armin@codeAuLait.de">Armin Waibel</a>
 * @version $Id: QueryReferenceBroker.java,v 1.1 2007-08-24 22:17:35 ewestfal Exp $
 */
public class QueryReferenceBroker
{
    private Logger log = LoggerFactory.getLogger(QueryReferenceBroker.class);

    private PersistenceBrokerImpl pb;
    private HashMap m_retrievalTasks;
    private ArrayList prefetchingListeners;
    private final boolean batchRetrieval = true;
    private final boolean prefetchProxies = true;
    private Class classToPrefetch;
    private PBLifeCycleEvent afterLookupEvent;

    public QueryReferenceBroker(final PersistenceBrokerImpl pb)
    {
        this.pb = pb;
        afterLookupEvent = new PBLifeCycleEvent(pb, PBLifeCycleEvent.Type.AFTER_LOOKUP);
    }

    /**
     * retrieve a collection of itemClass Objects matching the Query query
     * @param collectionClass type the collection to be returned
     * @param itemClass Class of item in collection
     * @param query the query
     */
    private ManageableCollection getCollectionByQuery(Class collectionClass, Class itemClass, Query query)
            throws ClassNotPersistenceCapableException, PersistenceBrokerException
    {
        if (log.isDebugEnabled()) log.debug("getCollectionByQuery (" + collectionClass + ", " + itemClass + ", " + query + ")");

        ClassDescriptor cld = pb.getClassDescriptor(itemClass);
        ManageableCollection result = null;
        OJBIterator iter = null;
        int fullSize = -1;
        int size = 0;

        final boolean isRetrievalTasksCreated = batchRetrieval && m_retrievalTasks == null;
        if (isRetrievalTasksCreated)
        {
            // Maps ReferenceDescriptors to HashSets of owners
            m_retrievalTasks = new HashMap();
        }

        // ==> enable materialization cache
        pb.getInternalCache().enableMaterializationCache();
        try
        {
            result = (ManageableCollection) collectionClass.newInstance();
            
            // now iterate over all elements and add them to the new collection
            // lifecycle events are disabled
            iter = pb.getIteratorFromQuery(query, cld);
            iter.disableLifeCycleEvents();

            // BRJ : get fullSizefor Query
            // to be removed when Query.fullSize is removed
            if (iter instanceof PagingIterator)
            {
                fullSize = iter.fullSize();
            }

            while (iter.hasNext())
            {
                Object candidate = iter.next();

                /**
                 * MBAIRD
                 * candidate CAN be null in the case of materializing from an iterator based
                 * on a query for a class that is mapped to a table that has other classes
                 * mapped to that table as well, but aren't extents.
                 */
                if (candidate != null)
                {
                    IndirectionHandler handler = ProxyHelper.getIndirectionHandler(candidate);

                    if ((handler != null) || itemClass.isAssignableFrom(candidate.getClass()))
                    {
                        result.ojbAdd(candidate);

                        // BRJ: count added objects
                        // to be removed when Query.fullSize is removed
                        size++;
                    }
                    else
                    {
                        //warn the user
                        log.warn("Candidate object ["+candidate
                                    +"] class ["+candidate.getClass().getName()
                                    +"] is not a subtype of ["+itemClass.getName()
                                    +"] or any type of proxy. NOT INCLUDED in result collection");
                    }
                    if (prefetchProxies && (handler != null)
                            && (cld.getProxyPrefetchingLimit() > 0)
                            && addRetrievalTask(candidate, this))
                    {
                        new PBMaterializationListener(candidate, m_retrievalTasks,
                                this, cld.getProxyPrefetchingLimit());
                    }
                }
            }

            if (isRetrievalTasksCreated)
            {
                // turn off auto prefetching for related proxies
                final Class saveClassToPrefetch = classToPrefetch;
                classToPrefetch = null;
                try
                {
                    performRetrievalTasks();
                }
                finally
                {
                    classToPrefetch = saveClassToPrefetch;
                }
            }

            // BRJ: fire LifeCycleEvents after execution of RetrievalTasks
            // to ensure objects are fully materialized
            Iterator resultIter = result.ojbIterator();
            while (resultIter.hasNext())
            {
                Object obj = resultIter.next();
                afterLookupEvent.setTarget(obj);
                pb.fireBrokerEvent(afterLookupEvent);
                afterLookupEvent.setTarget(null);
            }

            // ==> disable materialization cache
            pb.getInternalCache().disableMaterializationCache();
        }
        catch(RuntimeException e)
        {
            // ==> clear materialization cache
            pb.getInternalCache().doLocalClear();
            throw e;
        }
        catch (Exception ex)
        {
            // ==> clear materialization cache
            pb.getInternalCache().doLocalClear();
            log.error(ex);
            throw new PersistenceBrokerException(ex);
        }
        finally
        {
            if (iter != null)
            {
                iter.releaseDbResources();
            }
            if (isRetrievalTasksCreated)
            {
                m_retrievalTasks = null;
            }
        }

        // BRJ: store fullSize in Query to re-enable deprecated functionality
        // to be removed when Query.fullSize is removed
        if (fullSize < 0)
        {
            fullSize = size;	// use size of result
        }
        query.fullSize(fullSize);
        
        return result;
    }

    /**
     * retrieve a collection of type collectionClass matching the Query query
     * if lazy = true return a CollectionProxy
     *
     * @param collectionClass
     * @param query
     * @param lazy
     * @return ManageableCollection
     * @throws PersistenceBrokerException
     */
    public ManageableCollection getCollectionByQuery(Class collectionClass, Query query, boolean lazy) throws PersistenceBrokerException
    {
        ManageableCollection result;

        try
        {
            // BRJ: return empty Collection  for null query
            if (query == null)
            {
                result = (ManageableCollection)collectionClass.newInstance();
            }
            else
            {
                if (lazy)
                {
                    result = pb.getProxyFactory().createCollectionProxy(pb.getPBKey(), query, collectionClass);
                }
                else
                {
                    result = getCollectionByQuery(collectionClass, query.getSearchClass(), query);
                }
            }
            return result;
        }
        catch (Exception e)
        {
            if(e instanceof PersistenceBrokerException)
            {
                throw (PersistenceBrokerException) e;
            }
            else
            {
                throw new PersistenceBrokerException(e);
            }
        }
    }

    /**
     * retrieve a collection of itemClass Objects matching the Query query
     */
    public Collection getCollectionByQuery(Query query, boolean lazy) throws PersistenceBrokerException
    {
        // thma: the following cast is safe because:
        // 1. ManageableVector implements Collection (will be returned if lazy == false)
        // 2. CollectionProxy implements Collection (will be returned if lazy == true)
        return (Collection) getCollectionByQuery(RemovalAwareCollection.class, query, lazy);
    }

    
    private Class getCollectionTypeClass(CollectionDescriptor cds) throws PersistenceBrokerException{
        // BRJ: do not use RemovalAwareCollection for m:n relationships
        // see http://db.apache.org/ojb/docu/guides/basic-technique.html#Mapping+m%3An+associations

        Class fieldType = cds.getPersistentField().getType();
        Class collType;

        if (fieldType.isArray() || fieldType.isAssignableFrom(RemovalAwareCollection.class))
        {
            collType = cds.isMtoNRelation() ? ManageableArrayList.class : RemovalAwareCollection.class;
        }
        else if (fieldType.isAssignableFrom(RemovalAwareList.class))
        {
            collType = cds.isMtoNRelation() ? ManageableArrayList.class : RemovalAwareList.class;
        }
        else if (fieldType.isAssignableFrom(RemovalAwareSet.class))
        {
            collType = cds.isMtoNRelation() ? ManageableHashSet.class : RemovalAwareSet.class;
        }
        else if (ManageableCollection.class.isAssignableFrom(fieldType))
        {
            collType = fieldType;
        }
        else
        {
            throw new MetadataException("Cannot determine a default collection type for collection "+cds.getAttributeName()+" in type "+cds.getClassDescriptor().getClassNameOfObject());
        }
        return collType;
    }
    
    
    /**
     * @return true if this is the first task for the given ObjectReferenceDescriptor
     */
    private boolean addRetrievalTask(Object obj, Object key)
    {
        ArrayList owners = (ArrayList) m_retrievalTasks.get(key);
        boolean isFirst = false;

        if (owners == null)
        {
            owners = new ArrayList();
            m_retrievalTasks.put(key, owners);
            isFirst = true;
        }
        owners.add(obj);
        return isFirst;
    }

    /**
     * Perform the stored retrieval tasks
     * BRJ: made it public to access it from BasePrefetcher
     * TODO: this is a quick fix !
     */
    public void performRetrievalTasks()
    {
        if (m_retrievalTasks == null)
        {
            return;
        }

        while (m_retrievalTasks.size() > 0)
        {
            HashMap tmp = m_retrievalTasks;
            m_retrievalTasks = new HashMap();
            // during execution of these tasks new tasks may be added

            for (Iterator it = tmp.entrySet().iterator(); it.hasNext(); )
            {
                Map.Entry entry = (Map.Entry) it.next();
                Object key = entry.getKey();

                if (!(key instanceof ObjectReferenceDescriptor))
                {
                    continue;
                }

                ObjectReferenceDescriptor ord = (ObjectReferenceDescriptor) key;
                RelationshipPrefetcher prefetcher;
                ArrayList owners = (ArrayList) entry.getValue();

//                if (ord instanceof SuperReferenceDescriptor || ord.isLazy() || (ord.getItemProxyClass() != null))
                if (ord.isLazy() || (ord.getItemProxyClass() != null))
                {
                    continue;
                }

                prefetcher = pb.getRelationshipPrefetcherFactory().createRelationshipPrefetcher(ord);
                prefetcher.prefetchRelationship(owners);
                it.remove();
            }
        }
    }

    /**
     * Retrieve a single Reference.
     * This implementation retrieves a referenced object from the data backend
     * if <b>cascade-retrieve</b> is true or if <b>forced</b> is true.
     *
     * @param obj - object that will have it's field set with a referenced object.
     * @param cld - the ClassDescriptor describring obj
     * @param rds - the ObjectReferenceDescriptor of the reference attribute to be loaded
     * @param forced - if set to true, the reference is loaded even if the rds differs.
     */
    public void retrieveReference(Object obj, ClassDescriptor cld, ObjectReferenceDescriptor rds, boolean forced)
    {
        PersistentField refField;
        Object refObj = null;
        
        if (forced || rds.getCascadeRetrieve())
        {
            pb.getInternalCache().enableMaterializationCache();
            try
            {
                Identity id = getReferencedObjectIdentity(obj, rds, cld);
                boolean isRefObjDefined = true;

                if (id == null)
                {
                    refObj = null;
                } //JMM : why not see if the object has already been loaded
                else if ( pb.serviceObjectCache().lookup(id) != null )
                {
                    refObj = pb.doGetObjectByIdentity(id);
                    if (rds.isSuperReferenceDescriptor()) 
                    {
                        // walk the super-references
                        ClassDescriptor superCld = cld.getRepository().getDescriptorFor(rds.getItemClass());
                        retrieveReferences(refObj, superCld, false);
                        retrieveCollections(refObj, superCld, false);                        
                    }
                }
                else if ((m_retrievalTasks != null)
                        && !rds.isLazy()
                        && (rds.getItemProxyClass() == null))
                {
                    addRetrievalTask(obj, rds);
                    isRefObjDefined = false;
                }
                else
                {
                    refObj = getReferencedObject(id, rds);
                }

                if (isRefObjDefined)
                {
                    refField = rds.getPersistentField();
                    refField.set(obj, refObj);

                    if ((refObj != null) && prefetchProxies
                            && (m_retrievalTasks != null)
                            && (rds.getProxyPrefetchingLimit() > 0))
                    {
                        IndirectionHandler handler = ProxyHelper.getIndirectionHandler(refObj);

                        if ((handler != null)
                                && addRetrievalTask(obj, rds))
                        {
                            new PBMaterializationListener(obj, m_retrievalTasks,
                                    rds, rds.getProxyPrefetchingLimit());
                        }
                    }
                }

                pb.getInternalCache().disableMaterializationCache();
            }
            catch(RuntimeException e)
            {
                pb.getInternalCache().doLocalClear();
                throw e;
            }
        }
    }
    
    /**
     * Retrieve a single Reference.
     * This implementation retrieves a referenced object from the data backend
     * if <b>cascade-retrieve</b> is true or if <b>forced</b> is true.
     *
     * @param obj - object that will have it's field set with a referenced object.
     * @param cld - the ClassDescriptor describring obj
     * @param rds - the ObjectReferenceDescriptor of the reference attribute to be loaded
     * @param forced - if set to true, the reference is loaded even if the rds differs.
     */
    public void retrieveProxyReference(Object obj, ClassDescriptor cld, ObjectReferenceDescriptor rds, boolean forced)
    {
        PersistentField refField;
        Object refObj = null;

            pb.getInternalCache().enableMaterializationCache();
            try
            {
                Identity id = getReferencedObjectIdentity(obj, rds, cld);
                if (id != null){
                    refObj = pb.createProxy(rds.getItemClass(), id);
                }
                refField = rds.getPersistentField();
                refField.set(obj, refObj);

                if ((refObj != null) && prefetchProxies
                        && (m_retrievalTasks != null)
                        && (rds.getProxyPrefetchingLimit() > 0))
                {
                    IndirectionHandler handler = ProxyHelper.getIndirectionHandler(refObj);

                    if ((handler != null)
                            && addRetrievalTask(obj, rds))
                    {
                        new PBMaterializationListener(obj, m_retrievalTasks,
                                rds, rds.getProxyPrefetchingLimit());
                    }
                }
                

                pb.getInternalCache().disableMaterializationCache();
            }
            catch(RuntimeException e)
            {
                pb.getInternalCache().doLocalClear();
                throw e;
            }
        
    }

    /**
     * Retrieve all References
     *
     * @param newObj the instance to be loaded or refreshed
     * @param cld the ClassDescriptor of the instance
     * @param forced if set to true loading is forced even if cld differs.
     */
    public void retrieveReferences(Object newObj, ClassDescriptor cld, boolean forced) throws PersistenceBrokerException
    {
        Iterator i = cld.getObjectReferenceDescriptors().iterator();

        // turn off auto prefetching for related proxies
        final Class saveClassToPrefetch = classToPrefetch;
        classToPrefetch = null;

        pb.getInternalCache().enableMaterializationCache();
        try
        {
            while (i.hasNext())
            {
                ObjectReferenceDescriptor rds = (ObjectReferenceDescriptor) i.next();
                retrieveReference(newObj, cld, rds, forced);
            }

            pb.getInternalCache().disableMaterializationCache();
        }
        catch(RuntimeException e)
        {
            pb.getInternalCache().doLocalClear();
            throw e;
        }
        finally
        {
            classToPrefetch = saveClassToPrefetch;
        }
    }
    
    /**
     * Retrieve all References
     *
     * @param newObj the instance to be loaded or refreshed
     * @param cld the ClassDescriptor of the instance
     * @param forced if set to true loading is forced even if cld differs.
     */
    public void retrieveProxyReferences(Object newObj, ClassDescriptor cld, boolean forced) throws PersistenceBrokerException
    {
        Iterator i = cld.getObjectReferenceDescriptors().iterator();

        // turn off auto prefetching for related proxies
        final Class saveClassToPrefetch = classToPrefetch;
        classToPrefetch = null;

        pb.getInternalCache().enableMaterializationCache();
        try
        {
            while (i.hasNext())
            {
                ObjectReferenceDescriptor rds = (ObjectReferenceDescriptor) i.next();
                retrieveProxyReference(newObj, cld, rds, forced);
            }

            pb.getInternalCache().disableMaterializationCache();
        }
        catch(RuntimeException e)
        {
            pb.getInternalCache().doLocalClear();
            throw e;
        }
        finally
        {
            classToPrefetch = saveClassToPrefetch;
        }
    }

   /**
     * retrieves an Object reference's Identity.
     * <br>
     * Null is returned if all foreign keys are null
     */
    private Identity getReferencedObjectIdentity(Object obj, ObjectReferenceDescriptor rds, ClassDescriptor cld)
    {
        Object[] fkValues = rds.getForeignKeyValues(obj, cld);
        FieldDescriptor[] fkFieldDescriptors = rds.getForeignKeyFieldDescriptors(cld);
        boolean hasNullifiedFKValue = hasNullifiedFK(fkFieldDescriptors, fkValues);
        /*
        BRJ: if all fk values are null there's no referenced object

        arminw: Supposed the given object has nullified FK values but the referenced
        object still exists. This could happend after serialization of the main object. In
        this case all anonymous field (AK) information is lost, because AnonymousPersistentField class
        use the object identity to cache the AK values. But we can build Identity anyway from the reference
        */
        if (hasNullifiedFKValue)
        {
            if(BrokerHelper.hasAnonymousKeyReference(cld, rds))
            {
                Object referencedObject = rds.getPersistentField().get(obj);
                if(referencedObject != null)
                {
                    return pb.serviceIdentity().buildIdentity(referencedObject);
                }
            }
        }
        else
        {
            // ensure that top-level extents are used for Identities
            return pb.serviceIdentity().buildIdentity(rds.getItemClass(), pb.getTopLevelClass(rds.getItemClass()), fkValues);
        }
        return null;
    }

    // BRJ: check if we have non null fk values
    // TBD  we should also check primitives
    // to avoid creation of unmaterializable proxies
    private boolean hasNullifiedFK(FieldDescriptor[] fkFieldDescriptors, Object[] fkValues)
    {
        boolean result = true;
        for (int i = 0; i < fkValues.length; i++)
        {
            if (!pb.serviceBrokerHelper().representsNull(fkFieldDescriptors[i], fkValues[i]))
            {
                result = false;
                break;
            }
        }
        return result;
    }

    /**
     * retrieves an Object reference by its Identity.
     * <br>
     * If there is a Proxy-class is defined in the ReferenceDescriptor or
     * if the ReferenceDescriptor is lazy, a Proxy-object is returned.
     * <br>
     * If no Proxy-class is defined, a getObjectByIdentity(...) lookup is performed.
     */
    private Object getReferencedObject(Identity id, ObjectReferenceDescriptor rds)
    {
        Class baseClassForProxy;

        if (rds.isLazy())
        {
            /*
            arminw:
            use real reference class instead of the top-level class,
            because we want to use a proxy representing the real class
            not only the top-level class - right?
            */
            // referencedProxy = getClassDescriptor(referencedClass).getDynamicProxyClass();
            //referencedProxy = rds.getItemClass(); 
            
            /*
             * andrew.clute:
             * With proxy generation now handled by the ProxyFactory implementations, the class of the Item
             * is now the nessecary parameter to generate a proxy.
             */
            baseClassForProxy = rds.getItemClass();
        }
        else
        {
            /*
            * andrew.clute:
            * If the descriptor does not mark it as lazy, then the class for the proxy must be of type VirtualProxy
            */
           baseClassForProxy = rds.getItemProxyClass();
        }

        if (baseClassForProxy != null)
        {
            try
            {
                return pb.createProxy(baseClassForProxy, id);
            }
            catch (Exception e)
            {
                log.error("Error while instantiate object " + id + ", msg: "+ e.getMessage(), e);
                if(e instanceof PersistenceBrokerException)
                {
                    throw (PersistenceBrokerException) e;
                }
                else
                {
                    throw new PersistenceBrokerException(e);
                }
            }
        }
        else
        {
            return pb.doGetObjectByIdentity(id);
        }
    }
    
    /**
     * Retrieve a single Collection on behalf of <b>obj</b>.
     * The Collection is retrieved only if <b>cascade.retrieve is true</b>
     * or if <b>forced</b> is set to true.     *
     *
     * @param obj - the object to be updated
     * @param cld - the ClassDescriptor describing obj
     * @param cds - the CollectionDescriptor describing the collection attribute to be loaded
     * @param forced - if set to true loading is forced, even if cds differs.
     *
     */
    public void retrieveCollection(Object obj, ClassDescriptor cld, CollectionDescriptor cds, boolean forced)
    {
        doRetrieveCollection(obj, cld, cds, forced, cds.isLazy());
    }
    
    /**
     * Retrieve a single Proxied Collection on behalf of <b>obj</b>.
     * The Collection is retrieved only if <b>cascade.retrieve is true</b>
     * or if <b>forced</b> is set to true.     *
     *
     * @param obj - the object to be updated
     * @param cld - the ClassDescriptor describing obj
     * @param cds - the CollectionDescriptor describing the collection attribute to be loaded
     * @param forced - if set to true a proxy will be placed, even if cds differs.
     *
     */
    public void retrieveProxyCollection(Object obj, ClassDescriptor cld, CollectionDescriptor cds, boolean forced)
    {
        doRetrieveCollection(obj, cld, cds, forced, true);
    }
    
    private void doRetrieveCollection(Object obj, ClassDescriptor cld, CollectionDescriptor cds, boolean forced, boolean lazyLoad)
    {
        if (forced || cds.getCascadeRetrieve())
        {
            if ((m_retrievalTasks != null) && !cds.isLazy()
                    && !cds.hasProxyItems()
                    && (cds.getQueryCustomizer() == null))
            {
                addRetrievalTask(obj, cds);
            }
            else
            {
                // this collection type will be used:
                Class collectionClass = cds.getCollectionClass();
                PersistentField collectionField = cds.getPersistentField();
                Query fkQuery = getFKQuery(obj, cld, cds);
                Object value;

                pb.getInternalCache().enableMaterializationCache();
                try
                {
                    if (collectionClass == null)
                    {
                        Collection result = (Collection)getCollectionByQuery(getCollectionTypeClass(cds), fkQuery, lazyLoad);

                        // assign collection to objects attribute
                        // if attribute has an array type build an array, else assign collection directly
                        if (collectionField.getType().isArray())
                        {
                            int length = result.size();
                            Class itemtype = collectionField.getType().getComponentType();
                            Object resultArray = Array.newInstance(itemtype, length);
                            int j = 0;
                            for (Iterator iter = result.iterator(); iter.hasNext();j++)
                            {
                                Array.set(resultArray, j, iter.next());
                            }
                            collectionField.set(obj, resultArray);
                        }
                        else
                        {
                            collectionField.set(obj, result);
                        }
                        value = result;
                    }
                    else
                    {
                        ManageableCollection result = getCollectionByQuery(collectionClass, fkQuery, lazyLoad);
                        collectionField.set(obj, result);
                        value = result;
                    }

                    if (prefetchProxies && (m_retrievalTasks != null)
                            && (cds.getProxyPrefetchingLimit() > 0)
                            && (cds.getQueryCustomizer() == null)
                            && (ProxyHelper.isCollectionProxy(value)))
                    {
                        if (addRetrievalTask(obj, cds))
                        {
                            new PBCollectionProxyListener(obj,
                                    m_retrievalTasks, cds, cds.getProxyPrefetchingLimit());
                        }
                    }

                    pb.getInternalCache().disableMaterializationCache();
                }
                catch(RuntimeException e)
                {
                    pb.getInternalCache().doLocalClear();
                    throw e;
                }
            }
        }
    }

    /**
     * Answer the foreign key query to retrieve the collection
     * defined by CollectionDescriptor
     */
    private Query getFKQuery(Object obj, ClassDescriptor cld, CollectionDescriptor cds)
    {
        Query fkQuery;
        QueryByCriteria fkQueryCrit;

        if (cds.isMtoNRelation())
        {
            fkQueryCrit = getFKQueryMtoN(obj, cld, cds);
        }
        else
        {
            fkQueryCrit = getFKQuery1toN(obj, cld, cds);
        }

        // check if collection must be ordered
        if (!cds.getOrderBy().isEmpty())
        {
            Iterator iter = cds.getOrderBy().iterator();
            while (iter.hasNext())
            {
                fkQueryCrit.addOrderBy((FieldHelper)iter.next());
            }
        }

        // BRJ: customize the query
        if (cds.getQueryCustomizer() != null)
        {
            fkQuery = cds.getQueryCustomizer().customizeQuery(obj, pb, cds, fkQueryCrit);
        }
        else
        {
            fkQuery = fkQueryCrit;
        }

        return fkQuery;
    }

    /**
     * Get Foreign key query for m:n <br>
     * supports UNIDIRECTIONAL m:n using QueryByMtoNCriteria
     * @return org.apache.ojb.broker.query.QueryByCriteria
     * @param obj the owner of the relationship
     * @param cld the ClassDescriptor for the owner
     * @param cod the CollectionDescriptor
     */
    private QueryByCriteria getFKQueryMtoN(Object obj, ClassDescriptor cld, CollectionDescriptor cod)
    {
        ValueContainer[] values = pb.serviceBrokerHelper().getKeyValues(cld, obj);
        Object[] thisClassFks = cod.getFksToThisClass();
        Object[] itemClassFks = cod.getFksToItemClass();
        ClassDescriptor refCld = pb.getClassDescriptor(cod.getItemClass());
        Criteria criteria = new Criteria();

        for (int i = 0; i < thisClassFks.length; i++)
        {
            criteria.addEqualTo(cod.getIndirectionTable() + "." + thisClassFks[i], values[i].getValue());
        }
        for (int i = 0; i < itemClassFks.length; i++)
        {
            criteria.addEqualToField(cod.getIndirectionTable() + "." + itemClassFks[i],
                    refCld.getPkFields()[i].getAttributeName());
        }

        return QueryFactory.newQuery(refCld.getClassOfObject(), cod.getIndirectionTable(), criteria);
    }

    /**
     * Get Foreign key query for 1:n
     * @return org.apache.ojb.broker.query.QueryByCriteria
     * @param obj
     * @param cld
     * @param cod
     */
    private QueryByCriteria getFKQuery1toN(Object obj, ClassDescriptor cld, CollectionDescriptor cod)
    {
        ValueContainer[] container = pb.serviceBrokerHelper().getKeyValues(cld, obj);
        ClassDescriptor refCld = pb.getClassDescriptor(cod.getItemClass());
        FieldDescriptor[] fields = cod.getForeignKeyFieldDescriptors(refCld);
        Criteria criteria = new Criteria();

        for (int i = 0; i < fields.length; i++)
        {
            FieldDescriptor fld = fields[i];
            criteria.addEqualTo(fld.getAttributeName(), container[i].getValue());
        }

        return QueryFactory.newQuery(refCld.getClassOfObject(), criteria);
    }

    /**
     * Answer the primary key query to retrieve an Object
     *
     * @param oid the Identity of the Object to retrieve
     * @return The resulting query
     */
    public Query getPKQuery(Identity oid)
    {
        Object[] values = oid.getPrimaryKeyValues();
        ClassDescriptor cld = pb.getClassDescriptor(oid.getObjectsTopLevelClass());
        FieldDescriptor[] fields = cld.getPkFields();
        Criteria criteria = new Criteria();

        for (int i = 0; i < fields.length; i++)
        {
            FieldDescriptor fld = fields[i];
            criteria.addEqualTo(fld.getAttributeName(), values[i]);
        }
        return QueryFactory.newQuery(cld.getClassOfObject(), criteria);
    }

    /**
     * Retrieve all Collection attributes of a given instance
     *
     * @param newObj the instance to be loaded or refreshed
     * @param cld the ClassDescriptor of the instance
     * @param forced if set to true, loading is forced even if cld differs
     *
     */
    public void retrieveCollections(Object newObj, ClassDescriptor cld, boolean forced) throws PersistenceBrokerException
    {
        doRetrieveCollections(newObj, cld, forced, false);
    }
    
    /**
     * Retrieve all Collection attributes of a given instance, and make all of the Proxy Collections
     *
     * @param newObj the instance to be loaded or refreshed
     * @param cld the ClassDescriptor of the instance
     * @param forced if set to true, loading is forced even if cld differs
     *
     */
    public void retrieveProxyCollections(Object newObj, ClassDescriptor cld, boolean forced) throws PersistenceBrokerException
    {
        doRetrieveCollections(newObj, cld, forced, true);
    }
    
    private void doRetrieveCollections(Object newObj, ClassDescriptor cld, boolean forced, boolean forceProxyCollection) throws PersistenceBrokerException
    {
        Iterator i = cld.getCollectionDescriptors().iterator();

        // turn off auto prefetching for related proxies
        final Class saveClassToPrefetch = classToPrefetch;
        classToPrefetch = null;

        pb.getInternalCache().enableMaterializationCache();
        try
        {
            while (i.hasNext())
            {
                CollectionDescriptor cds = (CollectionDescriptor) i.next();
                if (forceProxyCollection){
                    retrieveProxyCollection(newObj, cld, cds, forced);
                } else {
                    retrieveCollection(newObj, cld, cds, forced);
                }
            }
            pb.getInternalCache().disableMaterializationCache();
        }
        catch (RuntimeException e)
        {
            pb.getInternalCache().doLocalClear();
            throw e;
        }
        finally
        {
            classToPrefetch = saveClassToPrefetch;
        }
    }


    /**
     * remove all prefetching listeners
     */
    public void removePrefetchingListeners()
    {
        if (prefetchingListeners != null)
        {
            for (Iterator it = prefetchingListeners.iterator(); it.hasNext(); )
            {
                PBPrefetchingListener listener = (PBPrefetchingListener) it.next();
                listener.removeThisListener();
            }
            prefetchingListeners.clear();
        }
    }

    public Class getClassToPrefetch()
    {
        return classToPrefetch;
    }

    //**********************************************************************
    // inner classes
    //**********************************************************************

    class PBMaterializationListener extends PBPrefetchingListener implements MaterializationListener
    {
        private IndirectionHandler _listenedHandler;        

        PBMaterializationListener(Object owner,
                HashMap retrievalTasks, Object key, int limit)
        {
            super(owner, retrievalTasks, key, limit);            
        }

        protected void addThisListenerTo(Object owner)
        {
            _listenedHandler = ProxyHelper.getIndirectionHandler(owner);

            if (_listenedHandler != null)
            {
                _listenedHandler.addListener(this);
            }
        }

        protected void removeThisListener()
        {
            if (_listenedHandler != null)
            {
                _listenedHandler.removeListener(this);
                _listenedHandler = null;
            }
        }

        protected RelationshipPrefetcher getPrefetcher(Object listenedObject)
        {
            if (_key instanceof ObjectReferenceDescriptor)
            {
                return pb.getRelationshipPrefetcherFactory().createRelationshipPrefetcher((ObjectReferenceDescriptor) _key);
            }
            else // PersistentBrokerImpl.this
            {
                // a special case: current collection being loaded contains proxies,
                // just load them without setting any fields
                IndirectionHandler handler = (IndirectionHandler) listenedObject;
                return new PlainPrefetcher(pb, handler.getIdentity().getObjectsTopLevelClass());
            }
        }

        public void beforeMaterialization(IndirectionHandler handler, Identity oid)
        {
            prefetch(handler);
        }

        public void afterMaterialization(IndirectionHandler handler, Object materializedObject)
        {
            //do nothing            
        }
    }

    abstract class PBPrefetchingListener
    {
        private HashMap _retrievalTasks;
        private int _limit;
        protected Object _key;

        PBPrefetchingListener(Object owner, HashMap retrievalTasks,
                Object key, int limit)
        {
            _retrievalTasks = retrievalTasks;
            _key = key;
            _limit = limit + 1; // lestenedObject + next limit objects
            if (prefetchingListeners == null)
            {
                prefetchingListeners = new ArrayList();
            }
            addThisListenerTo(owner);
            prefetchingListeners.add(this);
        }

        abstract protected void addThisListenerTo(Object owner);

        abstract protected void removeThisListener();

        abstract protected RelationshipPrefetcher getPrefetcher(Object listenedObject);

        protected void prefetch(Object listenedObject)
        {
            ArrayList owners = (ArrayList) _retrievalTasks.get(_key);
            List toPrefetch;
            RelationshipPrefetcher prefetcher;
            boolean prefetchingAll;

            removeThisListener();

            if (owners == null)
            {
                return;
            }

            prefetcher = getPrefetcher(listenedObject);

            if (owners.size() <= _limit)
            {
                toPrefetch = owners;
                prefetchingAll = true;
            }
            else
            {
                toPrefetch = owners.subList(0, _limit);
                prefetchingAll = false;
            }

            final Class saveClassToPrefetch = classToPrefetch;
            classToPrefetch = prefetcher.getItemClassDescriptor().getClassOfObject();
            try
            {
                prefetcher.prefetchRelationship(toPrefetch);
            }
            finally
            {
                classToPrefetch = saveClassToPrefetch;
            }

            if (prefetchingAll)
            {
                _retrievalTasks.remove(_key);
            }
            else
            {
                // ArrayList documented trick:
                // "the following idiom removes a range of elements from a list:
                // list.subList(from, to).clear();
                toPrefetch.clear();
                addThisListenerTo(owners.get(0));
            }
        }
    }

    class PBCollectionProxyListener extends PBPrefetchingListener
            implements CollectionProxyListener
    {
        CollectionProxyDefaultImpl _listenedCollection;

        PBCollectionProxyListener(Object owner,
               HashMap retrievalTasks, CollectionDescriptor key, int limit)
        {
            super(owner, retrievalTasks, key, limit);
        }

        protected void addThisListenerTo(Object owner)
        {
            PersistentField collectionField =
                    ((CollectionDescriptor) _key).getPersistentField();
            _listenedCollection = (CollectionProxyDefaultImpl) collectionField.get(owner);
            _listenedCollection.addListener(this);
        }

        protected void removeThisListener()
        {
            if (_listenedCollection != null)
            {
                _listenedCollection.removeListener(this);
                _listenedCollection = null;
            }
        }

        protected RelationshipPrefetcher getPrefetcher(Object listenedObject)
        {
            return pb.getRelationshipPrefetcherFactory().createRelationshipPrefetcher((CollectionDescriptor)_key);
        }

        public void beforeLoading(CollectionProxyDefaultImpl col)
        {
            prefetch(col);
        }

        public void afterLoading(CollectionProxyDefaultImpl col)
        {
            //do nothing
        }
    }
}
