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
public class UCEditProduct extends AbstractUseCase
{
    /**
     * UCEditProduct constructor comment.
     */
    public UCEditProduct(org.apache.ojb.broker.PersistenceBroker b)
    {
        super(b);
    }

    /** perform this use case*/
    public void apply()
    {
        String in = readLineWithMessage("Edit Product with id:");
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
            // 3. start broker transaction
            broker.beginTransaction();

            // 4. lookup the product specified by the QBE
            Product toBeEdited = (Product) broker.getObjectByQuery(query);

            // 5. edit the existing entry
            System.out.println("please edit the product entry");
            in = readLineWithMessage("enter name (was " + toBeEdited.getName() + "):");
            toBeEdited.setName(in);
            in = readLineWithMessage("enter price (was " + toBeEdited.getPrice() + "):");
            toBeEdited.setPrice(Double.parseDouble(in));
            in = readLineWithMessage("enter available stock (was " + toBeEdited.getStock() + "):");
            toBeEdited.setStock(Integer.parseInt(in));



            // 6. now ask broker to store the edited object
            broker.store(toBeEdited);
            // 7. commit transaction
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
        return "Edit a product entry";
    }
}
