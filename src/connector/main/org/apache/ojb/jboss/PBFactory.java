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

import org.jboss.system.ServiceMBeanSupport;
import org.apache.ojb.broker.core.PersistenceBrokerFactoryIF;
import org.apache.ojb.broker.core.PersistenceBrokerFactoryFactory;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.naming.Context;
import javax.naming.Name;
import javax.naming.NameNotFoundException;
import java.io.Serializable;

/**
 * mbean for the PersistenceBrokerFactory
 *
 * @author <a href="mailto:armin@codeAuLait.de">Armin Waibel</a>
 */

public class PBFactory extends ServiceMBeanSupport implements PBFactoryMBean, Serializable
{
    private String _jndiName;
    private static final String JAVA_NAMESPACE = "java:/";

    public PersistenceBrokerFactoryIF getInstance()
    {
        return PersistenceBrokerFactoryFactory.instance();
    }

    public String getName()
    {
        return "PBAPI-Implementation";
    }

    protected void startService()
            throws Exception
    {
        try
        {
            bind(new InitialContext(), JAVA_NAMESPACE + _jndiName, this);
        }
        catch (Throwable e)
        {
            e.printStackTrace();
        }
        System.out.println("** OJB-PB MBean integration");
        System.out.println("** PBFactory: "+this.getClass().getName()+" / "+this.getServiceName().toString());
        System.out.println("** Lookup PersistenceBrokerFactory via '"+JAVA_NAMESPACE+_jndiName+"'");
    }

    public void stopService()
    {
        try
        {
            (new InitialContext()).unbind(JAVA_NAMESPACE + _jndiName);
        }
        catch (NamingException namingexception)
        {
            namingexception.printStackTrace();
        }
    }

    public void setJndiName(String jndiName)
    {
        _jndiName = jndiName;
    }

    public String getJndiName()
    {
        return _jndiName;
    }

    private void bind(Context ctx, String name, Object val)
            throws NamingException
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
