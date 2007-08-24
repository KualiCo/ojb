package org.apache.ojb.broker.core.proxy;

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

import java.io.Serializable;
import java.lang.reflect.Method;

import org.apache.ojb.broker.Identity;
import org.apache.ojb.broker.PBKey;
import org.apache.ojb.broker.PersistenceBrokerException;



/**
 * Base interface for indirection handlers used by OJB's proxies. Note that this interface needs JDK 1.3 or above.
 * Implemening classes are required to have a public constructor with the signature
 * ({@link org.apache.ojb.broker.PBKey}, {@link org.apache.ojb.broker.Identity}).<br/>
 * Implementing classes should use this interface to acquire a logger.
 * 
 * @version $Id: IndirectionHandler.java,v 1.1 2007-08-24 22:17:32 ewestfal Exp $
 */
public interface IndirectionHandler extends Serializable
{
    /**
     * Returns the key of the persistence broker used by this indirection handler.
     * .
     * @return The broker key
     */
    PBKey getBrokerKey();

    /**
     * Returns the identity of the subject.
     * 
     * @return The identity
     */
    Identity getIdentity();

    /**
     * [Copied from {@link java.lang.reflect.InvocationHandler}]:<br/>
     * Processes a method invocation on a proxy instance and returns
     * the result.  This method will be invoked on an invocation handler
     * when a method is invoked on a proxy instance that it is
     * associated with.
     *
     * @param   proxy The proxy instance that the method was invoked on
     *
     * @param   method The <code>Method</code> instance corresponding to
     * the interface method invoked on the proxy instance.  The declaring
     * class of the <code>Method</code> object will be the interface that
     * the method was declared in, which may be a superinterface of the
     * proxy interface that the proxy class inherits the method through.
     *
     * @param   args An array of objects containing the values of the
     * arguments passed in the method invocation on the proxy instance,
     * or <code>null</code> if interface method takes no arguments.
     * Arguments of primitive types are wrapped in instances of the
     * appropriate primitive wrapper class, such as
     * <code>java.lang.Integer</code> or <code>java.lang.Boolean</code>.
     *
     * @return  The value to return from the method invocation on the
     * proxy instance.  If the declared return type of the interface
     * method is a primitive type, then the value returned by
     * this method must be an instance of the corresponding primitive
     * wrapper class; otherwise, it must be a type assignable to the
     * declared return type.  If the value returned by this method is
     * <code>null</code> and the interface method's return type is
     * primitive, then a <code>NullPointerException</code> will be
     * thrown by the method invocation on the proxy instance.  If the
     * value returned by this method is otherwise not compatible with
     * the interface method's declared return type as described above,
     * a <code>ClassCastException</code> will be thrown by the method
     * invocation on the proxy instance.
     *
     * @throws  PersistenceBrokerException The exception to throw from the method
     * invocation on the proxy instance.  The exception's type must be
     * assignable either to any of the exception types declared in the
     * <code>throws</code> clause of the interface method or to the
     * unchecked exception types <code>java.lang.RuntimeException</code>
     * or <code>java.lang.Error</code>.  If a checked exception is
     * thrown by this method that is not assignable to any of the
     * exception types declared in the <code>throws</code> clause of
     * the interface method, then an
     * {@link java.lang.reflect.UndeclaredThrowableException} containing the
     * exception that was thrown by this method will be thrown by the
     * method invocation on the proxy instance.
     *
     * @see java.lang.reflect.UndeclaredThrowableException
     */
    Object invoke(Object proxy, Method method, Object[] args);

    /**
     * Returns the proxies real subject. The subject will be materialized if necessary.
     * 
     * @return The subject
     */
    Object getRealSubject() throws PersistenceBrokerException;

    /**
     * Sets the real subject of this proxy.
     * [olegnitz] This looks stupid, but is really necessary for OTM:
     * the materialization listener replaces the real subject
     * by its clone to ensure transaction isolation.
     * Is there a better way to do this?
     */
    void setRealSubject(Object object);

    /**
     * Determines whether the real subject already has been materialized.
     * 
     * @return <code>true</code> if the real subject has already been loaded
     */
    boolean alreadyMaterialized();

    /**
     * Adds a materialization listener.
     * 
     * @param l The listener to add
     */
    void addListener(MaterializationListener l);

    /**
     * Removes a materialization listener.
     * 
     * @param l The listener to remove
     */
    void removeListener(MaterializationListener l);
}
