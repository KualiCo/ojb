package xdoclet.modules.ojb.tests;

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

import java.sql.ResultSet;
import java.util.Map;

import org.apache.ojb.broker.accesslayer.RowReader;
import org.apache.ojb.broker.accesslayer.ResultSetAndStatement;
import org.apache.ojb.broker.metadata.ClassDescriptor;

/**
 * Row reader class used in some of the unit tests.
 */
public class TestRowReader implements RowReader
{
    public Object readObjectFrom(Map row)
    {
        return null;
    }

    public void refreshObject(Object instance, Map row)
    {
    }

    public void readObjectArrayFrom(ResultSetAndStatement rs, Map row)
    {
    }

    public void readPkValuesFrom(ResultSetAndStatement rs, Map row)
    {
    }

    public void setClassDescriptor(ClassDescriptor cld)
    {
    }

    public ClassDescriptor getClassDescriptor()
    {
        return null;
    }
}
