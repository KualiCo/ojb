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

import java.io.BufferedReader;
import java.io.InputStreamReader;

/**
 * Insert the type's description here.
 * Creation date: (04.03.2001 11:31:41)
 * @author Thomas Mahler
 */
public abstract class AbstractUseCase implements UseCase
{
    protected PersistenceBroker broker;

    /**
     * AbstractUseCase constructor comment.
     */
    public AbstractUseCase(PersistenceBroker broker)
    {
        this.broker = broker;
    }

    /** perform this use case*/
    public abstract void apply();

    /** get descriptive information on use case*/
    public abstract String getDescription();

    /**
     * read a single line from stdin and return as String
     */
    protected String readLineWithMessage(String message)
    {
        System.out.print(message + " ");
        try
        {
            BufferedReader rin = new BufferedReader(new InputStreamReader(System.in));
            return rin.readLine();
        }
        catch (Exception e)
        {
            return "";
        }
    }
}
