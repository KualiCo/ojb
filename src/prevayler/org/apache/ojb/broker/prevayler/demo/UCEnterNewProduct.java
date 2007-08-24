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

import org.apache.ojb.broker.PersistenceBroker;
import org.apache.ojb.broker.PersistenceBrokerException;
import org.apache.ojb.tutorial1.Product;

/**
 * Insert the type's description here.
 * Creation date: (04.03.2001 10:34:15)
 * @author Thomas Mahler
 */
public class UCEnterNewProduct extends AbstractUseCase
{
    /**
     * UCEnterNewProduct constructor comment.
     */
    public UCEnterNewProduct(PersistenceBroker broker)
    {
        super(broker);
    }

    /** perform this use case*/
    public void apply()
    {
        // this will be our new object
        Product newProduct = new Product();
        
        // thma: attention, no sequence numbers yet for ojb/prevalyer        
        newProduct.setId((int)System.currentTimeMillis());
        
        // now read in all relevant information and fill the new object:
        System.out.println("please enter a new product");
        String in = readLineWithMessage("enter name:");
        newProduct.setName(in);
        in = readLineWithMessage("enter price:");
        newProduct.setPrice(Double.parseDouble(in));
        in = readLineWithMessage("enter available stock:");
        newProduct.setStock(Integer.parseInt(in));

        // now perform persistence operations
        try
        {
            // 1. open transaction
            broker.beginTransaction();

            // 2. make the new object persistent
            broker.store(newProduct);
            broker.commitTransaction();
        }
        catch (PersistenceBrokerException ex)
        {
            // if something went wrong: rollback
            broker.abortTransaction();
            System.out.println(ex.getMessage());
            ex.printStackTrace();
        }
    }

    /** get descriptive information on use case*/
    public String getDescription()
    {
        return "Enter a new product";
    }
}
