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
import java.util.HashMap;

import net.sf.cglib.proxy.Callback;
import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.Factory;

/**
 * @author andrew.clute
 *
 */
public class ProxyFactoryCGLIBImpl extends AbstractProxyFactory {

    HashMap proxyFactories = new HashMap();

    public Class getDefaultIndirectionHandlerClass() {
        return IndirectionHandlerCGLIBImpl.class;
    }

    /**
     * Returns the class of the base class that the given IndirectionHandler must extend/implement
     * 
     */
    public Class getIndirectionHandlerBaseClass() {
        return IndirectionHandlerCGLIB.class;
    }

    public OJBProxy createProxy(Class proxyClass, IndirectionHandler handler) throws Exception {

        Factory factory = (Factory)proxyFactories.get(proxyClass);
        Object result = null;
        if (factory == null) {
            Class[] interfaces;
            if (proxyClass.isInterface()) {
                interfaces = new Class[] { proxyClass, OJBProxy.class };
            } else {
                interfaces = new Class[] { OJBProxy.class };
            }

            result = (Factory)Enhancer.create(proxyClass, interfaces, (Callback)handler);
            proxyFactories.put(proxyClass, result);
        } else {
            result = factory.newInstance((Callback)handler);
        }
        return (OJBProxy)result;
    }

    public boolean isNormalOjbProxy(Object proxyOrObject) {
        return super.isNormalOjbProxy(proxyOrObject) && (proxyOrObject instanceof Factory);
    }

    public IndirectionHandler getDynamicIndirectionHandler(Object obj) {
        return (IndirectionHandler)((Factory)obj).getCallbacks()[0];

    }

    public boolean interfaceRequiredForProxyGeneration() {
        return false;
    }
    
    

}
