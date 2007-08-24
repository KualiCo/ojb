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

import javax.ejb.EJBHome;
import javax.rmi.PortableRemoteObject;
import java.util.ArrayList;
import java.util.Collection;

import junit.framework.TestCase;
import org.apache.ojb.ejb.ArticleVO;
import org.apache.ojb.ejb.CategoryVO;
import org.apache.ojb.ejb.ContextHelper;
import org.apache.ojb.ejb.VOHelper;

/**
 * Common test client class.
 *
 * @author <a href="mailto:armin@codeAuLait.de">Armin Waibel</a>
 * @version $Id: ArticleTestClient.java,v 1.1 2007-08-24 22:17:37 ewestfal Exp $
 */
public class ArticleTestClient extends TestCase
{
    private ArticleManagerPBRemote pbArticleBean;

    public ArticleTestClient(String s)
    {
        super(s);
    }

    public ArticleTestClient()
    {
        super(ArticleTestClient.class.getName());
    }

    public static void main(String[] args)
    {
        String[] arr = {ArticleTestClient.class.getName()};
        junit.textui.TestRunner.main(arr);
    }

    public void testPBCollectionRetrieve() throws Exception
    {
        long timestamp = System.currentTimeMillis();
        String articleame = "collection_test_article" + timestamp;
        String categoryName = "collection_test_category" + timestamp;
        CategoryVO cat = pbCreatePersistentCategoryWithArticles(categoryName, articleame, 5);

        assertNotNull(cat.getObjId());
        assertNotNull(cat.getAssignedArticles());
        assertEquals("Wrong number of referenced articles found", 5, cat.getAssignedArticles().size());

        Collection result = pbArticleBean.getCategoryByName(categoryName);
        assertNotNull(result);
        assertEquals(1, result.size());
        cat = (CategoryVO) result.iterator().next();
        Collection articlesCol = cat.getAssignedArticles();
        assertNotNull(articlesCol);
        assertEquals("Wrong number of referenced articles found", 5, articlesCol.size());
    }

    public void testPBQueryObjects() throws Exception
    {
        long timestamp = System.currentTimeMillis();
        String articleName = "query_test_article_" + timestamp;
        String categoryName = "query_test_category_" + timestamp;
        CategoryVO cat1 = pbCreatePersistentCategoryWithArticles(categoryName, articleName, 6);
        CategoryVO cat2 = pbCreatePersistentCategoryWithArticles(categoryName, articleName, 6);
        CategoryVO cat3 = pbCreatePersistentCategoryWithArticles(categoryName, articleName, 6);

        Collection result = pbArticleBean.getArticles(articleName);
        assertNotNull(result);
        assertEquals("Wrong number of articles", 18, result.size());

        result = pbArticleBean.getCategoryByName(categoryName);
        assertNotNull(result);
        assertEquals("Wrong number of returned category objects", 3, result.size());
        CategoryVO cat = (CategoryVO) result.iterator().next();
        assertNotNull(cat);
        Collection articles = cat.getAssignedArticles();
        assertNotNull(articles);
        assertEquals("Wrong number of referenced articles", 6, articles.size());
    }

    private CategoryVO pbCreatePersistentCategoryWithArticles(
            String categoryName, String articleName, int articleCount) throws Exception
    {
        CategoryVO cat = VOHelper.createNewCategory(categoryName);
        // store new category
        cat = pbArticleBean.storeCategory(cat);
        ArrayList articles = new ArrayList();
        for (int i = 0; i < articleCount; i++)
        {
            ArticleVO art = VOHelper.createNewArticle(articleName, 1);
            // set category
            art.setCategory(cat);
            // store article
            art = pbArticleBean.storeArticle(art);
            articles.add(art);
        }
        // set article collection
        if(articles.size() > 0) cat.setAssignedArticles(articles);
        // persist updated category
        cat = pbArticleBean.storeCategory(cat);

        return cat;
    }

    protected void setUp() throws Exception
    {
        super.setUp();
        init();
    }

    protected void init() throws Exception
    {
        try
        {
            Object object = PortableRemoteObject.narrow(
                    ContextHelper.getContext().lookup(ArticleManagerPBHome.JNDI_NAME), EJBHome.class);
            pbArticleBean = ((ArticleManagerPBHome) object).create();
        }
        catch (Exception e)
        {
            e.printStackTrace();
            throw e;
        }
    }

}
