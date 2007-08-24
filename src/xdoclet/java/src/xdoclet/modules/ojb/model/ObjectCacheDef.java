package xdoclet.modules.ojb.model;

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

/**
 * Definition of an object cache for a ojb-persistent class.
 *
 * @author    <a href="mailto:tomdz@users.sourceforge.net">Thomas Dudziak (tomdz@users.sourceforge.net)</a>
 * @created   November 9, 2003
 */
public class ObjectCacheDef extends DefBase
{
    /**
     * Creates a new object cache definition object.
     *
     * @param name The name of the object cache class
     */
    public ObjectCacheDef(String name)
    {
        super(name);
    }
}
