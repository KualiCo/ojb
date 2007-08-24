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

import javax.ejb.EJBException;
import javax.ejb.SessionBean;
import java.util.Collection;

import org.apache.ojb.broker.PersistenceBroker;
import org.apache.ojb.broker.query.Criteria;
import org.apache.ojb.broker.query.Query;
import org.apache.ojb.broker.query.QueryByCriteria;
import org.apache.ojb.ejb.ArticleVO;
import org.apache.ojb.ejb.CategoryVO;

/**
 * Simple example bean for manage articles using the PB-api
 * by subclassing {@link org.apache.ojb.ejb.pb.PBBaseBeanImpl}
 *
 * @ejb:bean
 * 			type="Stateless"
 * 			name="ArticleManagerPBBean"
 * 			jndi-name="org.apache.ojb.ejb.pb.ArticleManagerPBBean"
 * 			local-jndi-name="org.apache.ojb.ejb.pb.ArticleManagerPBBeanLocal"
 * 			view-type="both"
 * 			transaction-type="Container"
 *
 * @ejb:interface
 * 		remote-class="org.apache.ojb.ejb.pb.ArticleManagerPBRemote"
 * 		local-class="org.apache.ojb.ejb.pb.ArticleManagerPBLocal"
 * 		extends="javax.ejb.EJBObject"
 *
 * @ejb:home
 * 		remote-class="org.apache.ojb.ejb.pb.ArticleManagerPBHome"
 * 		local-class="org.apache.ojb.ejb.pb.ArticleManagerPBLocalHome"
 * 		extends="javax.ejb.EJBHome"
 *
 * @ejb:transaction
 * 		type="Required"
 *
 *
 * @author <a href="mailto:armin@codeAuLait.de">Armin Waibel</a>
 * @version $Id: ArticleManagerPBBean.java,v 1.1 2007-08-24 22:17:37 ewestfal Exp $
 */
public class ArticleManagerPBBean extends PBBaseBeanImpl implements SessionBean
{
    public ArticleManagerPBBean()
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
    public Collection getArticles(String articleName)
    {
        PersistenceBroker broker = getBroker();
        Collection result;
        try
        {
            Criteria criteria = new Criteria();
            if (articleName != null) criteria.addEqualTo("name", articleName);
            Query q = new QueryByCriteria(ArticleVO.class, criteria);
            result = broker.getCollectionByQuery(q);
        }
        finally
        {
            if (broker != null) broker.close();
        }
        return result;
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
        PersistenceBroker broker = getBroker();
        Collection result;
        try
        {
            Criteria criteria = new Criteria();
            if (categoryName != null) criteria.addEqualTo("categoryName", categoryName);
            Query q = new QueryByCriteria(CategoryVO.class, criteria);
            result = broker.getCollectionByQuery(q);
        }
        finally
        {
            if (broker != null) broker.close();
        }
        return result;
    }
}
