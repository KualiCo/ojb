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
import javax.transaction.UserTransaction;
import java.rmi.RemoteException;
import java.util.List;

import junit.framework.TestCase;
import org.apache.ojb.ejb.ContextHelper;
import org.apache.ojb.ejb.VOHelper;

/**
 * Test client using the {@link RollbackBean}.
 *
 * @author <a href="mailto:arminw@apache.de">Armin Waibel</a>
 * @version $Id: RollbackClient.java,v 1.1 2007-08-24 22:17:30 ewestfal Exp $
 */
public class RollbackClient extends TestCase
{
    RollbackRemote rollbackBean;

    public RollbackClient(String s)
    {
        super(s);
    }

    public RollbackClient()
    {
        super(RollbackClient.class.getName());
    }

    public static void main(String[] args)
    {
        junit.textui.TestRunner.main(new String[] {RollbackClient.class.getName()});
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
            Object object = PortableRemoteObject.narrow(ctx.lookup(RollbackHome.JNDI_NAME), EJBHome.class);
            rollbackBean = ((RollbackHome) object).create();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    /*
    TODO: Make this work
    */
    public void YYYtestRollbackRemoteUserTransaction() throws Exception
    {
        System.out.println("## testRollbackRemoteUserTransaction");
        int articlesBefore = rollbackBean.getArticleCount();
        int personsBefore = rollbackBean.getPersonCount();
        try
        {
            UserTransaction tx = (UserTransaction) ContextHelper.getContext().lookup("UserTransaction");
            tx.begin();
            List articles = VOHelper.createNewArticleList(10);
            List persons = VOHelper.createNewPersonList(5);
            articles = rollbackBean.storeObjects(articles);
            persons = rollbackBean.storeObjects(persons);
            tx.rollback();
        }
        catch (RemoteException e)
        {
            // should we expect that??
            e.printStackTrace();
        }

        int articlesAfterStore = rollbackBean.getArticleCount();
        int personsAfterStore = rollbackBean.getPersonCount();
        assertEquals("Storing of articles failed", articlesBefore+10, articlesAfterStore);
        assertEquals("Storing of persons faile", personsBefore+5, personsAfterStore);
    }

    public void testRollbackOtherBeanUsing() throws Exception
    {
        System.out.println("## testRollbackOtherBeanUsing");
        int articlesBefore = rollbackBean.getArticleCount();
        int personsBefore = rollbackBean.getPersonCount();

        try
        {
            rollbackBean.rollbackOtherBeanUsing(VOHelper.createNewArticleList(4), VOHelper.createNewPersonList(6));
            // we should get an exception
            fail("Expect an RemoteException");
        }
        catch (RemoteException e)
        {
            assertTrue(true);
        }

        int personsAfter = rollbackBean.getPersonCount();
        int articlesAfter = rollbackBean.getArticleCount();

        assertEquals(articlesBefore, articlesAfter);
        assertEquals(personsBefore, personsAfter);
    }

    public void testRollbackOtherBeanUsing_2() throws Exception
    {
        System.out.println("## testRollbackOtherBeanUsing_2");
        int articlesBefore = rollbackBean.getArticleCount();
        int personsBefore = rollbackBean.getPersonCount();

        try
        {
            rollbackBean.rollbackOtherBeanUsing_2(VOHelper.createNewArticle(13), VOHelper.createNewPersonList(6));
            // we should get an exception
            fail("Expect an RemoteException");
        }
        catch (RemoteException e)
        {
            assertTrue(true);
        }

        int personsAfter = rollbackBean.getPersonCount();
        int articlesAfter = rollbackBean.getArticleCount();

        assertEquals(articlesBefore, articlesAfter);
        assertEquals(personsBefore, personsAfter);
    }

    public void testRollbackClientWrongInput() throws Exception
    {
        System.out.println("## testRollbackClientWrongInput");
        int articlesBefore = rollbackBean.getArticleCount();
        int personsBefore = rollbackBean.getPersonCount();

        try
        {
            List persons = VOHelper.createNewPersonList(6);
            // add non-persistent object to cause failure
            persons.add(new Object());
            rollbackBean.rollbackClientWrongInput(VOHelper.createNewArticleList(4), persons);
            // we should get an exception
            fail("Expect an RemoteException");
        }
        catch (RemoteException e)
        {
            assertTrue(true);
        }

        int personsAfter = rollbackBean.getPersonCount();
        int articlesAfter = rollbackBean.getArticleCount();

        assertEquals(articlesBefore, articlesAfter);
        assertEquals(personsBefore, personsAfter);
    }

    public void testRollbackThrowException() throws Exception
    {
        System.out.println("## testRollbackThrowException");
        int personsBefore = rollbackBean.getPersonCount();

        List persons = VOHelper.createNewPersonList(7);
        try
        {
            rollbackBean.rollbackThrowException(persons);
            fail("RemoteException expected");
        }
        catch (RemoteException e)
        {
            // we expect this exception
            assertTrue(true);
        }

        int personsAfterFailedStore = rollbackBean.getPersonCount();
        assertEquals("Rollback of stored objects failed", personsBefore, personsAfterFailedStore);
    }

    public void testRollbackPassInvalidObject() throws Exception
    {
        System.out.println("## testRollbackPassInvalidObject");
        int personsBefore = rollbackBean.getPersonCount();

        List persons = VOHelper.createNewPersonList(7);
        // add invalid non-persistent object
        persons.add(new Object());
        try
        {
            rollbackBean.rollbackPassInvalidObject(persons);
            fail("RemoteException expected");
        }
        catch (RemoteException e)
        {
            // we expect this exception
            assertTrue(true);
        }

        int personsAfterFailedStore = rollbackBean.getPersonCount();
        assertEquals("Rollback of stored objects failed", personsBefore, personsAfterFailedStore);
    }

    public void testRollbackOdmgAbort() throws Exception
    {
        System.out.println("## testRollbackOdmgAbort");
        int personsBefore = rollbackBean.getPersonCount();

        List persons = VOHelper.createNewPersonList(4);
        /*
        only odmg-abort call was done on server side, this does not thrown
        an RemoteExeption, bean will silent be rollback
        */
        rollbackBean.rollbackOdmgAbort(persons);

        int personsAfterFailedStore = rollbackBean.getPersonCount();
        assertEquals("Rollback of stored objects failed", personsBefore, personsAfterFailedStore);
    }

    public void testRollbackSetRollbackOnly() throws Exception
    {
        System.out.println("## testRollbackSetRollbackOnly");
        int personsBefore = rollbackBean.getPersonCount();

        List persons = VOHelper.createNewPersonList(7);
        // silient rollback without any insert expected
        rollbackBean.rollbackSetRollbackOnly(persons);

        int personsAfterFailedStore = rollbackBean.getPersonCount();
        assertEquals("Rollback of stored objects failed", personsBefore, personsAfterFailedStore);
    }

    public void testRollbackSetRollbackAndAbort() throws Exception
    {
        System.out.println("## testRollbackSetRollbackAndAbort");
        int personsBefore = rollbackBean.getPersonCount();

        List persons = VOHelper.createNewPersonList(7);
        try
        {
            rollbackBean.rollbackSetRollbackAndThrowException(persons);
            fail("RemoteException expected");
        }
        catch (RemoteException e)
        {
            // we expect this exception
            assertTrue(true);
        }

        int personsAfterFailedStore = rollbackBean.getPersonCount();
        assertEquals("Rollback of stored objects failed", personsBefore, personsAfterFailedStore);
    }

    public void testRollbackBreakIteration() throws Exception
    {
        System.out.println("## testRollbackBreakIteration");
        int personsBefore = rollbackBean.getPersonCount();

        List persons = VOHelper.createNewPersonList(5);
        try
        {
            rollbackBean.rollbackBreakIteration(persons);
            fail("RemoteException expected");
        }
        catch (RemoteException e)
        {
            // we expect this exception
            assertTrue(true);
        }

        int personsAfterFailedStore = rollbackBean.getPersonCount();
        assertEquals("Rollback of stored objects failed", personsBefore, personsAfterFailedStore);
    }
}
