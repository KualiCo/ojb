package org.apache.ojb.broker.prevayler.demo;

/* Copyright 2003-2005 The Apache Software Foundation
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

import org.apache.ojb.broker.query.Query;
import org.apache.ojb.broker.query.QueryByCriteria;
import org.apache.ojb.tutorial1.Product;

import java.util.Collection;

/**
 * Insert the type's description here.
 * Creation date: (04.03.2001 10:34:15)
 * @author Thomas Mahler
 */
public class UCListAllProducts extends AbstractUseCase
{
    /**
     * UCEnterNewProduct constructor comment.
     */
    public UCListAllProducts(org.apache.ojb.broker.PersistenceBroker b)
    {
        super(b);
    }

    /** perform this use case*/
    public void apply()
    {
        System.out.println("The list of available products:");
        // build a query that select all objects of Class Product, without any further criteria
        // according to ODMG the Collection containing all instances of a persistent class is called "Extent"
        Query query = new QueryByCriteria(Product.class, null);
        try
        {
            // ask the broker to retrieve the Extent collection
            Collection allProducts = broker.getCollectionByQuery(query);
            // now iterate over the result to print each product
            java.util.Iterator iter = allProducts.iterator();
            while (iter.hasNext())
            {
                System.out.println(iter.next());
            }
        }
        catch (Throwable t)
        {
            t.printStackTrace();
        }
    }

    /** get descriptive information on use case*/
    public String getDescription()
    {
        return "List all product entries";
    }
}
