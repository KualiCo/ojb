package org.apache.ojb.ejb;

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
import javax.naming.NamingException;
import java.util.Properties;

/**
 * Helper class for client side JNDI context lookup.
 * This class lookup system properties for {@link Context#INITIAL_CONTEXT_FACTORY},
 * {@link Context#PROVIDER_URL} and {@link Context#URL_PKG_PREFIXES}. If not set,
 * InitialContext for JBoss (with default settings) was instantiated.
 *
 * @author <a href="mailto:armin@codeAuLait.de">Armin Waibel</a>
 * @version $Id: ContextHelper.java,v 1.1 2007-08-24 22:17:39 ewestfal Exp $
 */
public class ContextHelper
{
    private static InitialContext ctx;

    public synchronized static Context getContext()
    {
        if(ctx == null)
        {
            Properties prop = System.getProperties();
            // if jndi-properties not set as system properties, take jboss/jonas jndi
            // properties as default
            if (prop.getProperty(Context.INITIAL_CONTEXT_FACTORY) == null)
            {
                System.out.println("System property " + Context.INITIAL_CONTEXT_FACTORY
                        + " is not set. Try to use default setting for JNDI lookup");
                ctx = jbossJNDI();
                if(ctx == null) ctx = jonasJNDI();
                if(ctx == null) throw new RuntimeException("Can't initialize naming context");
            }
            else
            {
                try
                {
                    ctx = new InitialContext(prop);
                }
                catch (NamingException e)
                {
                    e.printStackTrace();
                }
            }
            try
            {
                System.out.println("Used JNDI Context: " + ctx.getEnvironment());
//                System.out.println("Namespace=" + ctx.getNameInNamespace());
//                System.out.println("Bindings: ==>");
//                String nameSp = ctx.getNameInNamespace();
//                NamingEnumeration enu = ctx.listBindings(nameSp);
//                if(!enu.hasMore())
//                {
//                    System.out.println("no bindings found for '" + ctx.getNameInNamespace() + "'");
//                }
//                while(enu.hasMore())
//                {
//                    System.out.println("element: " + enu.nextElement());
//                }
            }
            catch(NamingException e)
            {
                e.printStackTrace();
            }
        }

        return ctx;
    }

    private static InitialContext jbossJNDI()
    {
        InitialContext ctx = null;
        try
        {
            System.out.println("Try to use JBoss naming service");
            Properties prop = jbossNamingProperties();
            ctx = new InitialContext(prop);
        }
        catch(NamingException e)
        {
            System.err.println("JBoss JNDI lookup failed: " + e.getMessage());
        }
        return ctx;
    }

    private static InitialContext jonasJNDI()
    {
        InitialContext ctx = null;
        try
        {
            System.out.println("Try to use JOnAS naming service");
            Properties prop = jonasNamingProperties();
            ctx = new InitialContext(prop);
        }
        catch(NamingException e)
        {
            System.err.println("JonAS JNDI lookup failed: " + e.getMessage());
            e.printStackTrace();
        }
        return ctx;
    }

    private static Properties jbossNamingProperties()
    {
        Properties prop = new Properties();
        prop.setProperty(Context.INITIAL_CONTEXT_FACTORY, "org.jnp.interfaces.NamingContextFactory");
        prop.setProperty(Context.PROVIDER_URL, "jnp://localhost:1099");
        prop.setProperty(Context.URL_PKG_PREFIXES, "org.jboss.naming:org.jnp.interfaces");
        return prop;
    }

    private static Properties jonasNamingProperties()
    {
        Properties prop = new Properties();
        prop.setProperty(Context.INITIAL_CONTEXT_FACTORY, "org.objectweb.carol.jndi.spi.MultiOrbInitialContextFactory");
        //prop.setProperty(Context.PROVIDER_URL, "jrmi://localhost:2000");
        //prop.setProperty(Context.URL_PKG_PREFIXES, "org.objectweb.jonas.naming");
        return prop;
    }
}
