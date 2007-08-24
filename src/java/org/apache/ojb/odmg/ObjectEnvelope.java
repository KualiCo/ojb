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
/**
 *
 * @author <a href="mailto:thma@apache.org">Thomas Mahler</a>
 * @author <a href="mailto:mattbaird@yahoo.com">Matthew Baird</a>
 *
 */

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.ojb.broker.Identity;
import org.apache.ojb.broker.PersistenceBroker;
import org.apache.ojb.broker.PersistenceBrokerException;
import org.apache.ojb.broker.OJBRuntimeException;
import org.apache.ojb.broker.PersistenceBrokerInternal;
import org.apache.ojb.broker.core.proxy.IndirectionHandler;
import org.apache.ojb.broker.core.proxy.ProxyHelper;
import org.apache.ojb.broker.metadata.ClassDescriptor;
import org.apache.ojb.broker.metadata.CollectionDescriptor;
import org.apache.ojb.broker.metadata.FieldDescriptor;
import org.apache.ojb.broker.metadata.ObjectReferenceDescriptor;
import org.apache.ojb.broker.util.BrokerHelper;
import org.apache.ojb.broker.util.ObjectModification;
import org.apache.ojb.broker.util.logging.Logger;
import org.apache.ojb.broker.util.logging.LoggerFactory;
import org.apache.ojb.odmg.states.ModificationState;
import org.apache.ojb.odmg.states.StateNewDirty;
import org.apache.ojb.odmg.states.StateOldClean;
import org.apache.ojb.odmg.states.StateOldDirty;
import org.apache.ojb.odmg.link.LinkEntry;
import org.apache.ojb.odmg.link.LinkEntryOneToOne;
import org.apache.ojb.odmg.link.LinkEntryOneToN;

/**
 * ObjectEnvelope is used during ODMG transactions as a wrapper for a
 * persistent objects declaration
 *
 */
public class ObjectEnvelope implements ObjectModification, Image.ImageListener
{
    private Logger log = LoggerFactory.getLogger(ObjectEnvelope.class);

    static final long serialVersionUID = -829177767933340522L;

    static final int IS_MATERIALIZED_OBJECT = 11;
    static final int IS_MATERIALIZED_PROXY = 13;
    static final int IS_UNMATERIALIZED_PROXY = 17;

    /**
     * The objects modification state, e.g. Old and Clean
     */
    private ModificationState modificationState = null;
    private Identity oid;
    private Boolean hasChanged;
    private boolean writeLocked;

    /**
     * myObj holds the object we are wrapping.
     */
    private Object myObj;

    /**
     * beforeImage holds a mapping between field
     * names and values at the start of the transaction.
     * currentImage holds the mapping at the
     * end of the transaction.
     */
    private Map beforeImage;
    private Map currentImage;
    private ObjectEnvelopeTable buffer;
    // list of all LinkEntry's
    private List linkEntryList;

    /**
     * Create a wrapper by providing an Object.
     */
    public ObjectEnvelope(ObjectEnvelopeTable buffer, Identity oid, Object obj, boolean isNewObject)
    {
        this.linkEntryList = new ArrayList();
        this.buffer = buffer;
        this.oid = oid;
        // TODO: do we really need to materialize??
        myObj = ProxyHelper.getRealObject(obj);
        prepareInitialState(isNewObject);
        /*
        TODO: is it possible to improve this? Take care that "new"
        objects should support "persistence by reachability" too
        (detection of new/persistent reference objects after maon object lock)
        */
        beforeImage = buildObjectImage(getBroker());
    }

    public PersistenceBrokerInternal getBroker()
    {
        return buffer.getTransaction().getBrokerInternal();
    }

    TransactionImpl getTx()
    {
        return buffer.getTransaction();
    }

    ObjectEnvelopeTable getEnvelopeTable()
    {
        return buffer;
    }

    public Map getBeforeImage()
    {
        if(beforeImage == null)
        {
            beforeImage = buildObjectImage(getBroker());
        }
        return beforeImage;
    }

    public Map getCurrentImage()
    {
        if(currentImage == null)
        {
            currentImage = buildObjectImage(getBroker());
        }
        return currentImage;
    }

    /**
     * This method should be called before transaction ends
     * to allow cleanup of used resources, e.g. remove proxy listener objects
     * to avoid invoke of registered objects after tx end.
     */
    public void cleanup(boolean reuse, boolean wasInsert)
    {
        if(currentImage != null)
        {
            performImageCleanup(currentImage, reuse);
        }
        if(beforeImage != null)
        {
            // we always free all resources of the old image
            performImageCleanup(beforeImage, false);
        }
        if(reuse)
        {
            refreshObjectImage(wasInsert);
        }
        else
        {
            myObj = null;
        }
    }

    private void performImageCleanup(Map imageMap, boolean reuse)
    {
        Iterator iterator = imageMap.values().iterator();
        while(iterator.hasNext())
        {
            Image base =  (Image) iterator.next();
            if(base != null) base.cleanup(reuse);
        }
    }

    private void refreshObjectImage(boolean wasInsert)
    {
        try
        {
            // if an image already exists we
            // replace the Identity too, maybe a temporary
            // used PK value was replaced by the real one,
            // see in docs SequenceManagerNativeImpl
            if(getIdentity().isTransient())
            {
                refreshIdentity();
            }
            if(currentImage != null)
            {
                beforeImage = currentImage;
            }
            else
            {
                if(beforeImage == null)
                {
                    beforeImage = buildObjectImage(getBroker());
                }
            }
            currentImage = null;
            hasChanged = null;
            if(wasInsert)
            {
                /*
                on insert we have to replace the PK fields and the version fields, because
                they populated after the object was written to DB, thus replace all field image values
                */
                refreshPKFields();
            }
            // TODO: How to handle version fields incremented by the DB?
            // always refresh the version fields, because these fields will change when written to DB
            refreshLockingFields();
        }
        catch(PersistenceBrokerException e)
        {
            beforeImage = null;
            currentImage = null;
            hasChanged = null;
            log.error("Can't refresh object image for " + getIdentity(), e);
            throw e;
        }
    }

    private void refreshPKFields()
    {
        FieldDescriptor[] flds = getClassDescriptor().getPkFields();
        for(int i = 0; i < flds.length; i++)
        {
            FieldDescriptor fld = flds[i];
            addFieldImage(beforeImage, fld);
        }
    }

    private void refreshLockingFields()
    {
        if(getClassDescriptor().isLocking())
        {
            FieldDescriptor[] flds = getClassDescriptor().getLockingFields();
            for(int i = 0; i < flds.length; i++)
            {
                FieldDescriptor fld = flds[i];
                addFieldImage(beforeImage, fld);
            }
        }
    }

    /**
     * Replace the current with a new generated identity object and
     * returns the old one.
     */
    public Identity refreshIdentity()
    {
        Identity oldOid = getIdentity();
        this.oid = getBroker().serviceIdentity().buildIdentity(myObj);
        return oldOid;
    }

    public Identity getIdentity()
    {
        if(oid == null)
        {
            oid = getBroker().serviceIdentity().buildIdentity(getObject());
        }
        return oid;
    }

    /**
     * Returns the managed materialized object.
     */
    public Object getObject()
    {
        return myObj;
    }

    public Object getRealObject()
    {
        return ProxyHelper.getRealObject(getObject());
    }

    public void refreshObjectIfNeeded(Object obj)
    {
        if(this.myObj != obj)
        {
            this.myObj = obj;
        }
    }

    /**
     * We need to implement the Two-Phase Commit
     * protocol.
     *
     * beginCommit is where we say if we can or cannot
     * commit the transaction.  At the begining however,
     * we need to attain the after image so we can isolate
     * everything.
     *
     * We should issue the call against the database
     * at this point.  If we get a SQL Exception, we
     * should throw the org.odmg.TransactionAbortedException.
     *
     * We should also check to see if the object is
     * TransactionAware.  If so, we should give it a chance
     * to kill the transaction before we toss it to the
     * database.
     */
    public void beforeCommit()
    {
        if(myObj instanceof TransactionAware)
        {
            TransactionAware ta = (TransactionAware) myObj;
            ta.beforeCommit();
        }
    }

    /**
     * Method declaration
     */
    public void afterCommit()
    {
        if(myObj instanceof TransactionAware)
        {
            TransactionAware ta = (TransactionAware) myObj;
            ta.afterCommit();
        }
    }

    /**
     * Method declaration
     */
    public void beforeAbort()
    {
        if(myObj instanceof TransactionAware)
        {
            TransactionAware ta = (TransactionAware) myObj;
            ta.beforeAbort();
        }
    }

    /**
     * Method declaration
     */
    public void afterAbort()
    {
        if(myObj instanceof TransactionAware)
        {
            TransactionAware ta = (TransactionAware) myObj;
            ta.afterAbort();
        }
    }

    /**
     * buildObjectImage() will return the image of the Object.
     */
    private Map buildObjectImage(PersistenceBroker broker) throws PersistenceBrokerException
    {
        Map imageMap = new HashMap();
        ClassDescriptor cld = broker.getClassDescriptor(getObject().getClass());
        //System.out.println("++++ build image: " + getObject());
        // register 1:1 references in image
        buildImageForSingleReferences(imageMap, cld);
        // put object values to image map
        buildImageForFields(imageMap, cld);
        // register 1:n and m:n references in image
        buildImageForCollectionReferences(imageMap, cld);
        return imageMap;
    }

    private void buildImageForSingleReferences(Map imageMap, ClassDescriptor cld)
    {
        // register all 1:1 references
        Iterator iter = cld.getObjectReferenceDescriptors(true).iterator();
        ObjectReferenceDescriptor rds;
        while(iter.hasNext())
        {
            rds = (ObjectReferenceDescriptor) iter.next();
            /*
            arminw:
            if a "super-reference" is matched (a 1:1 reference used to represent a super class)
            we don't handle it, because this will be done by the PB-api and will never be change
            */
            if(!rds.isSuperReferenceDescriptor())
            {
                Object referenceObject = rds.getPersistentField().get(myObj);

                IndirectionHandler handler = ProxyHelper.getIndirectionHandler(referenceObject);
                /*
                arminw:
                if object was serialized and anonymous FK are used in the main object, the FK
                values are null, we have to refresh (re-assign) these values before building field images.
                This will not touch the main object itself, because we only reassign anonymous FK fields.
                */
                if(handler == null && referenceObject != null
                        && BrokerHelper.hasAnonymousKeyReference(rds.getClassDescriptor(), rds))
                {
                    getBroker().serviceBrokerHelper().link(myObj, rds, false);
                }
                Image.SingleRef singleRef = new Image.SingleRef(this, rds, referenceObject);
                imageMap.put(rds, singleRef);
            }
        }
    }

    private void buildImageForFields(Map imageMap, ClassDescriptor cld)
    {
        // register all non reference fields of object (with inherited fields)
        FieldDescriptor[] fieldDescs = cld.getFieldDescriptor(true);
        for(int i = 0; i < fieldDescs.length; i++)
        {
            addFieldImage(imageMap, fieldDescs[i]);
        }
    }

    private void addFieldImage(Map imageMap, FieldDescriptor fld)
    {
        // register copies of all field values
        Object value = fld.getPersistentField().get(myObj);
        // get the real sql type value
        value = fld.getFieldConversion().javaToSql(value);
        // make copy of the sql type value
        value = fld.getJdbcType().getFieldType().copy(value);
        // buffer in image the field name and the sql type value
        // wrapped by a helper class
        imageMap.put(fld.getPersistentField().getName(), new Image.Field(fld.getJdbcType().getFieldType(), value));
    }

    private void buildImageForCollectionReferences(Map imageMap, ClassDescriptor cld)
    {
        // register the 1:n and m:n references
        Iterator collections = cld.getCollectionDescriptors(true).iterator();
        CollectionDescriptor cds;
        while(collections.hasNext())
        {
            cds = (CollectionDescriptor) collections.next();
            Object collectionOrArray = cds.getPersistentField().get(myObj);
            Image.MultipleRef colRef = new Image.MultipleRef(this, cds, collectionOrArray);
            imageMap.put(cds, colRef);
        }
    }

    /**
     * Returns the Modification-state.
     * @return org.apache.ojb.server.states.ModificationState
     */
    public ModificationState getModificationState()
    {
        return modificationState;
    }

    /**
     * Returns true if the underlying Object needs an INSERT statement, else returns false.
     */
    public boolean needsInsert()
    {
        return this.getModificationState().needsInsert();
    }

    /**
     * Returns true if the underlying Object needs an UPDATE statement, else returns false.
     */
    public boolean needsUpdate()
    {
        return this.getModificationState().needsUpdate();
    }

    /**
     * Returns true if the underlying Object needs an UPDATE statement, else returns false.
     */
    public boolean needsDelete()
    {
        return this.getModificationState().needsDelete();
    }

    /**
     * Sets the initial MoificationState of the wrapped object myObj. The initial state will be StateNewDirty if myObj
     * is not persisten already. The state will be set to StateOldClean if the object is already persistent.
     */
    private void prepareInitialState(boolean isNewObject)
    {
        // determine appropriate modification state
        ModificationState initialState;
        if(isNewObject)
        {
            // if object is not already persistent it must be marked as new
            // it must be marked as dirty because it must be stored even if it will not modified during tx
            initialState = StateNewDirty.getInstance();
        }
        else if(isDeleted(oid))
        {
            // if object is already persistent it will be marked as old.
            // it is marked as dirty as it has been deleted during tx and now it is inserted again,
            // possibly with new field values.
            initialState = StateOldDirty.getInstance();
        }
        else
        {
            // if object is already persistent it will be marked as old.
            // it is marked as clean as it has not been modified during tx already
            initialState = StateOldClean.getInstance();
        }
        // remember it:
        modificationState = initialState;
    }

    /**
     * Checks if the object with the given identity has been deleted
     * within the transaction.
     * @param id The identity
     * @return true if the object has been deleted
     * @throws PersistenceBrokerException
     */
    public boolean isDeleted(Identity id)
    {
        ObjectEnvelope envelope = buffer.getByIdentity(id);

        return (envelope != null && envelope.needsDelete());
    }

    /**
     * set the Modification state to a new value. Used during state transitions.
     * @param newModificationState org.apache.ojb.server.states.ModificationState
     */
    public void setModificationState(ModificationState newModificationState)
    {
        if(newModificationState != modificationState)
        {
            if(log.isDebugEnabled())
            {
                log.debug("object state transition for object " + this.oid + " ("
                        + modificationState + " --> " + newModificationState + ")");
//                try{throw new Exception();}catch(Exception e)
//                {
//                e.printStackTrace();
//                }
            }
            modificationState = newModificationState;
        }
    }

    /**
     * returns a String representation.
     * @return java.lang.String
     */
    public String toString()
    {
        ToStringBuilder buf = new ToStringBuilder(this);
        buf.append("Identity", oid)
            .append("ModificationState", modificationState.toString());
        return buf.toString();
    }

    /**
     * For internal use only! Only call immediately before commit to guarantee
     * that all changes can be detected (because this method cache the detected "change state"
     * thus on eager call changes could be ignored). Checks whether object and internal clone
     * differ and returns <em>true</em> if so, returns <em>false</em> else.
     *
     * @return boolean The result.
     */
    public boolean hasChanged(PersistenceBroker broker)
    {
        if(hasChanged == null)
        {
            Map current = null;
            try
            {
                current = getCurrentImage();
            }
            catch(Exception e)
            {
                log.warn("Could not verify object changes, mark dirty: " + getIdentity(), e);
            }
            if(beforeImage != null && current != null)
            {
                Iterator it = beforeImage.entrySet().iterator();
                hasChanged = Boolean.FALSE;
                while(it.hasNext())
                {
                    Map.Entry entry =  (Map.Entry) it.next();
                    Image imageBefore = (Image) entry.getValue();
                    Image imageCurrent = (Image) current.get(entry.getKey());
                    if(imageBefore.modified(imageCurrent))
                    {
                        hasChanged = Boolean.TRUE;
                        break;
                    }
                }
            }
            else
            {
                hasChanged = Boolean.TRUE;
            }
            if(log.isDebugEnabled())
            {
                log.debug("State detection for " + getIdentity() + " --> object "
                        + (hasChanged.booleanValue() ? "has changed" : "unchanged"));
            }
        }
        return hasChanged.booleanValue();
    }

    /**
     * Mark new or deleted reference elements
     * @param broker
     */
    void markReferenceElements(PersistenceBroker broker)
    {
        // these cases will be handled by ObjectEnvelopeTable#cascadingDependents()
        // if(getModificationState().needsInsert() || getModificationState().needsDelete()) return;

        Map oldImage = getBeforeImage();
        Map newImage = getCurrentImage();

        Iterator iter = newImage.entrySet().iterator();
        while (iter.hasNext())
        {
            Map.Entry entry = (Map.Entry) iter.next();
            Object key = entry.getKey();
            // we only interested in references
            if(key instanceof ObjectReferenceDescriptor)
            {
                Image oldRefImage = (Image) oldImage.get(key);
                Image newRefImage = (Image) entry.getValue();
                newRefImage.performReferenceDetection(oldRefImage);
            }
        }
    }

    public void doUpdate()
    {
        if(log.isDebugEnabled()) log.debug("Start UPDATE action for " + getIdentity());
        performLinkEntries();
        getBroker().store(getObject(), getIdentity(), getClassDescriptor(), false, true);
    }

    public void doInsert()
    {
        if(log.isDebugEnabled()) log.debug("Start INSERT action for " + getIdentity());
        performLinkEntries();
        getBroker().store(getObject(), getIdentity(), getClassDescriptor(), true, true);
        Identity oldOid = refreshIdentity();
        buffer.replaceRegisteredIdentity(getIdentity(), oldOid);
    }

    public void doDelete()
    {
        if(log.isDebugEnabled()) log.debug("Start DELETE action for " + getIdentity());
        getBroker().delete(getObject(), true);
    }

    public void doEvictFromCache()
    {
        if(log.isDebugEnabled()) log.debug("Remove from cache " + getIdentity());
        getBroker().removeFromCache(getIdentity());
    }

    public boolean isWriteLocked()
    {
        return writeLocked;
    }

    public void setWriteLocked(boolean writeLocked)
    {
        this.writeLocked = writeLocked;
    }

    ClassDescriptor getClassDescriptor()
    {
        return getBroker().getClassDescriptor(ProxyHelper.getRealClass(getObject()));
    }

    void addLinkOneToOne(ObjectReferenceDescriptor ord, boolean unlink)
    {
        LinkEntry entry = new LinkEntryOneToOne(ord, getObject(), unlink);
        linkEntryList.add(entry);
    }

    void addLinkOneToN(CollectionDescriptor col, Object source, boolean unlink)
    {
        if(col.isMtoNRelation()) throw new OJBRuntimeException("Expected an 1:n relation, but specified a m:n");
        LinkEntry entry = new LinkEntryOneToN(source, col, getObject(), unlink);
        linkEntryList.add(entry);
    }

    private void performLinkEntries()
    {
        PersistenceBroker broker = getBroker();
        for(int i = 0; i < linkEntryList.size(); i++)
        {
            LinkEntry linkEntry = (LinkEntry) linkEntryList.get(i);
            linkEntry.execute(broker);
        }
    }

    public void addedOneToOne(ObjectReferenceDescriptor ord, Object refObjOrProxy, Identity oid)
    {
        // the main objects needs link/unlink of the FK to 1:1 reference,
        // so mark this dirty
        setModificationState(getModificationState().markDirty());
        // if the object is already registered, OJB knows about
        // else lock and register object, get read lock, because we
        // don't know if the object is new or moved from an existing other object
        ObjectEnvelope oe = buffer.getByIdentity(oid);
        if(oe == null)
        {
            RuntimeObject rt = new RuntimeObject(refObjOrProxy, getTx());
            // we don't use cascade locking, because new reference object
            // will be detected by ObjectEnvelopeTable#cascadeMarkedForInsert()
            getTx().lockAndRegister(rt, TransactionExt.READ, false, getTx().getRegistrationList());
        }
        // in any case we need to link the main object
        addLinkOneToOne(ord, false);
    }

    public void deletedOneToOne(ObjectReferenceDescriptor ord, Object refObjOrProxy, Identity oid, boolean needsUnlink)
    {
        // the main objects needs link/unlink of the FK to 1:1 reference,
        // so mark this dirty
        setModificationState(getModificationState().markDirty());
        ObjectEnvelope oldRefMod = buffer.getByIdentity(oid);
        // only delete when the reference wasn't assigned with another object
        if(!buffer.isNewAssociatedObject(oid))
        {
            // if cascading delete is enabled, remove the 1:1 reference
            // because it was removed from the main object
            if(buffer.getTransaction().cascadeDeleteFor(ord))
            {
                oldRefMod.setModificationState(oldRefMod.getModificationState().markDelete());
            }
            // unlink the main object
            if(needsUnlink) addLinkOneToOne(ord, true);
        }
    }

    public void addedXToN(CollectionDescriptor cod, Object refObjOrProxy, Identity oid)
    {
        ObjectEnvelope mod = buffer.getByIdentity(oid);
        // if the object isn't registered already, it can be 'new' or already 'persistent'
        if(mod == null)
        {
            boolean isNew = getTx().isTransient(null, refObjOrProxy, oid);
            mod = buffer.get(oid, refObjOrProxy, isNew);
        }
        // if the object was deleted in an previous action, mark as new
        // to avoid deletion, else mark object as dirty to assign the FK of
        // the main object
        if(mod.needsDelete())
        {
            mod.setModificationState(mod.getModificationState().markNew());
        }
        else
        {
            /*
            arminw: if the reference is a m:n relation and the object state is
            old clean, no need to update the reference.
            */
            if(!(cod.isMtoNRelation() && mod.getModificationState().equals(StateOldClean.getInstance())))
            {
                mod.setModificationState(mod.getModificationState().markDirty());
            }
        }
        // buffer this object as "new" in a list to prevent deletion
        // when object was moved from one collection to another
        buffer.addNewAssociatedIdentity(oid);
        // new referenced object found, so register all m:n relation for "linking"
        if(cod.isMtoNRelation())
        {
            buffer.addM2NLinkEntry(cod, getObject(), refObjOrProxy);
        }
        else
        {
            // we have to link the new object
            mod.addLinkOneToN(cod, getObject(), false);
        }
        if(mod.needsInsert())
        {
            buffer.addForInsertDependent(mod);
        }
    }

    public void deletedXToN(CollectionDescriptor cod, Object refObjOrProxy, Identity oid)
    {
        ObjectEnvelope mod = buffer.getByIdentity(oid);
        // if this object is associated with another object it's
        // not allowed to remove it, thus nothing will change
        if(!buffer.isNewAssociatedObject(oid))
        {
            if(mod != null)
            {
                boolean cascade = buffer.getTransaction().cascadeDeleteFor(cod);
                if(cascade)
                {
                    mod.setModificationState(mod.getModificationState().markDelete());
                    buffer.addForDeletionDependent(mod);
                }
                if(cod.isMtoNRelation())
                {
                    buffer.addM2NUnlinkEntry(cod, getObject(), refObjOrProxy);
                }
                else
                {
                    // when cascade 'true' we remove all dependent objects, so no need
                    // to unlink, else we have to unlink all referenced objects of this
                    // object
                    if(!cascade)
                    {
                        mod.setModificationState(mod.getModificationState().markDirty());
                        mod.addLinkOneToN(cod, getObject(), true);
                    }
                }
            }
            else
            {
                throw new Image.ImageException("Unexpected behaviour, unregistered object to delete: "
                        + oid + ", main object is " + getIdentity()+ ", envelope object is " + this.toString());
            }
        }
    }
}