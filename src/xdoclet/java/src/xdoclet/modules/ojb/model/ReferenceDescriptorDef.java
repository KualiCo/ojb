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
 * A reference descriptor for the ojb repository file.
 *
 * @author    <a href="mailto:tomdz@users.sourceforge.net">Thomas Dudziak (tomdz@users.sourceforge.net)</a>
 * @created   April 13, 2003
 */
public class ReferenceDescriptorDef extends FeatureDescriptorDef
{
    /** Whether this is an anonymous reference */
    private boolean _isAnonymous = false;

    /**
     * Creates a new reference descriptor object.
     *
     * @param name The name of the reference field
     */
    public ReferenceDescriptorDef(String name)
    {
        super(name);
    }

    /**
     * Creates copy of the given reference descriptor object. Note that the copy has no owner initially.
     *
     * @param src    The original reference
     * @param prefix A prefix for the name
     */
    public ReferenceDescriptorDef(ReferenceDescriptorDef src, String prefix)
    {
        super(src, prefix);
    }

    /**
     * Declares this reference to be anonymous.
     */
    public void setAnonymous()
    {
        _isAnonymous = true;
    }
    
    /**
     * Returns whether this reference is anonymous.
     * 
     * @return <code>true</code> if it is anonymous
     */
    public boolean isAnonymous()
    {
        return _isAnonymous;
    }
}
