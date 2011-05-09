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

import org.apache.ojb.broker.*;
import org.apache.ojb.broker.core.PersistenceBrokerThreadMapping;
import org.apache.ojb.broker.util.logging.LoggerFactory;

import java.lang.reflect.Method;
import java.util.ArrayList;

/**
 * Abstract implementation for the indirection handler used by ojb's proxies.
 *
 * @version $Id: AbstractIndirectionHandler.java,v 1.1 2007-08-24 22:17:32 ewestfal Exp $
 */
public abstract class AbstractIndirectionHandler implements IndirectionHandler
{
    static final long serialVersionUID = -1993879565033755826L;

    /** The key for acquiring the above broker */
    private PBKey _brokerKey;
    /** The real subject which this is hidden by the proxy */
    private Object _realSubject = null;
    /** Represents the identity of the real subject. When the real subject is not
     *  yet materialized, it can be loaded from the underlying db by this identity object */
    private Identity _id = null;
    /** The materialization listeners */
    private transient ArrayList _listeners;

	/**
	 * Creates a new indirection handler for the indicated object.
	 *
	 * @param brokerKey
	 *            The key of the persistence broker
	 * @param id
	 *            The identity of the subject
	 */
	public AbstractIndirectionHandler(PBKey brokerKey, Identity id)
	{
		setBrokerKey(brokerKey);
		setIdentity(id);
	}

	/**
	 * Returns the identity of the subject.
	 *
	 * @return The identity
	 */
	public Identity getIdentity()
	{
		return _id;
	}

	/**
	 * Sets the identity of the subject of this indirection handler.
	 *
	 * @param identity
	 */
	protected void setIdentity(Identity identity)
	{
		_id = identity;
	}

	/**
	 * Returns the key of the persistence broker used by this indirection
	 * handler.
	 *
	 * @return The broker key
	 */
	public PBKey getBrokerKey()
	{
		return _brokerKey;
	}

	/**
	 * Sets the key of the persistence broker used by this indirection handler.
	 *
	 * @param brokerKey
	 *            The broker key
	 */
	protected void setBrokerKey(PBKey brokerKey)
	{
		_brokerKey = brokerKey;
	}

	/**
	 * Adds a materialization listener.
	 *
	 * @param listener
	 *            The listener to add
	 */
	public synchronized void addListener(MaterializationListener listener)
	{
		if (_listeners == null)
		{
			_listeners = new ArrayList();
		}
		// add listener only once
		if (!_listeners.contains(listener))
		{
			_listeners.add(listener);
		}
	}

	/**
	 * Removes a materialization listener.
	 *
	 * @param listener
	 *            The listener to remove
	 */
	public synchronized void removeListener(MaterializationListener listener)
	{
		if (_listeners != null)
		{
			_listeners.remove(listener);
		}
	}

	/**
	 * Calls beforeMaterialization on all registered listeners in the reverse
	 * order of registration.
	 */
	protected void beforeMaterialization()
	{
		if (_listeners != null)
		{
			MaterializationListener listener;

			for (int idx = _listeners.size() - 1; idx >= 0; idx--)
			{
				listener = (MaterializationListener) _listeners.get(idx);
				listener.beforeMaterialization(this, _id);
			}
		}
	}

	/**
	 * Calls afterMaterialization on all registered listeners in the reverse
	 * order of registration.
	 */
	protected void afterMaterialization()
	{
		if (_listeners != null)
		{
			MaterializationListener listener;

			// listeners may remove themselves during the afterMaterialization
			// callback.
			// thus we must iterate through the listeners vector from back to
			// front
			// to avoid index problems.
			for (int idx = _listeners.size() - 1; idx >= 0; idx--)
			{
				listener = (MaterializationListener) _listeners.get(idx);
				listener.afterMaterialization(this, _realSubject);
			}
		}
	}

    /**
     * Gets the persistence broker used by this indirection handler.
     * If no PBKey is available a runtime exception will be thrown.
     *
     * @return a PersistenceBroker
     */
    protected TemporaryBrokerWrapper getBroker() throws PBFactoryException
    {
        PersistenceBrokerInternal broker;
        boolean needsClose = false;

        if (getBrokerKey() == null)
        {
            /*
            arminw:
            if no PBKey is set we throw an exception, because we don't
            know which PB (connection) should be used.
            */
            throw new OJBRuntimeException("Can't find associated PBKey. Need PBKey to obtain a valid" +
                                          "PersistenceBroker instance from intern resources.");
        }
        // first try to use the current threaded broker to avoid blocking
        broker = PersistenceBrokerThreadMapping.currentPersistenceBroker(getBrokerKey());
        // current broker not found, create a intern new one
        if ((broker == null) || broker.isClosed())
        {
            broker = (PersistenceBrokerInternal) PersistenceBrokerFactory.createPersistenceBroker(getBrokerKey());
            /** Specifies whether we obtained a fresh broker which we have to close after we used it */
            needsClose = true;
        }
        return new TemporaryBrokerWrapper(broker, needsClose);
    }

	/**
	 * [Copied from {@link java.lang.reflect.InvocationHandler}]:<br/>
	 * Processes a method invocation on a proxy instance and returns the result.
	 * This method will be invoked on an invocation handler when a method is
	 * invoked on a proxy instance that it is associated with.
	 *
	 * @param proxy
	 *            The proxy instance that the method was invoked on
	 *
	 * @param method
	 *            The <code>Method</code> instance corresponding to the
	 *            interface method invoked on the proxy instance. The declaring
	 *            class of the <code>Method</code> object will be the
	 *            interface that the method was declared in, which may be a
	 *            superinterface of the proxy interface that the proxy class
	 *            inherits the method through.
	 *
	 * @param args
	 *            An array of objects containing the values of the arguments
	 *            passed in the method invocation on the proxy instance, or
	 *            <code>null</code> if interface method takes no arguments.
	 *            Arguments of primitive types are wrapped in instances of the
	 *            appropriate primitive wrapper class, such as
	 *            <code>java.lang.Integer</code> or
	 *            <code>java.lang.Boolean</code>.
	 *
	 * @return The value to return from the method invocation on the proxy
	 *         instance. If the declared return type of the interface method is
	 *         a primitive type, then the value returned by this method must be
	 *         an instance of the corresponding primitive wrapper class;
	 *         otherwise, it must be a type assignable to the declared return
	 *         type. If the value returned by this method is <code>null</code>
	 *         and the interface method's return type is primitive, then a
	 *         <code>NullPointerException</code> will be thrown by the method
	 *         invocation on the proxy instance. If the value returned by this
	 *         method is otherwise not compatible with the interface method's
	 *         declared return type as described above, a
	 *         <code>ClassCastException</code> will be thrown by the method
	 *         invocation on the proxy instance.
	 *
	 * @throws PersistenceBrokerException
	 *             The exception to throw from the method invocation on the
	 *             proxy instance. The exception's type must be assignable
	 *             either to any of the exception types declared in the
	 *             <code>throws</code> clause of the interface method or to
	 *             the unchecked exception types
	 *             <code>java.lang.RuntimeException</code> or
	 *             <code>java.lang.Error</code>. If a checked exception is
	 *             thrown by this method that is not assignable to any of the
	 *             exception types declared in the <code>throws</code> clause
	 *             of the interface method, then an
	 *             {@link java.lang.reflect.UndeclaredThrowableException}
	 *             containing the exception that was thrown by this method will
	 *             be thrown by the method invocation on the proxy instance.
	 *
	 * @see java.lang.reflect.UndeclaredThrowableException
	 */
	public Object invoke(Object proxy, Method method, Object[] args)
	{
		Object subject;
		String methodName = method.getName();

		try
		{
			// [andrew clute]
			// short-circuit any calls to a finalize methjod if the subject
			// has not been retrieved yet
			if ("finalize".equals(methodName) && _realSubject == null)
			{
				return null;
			}

			// [andrew clute]
			// When trying to serialize a proxy, we need to determine how to
			// handle it
			if ("writeReplace".equals(methodName))
			{
				if (_realSubject == null)
				{
					// Unmaterialized proxies are replaced by simple
					// serializable
					// objects that can be unserialized without classloader
					// issues
					return generateSerializableProxy();
				} else
				{
					// Materiliazed objects should be passed back as they might
					// have
					// been mutated
					return getRealSubject();
				}
			}

			// [tomdz]
			// Previously the hashcode of the identity would have been used
			// but this requires a compatible hashCode implementation in the
			// proxied object (which is somewhat unlikely, even the default
			// hashCode implementation does not fulfill this requirement)
			// for those that require this behavior, a custom indirection
			// handler can be used, or the hashCode of the identity
			/*
			 * if ("hashCode".equals(methodName)) { return new
			 * Integer(_id.hashCode()); }
			 */

			// [tomdz]
			// this would handle toString differently for non-materialized
			// proxies
			// (to avoid materialization due to logging)
			// however toString should be a normal business method which
			// materializes the proxy
			// if this is not desired, then the ProxyHandler.toString(Object)
			// method
			// should be used instead (e.g. for logging within OJB)
			/*
			 * if ((realSubject == null) && "toString".equals(methodName)) {
			 * return "unmaterialized proxy for " + id; }
			 */

			// BRJ: make sure that the object to be compared is a real object
			// otherwise equals may return false.
			if ("equals".equals(methodName) && args[0] != null)
			{
                TemporaryBrokerWrapper tmp = getBroker();
                try
                {
                    args[0] = tmp.broker.getProxyFactory().getRealObject(args[0]);
                }
                finally
                {
                    tmp.close();
                }
            }

			if ("getIndirectionHandler".equals(methodName) && args[0] != null)
			{
				return this;
			}

			subject = getRealSubject();

            //kuali modification start
			try {
                method.setAccessible(true);
            } catch (SecurityException ex) {
                LoggerFactory.getLogger(IndirectionHandler.class).warn(
                        "Error calling setAccessible for method " + method.getName(), ex);
            }
            //kuali modification end

			return method.invoke(subject, args);
			// [olegnitz] I've changed the following strange lines
			// to the above one. Why was this done in such complicated way?
			// Is it possible that subject doesn't implement the method's
			// interface?
			// Method m = subject.getClass().getMethod(method.getName(),
			// method.getParameterTypes());
			// return m.invoke(subject, args);
		} catch (Exception ex)
		{
			throw new PersistenceBrokerException("Error invoking method " + method.getName(), ex);
		}
	}

	/**
	 * Returns the proxies real subject. The subject will be materialized if
	 * necessary.
	 *
	 * @return The subject
	 */
	public Object getRealSubject() throws PersistenceBrokerException
	{
		if (_realSubject == null)
		{
			beforeMaterialization();
			_realSubject = materializeSubject();
			afterMaterialization();
		}
		return _realSubject;
	}

	/**
	 * [olegnitz] This looks stupid, but is really necessary for OTM: the
	 * materialization listener replaces the real subject by its clone to ensure
	 * transaction isolation. Is there a better way to do this?
	 */
	public void setRealSubject(Object object)
	{
		_realSubject = object;
	}

	/**
	 * Retrieves the real subject from the underlying RDBMS. Override this
	 * method if the object is to be materialized in a specific way.
	 *
	 * @return The real subject of the proxy
	 */
	protected synchronized Object materializeSubject() throws PersistenceBrokerException
	{
		TemporaryBrokerWrapper tmp = getBroker();
        try
		{
			Object realSubject = tmp.broker.getObjectByIdentity(_id);
			if (realSubject == null)
			{
				LoggerFactory.getLogger(IndirectionHandler.class).warn(
						"Can not materialize object for Identity " + _id + " - using PBKey " + getBrokerKey());
			}
			return realSubject;
		} catch (Exception ex)
		{
			throw new PersistenceBrokerException(ex);
		} finally
		{
			tmp.close();
		}
	}

	/**
	 * Determines whether the real subject already has been materialized.
	 *
	 * @return <code>true</code> if the real subject has already been loaded
	 */
	public boolean alreadyMaterialized()
	{
		return _realSubject != null;
	}

	/**
	 * Generate a simple object that is serializable and placeholder for
	 * proxies.
	 *
	 */
	private Object generateSerializableProxy()
	{
		return new OJBSerializableProxy(getIdentity().getObjectsRealClass(), this);
	}

    //===================================================================
    // inner class
    //===================================================================
    /**
     * wrapper class for temporary used broker instances.
     */
    static final class TemporaryBrokerWrapper
    {
        /** Specifies whether we obtained a fresh broker which we have to close after we used it */
        boolean needsClose;
        PersistenceBrokerInternal broker;

        public TemporaryBrokerWrapper(PersistenceBrokerInternal broker, boolean needsClose)
        {
            this.broker = broker;
            this.needsClose = needsClose;
        }

        /**
         * Cleanup the used broker instance, it's mandatory to call
         * this method after use.
         */
        public void close()
        {
            if(needsClose)
            {
                broker.close();
            }
        }
    }
}
