package org.apache.ojb.jdori.sql;
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

import javax.jdo.JDOFatalInternalException;

/**
* This is exception indicates internal errors
*
* @author Thomas Mahler
*/
public class OjbStoreFatalInternalException extends JDOFatalInternalException
{
    /**
     * @param className class in which the exception is thrown.
     * @param methodName method throwing the exception.
     */
    OjbStoreFatalInternalException(Class clazz, String methodName, String msg)
    {
        super(clazz.getName() + "." + methodName + ": " + msg);
    }

    /**
     * @param className class in which the exception is thrown.
     * @param methodName method throwing the exception.
     * @param nested nested Exception
     */
    OjbStoreFatalInternalException(Class clazz, String methodName, Exception nested)
    {
        super(clazz.getName() + "." + methodName, new Exception[] { nested });
    }

    /**
     * @param className class in which the exception is thrown.
     * @param methodName method throwing the exception.
     * @param msg the error message
     * @param nested nested Exception
     */
    OjbStoreFatalInternalException(Class clazz, String methodName, String msg, Exception nested)
    {
        super(clazz.getName() + "." + methodName + ": " + msg, new Exception[] { nested });
    }
}
