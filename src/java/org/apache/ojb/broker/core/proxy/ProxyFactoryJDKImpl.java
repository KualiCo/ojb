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


import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.HashMap;



/**
 * @author andrew.clute
 *
 */
public class ProxyFactoryJDKImpl extends AbstractProxyFactory {

    /**
     * JMM: Cache information about the interfaces need for dynamic proxy.
     */
    private HashMap foundInterfaces = new HashMap();
    
    
    public Class getDefaultIndirectionHandlerClass()
    {
        return IndirectionHandlerJDKImpl.class;
    }
    
    
    /**
     * Returns the class of the base class that the given IndirectionHandler must extend/implement
     * 
     */
    public Class getIndirectionHandlerBaseClass()
    {
        return IndirectionHandlerJDK.class;
    }
        
    

    public OJBProxy createProxy(Class baseClass, IndirectionHandler handler) throws Exception {
        Class proxyClass = getDynamicProxyClass(baseClass);
        Constructor constructor = proxyClass.getDeclaredConstructor(new Class[] { InvocationHandler.class });
        return (OJBProxy)constructor.newInstance(new Object[] { handler });
    }

    public IndirectionHandler getDynamicIndirectionHandler(Object obj) {
        return (IndirectionHandler)Proxy.getInvocationHandler(obj);
    }        

    public boolean isNormalOjbProxy(Object proxyOrObject) {
        return super.isNormalOjbProxy(proxyOrObject) && (proxyOrObject instanceof Proxy) && Proxy.isProxyClass(proxyOrObject.getClass());
    }

    /**
     * returns a dynamic Proxy that implements all interfaces of the
     * class described by this ClassDescriptor.
     *
     * @return Class the dynamically created proxy class
     */
    private Class getDynamicProxyClass(Class baseClass) {
        Class[] m_dynamicProxyClassInterfaces;
        if (foundInterfaces.containsKey(baseClass)) {
            m_dynamicProxyClassInterfaces = (Class[])foundInterfaces.get(baseClass);
        } else {
            m_dynamicProxyClassInterfaces = getInterfaces(baseClass);
            foundInterfaces.put(baseClass, m_dynamicProxyClassInterfaces);
        }

        // return dynymic Proxy Class implementing all interfaces
        Class proxyClazz = Proxy.getProxyClass(baseClass.getClassLoader(), m_dynamicProxyClassInterfaces);
        return proxyClazz;
    }

    /**
     * Get interfaces implemented by clazz
     *
     * @param clazz
     * @return
     */
    private Class[] getInterfaces(Class clazz) {
        Class superClazz = clazz;
        Class[] interfaces = clazz.getInterfaces();

        // clazz can be an interface itself and when getInterfaces()
        // is called on an interface it returns only the extending
        // interfaces, not the interface itself.
        if (clazz.isInterface()) {
            Class[] tempInterfaces = new Class[interfaces.length + 1];
            tempInterfaces[0] = clazz;

            System.arraycopy(interfaces, 0, tempInterfaces, 1, interfaces.length);
            interfaces = tempInterfaces;
        }

        // add all interfaces implemented by superclasses to the interfaces array
        while ((superClazz = superClazz.getSuperclass()) != null) {
            Class[] superInterfaces = superClazz.getInterfaces();
            Class[] combInterfaces = new Class[interfaces.length + superInterfaces.length];
            System.arraycopy(interfaces, 0, combInterfaces, 0, interfaces.length);
            System.arraycopy(superInterfaces, 0, combInterfaces, interfaces.length, superInterfaces.length);
            interfaces = combInterfaces;
        }

        /**
         * Must remove duplicate interfaces before calling Proxy.getProxyClass().
         * Duplicates can occur if a subclass re-declares that it implements
         * the same interface as one of its ancestor classes.
         **/
        HashMap unique = new HashMap();
        for (int i = 0; i < interfaces.length; i++) {
            unique.put(interfaces[i].getName(), interfaces[i]);
        }
        /* Add the OJBProxy interface as well */
        unique.put(OJBProxy.class.getName(), OJBProxy.class);

        interfaces = (Class[])unique.values().toArray(new Class[unique.size()]);

        return interfaces;
    }
    
    public boolean interfaceRequiredForProxyGeneration() {
        return true;
    }

}
