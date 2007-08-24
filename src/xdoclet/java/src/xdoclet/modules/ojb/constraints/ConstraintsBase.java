package xdoclet.modules.ojb.constraints;

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

import xdoclet.modules.ojb.LogHelper;
import xdoclet.modules.ojb.model.ClassDescriptorDef;
import xdoclet.modules.ojb.model.DefBase;
import xdoclet.modules.ojb.model.PropertyHelper;

/**
 * Base class for constraints providing some common functionality.
 *
 * @author <a href="mailto:tomdz@users.sourceforge.net">Thomas Dudziak (tomdz@users.sourceforge.net)</a>
 */
public abstract class ConstraintsBase
{
    /** The checking levels */
    public static final String CHECKLEVEL_NONE   = "none";
    public static final String CHECKLEVEL_BASIC  = "basic";
    public static final String CHECKLEVEL_STRICT = "strict";

    /**
     * Constraint that ensures that the proxy-prefetching-limit has a valid value.
     * 
     * @param def        The descriptor (class, reference, collection)
     * @param checkLevel The current check level (this constraint is checked in basic and strict)
     */
    protected void checkProxyPrefetchingLimit(DefBase def, String checkLevel) throws ConstraintException
    {
        if (CHECKLEVEL_NONE.equals(checkLevel))
        {
            return;
        }
        if (def.hasProperty(PropertyHelper.OJB_PROPERTY_PROXY_PREFETCHING_LIMIT))
        {
            if (!def.hasProperty(PropertyHelper.OJB_PROPERTY_PROXY))
            {
                if (def instanceof ClassDescriptorDef)
                {
                    LogHelper.warn(true,
                                   ConstraintsBase.class,
                                   "checkProxyPrefetchingLimit",
                                   "The class "+def.getName()+" has a proxy-prefetching-limit property but no proxy property");
                }
                else
                {    
                    LogHelper.warn(true,
                                   ConstraintsBase.class,
                                   "checkProxyPrefetchingLimit",
                                   "The feature "+def.getName()+" in class "+def.getOwner().getName()+" has a proxy-prefetching-limit property but no proxy property");
                }
            }
    
            String propValue = def.getProperty(PropertyHelper.OJB_PROPERTY_PROXY_PREFETCHING_LIMIT);
    
            try
            {
                int value = Integer.parseInt(propValue);
    
                if (value < 0)
                {
                    if (def instanceof ClassDescriptorDef)
                    {
                        throw new ConstraintException("The proxy-prefetching-limit value of class "+def.getName()+" must be a non-negative number");
                    }
                    else
                    {    
                        throw new ConstraintException("The proxy-prefetching-limit value of the feature "+def.getName()+" in class "+def.getOwner().getName()+" must be a non-negative number");
                    }
                }
            }
            catch (NumberFormatException ex)
            {
                if (def instanceof ClassDescriptorDef)
                {
                    throw new ConstraintException("The proxy-prefetching-limit value of the class "+def.getName()+" is not a number");
                }
                else
                {    
                    throw new ConstraintException("The proxy-prefetching-limit value of the feature "+def.getName()+" in class "+def.getOwner().getName()+" is not a number");
                }
            }
        }
    }
}
