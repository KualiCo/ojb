package org.apache.ojb.jboss;

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

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.Name;
import javax.naming.NameNotFoundException;
import javax.naming.NamingException;
import java.io.Serializable;

import org.apache.ojb.odmg.ODMGJ2EEFactory;
import org.apache.ojb.odmg.OJB;
import org.jboss.system.ServiceMBeanSupport;
import org.odmg.Implementation;

public class ODMGFactory extends ServiceMBeanSupport
        implements ODMGJ2EEFactory, Serializable, ODMGFactoryMBean
{
    private static final String JAVA_NAMESPACE = "java:/";

    private String jndiName;

    public ODMGFactory()
    {
		System.out.println("ODMGFactory called");
    }

    protected void startService() throws Exception
    {
        try
        {
            bind(new InitialContext(), JAVA_NAMESPACE + jndiName, this);
        }
        catch (Exception e)
        {
            System.out.println("Binding ODMG to JNDI failed");
            e.printStackTrace();
            throw e;
        }
        System.out.println("** OJB-ODMG MBean integration");
        System.out.println("** ODMGFactory: " + this.getClass().getName());
        System.out.println("** Use ODMGFactory via lookup:");
        System.out.println("** ODMGFactory factory = (ODMGFactory) ctx.lookup(" + JAVA_NAMESPACE + jndiName + ")");
        System.out.println("** Implementation odmg = factory.getInstance();");
    }

    public void stopService()
    {
        try
        {
            (new InitialContext()).unbind(JAVA_NAMESPACE + jndiName);
        }
        catch (NamingException namingexception)
        {
            namingexception.printStackTrace();
        }
    }

    public Implementation getInstance()
    {
        return OJB.getInstance();
    }

    public void setJndiName(String jndiName)
    {
        this.jndiName = jndiName;
    }

    public String getJndiName()
    {
        return jndiName;
    }

    private void bind(Context ctx, String name, Object val) throws NamingException
    {
        Name n;
        for (n = ctx.getNameParser("").parse(name); n.size() > 1; n = n.getSuffix(1))
        {
            String ctxName = n.get(0);
            try
            {
                ctx = (Context) ctx.lookup(ctxName);
            }
            catch (NameNotFoundException namenotfoundexception)
            {
                ctx = ctx.createSubcontext(ctxName);
            }
        }
        ctx.bind(n.get(0), val);
    }
}
