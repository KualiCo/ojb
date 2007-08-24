package org.apache.ojb.ejb.odmg;

/* Copyright 2004-2005 The Apache Software Foundation
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
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.apache.ojb.broker.OJBRuntimeException;
import org.apache.ojb.broker.util.logging.Logger;
import org.apache.ojb.broker.util.logging.LoggerFactory;
import org.apache.ojb.ejb.ArticleVO;
import org.apache.ojb.ejb.PersonVO;
import org.odmg.Database;
import org.odmg.Implementation;
import org.odmg.OQLQuery;
import org.odmg.QueryException;
import org.odmg.Transaction;

/**
 * This is an session bean implementation using odmg implementation.
 * <br/>
 * For more structured implementations take a look at
 * <br/>
 * {@link org.apache.ojb.ejb.SessionBeanImpl}<br/>
 * {@link org.apache.ojb.ejb.odmg.ODMGBaseBeanImpl}<br/>
 * {@link org.apache.ojb.ejb.pb.PBBaseBeanImpl}
 * <p>
 * <b>How to use ODMG</b> <br>
 *
 * To keep this example as simple as possible, we lookup a static OJB ODMG implementation instance
 * on each bean instance.
 * But it's recommended to bind an instance of the Implementation class in JNDI
 * (at appServer start), open the database and lookup this instances via JNDI in
 * ejbCreate().
 *
 * To use the odmg-api within your bean, you can do:
 *
 * <ol type="a">
 *	<li>
 *	Obtain the current Database from the Implementation instance - Attend<br>
 *	that there must be already a Database opened before.<br><i>
 *	db = odmg.getDatabase(null);<br>
 *	// ... do something<br>
 *	</i></li>
 *	<li>
 *	Obtain the current odmg-Transaction from the Implementation instance<br>
 *	to lock objects - Attend that there must be already a Database opened before.<br><i>
 *	Transaction tx = odmg.currentTransaction();<br>
 *	tx.lock(aObject, mode);
 *	</i></li>
 * </ol>
 * </p>
 *
 *
 * @ejb:bean
 * 			type="Stateless"
 * 			name="ODMGSessionBean"
 * 			jndi-name="org.apache.ojb.ejb.odmg.ODMGSessionBean"
 * 			local-jndi-name="org.apache.ojb.ejb.odmg.ODMGSessionBeanLocal"
 * 			view-type="both"
 * 			transaction-type="Container"
 *
 * @ejb:interface
 * 		remote-class="org.apache.ojb.ejb.odmg.ODMGSessionRemote"
 * 		local-class="org.apache.ojb.ejb.odmg.ODMGSessionLocal"
 * 		extends="javax.ejb.EJBObject"
 *
 * @ejb:home
 * 		remote-class="org.apache.ojb.ejb.odmg.ODMGSessionHome"
 * 		local-class="org.apache.ojb.ejb.odmg.ODMGSessionLocalHome"
 * 		extends="javax.ejb.EJBHome"
 *
 * @ejb:transaction
 * 		type="Required"
 *
 *
 * @author <a href="mailto:armin@codeAuLait.de">Armin Waibel</a>
 * @version $Id: ODMGSessionBean.java,v 1.1 2007-08-24 22:17:30 ewestfal Exp $
 */
public class ODMGSessionBean implements SessionBean
{
    private Logger log = LoggerFactory.getLogger(ODMGSessionBean.class);
    private SessionContext ctx;
    private Implementation odmg;
    private Database db;

    public ODMGSessionBean()
    {
    }

    /**
     * Lookup the OJB ODMG implementation.
     * It's recommended to bind an instance of the Implementation class in JNDI
     * (at appServer start), open the database and lookup this instance via JNDI in
     * ejbCreate().
     */
    public void ejbCreate()
    {
        log.info("ejbCreate was called");
        odmg = ODMGHelper.getODMG();
        db = odmg.getDatabase(null);
    }

    public void ejbRemove()
    {
        db = null;
        odmg = null;
        ctx = null;
    }

    /**
     * @ejb:interface-method
     */
    public List storeObjects(List objects)
    {
        if(log.isDebugEnabled()) log.debug("storeObjects");

        /* One possibility of storing objects is to use the current transaction
         associated with the container */
        Transaction tx = odmg.currentTransaction();
        for (Iterator iterator = objects.iterator(); iterator.hasNext();)
        {
            tx.lock(iterator.next(), Transaction.WRITE);
        }
        return objects;
    }

    /**
     * @ejb:interface-method
     */
    public void deleteObjects(List objects)
    {
        if(log.isDebugEnabled()) log.debug("deleteObjects");
        db = odmg.getDatabase(null);
        for (Iterator iterator = objects.iterator(); iterator.hasNext();)
        {
            db.deletePersistent(iterator.next());
        }
    }

    protected int getObjectCount(Implementation ojb, Class target)
    {
        if(log.isDebugEnabled()) log.debug("getObjectCount was called");
        List list;
        try
        {
            OQLQuery query = ojb.newOQLQuery();
            query.create("select allObjects from " + target.getName());
            list = (List) query.execute();
            return list.size();
        }
        catch (QueryException e)
        {
            throw new EJBException("Query objects failed", e);
        }
    }

    /**
     * @ejb:interface-method
     */
    public int getArticleCount()
    {
        if(log.isDebugEnabled()) log.debug("getArticleCount was called");
        return getObjectCount(odmg, ArticleVO.class);
    }

    /**
     * @ejb:interface-method
     */
    public Collection getArticlesByName(String articleName)
    {
        if(log.isDebugEnabled()) log.debug("getArticlesByName was called");
        try
        {
            OQLQuery query = odmg.newOQLQuery();
            query.create("select allArticles from " + ArticleVO.class.getName() + " where name like $1");
            query.bind(articleName);
            return (Collection) query.execute();
        }
        catch (QueryException e)
        {
            throw new EJBException("Query objects failed", e);
        }
    }

    /**
     * @ejb:interface-method
     */
    public int getPersonCount()
    {
        if(log.isDebugEnabled()) log.debug("getPersonCount was called");
        return getObjectCount(odmg, PersonVO.class);
    }

    /**
     * @ejb:interface-method
     */
    public boolean allInOne(List articles, List persons)
    {
        boolean passedWell = true;
        if(log.isDebugEnabled()) log.debug("allInOne method was called");
        StringBuffer buf = new StringBuffer();

        String sep = System.getProperty("line.separator");

        db = odmg.getDatabase(null);

        int personsBefore = getPersonCount();
        int articlesBefore = getArticleCount();
        buf.append(sep + "# Start with " + personsBefore + " persons");
        buf.append(sep + "# Start with " + articlesBefore + " articles");
        storeObjects(articles);
        storeObjects(persons);
        int personsAfterStore = getPersonCount();
        int articlesAfterStore = getArticleCount();
        buf.append(sep + "# After store: " + personsAfterStore + " persons");
        buf.append(sep + "# After store: " + articlesAfterStore + " articles");
        deleteObjects(articles);
        deleteObjects(persons);
        int personsAfterDelete = getPersonCount();
        int articlesAfterDelete = getArticleCount();
        buf.append(sep + "# After delete: " + personsAfterDelete + " persons");
        buf.append(sep + "# After delete: " + articlesAfterDelete + " articles");
        log.info("## allInOne-Method call: " + buf.toString());
//        passedWell = (personsBefore + persons.size()) == personsAfterStore &&
//                     (articlesBefore + articles.size()) == articlesAfterStore &&
//                     (personsBefore) == personsAfterDelete &&
//                     (personsBefore) == personsAfterDelete;
        // in the current odmg implementation you cannot see
        // objects added/delete within a transaction using a OQLQuery
        passedWell = (personsBefore) == personsAfterStore &&
                (articlesBefore) == articlesAfterStore &&
                (personsBefore) == personsAfterDelete &&
                (personsBefore) == personsAfterDelete;
        return passedWell;
    }

    /**
     * @ejb:interface-method
     */
    public Collection getAllObjects(Class target)
    {
        if(log.isDebugEnabled()) log.debug("getAllObjects was called");
        OQLQuery query = odmg.newOQLQuery();
        try
        {
            query.create("select allObjects from " + target.getName());
            return (Collection) query.execute();
        }
        catch (Exception e)
        {
            log.error("OQLQuery failed", e);
            throw new OJBRuntimeException("OQLQuery failed", e);
        }
    }

    public void ejbActivate()
    {/* unused */
    }

    public void ejbPassivate()
    {/* unused */
    }

    public void setSessionContext(SessionContext ctx)
    {
        this.ctx = ctx;
    }

    public SessionContext getSessionContext()
    {
        return ctx;
    }
}
