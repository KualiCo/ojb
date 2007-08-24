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

import javax.ejb.EJBHome;
import javax.rmi.PortableRemoteObject;
import java.util.List;

import junit.framework.TestCase;
import org.apache.ojb.ejb.ContextHelper;
import org.apache.ojb.ejb.VOHelper;

/**
 * Test client class using {@link org.apache.ojb.ejb.odmg.PersonArticleManagerODMGBean}
 *
 * @author <a href="mailto:armin@codeAuLait.de">Armin Waibel</a>
 * @version $Id: PersonArticleClientODMG.java,v 1.1 2007-08-24 22:17:30 ewestfal Exp $
 */
public class PersonArticleClientODMG extends TestCase
{
    PersonArticleManagerODMGRemote bean;

    public PersonArticleClientODMG(String s)
    {
        super(s);
    }

    public PersonArticleClientODMG()
    {
        super(PersonArticleClientODMG.class.getName());
    }

    public static void main(String[] args)
    {
        String[] arr = {PersonArticleClientODMG.class.getName()};
        junit.textui.TestRunner.main(arr);
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
            ContextHelper.getContext().lookup(PersonArticleManagerODMGHome.JNDI_NAME), EJBHome.class);
            bean = ((PersonArticleManagerODMGHome) object).create();
        }
        catch (Exception e)
        {
            e.printStackTrace();
            throw e;
        }
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
}
