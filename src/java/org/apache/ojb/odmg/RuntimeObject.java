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

import org.apache.ojb.broker.Identity;
import org.apache.ojb.broker.core.proxy.IndirectionHandler;
import org.apache.ojb.broker.core.proxy.ProxyHelper;
import org.apache.ojb.broker.metadata.ClassDescriptor;
import org.apache.commons.lang.builder.ToStringBuilder;

/**
 * Helper object encapsulates common used object properties/states, help to reduce
 * needless metadata calls.
 *
 * @author <a href="mailto:arminw@apache.org">Armin Waibel</a>
 * @version $Id: RuntimeObject.java,v 1.1 2007-08-24 22:17:37 ewestfal Exp $
 */
public final class RuntimeObject
{
    private final Object obj;
    private Identity identity;
    private final TransactionImpl tx;
    private Boolean isNew;
    private ClassDescriptor cld;
    private IndirectionHandler handler;

    public RuntimeObject(final Object obj, final TransactionImpl tx)
    {
        this.tx = tx;
        this.obj = obj;
        initCld(tx);
        doIsNewObjectCheck(tx);
    }

    public RuntimeObject(final Object obj, final TransactionImpl tx, final boolean isNew)
    {
        this.tx = tx;
        this.obj = obj;
        this.isNew = isNew ? Boolean.TRUE : Boolean.FALSE;
        initCld(tx);
    }

    public RuntimeObject(final Object obj, final Identity identity, final TransactionImpl tx, final boolean isNew)
    {
        this.tx = tx;
        this.obj = obj;
        this.identity = identity;
        this.isNew = isNew ? Boolean.TRUE : Boolean.FALSE;
        initCld(tx);
    }

    public RuntimeObject(final Object obj, final Identity oid, final ClassDescriptor cld, final boolean isNew, final boolean isProxy)
    {
        this.tx = null;
        this.obj = obj;
        this.identity = oid;
        this.isNew = isNew ? Boolean.TRUE : Boolean.FALSE;
        this.cld = cld;
        if(isProxy)
        {
            this.handler = ProxyHelper.getIndirectionHandler(obj);
        }
    }

    /*
    try to avoid needless and unused method calls to provide
    best performance, thus create Identity object only if needed
    and do 'is new object' check only if needed.
    */
    private void initCld(final TransactionImpl tx)
    {
        final IndirectionHandler handler = ProxyHelper.getIndirectionHandler(obj);
        if(handler != null)
        {
            this.handler = handler;
            isNew = Boolean.FALSE;
            identity = handler.getIdentity();
            if(handler.alreadyMaterialized())
            {
                cld = tx.getBroker().getClassDescriptor(handler.getRealSubject().getClass());
            }
            else
            {
                cld = tx.getBroker().getClassDescriptor(identity.getObjectsRealClass());
            }
        }
        else
        {
            cld = tx.getBroker().getClassDescriptor(obj.getClass());
        }
    }

    void doIsNewObjectCheck(final TransactionImpl tx)
    {
        boolean isNew = tx.isTransient(cld, obj, null);
        this.isNew = isNew ? Boolean.TRUE : Boolean.FALSE;
    }

    /**
     * Return the associated persistent object.
     */
    public Object getObj()
    {
        return obj;
    }

    /**
     * Returns the materialized object (if proxy is materialized or a "normal"
     * persistent object) or <em>null</em> if associated with unmaterialized proxy object.
     */
    public Object getObjMaterialized()
    {
        return handler != null ? (handler.alreadyMaterialized() ? handler.getRealSubject() : null) : obj;
    }

    /**
     * Returns the associated object {@link org.apache.ojb.broker.Identity}.
     */
    public Identity getIdentity()
    {
        if(identity == null)
        {
            identity = tx.getBroker().serviceIdentity().buildIdentity(obj);
        }
        return identity;
    }

    /**
     * Returns the associated object {@link org.apache.ojb.broker.metadata.ClassDescriptor}.
     */
    public ClassDescriptor getCld()
    {
        return cld;
    }

    /**
     * Returns <code>true</code> if the represented object is
     * not yet persisted.
     */
    public boolean isNew()
    {
        return isNew.booleanValue();
    }

    public boolean isProxy()
    {
        return handler != null;
    }

    public IndirectionHandler getHandler()
    {
        return handler;
    }

    public String toString()
    {
        return new ToStringBuilder(this)
                .append("identity", identity)
                .append("isNew", isNew)
                .append("isProxy", handler != null)
                .append("handler", handler)
                .append("tx", tx)
                .toString();
    }
}

