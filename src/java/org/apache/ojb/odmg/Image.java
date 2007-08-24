package org.apache.ojb.odmg;

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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.ClassUtils;
import org.apache.ojb.broker.Identity;
import org.apache.ojb.broker.IdentityFactory;
import org.apache.ojb.broker.OJBRuntimeException;
import org.apache.ojb.broker.PersistenceBrokerInternal;
import org.apache.ojb.broker.core.proxy.CollectionProxy;
import org.apache.ojb.broker.core.proxy.CollectionProxyDefaultImpl;
import org.apache.ojb.broker.core.proxy.CollectionProxyListener;
import org.apache.ojb.broker.core.proxy.ProxyHelper;
import org.apache.ojb.broker.metadata.CollectionDescriptor;
import org.apache.ojb.broker.metadata.FieldType;
import org.apache.ojb.broker.metadata.ObjectReferenceDescriptor;
import org.apache.ojb.broker.util.BrokerHelper;
import org.apache.ojb.broker.util.logging.Logger;
import org.apache.ojb.broker.util.logging.LoggerFactory;

/**
 * This class encapsulates classes used to take persistence capable object
 * state snapshoot and to detect changed fields or references.
 *
 * @version $Id: Image.java,v 1.1 2007-08-24 22:17:37 ewestfal Exp $
 */
public abstract class Image
{
    static Logger log = LoggerFactory.getLogger(Image.class);
    private long timestamp = System.currentTimeMillis();

    private Image()
    {
    }

    boolean illegalImageComparison(Image oldImage)
    {
        return timestamp < oldImage.timestamp;
    }

    public abstract void cleanup(boolean reuse);

    public abstract boolean modified(Image other);

    abstract void referenceProcessing(Image oldImage);

    public void performReferenceDetection(Image oldImage)
    {
        if(illegalImageComparison(oldImage))
        {
            throw new ImageException("The specified Image object is newer than current one, wrong Image order!");
        }
        referenceProcessing(oldImage);
    }

    //===================================================================
    // inner class
    //===================================================================
    public static class MultipleRef extends Image implements CollectionProxyListener
    {
        static final int IS_NORMAL_OBJECT = 11;
        static final int IS_MATERIALIZED_PROXY = 13;
        static final int IS_UNMATERIALIZED_PROXY = 17;

        private ImageListener listener;
        private final CollectionDescriptor cod;
        private final Object collectionOrArray;
        private Map references;
        private int status;
        private boolean hasTransientIdentity;
        private boolean isRefreshed;

        public MultipleRef(ImageListener listener, CollectionDescriptor cod, Object collectionOrArray)
        {
            this.listener = listener;
            this.cod = cod;
            this.collectionOrArray = collectionOrArray;
            this.isRefreshed = true;
            this.hasTransientIdentity = false;
            this.references = Collections.EMPTY_MAP;
            init();
        }

        private void init()
        {
            CollectionProxy colProxy = ProxyHelper.getCollectionProxy(collectionOrArray);
            if(colProxy != null)
            {
                if(colProxy.isLoaded())
                {
                    status = IS_MATERIALIZED_PROXY;
                    /*
                    TODO: avoid this cast
                    e.g. change CollectionProxy interface - CollectionProxy should
                    extend Collection to support Iterator
                    */
                    handleReferencedObjects(((Collection) colProxy).iterator());
                }
                else
                {
                    status = IS_UNMATERIALIZED_PROXY;
                    if(log.isDebugEnabled()) log.debug("Unmaterialized proxy collection, use proxy listener");
                    colProxy.addListener(this);
                }
            }
            else
            {
                status = IS_NORMAL_OBJECT;
                if(collectionOrArray != null)
                {
                    Iterator it = BrokerHelper.getCollectionIterator(collectionOrArray);
                    handleReferencedObjects(it);
                }
            }
        }

        void handleReferencedObjects(Iterator it)
        {
            if(it == null) return;
            references = new HashMap();
            if(log.isDebugEnabled()) log.debug("Handle collection references");
            IdentityFactory idFac = listener.getBroker().serviceIdentity();
            Identity oid;
            Object obj;
            while(it.hasNext())
            {
                obj = it.next();
                oid = idFac.buildIdentity(obj);
                if(!hasTransientIdentity && oid.isTransient())
                {
                    hasTransientIdentity = true;
                }
                references.put(oid, obj);
            }
        }

        public void cleanup(boolean reuse)
        {
            if(log.isDebugEnabled()) log.debug("Cleanup collection image, reuse=" + reuse);
            if(reuse)
            {
                isRefreshed = false;
            }
            else
            {
                if(status == IS_UNMATERIALIZED_PROXY)
                {
                    CollectionProxy colProxy = ProxyHelper.getCollectionProxy(collectionOrArray);
                    if(colProxy != null)
                    {
                        colProxy.removeListener(this);
                    }
                }
            }
        }

        void referenceProcessing(Image oldImage)
        {
            MultipleRef oldRefs = (MultipleRef) oldImage;
            if(incommensurableProxies(oldRefs))
            {
                if(isUnmaterializedProxy()) handleReferencedObjects(BrokerHelper.getCollectionIterator(collectionOrArray));
                if(oldRefs.isUnmaterializedProxy()) oldRefs.handleReferencedObjects(BrokerHelper.getCollectionIterator(oldRefs.collectionOrArray));
            }
            if(!isRefreshed) refreshIdentities();
            if(!oldRefs.isRefreshed) oldRefs.refreshIdentities();

            // find deleted reference objects
            if(oldRefs.references.size() > 0)
            {
                Iterator oldIter = oldRefs.references.entrySet().iterator();
                while(oldIter.hasNext())
                {
                    Map.Entry entry = (Map.Entry) oldIter.next();
                    Identity oldOid = (Identity) entry.getKey();
                    /*
                    search for deleted objects: if in the new image an object
                    from the old image is not contained, we found a deleted object
                    */
                    if(!isUnmaterializedProxy() && !containsReference(oldOid))
                    {
                        listener.deletedXToN(cod, entry.getValue(), oldOid);
                    }
                }
            }

            // find new reference objects
            if(references.size() > 0)
            {
                Iterator newIter = references.entrySet().iterator();
                while(newIter.hasNext())
                {
                    Map.Entry entry = (Map.Entry) newIter.next();
                    Identity newOid = (Identity) entry.getKey();
                    /*
                    search for added objects: if in the old image an object
                    from the new image is not contained, we found a added object
                    */
                    if(!oldRefs.containsReference(newOid))
                    {
                        listener.addedXToN(cod, entry.getValue(), newOid);
                    }
                }
            }
        }

        /**
         * To detect deleted (added) collection objects it's necessary iterate over the old (new) image collection.
         * If the old (new) image collection is a unmaterialized proxy we have to check if the new (old) image collection
         * is the same proxy instance or not.
         * E.g. if the user exchange one another the unmaterialized proxy collection objects of two main objects,
         * then both proxy need to be materialized to assign the changed FK field values.
         */
        private boolean incommensurableProxies(MultipleRef oldImage)
        {
            boolean result = false;
            // deleted objects
            if(oldImage.isUnmaterializedProxy() || isUnmaterializedProxy())
            {
                result = !collectionOrArray.equals(oldImage.collectionOrArray);
            }
            return result;
        }

        private void refreshIdentities()
        {
            // if no transient identities are used, nothing to do
            if(hasTransientIdentity && references.size() > 0)
            {
                hasTransientIdentity = false;
                // we need independent key list from Map
                List list = new ArrayList(references.keySet());
                IdentityFactory idFac = listener.getBroker().serviceIdentity();
                Identity oid, newOid;
                Object obj;
                for(int i = 0; i < list.size(); i++)
                {
                    oid = (Identity) list.get(i);
                    if(oid.isTransient())
                    {
                        obj = references.remove(oid);
                        newOid = idFac.buildIdentity(obj);
                        references.put(newOid, obj);
                        if(!hasTransientIdentity && oid.isTransient())
                        {
                            hasTransientIdentity = true;
                        }
                    }
                }
                isRefreshed = true;
            }
        }

        /**
         * Always return 'false', because changed 1:n or m:n references do not
         * affect the main object.
         */
        public boolean modified(Image other)
        {
            return false;
        }

        boolean containsReference(Identity oid)
        {
            if(!isRefreshed) refreshIdentities();
            return references.containsKey(oid);
        }

        Map getIdentityReferenceObjectMap()
        {
            if(!isRefreshed) refreshIdentities();
            return references;
        }

        boolean isMaterializedProxy()
        {
            return status == IS_MATERIALIZED_PROXY;
        }

        boolean isUnmaterializedProxy()
        {
            return status == IS_UNMATERIALIZED_PROXY;
        }


        // CollectionProxy Listener methods
        //---------------------------------
        public void beforeLoading(CollectionProxyDefaultImpl colProxy)
        {
            //noop
        }

        public void afterLoading(CollectionProxyDefaultImpl colProxy)
        {
            if(status == IS_UNMATERIALIZED_PROXY)
            {
                status = IS_MATERIALIZED_PROXY;
                handleReferencedObjects(colProxy.iterator());
                colProxy.removeListener(this);
            }
        }

        public String toString()
        {
            return ClassUtils.getShortClassName(this.getClass()) + "[references-size="
                    + (references != null ? "" + references.size() : "undefined") + "]";
        }
    }

    //===================================================================
    // inner class
    //===================================================================
    public static class SingleRef extends Image
    {
        private Object referenceObjOrProxy;
        private Identity oid = null;
        private final ImageListener listener;
        private final ObjectReferenceDescriptor ord;

        public SingleRef(ImageListener listener, ObjectReferenceDescriptor ord, Object reference)
        {
            this.listener = listener;
            this.ord = ord;
            this.referenceObjOrProxy = reference;
        }

        public void cleanup(boolean reuse)
        {
            if(!reuse)
            {
                referenceObjOrProxy = null;
            }
        }

        void referenceProcessing(Image oldImage)
        {
            SingleRef oldRef = (SingleRef) oldImage;
            boolean isSame = getReferenceObjectOrProxy() == oldRef.getReferenceObjectOrProxy();
            if(!isSame)
            {
                Identity newOid = getIdentity();
                Identity oldOid = oldRef.getIdentity();
                if(newOid == null)
                {
                    if(oldOid != null)
                    {
                        listener.deletedOneToOne(ord, oldRef.getReferenceObjectOrProxy(), oldOid, true);
                    }
                }
                else
                {
                    if(oldOid == null)
                    {
                        listener.addedOneToOne(ord, getReferenceObjectOrProxy(), newOid);
                    }
                    else
                    {
                        if(!newOid.equals(oldOid))
                        {
                            listener.deletedOneToOne(ord, oldRef.getReferenceObjectOrProxy(), oldOid, false);
                            listener.addedOneToOne(ord, getReferenceObjectOrProxy(), newOid);
                        }
                    }
                }
            }
        }

        public Object getReferenceObjectOrProxy()
        {
            return referenceObjOrProxy;
        }

        private Identity getIdentity()
        {
            if(oid == null || oid.isTransient())
            {
                if(referenceObjOrProxy != null)
                {
                    oid = listener.getBroker().serviceIdentity().buildIdentity(referenceObjOrProxy);
                }
            }
            return oid;
        }

        /**
         * If a 1:1 reference has changed it will
         * affects the main object (FK needs update).
         */
        public boolean modified(Image toCompare)
        {
            boolean modified = false;
            if(!(this == toCompare))
            {
                if(toCompare instanceof Image.SingleRef)
                {
                    Image.SingleRef other = (Image.SingleRef) toCompare;
                    Identity current = getIdentity();
                    Identity otherOid = other.getIdentity();
                    modified = current != null ? !current.equals(otherOid) : !(otherOid == null);
                }
            }
            return modified;
        }

        public String toString()
        {
            return ClassUtils.getShortClassName(this.getClass()) + "[reference=" + getIdentity() + "]";
        }
    }

    //===================================================================
    // inner class
    //===================================================================
    public static class Field extends Image
    {
        private final FieldType type;
        private final Object value;

        public Field(FieldType type, Object value)
        {
            this.type = type;
            this.value = value;
        }

        public void cleanup(boolean reuse)
        {
        }

        void referenceProcessing(Image oldImage)
        {
            // nothing to do
        }

        /** If a field value has changed return 'true'. */
        public boolean modified(Image other)
        {
            boolean result = false;
            if(this == other)
            {
                result = true;
            }
            else
            {
                if(other instanceof Field)
                {
                    result = !type.equals(value, ((Field) other).value);
                }
            }
            return result;
        }

        public String toString()
        {
            return ClassUtils.getShortClassName(this.getClass()) + "[type=" + type + ", value=" + value + "]";
        }
    }

    //===================================================================
    // inner interface
    //===================================================================
    public static interface ImageListener
    {
        public void addedOneToOne(ObjectReferenceDescriptor ord, Object refObjOrProxy, Identity oid);

        public void deletedOneToOne(ObjectReferenceDescriptor ord, Object refObjOrProxy, Identity oid, boolean needsUnlink);

        public void addedXToN(CollectionDescriptor ord, Object refObjOrProxy, Identity oid);

        public void deletedXToN(CollectionDescriptor ord, Object refObjOrProxy, Identity oid);

        public PersistenceBrokerInternal getBroker();
    }

    //====================================================
    // inner class
    //====================================================

    /**
     * Thrown if something unexpected is happen when handling the
     * object images for state detection.
     */
    public static class ImageException extends OJBRuntimeException
    {
        public ImageException()
        {
        }

        public ImageException(String msg)
        {
            super(msg);
        }

        public ImageException(Throwable cause)
        {
            super(cause);
        }

        public ImageException(String msg, Throwable cause)
        {
            super(msg, cause);
        }
    }
}
