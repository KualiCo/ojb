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

import java.util.*;

import xdoclet.modules.ojb.constraints.*;

/**
 * Defines the model (class descriptors etc.).
 *
 * @author <a href="mailto:tomdz@users.sourceforge.net">Thomas Dudziak (tomdz@users.sourceforge.net)</a>
 */
public class ModelDef extends DefBase
{
    /** The class definitions keyed by their names */
    private SortedMap _classDefs = new TreeMap();
    /** The tables keyed by their names */
    private SortedMap _tableDefs = new TreeMap();

    /**
     * Creates a new model object.
     */
    public ModelDef()
    {
        super("");
    }

    /**
     * Determines whether this model contains a class descriptor of the given name.
     * 
     * @param qualifiedName The qualified name
     * @return <code>true</code> if such a class descriptor exists in this model
     */
    public boolean hasClass(String qualifiedName)
    {
        return _classDefs.containsKey(qualifiedName.replace('$', '.'));
    }

    /**
     * Returns the class descriptor of the given name contained in this model. The name can be both
     * a fully qualified name as per java spec or a classloader-compatible full name (which uses
     * '$' for inner/nested classes).
     * 
     * @param qualifiedName The qualified name
     * @return The class descriptor or <code>null</code> if there is no such class in this model
     */
    public ClassDescriptorDef getClass(String qualifiedName)
    {
        return (ClassDescriptorDef)_classDefs.get(qualifiedName.replace('$', '.'));
    }

    /**
     * Adds the class descriptor to this model.
     * 
     * @param classDef The class descriptor
     * @return The class descriptor or <code>null</code> if there is no such class in this model
     */
    public void addClass(ClassDescriptorDef classDef)
    {
        classDef.setOwner(this);
        // Regardless of the format of the class name, we're using the fully qualified format
        // This is safe because of the package & class naming constraints of the Java language
        _classDefs.put(classDef.getQualifiedName(), classDef);
    }

    /**
     * Returns all classes in this model.
     * 
     * @return An iterator of all classes
     */
    public Iterator getClasses()
    {
        return _classDefs.values().iterator();
    }

    /**
     * Returns the number of classes contained in this model.
     * 
     * @return The number of classes
     */
    public int getNumClasses()
    {
        return _classDefs.size();
    }

    /**
     * Processes all classes (flattens the hierarchy such that every class has declarations for all fields,
     * references,collections that it will have in the descriptor) and applies modifications (removes ignored
     * features, changes declarations).
     * 
     * @throws ConstraintException If a constraint has been violated 
     */
    public void process() throws ConstraintException
    {
        ClassDescriptorDef classDef;

        // process all classes
        for (Iterator it = getClasses(); it.hasNext();)
        {
            classDef = (ClassDescriptorDef)it.next();
            if (!classDef.hasBeenProcessed())
            {
                classDef.process();
            }
        }
    }

    /**
     * Checks constraints on this model.
     * 
     * @param checkLevel The amount of checks to perform
     * @throws ConstraintException If a constraint has been violated 
     */
    public void checkConstraints(String checkLevel) throws ConstraintException
    {
        // check constraints now after all classes have been processed
        for (Iterator it = getClasses(); it.hasNext();)
        {
            ((ClassDescriptorDef)it.next()).checkConstraints(checkLevel);
        }
        // additional model constraints that either deal with bigger parts of the model or
        // can only be checked after the individual classes have been checked (e.g. specific
        // attributes have been ensured)
        new ModelConstraints().check(this, checkLevel);
    }
}
