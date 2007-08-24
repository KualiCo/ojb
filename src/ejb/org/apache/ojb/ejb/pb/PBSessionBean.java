package org.apache.ojb.ejb.pb;

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


import javax.ejb.SessionBean;
import javax.ejb.SessionContext;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.apache.ojb.broker.PersistenceBroker;
import org.apache.ojb.broker.core.PersistenceBrokerFactoryFactory;
import org.apache.ojb.broker.core.PersistenceBrokerFactoryIF;
import org.apache.ojb.broker.query.Criteria;
import org.apache.ojb.broker.query.Query;
import org.apache.ojb.broker.query.QueryByCriteria;
import org.apache.ojb.broker.util.logging.Logger;
import org.apache.ojb.broker.util.logging.LoggerFactory;
import org.apache.ojb.ejb.ArticleVO;
import org.apache.ojb.ejb.PersonVO;

/**
 * This is an session bean implementation using PB-api.
 * <br>
 * For more structured implementations take a look at<br/>
 * {@link org.apache.ojb.ejb.SessionBeanImpl}<br/>
 * {@link org.apache.ojb.ejb.odmg.ODMGBaseBeanImpl}<br/>
 * {@link org.apache.ojb.ejb.pb.PBBaseBeanImpl}<br/>
 *
 * @ejb:bean
 * 			type="Stateless"
 * 			name="PBSessionBean"
 * 			jndi-name="org.apache.ojb.ejb.pb.PBSessionBean"
 * 			local-jndi-name="org.apache.ojb.ejb.pb.PBSessionBeanLocal"
 * 			view-type="both"
 * 			transaction-type="Container"
 *
 * @ejb:interface
 * 		remote-class="org.apache.ojb.ejb.pb.PBSessionRemote"
 * 		local-class="org.apache.ojb.ejb.pb.PBSessionLocal"
 * 		extends="javax.ejb.EJBObject"
 *
 * @ejb:home
 * 		remote-class="org.apache.ojb.ejb.pb.PBSessionHome"
 * 		local-class="org.apache.ojb.ejb.pb.PBSessionLocalHome"
 * 		extends="javax.ejb.EJBHome"
 *
 * @ejb:transaction
 * 		type="Required"
 *
 * @jonas.bean
 *      ejb-name="org.apache.ojb.ejb.pb.PBSessionBean"
 *      jndi-name="org.apache.ojb.ejb.pb.PBSessionBean"
 *
 * @author <a href="mailto:armin@codeAuLait.de">Armin Waibel</a>
 * @version $Id: PBSessionBean.java,v 1.1 2007-08-24 22:17:37 ewestfal Exp $
 */
public class PBSessionBean implements SessionBean
{
    private Logger log = LoggerFactory.getLogger(PBSessionBean.class);
    private SessionContext ctx;
    private PersistenceBrokerFactoryIF pbf;

    public PBSessionBean()
    {
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

    public void ejbRemove()
    {
        ctx = null;
    }

    public void ejbCreate()
    {
        log.info("ejbCreate was called");
        pbf = PersistenceBrokerFactoryFactory.instance();
    }

    protected PersistenceBroker getBroker()
    {
        if(log.isDebugEnabled()) log.debug("getBroker was called");
        return pbf.defaultPersistenceBroker();
    }

    protected List storeObjects(PersistenceBroker broker, List objects)
    {
        for (Iterator it = objects.iterator(); it.hasNext();)
        {
            broker.store(it.next());
        }
        return objects;
    }

    protected void deleteObjects(PersistenceBroker broker, List objects)
    {
        for (Iterator it = objects.iterator(); it.hasNext();)
        {
            broker.delete(it.next());
        }
    }

    protected int getCount(Class target)
    {
        PersistenceBroker broker = getBroker();
        int result = broker.getCount(new QueryByCriteria(target));
        broker.close();
        return result;
    }

    /**
     * @ejb:interface-method
     */
    public Collection getAllObjects(Class target)
    {
        if(log.isDebugEnabled()) log.debug("getAllObjects was called");
        PersistenceBroker broker = getBroker();
        Query q = new QueryByCriteria(target);
        Collection result = broker.getCollectionByQuery(q);
        broker.close();
        return result;
    }

    /**
     * @ejb:interface-method
     */
    public Iterator iterateAllObjects(Class target)
    {
        if(log.isDebugEnabled()) log.debug("getAllObjects was called");
        PersistenceBroker broker = getBroker();
        Query q = new QueryByCriteria(target);
        Iterator result = broker.getIteratorByQuery(q);
        broker.close();
        return result;
    }

    /**
     * @ejb:interface-method
     */
    public int getArticleCount()
    {
        if(log.isDebugEnabled()) log.debug("getArticleCount was called");
        return getCount(ArticleVO.class);
    }

    /**
     * @ejb:interface-method
     */
    public Collection getArticlesByName(String articleName)
    {
        if(log.isDebugEnabled()) log.debug("getArticlesByName was called");
        PersistenceBroker broker = getBroker();
        Criteria crit = new Criteria();
        crit.addLike("name", articleName);
        Query q = new QueryByCriteria(ArticleVO.class, crit);
        Collection result = broker.getCollectionByQuery(q);
        broker.close();
        return result;
    }

    /**
     * @ejb:interface-method
     */
    public int getPersonCount()
    {
        if(log.isDebugEnabled()) log.debug("getPersonCount was called");
        return getCount(PersonVO.class);
    }

    /**
     * @ejb:interface-method
     */
    public List storeObjects(List objects)
    {
        if(log.isDebugEnabled()) log.debug("storeObjects was called");
        PersistenceBroker broker = getBroker();
        List stored = this.storeObjects(broker, objects);
        broker.close();
        return stored;
    }

    /**
     * @ejb:interface-method
     */
    public void deleteObjects(List objects)
    {
        if(log.isDebugEnabled()) log.debug("deleteObjects was called");
        PersistenceBroker broker = getBroker();
        this.deleteObjects(broker, objects);
        broker.close();
    }

    /**
     * @ejb:interface-method
     */
    public boolean allInOne(List articles, List persons)
    {
        if(log.isDebugEnabled()) log.debug("allInOne was called");
        StringBuffer buf = new StringBuffer();
        boolean passedWell = true;
        String sep = System.getProperty("line.separator");

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
        passedWell = (personsBefore + persons.size()) == personsAfterStore &&
                (articlesBefore + articles.size()) == articlesAfterStore &&
                (personsBefore) == personsAfterDelete &&
                (personsBefore) == personsAfterDelete;
        return passedWell;
    }
}
