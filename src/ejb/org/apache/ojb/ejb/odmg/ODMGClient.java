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
import javax.naming.Context;
import javax.rmi.PortableRemoteObject;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import junit.framework.TestCase;
import org.apache.ojb.ejb.ArticleVO;
import org.apache.ojb.ejb.ContextHelper;
import org.apache.ojb.ejb.VOHelper;

/**
 * Test client using the {@link org.apache.ojb.ejb.odmg.ODMGSessionBean}.
 *
 * @author <a href="mailto:armin@codeAuLait.de">Armin Waibel</a>
 * @version $Id: ODMGClient.java,v 1.1 2007-08-24 22:17:30 ewestfal Exp $
 */
public class ODMGClient extends TestCase
{
    ODMGSessionRemote sampleBean;
    static int loops = 500;

    public ODMGClient(String s)
    {
        super(s);
    }

    public ODMGClient()
    {
        super(ODMGClient.class.getName());
    }

    public static void main(String[] args)
    {
        loops = args.length > 0 ? new Integer(args[0]).intValue(): 500;
        junit.textui.TestRunner.main(new String[] {ODMGClient.class.getName()});
    }

    protected void setUp() throws Exception
    {
        super.setUp();
        init();
    }

    protected void tearDown() throws Exception
    {
        super.tearDown();
    }

    protected void init()
    {
        Context ctx = ContextHelper.getContext();
        try
        {
            Object object = PortableRemoteObject.narrow(ctx.lookup(ODMGSessionHome.JNDI_NAME), EJBHome.class);
            sampleBean = ((ODMGSessionHome) object).create();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    public void testInsertDelete() throws RemoteException
    {
        System.out.println("# test: testInsertDelete()");
        int articlesBefore = sampleBean.getArticleCount();
        int personsBefore = sampleBean.getPersonCount();

        List articles = VOHelper.createNewArticleList(10);
        List persons = VOHelper.createNewPersonList(5);
        articles = sampleBean.storeObjects(articles);
        persons = sampleBean.storeObjects(persons);

        int articlesAfterStore = sampleBean.getArticleCount();
        int personsAfterStore = sampleBean.getPersonCount();
        assertEquals("Storing of articles failed", articlesBefore + 10, articlesAfterStore);
        assertEquals("Storing of persons faile", personsBefore + 5, personsAfterStore);


        sampleBean.deleteObjects(articles);
        sampleBean.deleteObjects(persons);

        int articlesAfterDelete = sampleBean.getArticleCount();
        int personsAfterDelete = sampleBean.getPersonCount();
        assertEquals("Deleting of articles failed", articlesAfterStore - 10, articlesAfterDelete);
        assertEquals("Deleting of persons failed", personsAfterStore - 5, personsAfterDelete);
    }

    public void testStress() throws Exception
    {
        System.out.println("## ODMG-api testStress");
        System.out.println("Stress test will be done with " + loops + " loops");
        System.out.println("# Store #");
        for (int i = 0; i < loops; i++)
        {
            sampleBean.storeObjects(VOHelper.createNewArticleList(1));
            if(i%10==0)System.out.print(".");
            if(i%400==0)System.out.println();
        }
        Collection col = sampleBean.getAllObjects(ArticleVO.class);
        System.out.println("\n# Delete #");
        int i =0;
        for (Iterator iterator = col.iterator(); iterator.hasNext();)
        {
            ArticleVO article = (ArticleVO) iterator.next();
            List del = new ArrayList();
            del.add(article);
            sampleBean.deleteObjects(del);
            if(++i%10==0)System.out.print(".");
            if(i%400==0)System.out.println();
        }
        System.out.println("");
        System.out.println("## ODMG-api testStress END ##");
    }

    public void testServerSideMethods() throws RemoteException
    {
        System.out.println("## testServerSideMethods");
        boolean result = sampleBean.allInOne(VOHelper.createNewArticleList(10), VOHelper.createNewPersonList(5));
        assertTrue("Something happened on sever side test method - 'allInOne(...)'", result);
    }
}
