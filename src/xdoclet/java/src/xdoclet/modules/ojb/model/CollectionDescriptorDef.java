package xdoclet.modules.ojb.model;

import java.util.Iterator;

import xdoclet.modules.ojb.CommaListIterator;

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
 * A collection descriptor for the ojb repository file.
 *
 * @author <a href="mailto:tomdz@users.sourceforge.net">Thomas Dudziak (tomdz@users.sourceforge.net)</a>
 */
public class CollectionDescriptorDef extends FeatureDescriptorDef
{
    /**
     * Creates a new collection descriptor object.
     *
     * @param name The name of the collection field
     */
    public CollectionDescriptorDef(String name)
    {
        super(name);
    }

    /**
     * Creates copy of the given collection descriptor object. Note that the copy has no owner initially.
     *
     * @param src    The original collection descriptor
     * @param prefix A prefix for the name
     */
    public CollectionDescriptorDef(CollectionDescriptorDef src, String prefix)
    {
        super(src, prefix);
    }

    /**
     * Tries to find the corresponding remote collection for this m:n collection.
     * 
     * @return The corresponding remote collection
     */
    public CollectionDescriptorDef getRemoteCollection()
    {
        if (!hasProperty(PropertyHelper.OJB_PROPERTY_INDIRECTION_TABLE))
        {
            return null;
        }
        ModelDef                modelDef         = (ModelDef)getOwner().getOwner();
        String                  elementClassName = getProperty(PropertyHelper.OJB_PROPERTY_ELEMENT_CLASS_REF);
        ClassDescriptorDef      elementClass     = modelDef.getClass(elementClassName);
        String                  indirTable       = getProperty(PropertyHelper.OJB_PROPERTY_INDIRECTION_TABLE);
        boolean                 hasRemoteKey     = hasProperty(PropertyHelper.OJB_PROPERTY_REMOTE_FOREIGNKEY);
        String                  remoteKey        = getProperty(PropertyHelper.OJB_PROPERTY_REMOTE_FOREIGNKEY);
        CollectionDescriptorDef remoteCollDef    = null;

        // find the collection in the element class that has the same indirection table
        for (Iterator it = elementClass.getCollections(); it.hasNext();)
        {
            remoteCollDef = (CollectionDescriptorDef)it.next();
            if (indirTable.equals(remoteCollDef.getProperty(PropertyHelper.OJB_PROPERTY_INDIRECTION_TABLE)) &&
                (this != remoteCollDef) &&
                (!hasRemoteKey || CommaListIterator.sameLists(remoteKey, remoteCollDef.getProperty(PropertyHelper.OJB_PROPERTY_FOREIGNKEY))))
            {
                return remoteCollDef;
            }
        }
        return null;
    }
}
