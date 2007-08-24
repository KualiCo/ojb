package org.apache.ojb.ejb;

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

import javax.ejb.EJBException;
import javax.ejb.SessionBean;
import javax.ejb.SessionContext;

/**
 * Base session bean implementation class
 * for EJB2.0 or higher. Note: Derived classes
 * must implement the {@link javax.ejb.SessionBean}
 * interface (got problems using xdoclet when
 * implement this interface with this class).
 *
 * @author <a href="mailto:armin@codeAuLait.de">Armin Waibel</a>
 * @version $Id: SessionBeanImpl.java,v 1.1 2007-08-24 22:17:39 ewestfal Exp $
 */
public abstract class SessionBeanImpl
{
    private SessionContext ctx;

    public void ejbCreate()
    {

    }

    public void setSessionContext(SessionContext ctx)
    {
        this.ctx = ctx;
    }

    public SessionContext getSessionContext()
    {
        return ctx;
    }

    public void ejbActivate() throws EJBException
    {
    }

    public void ejbPassivate() throws EJBException
    {
    }

    public void ejbRemove() throws EJBException
    {
        this.ctx = null;
    }
}
