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
import java.util.List;

import junit.framework.TestCase;
import org.apache.ojb.ejb.ContextHelper;
import org.apache.ojb.ejb.VOHelper;

/**
 * Test client class using {@link org.apache.ojb.ejb.pb.PersonArticleManagerPBBean}
 *
 * @author <a href="mailto:armin@codeAuLait.de">Armin Waibel</a>
 * @version $Id: PersonArticleClient.java,v 1.1 2007-08-24 22:17:37 ewestfal Exp $
 */
public class PersonArticleClient extends TestCase
{
    PersonArticleManagerPBRemote bean;

    public PersonArticleClient(String s)
    {
        super(s);
    }

    public PersonArticleClient()
    {
        super(PersonArticleClient.class.getName());
    }

    public static void main(String[] args)
    {
        String[] arr = {PersonArticleClient.class.getName()};
        junit.textui.TestRunner.main(arr);
    }

    public void testNestedBrokerStore() throws Exception
    {
        int personsBefore = bean.personCount();
        int articlesBefore = bean.articleCount();

        List articleList = VOHelper.createNewArticleList(6);
        List personList = VOHelper.createNewPersonList(4);
        bean.storeUsingNestedPB(articleList, personList);

        int personsAfterStore = bean.personCount();
        int articlesAfterStore = bean.articleCount();
        assertEquals("wrong number of articles after store", articlesBefore + 6, articlesAfterStore);
        assertEquals("wrong number of persons after store", personsBefore + 4, personsAfterStore);
    }

    public void testNestedBeans() throws Exception
    {
        int personsBefore = bean.personCount();
        int articlesBefore = bean.articleCount();

        List articleList = VOHelper.createNewArticleList(6);
        List personList = VOHelper.createNewPersonList(4);
        // storing objects
        bean.storeUsingSubBeans(articleList, personList);

        int personsAfterStore = bean.personCount();
        int articlesAfterStore = bean.articleCount();
        assertEquals("wrong number of articles after store", articlesBefore + 6, articlesAfterStore);
        assertEquals("wrong number of persons after store", personsBefore + 4, personsAfterStore);
    }

    public void testNestedStoreDelete() throws Exception
    {
        int personsBefore = bean.personCount();
        int articlesBefore = bean.articleCount();

        List articleList = VOHelper.createNewArticleList(6);
        List personList = VOHelper.createNewPersonList(4);
        // storing objects
        articleList = bean.storeArticles(articleList);
        personList = bean.storePersons(personList);

        int personsAfterStore = bean.personCount();
        int articlesAfterStore = bean.articleCount();
        assertEquals("wrong number of articles after store", articlesBefore + 6, articlesAfterStore);
        assertEquals("wrong number of persons after store", personsBefore + 4, personsAfterStore);

        //delete objects
        bean.deleteArticles(articleList);
        bean.deletePersons(personList);

        int personsAfterDelete = bean.personCount();
        int articlesAfterDelete = bean.articleCount();
        assertEquals("wrong number of articles after delete", articlesBefore, articlesAfterDelete);
        assertEquals("wrong number of persons after delete", personsBefore, personsAfterDelete);
    }

    public void testStoreDelete() throws Exception
    {
        int count = 100;
        int articlesBefore = bean.articleCount();

        List articleList = VOHelper.createNewArticleList(count);
        // storing objects
        articleList = bean.storeArticlesIntricately(articleList);

        int articlesAfterStore = bean.articleCount();
        assertEquals("wrong number of articles after store", articlesBefore + count, articlesAfterStore);

        //delete objects
        bean.deleteArticlesIntricately(articleList);

        int articlesAfterDelete = bean.articleCount();
        assertEquals("wrong number of articles after delete", articlesBefore, articlesAfterDelete);
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
            ContextHelper.getContext().lookup(PersonArticleManagerPBHome.JNDI_NAME), EJBHome.class);
            bean = (PersonArticleManagerPBRemote) ((PersonArticleManagerPBHome) object).create();
            System.out.println("Bean found: " + bean);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            throw e;
        }
    }

}
