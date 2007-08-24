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
 * Base class for feature descriptors (field, reference, collection).
 *
 * @author <a href="mailto:tomdz@users.sourceforge.net">Thomas Dudziak (tomdz@users.sourceforge.net)</a>
 */
public abstract class FeatureDescriptorDef extends DefBase
{
    /** The original feature descriptor def that this descriptor def is a copy of */ 
    private FeatureDescriptorDef _originalDef;
    /** Whether this is an inherited feature */
    private boolean _isInherited = false;
    /** Whether this is a nested feature */
    private boolean _isNested    = false;

    /**
     * Creates a new feature descriptor object.
     *
     * @param name The name of the feature
     */
    public FeatureDescriptorDef(String name)
    {
        super(name);
    }

    /**
     * Creates copy of the given feature descriptor object. Note that the copy has no owner initially.
     *
     * @param src    The original feature
     * @param prefix A prefix for the name
     */
    public FeatureDescriptorDef(FeatureDescriptorDef src, String prefix)
    {
        super(src, prefix);
        _originalDef = src;
        _isInherited = src._isInherited;
        _isNested    = src._isNested;
    }

    /**
     * Returns the original feature descriptor object that this one is a copy of.
     * 
     * @return The original descriptor def or <code>null</code> if this feature is neither inherited nor nested
     */
    public FeatureDescriptorDef getOriginal()
    {
        return _originalDef;
    }

    /**
     * Declares this feature to be inherited.
     */
    public void setInherited()
    {
        _isInherited = true;
    }

    /**
     * Returns whether this feature has been inherited.
     * 
     * @return <code>true</code> if this feature has been inherited
     */
    public boolean isInherited()
    {
        return _isInherited;
    }
    
    /**
     * Declares this feature to be nested.
     */
    public void setNested()
    {
        _isNested = true;
    }

    /**
     * Returns whether this feature is a nested feature.
     * 
     * @return <code>true</code> if this feature is nested
     */
    public boolean isNested()
    {
        return _isNested;
    }
}
