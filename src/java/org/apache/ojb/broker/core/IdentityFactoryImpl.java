package org.apache.ojb.broker.core;

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

import java.util.Map;

import org.apache.ojb.broker.Identity;
import org.apache.ojb.broker.IdentityFactory;
import org.apache.ojb.broker.OJBRuntimeException;
import org.apache.ojb.broker.PersistenceBroker;
import org.apache.ojb.broker.PersistenceBrokerException;
import org.apache.ojb.broker.PBStateListener;
import org.apache.ojb.broker.PBStateEvent;
import org.apache.ojb.broker.core.proxy.IndirectionHandler;
import org.apache.ojb.broker.core.proxy.ProxyHelper;
import org.apache.ojb.broker.metadata.ClassDescriptor;
import org.apache.ojb.broker.metadata.ClassNotPersistenceCapableException;
import org.apache.ojb.broker.metadata.FieldDescriptor;
import org.apache.ojb.broker.util.BrokerHelper;
import org.apache.ojb.broker.util.sequence.SequenceManager;
import org.apache.ojb.broker.util.sequence.SequenceManagerTransientImpl;
import org.apache.commons.lang.SystemUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.collections.map.ReferenceIdentityMap;

/**
 * @author <a href="mailto:armin@codeAuLait.de">Armin Waibel</a>
 * @version $Id: IdentityFactoryImpl.java,v 1.1 2007-08-24 22:17:35 ewestfal Exp $
 * @see org.apache.ojb.broker.IdentityFactory
 */
public class IdentityFactoryImpl implements IdentityFactory, PBStateListener
{
    private PersistenceBroker broker;
    //private boolean activeTx;
    private Map objectToIdentityMap;
    private SequenceManager transientSequenceManager;

    public IdentityFactoryImpl(PersistenceBroker broker)
    {
        this.broker = broker;
        this.objectToIdentityMap = new ReferenceIdentityMap(ReferenceIdentityMap.WEAK, ReferenceIdentityMap.HARD, true);
        this.transientSequenceManager = new SequenceManagerTransientImpl(broker);
        broker.addListener(this, true);
    }

    /**
     * This methods creates a new transient (if at least one PK field is 'null') or persistent
     * (if the PK fields are populated) {@link org.apache.ojb.broker.Identity} instance. If the specified object
     * is transient and former call for the same object returns already a transient Identity, the same transient
     * Identity object will be returned.
     */
    protected Identity createTransientOrRealIdentity(ClassDescriptor cld, Object objOrProxy)
    {
        if(objOrProxy == null) throw new OJBRuntimeException("Can't create Identity for 'null'-object");
        Identity result = null;
        Class topLevelClass = null;
        Class realClass = null;
        Object[] pks = null;
        try
        {
            final IndirectionHandler handler = ProxyHelper.getIndirectionHandler(objOrProxy);

            synchronized(objOrProxy)
            {
                if(handler != null)
                {
                    result = handler.getIdentity();
                }
                else
                {
                    // now we are sure that the specified object is not a proxy
                    realClass = objOrProxy.getClass();
                    topLevelClass = broker.getTopLevelClass(objOrProxy.getClass());
                    if(cld == null)
                    {
                        cld = broker.getClassDescriptor(objOrProxy.getClass());
                    }
                    BrokerHelper helper = broker.serviceBrokerHelper();

                    FieldDescriptor[] fields = cld.getPkFields();
                    pks = new Object[fields.length];
                    FieldDescriptor fld;
                    for(int i = 0; i < fields.length; i++)
                    {
                        fld = fields[i];
                        /*
                        we check all PK fields for 'null'-values
                        */
                        Object value = fld.getPersistentField().get(objOrProxy);
                        if(helper.representsNull(fld, value))
                        {
                            result = (Identity) objectToIdentityMap.get(objOrProxy);
                            if(result == null)
                            {
                                pks[i] = transientSequenceManager.getUniqueValue(fld);
                                result = new Identity(realClass, topLevelClass, pks, true);
                                //if(activeTx) objectToIdentityMap.put(objOrProxy, result);
                                objectToIdentityMap.put(objOrProxy, result);
                            }
                            break;
                        }
                        else
                        {
                            pks[i] = value;
                        }
                    }
                    if(result == null)
                    {
                        result = new Identity(realClass, topLevelClass, pks, false);
                    }
                }
            }
        }
        catch(ClassNotPersistenceCapableException e)
        {
            throw e;
        }
        catch(Exception e)
        {
            throw createException(e, "Can not init Identity for given object.", objOrProxy, topLevelClass, realClass, pks);
        }
        return result;
    }

    /** @see org.apache.ojb.broker.IdentityFactory#buildIdentity(Object) */
    public Identity buildIdentity(Object obj)
    {
        return createTransientOrRealIdentity(broker.getClassDescriptor(ProxyHelper.getRealClass(obj)), obj);
    }

    /** @see org.apache.ojb.broker.IdentityFactory#buildIdentity(Object) */
    public Identity buildIdentity(ClassDescriptor cld, Object obj)
    {
        return createTransientOrRealIdentity(cld, obj);
    }

    /** @see org.apache.ojb.broker.IdentityFactory#buildIdentity(Class, Class, String[], Object[]) */
    public Identity buildIdentity(Class realClass, Class topLevelClass, String[] pkFieldNames, Object[] pkValues)
    {
        Object[] orderedPKValues = pkValues;
        if(pkValues == null)
        {
            throw new NullPointerException("Given primary key value array can't be null");
        }
        if(pkValues.length == 1 && (pkFieldNames == null || pkFieldNames.length == 1))
        {
            /*
            we assume only a single PK field is defined and do no further checks,
            we have nothing to do
            */
        }
        else
        {
            // in other more complex cases we do several check
            FieldDescriptor[] flds = broker.getClassDescriptor(realClass).getPkFields();
            if(!isOrdered(flds, pkFieldNames))
            {
                orderedPKValues = reorderFieldValues(flds, pkFieldNames, pkValues);
            }
        }
        return new Identity(realClass, topLevelClass, orderedPKValues);
    }

    /**
     * This method orders the specified field values based on the
     * specified {@link org.apache.ojb.broker.metadata.FieldDescriptor}.
     *
     * @param flds The {@link org.apache.ojb.broker.metadata.FieldDescriptor} array.
     * @param fieldNames The field names.
     * @param fieldValues The field values.
     * @return The ordered field values.
     */
    private Object[] reorderFieldValues(FieldDescriptor[] flds, String[] fieldNames, Object[] fieldValues)
    {
        String fieldName;
        Object[] orderedValues = new Object[flds.length];
        for(int i = 0; i < flds.length; i++)
        {
            fieldName = flds[i].getPersistentField().getName();
            int realPosition = findIndexForName(fieldNames, fieldName);
            orderedValues[i] = fieldValues[realPosition];
        }
        return orderedValues;
    }

    /**
     * Find the index of the specified name in field name array.
     */
    private int findIndexForName(String[] fieldNames, String searchName)
    {
        for(int i = 0; i < fieldNames.length; i++)
        {
            if(searchName.equals(fieldNames[i]))
            {
                return i;
            }
        }
        throw new PersistenceBrokerException("Can't find field name '" + searchName +
                "' in given array of field names");
    }

    /** Checks length and compare order of field names with declared PK fields in metadata. */
    private boolean isOrdered(FieldDescriptor[] flds, String[] pkFieldNames)
    {
        if((flds.length > 1 && pkFieldNames == null) || flds.length != pkFieldNames.length)
        {
            throw new PersistenceBrokerException("pkFieldName length does not match number of defined PK fields." +
                    " Expected number of PK fields is " + flds.length + ", given number was " +
                    (pkFieldNames != null ? pkFieldNames.length : 0));
        }
        boolean result = true;
        for(int i = 0; i < flds.length; i++)
        {
            FieldDescriptor fld = flds[i];
            result = result && fld.getPersistentField().getName().equals(pkFieldNames[i]);
        }
        return result;
    }

    /** @see org.apache.ojb.broker.IdentityFactory#buildIdentity(Class, String[], Object[]) */
    public Identity buildIdentity(Class realClass, String[] pkFieldNames, Object[] pkValues)
    {
        return buildIdentity(realClass, broker.getTopLevelClass(realClass), pkFieldNames, pkValues);
    }

    /** @see org.apache.ojb.broker.IdentityFactory#buildIdentity(Class, String[], Object[]) */
    public Identity buildIdentity(Class realClass, Class topLevelClass, Object[] pkValues)
    {
        return new Identity(realClass, topLevelClass, pkValues);
    }

    /** @see org.apache.ojb.broker.IdentityFactory#buildIdentity(Class, Object) */
    public Identity buildIdentity(Class realClass, Object pkValue)
    {
        return buildIdentity(realClass, (String[]) null, new Object[]{pkValue});
    }

    /**
     * Helper method which supports creation of proper error messages.
     *
     * @param ex An exception to include or <em>null</em>.
     * @param message The error message or <em>null</em>.
     * @param objectToIdentify The current used object or <em>null</em>.
     * @param topLevelClass The object top-level class or <em>null</em>.
     * @param realClass The object real class or <em>null</em>.
     * @param pks The associated PK values of the object or <em>null</em>.
     * @return The generated exception.
     */
    private PersistenceBrokerException createException(final Exception ex, String message, final Object objectToIdentify, Class topLevelClass, Class realClass, Object[] pks)
    {
        final String eol = SystemUtils.LINE_SEPARATOR;
        StringBuffer msg = new StringBuffer();
        if(message == null)
        {
            msg.append("Unexpected error: ");
        }
        else
        {
            msg.append(message).append(" :");
        }
        if(topLevelClass != null) msg.append(eol).append("objectTopLevelClass=").append(topLevelClass.getName());
        if(realClass != null) msg.append(eol).append("objectRealClass=").append(realClass.getName());
        if(pks != null) msg.append(eol).append("pkValues=").append(ArrayUtils.toString(pks));
        if(objectToIdentify != null) msg.append(eol).append("object to identify: ").append(objectToIdentify);
        if(ex != null)
        {
            // add causing stack trace
            Throwable rootCause = ExceptionUtils.getRootCause(ex);
            if(rootCause != null)
            {
                msg.append(eol).append("The root stack trace is --> ");
                String rootStack = ExceptionUtils.getStackTrace(rootCause);
                msg.append(eol).append(rootStack);
            }

            return new PersistenceBrokerException(msg.toString(), ex);
        }
        else
        {
            return new PersistenceBrokerException(msg.toString());
        }
    }

    //===================================================================
    // PBStateListener interface
    //===================================================================
    public void afterBegin(PBStateEvent event)
    {
    }

    public void afterCommit(PBStateEvent event)
    {
        if(objectToIdentityMap.size() > 0) objectToIdentityMap.clear();
    }

    public void afterRollback(PBStateEvent event)
    {
        if(objectToIdentityMap.size() > 0) objectToIdentityMap.clear();
    }

    public void beforeClose(PBStateEvent event)
    {
        if(objectToIdentityMap.size() > 0) objectToIdentityMap.clear();
    }

    public void beforeRollback(PBStateEvent event)
    {
    }
    public void afterOpen(PBStateEvent event)
    {
    }
    public void beforeBegin(PBStateEvent event)
    {
    }
    public void beforeCommit(PBStateEvent event)
    {
    }
}
