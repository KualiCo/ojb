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
import java.util.Collection;

import org.apache.ojb.broker.OJBRuntimeException;
import org.apache.ojb.broker.util.logging.Logger;
import org.apache.ojb.broker.util.logging.LoggerFactory;
import org.apache.ojb.ejb.ArticleVO;
import org.apache.ojb.ejb.CategoryVO;
import org.odmg.OQLQuery;

/**
 * Simple example bean for manage articles using the ODMG-api
 * by subclassing {@link org.apache.ojb.ejb.odmg.ODMGBaseBeanImpl}
 *
 * @ejb:bean
 * 			type="Stateless"
 * 			name="ArticleManagerODMGBean"
 * 			jndi-name="org.apache.ojb.ejb.odmg.ArticleManagerODMGBean"
 * 			local-jndi-name="org.apache.ojb.ejb.odmg.ArticleManagerODMGBeanLocal"
 * 			view-type="both"
 * 			transaction-type="Container"
 *
 * @ejb:interface
 * 		remote-class="org.apache.ojb.ejb.odmg.ArticleManagerODMGRemote"
 * 		local-class="org.apache.ojb.ejb.odmg.ArticleManagerODMGLocal"
 * 		extends="javax.ejb.EJBObject"
 *
 * @ejb:home
 * 		remote-class="org.apache.ojb.ejb.odmg.ArticleManagerODMGHome"
 * 		local-class="org.apache.ojb.ejb.odmg.ArticleManagerODMGLocalHome"
 * 		extends="javax.ejb.EJBHome"
 *
 * @ejb:transaction
 * 		type="Required"
 *
 * @author <a href="mailto:armin@codeAuLait.de">Armin Waibel</a>
 * @version $Id: ArticleManagerODMGBean.java,v 1.1 2007-08-24 22:17:30 ewestfal Exp $
 */
public class ArticleManagerODMGBean extends ODMGBaseBeanImpl implements SessionBean
{
    private Logger log = LoggerFactory.getLogger(ArticleManagerODMGBean.class);

    public ArticleManagerODMGBean()
    {
    }

    /**
     * @ejb:interface-method
     */
    public ArticleVO storeArticle(ArticleVO article)
    {
        return (ArticleVO) this.storeObject(article);
    }

    /**
     * @ejb:interface-method
     */
    public Collection storeArticles(Collection articles)
    {
        return this.storeObjects(articles);
    }

    /**
     * Simulate a failure store.
     *
     * @ejb:interface-method
     */
    public ArticleVO failureStore(ArticleVO article)
    {
        storeArticle(article);
        // now we want to rollback
        throw new EJBException("# failureStore method test #");
    }

    /**
     * @ejb:interface-method
     */
    public void deleteArticle(ArticleVO article)
    {
        this.deleteObject(article);
    }

    /**
     * @ejb:interface-method
     */
    public void deleteArticles(Collection articles)
    {
        this.deleteObjects(articles);
    }

    /**
     * @ejb:interface-method
     */
    public int countArticles()
    {
        return this.getCount(ArticleVO.class);
    }

    /**
     * @ejb:interface-method
     */
    public Collection getAllArticles()
    {
        return this.getAllObjects(ArticleVO.class);
    }

    /**
     * @ejb:interface-method
     */
    public Collection getArticles(String articleName)
    {
        OQLQuery query = getImplementation().newOQLQuery();
        try
        {
            StringBuffer buf = new StringBuffer("select allObjects from " + ArticleVO.class.getName());
            // buf.append(" where articleId not null");
            if (articleName != null)
                buf.append(" where name = $1");
            else
                buf.append(" where name is null");
            query.create(buf.toString());
            if (articleName != null) query.bind(articleName);
            return (Collection) query.execute();
        }
        catch (Exception e)
        {
            log.error("OQLQuery failed", e);
            throw new OJBRuntimeException("OQLQuery failed", e);
        }
    }

    /**
     * @ejb:interface-method
     */
    public CategoryVO storeCategory(CategoryVO category)
    {
        return (CategoryVO) this.storeObject(category);
    }

    /**
     * @ejb:interface-method
     */
    public void deleteCategory(CategoryVO category)
    {
        this.deleteObject(category);
    }

    /**
     * @ejb:interface-method
     */
    public Collection getCategoryByName(String categoryName)
    {
        OQLQuery query = getImplementation().newOQLQuery();
        try
        {
            StringBuffer buf = new StringBuffer("select allObjects from " + CategoryVO.class.getName());
            if (categoryName != null)
                buf.append(" where categoryName = $1");
            else
                buf.append(" where categoryName is null");
            query.create(buf.toString());
            if (categoryName != null) query.bind(categoryName);
            return (Collection) query.execute();
        }
        catch (Exception e)
        {
            log.error("OQLQuery failed", e);
            throw new OJBRuntimeException("OQLQuery failed", e);
        }
    }
}
