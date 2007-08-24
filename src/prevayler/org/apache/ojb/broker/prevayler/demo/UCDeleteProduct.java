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
import org.apache.ojb.broker.query.QueryByIdentity;
import org.apache.ojb.tutorial1.Product;

/**
 * Insert the type's description here.
 * Creation date: (04.03.2001 10:34:15)
 * @author Thomas Mahler
 */
public class UCDeleteProduct extends AbstractUseCase
{
    /**
     * UCEnterNewProduct constructor comment.
     */
    public UCDeleteProduct(org.apache.ojb.broker.PersistenceBroker b)
    {
        super(b);
    }

    /** perform this use case*/
    public void apply()
    {
        String in = readLineWithMessage("Delete Product with id:");
        int id = Integer.parseInt(in);

        // We don't have a reference to the selected Product.
        // So first we have to lookup the object,
        // we do this by a query by example (QBE):
        // 1. build an example object with matching primary key values:
        Product example = new Product();
        example.setId(id);
        // 2. build a QueryByIdentity from this sample instance:
        Query query = new QueryByIdentity(example);
        try
        {
            // start broker transaction
            broker.beginTransaction();
            // lookup the product specified by the QBE
            Product toBeDeleted = (Product) broker.getObjectByQuery(query);
            // now ask broker to delete the object
            broker.delete(toBeDeleted);
            // commit transaction
            broker.commitTransaction();
        }
        catch (Throwable t)
        {
            // rollback in case of errors
            broker.abortTransaction();
            t.printStackTrace();
        }
    }

    /** get descriptive information on use case*/
    public String getDescription()
    {
        return "Delete a product entry";
    }
}
