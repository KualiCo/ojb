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

/**
 * implements Use Case "Quit Application".
 * @author Thomas Mahler
 */
public class UCQuitApplication extends AbstractUseCase
{
    /**
     * UCQuitApplication constructor comment.
     */
    public UCQuitApplication(org.apache.ojb.broker.PersistenceBroker b)
    {
        super(b);
    }

    /**
     * apply method comment.
     */
    public void apply()
    {
        // release the broker in use
        broker.close();
        
        // no OJB API for quitting the application ;-)
        System.out.println("bye...");
        System.exit(0);
    }

    /**
     * getDescription method comment.
     */
    public String getDescription()
    {
        return "Quit Application";
    }
}
