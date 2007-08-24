package org.apache.ojb.otm;

/* Copyright 2002-2005 The Apache Software Foundation
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

import junit.framework.Assert;
import junit.framework.TestCase;
import org.apache.ojb.broker.Article;
import org.apache.ojb.broker.Identity;
import org.apache.ojb.broker.PersistenceBrokerFactory;
import org.apache.ojb.odmg.oql.EnhancedOQLQuery;
import org.apache.ojb.otm.core.Transaction;



/**
 * Ensure a Transaction can attach multiple connections
 */
public class MultipleConnectionsTest extends TestCase
{
    public MultipleConnectionsTest(String name)
    {
        super(name);
    }

    private TestKit _kit;

    public void setUp()
    {
        _kit = TestKit.getTestInstance();
    }

    public void tearDown()
    {

    }

    /**
     * TODO: I think this only passes because both transactions are in the same thread,
     *       otherwise it would throw an exception every time saying
     *       "Attempt to re-assign a different transaction to a open connection"
     *
     * @throws Throwable
     */
    public void testJustAttachConnections() throws Throwable
    {
        Transaction tx = null;
        Article example;

        OTMConnection conn1 = _kit.acquireConnection(PersistenceBrokerFactory.getDefaultKey());
        OTMConnection conn2 = _kit.acquireConnection(PersistenceBrokerFactory.getDefaultKey());
        try
        {
            tx = _kit.getTransaction(conn1);
            tx.begin();

            tx.registerConnection(conn2);

            example = (Article) conn1.getObjectByIdentity(
                    new Identity(Article.class, Article.class,
                                 new Object[]{new Integer(77779)}));
            if (example == null)
            {
                example = Article.createInstance();
                example.setArticleId(new Integer(77779));
            }
            example.setProductGroupId(new Integer(7));
            example.setStock(333);
            example.setArticleName("333");
            conn1.makePersistent(example);

            EnhancedOQLQuery query = conn2.newOQLQuery();
            query.create("select obj from " + Article.class.getName()
                         + " where " + "articleId = " + example.getArticleId());
            Article same = (Article) conn2.getIteratorByOQLQuery(query).next();
            Assert.assertNotNull("Didn't find object in context of transaction", same);

            tx.commit();

        }
        catch (Throwable ex)
        {
            try
            {
                if (tx != null && tx.isInProgress())
                {
                    tx.rollback();
                }
            }
            catch (Exception ex2)
            {
            }
            throw ex;
        }
        finally
        {
            conn1.close();
        }
    }
}
