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


import javax.ejb.CreateException;
import javax.ejb.EJBException;
import javax.ejb.SessionBean;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.apache.ojb.broker.PersistenceBroker;
import org.apache.ojb.broker.query.Criteria;
import org.apache.ojb.broker.query.Query;
import org.apache.ojb.broker.query.QueryByCriteria;
import org.apache.ojb.broker.util.logging.Logger;
import org.apache.ojb.broker.util.logging.LoggerFactory;
import org.apache.ojb.ejb.ArticleVO;
import org.apache.ojb.ejb.PersonVO;

/**
 * This is an session bean implementation used for testing different "rollback"
 * scenarios for the PB-api. The most important directive when using the PB-api within
 * EJB beans is in any case to close the used PB instance after use within the bean method.
 * Have a look in {@link PBBaseBeanImpl}.
 *
 *
 * @ejb:bean type="Stateless"
 * name="RollbackBeanPB"
 * jndi-name="org.apache.ojb.ejb.pb.RollbackBean"
 * local-jndi-name="org.apache.ojb.ejb.pb.RollbackBeanLocal"
 * view-type="both"
 * transaction-type="Container"
 *
 * @ejb:interface remote-class="org.apache.ojb.ejb.pb.RollbackRemote"
 * local-class="org.apache.ojb.ejb.pb.RollbackLocal"
 * extends="javax.ejb.EJBObject"
 *
 * @ejb:home remote-class="org.apache.ojb.ejb.pb.RollbackHome"
 * local-class="org.apache.ojb.ejb.pb.RollbackLocalHome"
 * extends="javax.ejb.EJBHome"
 *
 * @ejb:ejb-ref
 *      ejb-name="PersonManagerPBBean"
 *      view-type="local"
 *      ref-name="ejb/ojb/pb/PersonManager"
 *
 * @ejb:ejb-ref
 *      ejb-name="ArticleManagerPBBean"
 *      view-type="local"
 *      ref-name="ejb/ojb/pb/ArticleManager"
 *
 * @ejb:transaction type="Required"
 *
 * @author <a href="mailto:armin@codeAuLait.de">Armin Waibel</a>
 * @version $Id: RollbackBean.java,v 1.1 2007-08-24 22:17:37 ewestfal Exp $
 */
public class RollbackBean extends PBBaseBeanImpl implements SessionBean
{
    private Logger log = LoggerFactory.getLogger(RollbackBean.class);

    private static final String PERSON_MANAGER_EJB_REF_NAME = "java:comp/env/ejb/ojb/pb/PersonManager";
    private static final String ARTICLE_MANAGER_EJB_REF_NAME = "java:comp/env/ejb/ojb/pb/ArticleManager";

    private ArticleManagerPBLocal am;
    private PersonManagerPBLocal pm;

    public RollbackBean()
    {
    }

    /**
     * First stores all articles, persons form
     * the lists using ArticleManager and PersonManager
     * beans after doing that, a Exception will be thrown.
     *
     * @ejb:interface-method
     */
    public void rollbackOtherBeanUsing(List articles, List persons)
    {
        log.info("rollbackOtherBeanUsing method was called");
        //store all objects
        ArticleManagerPBLocal am = getArticleManager();
        PersonManagerPBLocal pm = getPersonManager();
        am.storeArticles(articles);
        pm.storePersons(persons);

        // after all is done we throw an exception to activate rollback process
        throw new EJBException("## Testing of rollback behaviour - rollbackOtherBeanUsing ##");
    }

    /**
     * First store a list of persons then we
     * store the article using a failure store
     * method in ArticleManager.
     *
     * @ejb:interface-method
     */
    public void rollbackOtherBeanUsing_2(ArticleVO article, List persons)
    {
        log.info("rollbackOtherBeanUsing_2 method was called");
        ArticleManagerPBLocal am = getArticleManager();
        PersonManagerPBLocal pm = getPersonManager();
        pm.storePersons(persons);
        am.failureStore(article);
    }

    /**
     * This test method expect an invalid object in the person list,
     * so that OJB cause an internal error.
     *
     * @ejb:interface-method
     */
    public void rollbackClientWrongInput(List articles, List persons)
    {
        log.info("rollbackClientWrongInput method was called");
        ArticleManagerPBLocal am = getArticleManager();
        PersonManagerPBLocal pm = getPersonManager();
        am.storeArticles(articles);
        pm.storePersons(persons);
    }

    /**
     * The bean will throw an exception before the method ends.
     *
     * @ejb:interface-method
     */
    public void rollbackThrowException(List objects)
    {
        log.info("rollbackThrowException method was called");
        storeObjects(objects);
        // now we throw an exception
        throw new EJBException("## Testing of rollback behaviour - rollbackThrowException ##");
    }

    /**
     * One of the objects passed by the client will cause an exception.
     *
     * @ejb:interface-method
     */
    public void rollbackPassInvalidObject(List objects)
    {
        log.info("rollbackPassInvalidObject method was called");
        storeObjects(objects);
    }

    /**
     * We do call ctx.setRollbackOnly and do odmg-tx.abort() call.
     *
     * @ejb:interface-method
     */
    public void rollbackSetRollbackOnly(List objects)
    {
        log.info("rollbackSetRollbackOnly method was called");
        storeObjects(objects);
        /*
        setRollbackOnly does only rollback the transaction, the client will not be
        notified by an RemoteException (tested on JBoss)
        */
        getSessionContext().setRollbackOnly();
    }

    /**
     * We do call ctx.setRollbackOnly and do odmg-tx.abort() call.
     *
     * @ejb:interface-method
     */
    public void rollbackSetRollbackAndThrowException(List objects)
    {
        log.info("rollbackSetRollbackAndThrowException method was called");
        storeObjects(objects);
        getSessionContext().setRollbackOnly();
        // to notify the client about the failure we throw an exception
        // if we don't throw such an exception the client don't get notified
        // about the failure
        throw new EJBException("## Testing of rollback behaviour - rollbackSetRollbackAndThrowException ##");
    }

    /**
     * We use several OJB services, start to iterate a query result and do
     * an odmg-tx.abort call.
     *
     * @ejb:interface-method
     */
    public void rollbackBreakIteration(List objectsToStore)
    {
        // now we mix up different api's and use PB-api too
        log.info("rollbackBreakIteration");
        /*
        store list of objects, then get these objects with Iterator, start
        iteration, then break
        */
        storeObjects(objectsToStore);
        Class searchClass = objectsToStore.get(0).getClass();
        PersistenceBroker broker = getBroker();
        try
        {
            Query q = new QueryByCriteria(searchClass);
            // we get the iterator and step into the first found object
            Iterator it = broker.getIteratorByQuery(q);
            it.next();
        }
        /*
        Now we want to break iteration or something wrong. In this case we have to
        cleanup the used PB instance by a close call
        */
        finally
        {
            if(broker != null) broker.close();
        }

        // to notify the client about the failure we throw an exception
        // if we don't throw such an exception the client don't get notified
        // about the failure
        throw new EJBException("## Testing of rollback behaviour - rollbackBreakIteration ##");
    }

    /**
     * @ejb:interface-method
     */
    public List storeObjects(List objects)
    {
        return new ArrayList(super.storeObjects(objects));
    }

    /**
     * @ejb:interface-method
     */
    public void deleteObjects(List objects)
    {
        log.info("deleteObjects");
        super.deleteObjects(objects);
    }

    /**
     * @ejb:interface-method
     */
    public int getArticleCount()
    {
        log.info("getArticleCount was called");
        return getCount(ArticleVO.class);
    }

    /**
     * @ejb:interface-method
     */
    public int getPersonCount()
    {
        log.info("getPersonCount was called");
        return getCount(PersonVO.class);
    }

    /**
     * @ejb:interface-method
     */
    public Collection getAllObjects(Class target)
    {
        if(log.isDebugEnabled()) log.debug("getAllObjects was called");
        return super.getAllObjects(target);
    }

    private ArticleManagerPBLocal getArticleManager()
    {
        if (am == null)
        {
            Context context = null;
            try
            {
                context = new InitialContext();
                am = ((ArticleManagerPBLocalHome) context.lookup(ARTICLE_MANAGER_EJB_REF_NAME)).create();
                log.info("** Found bean: " + am);
                return am;
            }
            catch (NamingException e)
            {
                log.error("Lookup using ejb-ref " + ARTICLE_MANAGER_EJB_REF_NAME + " failed", e);
                throw new EJBException(e);
            }
            catch (CreateException e)
            {
                log.error("Creation of ArticleManager failed", e);
                throw new EJBException(e);
            }
        }
        return am;
    }

    private PersonManagerPBLocal getPersonManager()
    {
        if (pm == null)
        {
            Context context = null;
            try
            {
                context = new InitialContext();
                pm = ((PersonManagerPBLocalHome) context.lookup(PERSON_MANAGER_EJB_REF_NAME)).create();
                log.info("** Found bean: " + pm);
                return pm;
            }
            catch (NamingException e)
            {
                log.error("Lookup using ejb-ref " + PERSON_MANAGER_EJB_REF_NAME + " failed", e);
                throw new EJBException(e);
            }
            catch (CreateException e)
            {
                log.error("Creation of PersonManager failed", e);
                throw new EJBException(e);
            }
        }
        return pm;
    }
}
