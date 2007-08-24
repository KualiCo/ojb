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
import javax.ejb.CreateException;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.apache.ojb.broker.OJBRuntimeException;
import org.apache.ojb.broker.PersistenceBroker;
import org.apache.ojb.broker.query.Criteria;
import org.apache.ojb.broker.query.Query;
import org.apache.ojb.broker.query.QueryByCriteria;
import org.apache.ojb.broker.util.logging.Logger;
import org.apache.ojb.broker.util.logging.LoggerFactory;
import org.apache.ojb.ejb.ArticleVO;
import org.apache.ojb.ejb.PersonVO;
import org.apache.ojb.odmg.TransactionExt;
import org.odmg.OQLQuery;
import org.odmg.QueryException;

/**
 * This is an session bean implementation used for testing different "rollback"
 * scenarios for the ODMG implementation.
 * <p/>
 * <p/>
 * <b>How to use ODMG</b> <br>
 * <p/>
 * To keep this example as simple as possible, we lookup a static OJB ODMG implementation instance
 * on each bean instance.
 * But it's recommended to bind an instance of the Implementation class in JNDI
 * (at appServer start), open the database and lookup this instances via JNDI in
 * ejbCreate().
 * <br/>
 * However the examples use a simple helper class to lookup the OJB resources.
 * <p/>
 * To use the odmg-api within your bean, you can do:
 * <p/>
 * <ol type="a">
 * <li>
 * Obtain the current Database from the Implementation instance - Attend<br>
 * that there must be already a Database opened before.<br><i>
 * db = odmg.getDatabase(null);<br>
 * // ... do something<br>
 * </i></li>
 * <li>
 * Obtain the current odmg-Transaction from the Implementation instance<br>
 * to lock objects - Attend that there must be already a Database opened before.<br><i>
 * Transaction tx = odmg.currentTransaction();<br>
 * tx.lock(aObject, mode);
 * </i></li>
 * </ol>
 * </p>
 *
 *
 * @ejb:bean type="Stateless"
 * name="RollbackBeanODMG"
 * jndi-name="org.apache.ojb.ejb.odmg.RollbackBean"
 * local-jndi-name="org.apache.ojb.ejb.odmg.RollbackBeanLocal"
 * view-type="both"
 * transaction-type="Container"
 *
 * @ejb:interface remote-class="org.apache.ojb.ejb.odmg.RollbackRemote"
 * local-class="org.apache.ojb.ejb.odmg.RollbackLocal"
 * extends="javax.ejb.EJBObject"
 *
 * @ejb:home remote-class="org.apache.ojb.ejb.odmg.RollbackHome"
 * local-class="org.apache.ojb.ejb.odmg.RollbackLocalHome"
 * extends="javax.ejb.EJBHome"
 *
 * @ejb:ejb-ref
 *      ejb-name="PersonManagerODMGBean"
 *      view-type="local"
 *      ref-name="ejb/ojb/odmg/PersonManager"
 *
 * @ejb:ejb-ref
 *      ejb-name="ArticleManagerODMGBean"
 *      view-type="local"
 *      ref-name="ejb/ojb/odmg/ArticleManager"
 *
 * @ejb:transaction type="Required"
 *
 * @author <a href="mailto:armin@codeAuLait.de">Armin Waibel</a>
 * @version $Id: RollbackBean.java,v 1.1 2007-08-24 22:17:30 ewestfal Exp $
 */
public class RollbackBean extends ODMGBaseBeanImpl implements SessionBean
{
    private Logger log = LoggerFactory.getLogger(RollbackBean.class);

    private static final String PERSON_MANAGER_EJB_REF_NAME = "java:comp/env/ejb/ojb/odmg/PersonManager";
    private static final String ARTICLE_MANAGER_EJB_REF_NAME = "java:comp/env/ejb/ojb/odmg/ArticleManager";

    private ArticleManagerODMGLocal am;
    private PersonManagerODMGLocal pm;

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
        ArticleManagerODMGLocal am = getArticleManager();
        PersonManagerODMGLocal pm = getPersonManager();
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
        ArticleManagerODMGLocal am = getArticleManager();
        PersonManagerODMGLocal pm = getPersonManager();
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
        ArticleManagerODMGLocal am = getArticleManager();
        PersonManagerODMGLocal pm = getPersonManager();
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
     * We do an odmg-tx.abort() call.
     *
     * @ejb:interface-method
     */
    public void rollbackOdmgAbort(List objects)
    {
        log.info("rollbackOdmgAbort method was called");
        storeObjects(objects);
        getImplementation().currentTransaction().abort();
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
        /*
        seems that some appServer expect that all used resources will be closed before
        the JTA-TXManager does call Synchronization#beforeCompletion(), so we have
        to cleanup used resources. This could be done by call abort on odmg-api
        */
        getImplementation().currentTransaction().abort();
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
        /*
        seems that some appServer expect that all used resources will be closed before
        the JTA-TXManager does call Synchronization#beforeCompletion(), so we have
        to cleanup used resources. This could be done by call abort on odmg-api
        */
        getImplementation().currentTransaction().abort();
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
        TransactionExt tx = ((TransactionExt) getImplementation().currentTransaction());
        // force writing to DB
        tx.flush();
        Class searchClass = objectsToStore.get(0).getClass();
        PersistenceBroker broker = tx.getBroker();
        Query q = new QueryByCriteria(searchClass);
        // we get the iterator and step into the first found object
        Iterator it = broker.getIteratorByQuery(q);
        it.next();
        /*
        seems that some appServer expect that all used resources are closed before
        the JTA-TXManager does call Synchronization#beforeCompletion(), so we have
        to cleanup used resources. This could be done by call abort on odmg-api
        */
        getImplementation().currentTransaction().abort();
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

    protected int getObjectCount(Class target)
    {
        log.info("getObjectCount was called");
        List list;
        try
        {
            OQLQuery query = getImplementation().newOQLQuery();
            query.create("select allObjects from " + target.getName());
            list = (List) query.execute();
            return list.size();
        }
        catch(QueryException e)
        {
            throw new EJBException("Query objects failed", e);
        }
    }

    /**
     * @ejb:interface-method
     */
    public int getArticleCount()
    {
        log.info("getArticleCount was called");
        return getObjectCount(ArticleVO.class);
    }

    /**
     * @ejb:interface-method
     */
    public int getPersonCount()
    {
        log.info("getPersonCount was called");
        return getObjectCount(PersonVO.class);
    }

    /**
     * @ejb:interface-method
     */
    public Collection getAllObjects(Class target)
    {
        if(log.isDebugEnabled()) log.debug("getAllObjects was called");
        OQLQuery query = getImplementation().newOQLQuery();
        try
        {
            query.create("select allObjects from " + target.getName());
            return (Collection) query.execute();
        }
        catch(Exception e)
        {
            log.error("OQLQuery failed", e);
            throw new OJBRuntimeException("OQLQuery failed", e);
        }
    }

    private ArticleManagerODMGLocal getArticleManager()
    {
        if (am == null)
        {
            Context context = null;
            try
            {
                context = new InitialContext();
                am = ((ArticleManagerODMGLocalHome) context.lookup(ARTICLE_MANAGER_EJB_REF_NAME)).create();
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

    private PersonManagerODMGLocal getPersonManager()
    {
        if (pm == null)
        {
            Context context = null;
            try
            {
                context = new InitialContext();
                pm = ((PersonManagerODMGLocalHome) context.lookup(PERSON_MANAGER_EJB_REF_NAME)).create();
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
