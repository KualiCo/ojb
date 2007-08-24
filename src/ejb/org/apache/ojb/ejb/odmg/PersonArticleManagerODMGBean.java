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

import javax.ejb.CreateException;
import javax.ejb.EJBException;
import javax.ejb.SessionBean;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.util.ArrayList;
import java.util.List;

import org.apache.ojb.broker.util.logging.Logger;
import org.apache.ojb.broker.util.logging.LoggerFactory;
import org.apache.ojb.ejb.SessionBeanImpl;

/**
 * Simple example bean do nested bean calls,
 * using {@link org.apache.ojb.ejb.odmg.PersonManagerODMGBean}
 * and {@link org.apache.ojb.ejb.odmg.ArticleManagerODMGBean}
 * within some test methods.
 *
 * @ejb:bean
 * 			type="Stateless"
 * 			name="PersonArticleManagerODMGBean"
 * 			jndi-name="org.apache.ojb.ejb.odmg.PersonArticleManagerODMGBean"
 * 			local-jndi-name="org.apache.ojb.ejb.odmg.PersonArticleManagerODMGBeanLocal"
 * 			view-type="both"
 * 			transaction-type="Container"
 *
 * @ejb:interface
 * 		remote-class="org.apache.ojb.ejb.odmg.PersonArticleManagerODMGRemote"
 * 		local-class="org.apache.ojb.ejb.odmg.PersonArticleManagerODMGLocal"
 * 		extends="javax.ejb.EJBObject"
 *
 * @ejb:home
 * 		remote-class="org.apache.ojb.ejb.odmg.PersonArticleManagerODMGHome"
 * 		local-class="org.apache.ojb.ejb.odmg.PersonArticleManagerODMGLocalHome"
 * 		extends="javax.ejb.EJBHome"
 *
 * @ejb:transaction
 * 		type="Required"
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
 *
 * @author <a href="mailto:armin@codeAuLait.de">Armin Waibel</a>
 * @version $Id: PersonArticleManagerODMGBean.java,v 1.1 2007-08-24 22:17:30 ewestfal Exp $
 */
public class PersonArticleManagerODMGBean extends SessionBeanImpl implements SessionBean
{
    private static final String PERSON_MANAGER_EJB_REF_NAME = "java:comp/env/ejb/ojb/odmg/PersonManager";
    private static final String ARTICLE_MANAGER_EJB_REF_NAME = "java:comp/env/ejb/ojb/odmg/ArticleManager";

    private Logger log = LoggerFactory.getLogger(PersonArticleManagerODMGBean.class);

    private ArticleManagerODMGLocal am;
    private PersonManagerODMGLocal pm;

    public PersonArticleManagerODMGBean()
    {
    }

    public void ejbCreate()
    {

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
