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
import org.apache.ojb.broker.core.PersistenceBrokerFactoryFactory;
import org.apache.ojb.broker.core.PersistenceBrokerFactoryIF;
import org.apache.ojb.broker.query.Query;
import org.apache.ojb.broker.query.QueryByCriteria;
import org.apache.ojb.broker.util.logging.Logger;
import org.apache.ojb.broker.util.logging.LoggerFactory;
import org.apache.ojb.ejb.ArticleVO;
import org.apache.ojb.ejb.PersonVO;
import org.apache.ojb.ejb.SessionBeanImpl;

/**
 * Simple example bean for nested bean calls,
 * using {@link org.apache.ojb.ejb.pb.PersonManagerPBBean}
 * and {@link org.apache.ojb.ejb.pb.ArticleManagerPBBean}
 * for some test methods.
 *
 * @ejb:bean
 * 			type="Stateless"
 * 			name="PersonArticleManagerPBBean"
 * 			jndi-name="org.apache.ojb.ejb.pb.PersonArticleManagerPBBean"
 * 			local-jndi-name="org.apache.ojb.ejb.pb.PersonArticleManagerPBBeanLocal"
 * 			view-type="both"
 * 			transaction-type="Container"
 *
 * @ejb:interface
 * 		remote-class="org.apache.ojb.ejb.pb.PersonArticleManagerPBRemote"
 * 		local-class="org.apache.ojb.ejb.pb.PersonArticleManagerPBLocal"
 * 		extends="javax.ejb.EJBObject"
 *
 * @ejb:home
 * 		remote-class="org.apache.ojb.ejb.pb.PersonArticleManagerPBHome"
 * 		local-class="org.apache.ojb.ejb.pb.PersonArticleManagerPBLocalHome"
 * 		extends="javax.ejb.EJBHome"
 *
 * @ejb:transaction
 * 		type="Required"
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
 *
 * @author <a href="mailto:armin@codeAuLait.de">Armin Waibel</a>
 * @version $Id: PersonArticleManagerPBBean.java,v 1.1 2007-08-24 22:17:37 ewestfal Exp $
 */
public class PersonArticleManagerPBBean extends SessionBeanImpl implements SessionBean
{
    private static final String PERSON_MANAGER_EJB_REF_NAME = "java:comp/env/ejb/ojb/pb/PersonManager";
    private static final String ARTICLE_MANAGER_EJB_REF_NAME = "java:comp/env/ejb/ojb/pb/ArticleManager";

    private Logger log = LoggerFactory.getLogger(PersonArticleManagerPBBean.class);
    private ArticleManagerPBLocal am;
    private PersonManagerPBLocal pm;
    private PersistenceBrokerFactoryIF pbf;

    public PersonArticleManagerPBBean()
    {
    }

    public void ejbCreate()
    {
        pbf = PersistenceBrokerFactoryFactory.instance();
    }

    /**
     * Stores article and persons using other beans.
     *
     * @ejb:interface-method
     */
    public void storeUsingNestedPB(List articles, List persons)
    {
        PersistenceBroker broker = pbf.defaultPersistenceBroker();
        try
        {
            // do something with broker
            Query q = new QueryByCriteria(PersonVO.class);
            broker.getCollectionByQuery(q);
            // System.out.println("## broker1: con=" + broker.serviceConnectionManager().getConnection());
            //now use nested bean call
            // System.out.println("####### DO nested bean call");
            ArticleManagerPBLocal am = getArticleManager();
            am.storeArticles(articles);
            // System.out.println("####### END nested bean call");
            // do more with broker
            // System.out.println("## broker1: now store objects");
            storeObjects(broker, persons);
            // System.out.println("## broker1: end store, con=" + broker.serviceConnectionManager().getConnection());
        }
//        catch(LookupException e)
//        {
//            throw new EJBException(e);
//        }
        finally
        {
            // System.out.println("## close broker1 now");
            if(broker != null) broker.close();
        }
    }

    private Collection storeObjects(PersistenceBroker broker, Collection objects)
    {
        for (Iterator it = objects.iterator(); it.hasNext();)
        {
            broker.store(it.next());
        }
        return objects;
    }

    /**
     * Stores article and persons using other beans.
     *
     * @ejb:interface-method
     */
    public void storeUsingSubBeans(List articles, List persons)
    {
        //store all objects
        ArticleManagerPBLocal am = getArticleManager();
        PersonManagerPBLocal pm = getPersonManager();
        am.storeArticles(articles);
        pm.storePersons(persons);
    }

    /**
     * @ejb:interface-method
     */
    public List storeArticles(List articles)
    {
        return new ArrayList(getArticleManager().storeArticles(articles));
    }

    /**
     * @ejb:interface-method
     */
    public void deleteArticles(List articles)
    {
        getArticleManager().deleteArticles(articles);
    }

    /**
     * @ejb:interface-method
     */
    public List storeArticlesIntricately(List articles)
    {
        ArrayList ret = new ArrayList();
        for (int i = 0; i < articles.size(); i++)
        {
            ret.add(getArticleManager().storeArticle((ArticleVO) articles.get(i)));
        }
        return ret;
    }

    /**
     * @ejb:interface-method
     */
    public void deleteArticlesIntricately(List articles)
    {
        for (int i = 0; i < articles.size(); i++)
        {
            getArticleManager().deleteArticle((ArticleVO) articles.get(i));
        }
    }

    /**
     * @ejb:interface-method
     */
    public List storePersons(List persons)
    {
        return new ArrayList(getPersonManager().storePersons(persons));
    }

    /**
     * @ejb:interface-method
     */
    public void deletePersons(List persons)
    {
        getPersonManager().deletePersons(persons);
    }

    /**
     * @ejb:interface-method
     */
    public int articleCount()
    {
        return getArticleManager().countArticles();
    }

    /**
     * @ejb:interface-method
     */
    public int personCount()
    {
        return getPersonManager().countPersons();
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
