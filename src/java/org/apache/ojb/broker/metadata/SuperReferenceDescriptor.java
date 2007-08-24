package org.apache.ojb.broker.metadata;

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

import java.util.List;
import java.util.Map;
import java.util.HashMap;

import org.apache.commons.lang.SystemUtils;
import org.apache.ojb.broker.metadata.fieldaccess.AnonymousPersistentField;
import org.apache.ojb.broker.metadata.fieldaccess.PersistentField;
import org.apache.ojb.broker.metadata.fieldaccess.PersistentFieldFactory;
import org.apache.ojb.broker.util.ClassHelper;
import org.apache.ojb.broker.util.logging.Logger;
import org.apache.ojb.broker.util.logging.LoggerFactory;

/**
 * This class handle inheritance as 1-1 association based on a anonymous field
 * (no field in persistent object needed).
 *
 * @version $Id: SuperReferenceDescriptor.java,v 1.1 2007-08-24 22:17:29 ewestfal Exp $
 */
public class SuperReferenceDescriptor extends ObjectReferenceDescriptor
{
    private transient Logger log;

    public static final String SUPER_FIELD_INTERNAL_NAME = "ojbSuperFieldInternal";
    public static final String SUPER_FIELD_NAME = RepositoryElements.TAG_SUPER;

    private Boolean javaInheritance;
    private Map declaredInheritanceFields = new HashMap();

    public SuperReferenceDescriptor(ClassDescriptor descriptor)
    {
        super(descriptor);
        // most important call, create new specific field for inheritance
        super.setPersistentField(new SuperReferenceField(this));
        // needed immutable settings
        super.setLazy(false);
        super.setCascadeRetrieve(true);
        super.setCascadingStore(CASCADE_OBJECT);
        super.setCascadingDelete(CASCADE_OBJECT);
    }

    public boolean isSuperReferenceDescriptor()
    {
        return true;
    }

    public void setItemClass(Class c)
    {
        super.setItemClass(c);
        getClassDescriptor().setBaseClass(c.getName());
    }

    /**
     * Noop, a specific {@link org.apache.ojb.broker.metadata.fieldaccess.PersistentField} is
     * used internal - {@link org.apache.ojb.broker.metadata.SuperReferenceDescriptor.SuperReferenceField}.
     */
    public void setPersistentField(Class c, String fieldname)
    {
        // noop
    }

    /**
     * Noop, a specific {@link org.apache.ojb.broker.metadata.fieldaccess.PersistentField} is
     * used internal - {@link org.apache.ojb.broker.metadata.SuperReferenceDescriptor.SuperReferenceField}.
     */
    public void setPersistentField(PersistentField pf)
    {
        // noop
    }

    public void setLazy(boolean lazy)
    {
        getLog().info("Not allowed to change this property, will ignore setting");
    }

    public void setCascadeRetrieve(boolean b)
    {
        getLog().info("Not allowed to change this property, will ignore setting");
    }

    public void setCascadingStore(int cascade)
    {
        getLog().info("Not allowed to change this property, will ignore setting");
    }

    public void setCascadingStore(String value)
    {
        getLog().info("Not allowed to change this property, will ignore setting");
    }

    public void setCascadingDelete(int cascade)
    {
        getLog().info("Not allowed to change this property, will ignore setting");
    }

    public void setCascadingDelete(String value)
    {
        getLog().info("Not allowed to change this property, will ignore setting");
    }

    public void setCascadeStore(boolean cascade)
    {
        getLog().info("Not allowed to change this property, will ignore setting");
    }

    public void setCascadeDelete(boolean cascade)
    {
        getLog().info("Not allowed to change this property, will ignore setting");
    }

    public SuperReferenceField getInheritanceField()
    {
        return (SuperReferenceField) getPersistentField();
    }

    /**
     * If this method returns <em>true</em> the inheritance described by this object
     * is a <em>normal</em> JAVA inheritance. If <em>false</em> the inheritance is only declared
     * in the O/R mapping it's a <em>declarative inheritance</em>, the referenced "super class" in <strong>not</strong>
     * a JAVA super class of the main class.
     */
    public boolean isJavaInheritance()
    {
        if(javaInheritance == null)
        {
            javaInheritance = getClassDescriptor().getSuperClassDescriptor().getClassOfObject()
                    .isAssignableFrom(getClassDescriptor().getClassOfObject()) ? Boolean.TRUE : Boolean.FALSE;
        }
        return javaInheritance.booleanValue();
    }

    synchronized PersistentField getDeclaredInheritanceField(Class target, String name)
    {
        Map fields = (HashMap) declaredInheritanceFields.get(target);
        if(fields == null)
        {
            fields = new HashMap();
            declaredInheritanceFields.put(target, fields);
        }
        PersistentField pf = (PersistentField) fields.get(name);
        if(pf == null)
        {
            pf = PersistentFieldFactory.createPersistentField(target, name);
            // System.out.println("## tmp field: " + target + ", name: " + name + ", field: " + pf);
            fields.put(name, pf);
        }
        return pf;
    }

    private Logger getLog()
    {
        if(log == null)
        {
            log = LoggerFactory.getLogger(SuperReferenceField.class);
        }
        return log;
    }


    //====================================================
    // inner class
    //====================================================

    public static final class SuperReferenceField extends AnonymousPersistentField
    {
        private transient Logger log;

        private SuperReferenceDescriptor superRef;

        public SuperReferenceField(SuperReferenceDescriptor superRef)
        {
            super(SUPER_FIELD_INTERNAL_NAME);
            this.superRef = superRef;
        }

        private Logger getLog()
        {
            if(log == null)
            {
                log = LoggerFactory.getLogger(SuperReferenceField.class);
            }
            return log;
        }

        /**
         * Field values of 'value' (base object) are copied to 'obj' (derived object)
         * then obj is saved in a map
         *
         * @param target - the base object instance
         * @param value  - the derived object instance
         * @throws MetadataException
         */
        public synchronized void set(Object target, Object value) throws MetadataException
        {
            // System.out.println("target: " + target + " value: " + value);
            ClassDescriptor superCld = superRef.getClassDescriptor().getSuperClassDescriptor();
            if(superRef.isJavaInheritance())
            {
                copyFields(superCld, target, superCld, value, true, true);
            }
            else
            {
                copyFields(superRef.getClassDescriptor(), target, superCld, value, false, false);
            }
        }

        /**
         * Field values of specified 'obj' (the derived object) are copied to
         * 'value' (base object) then value is returned as a referenced object.
         * If the base object is the super class of the specified 'obj', then
         * return the specified object.
         * Else a base class instance will be created at runtime and the field values
         * from the derived object are copied to the base class object.
         *
         * @param obj - the base object instance
         * @throws MetadataException
         */
        public synchronized Object get(Object obj) throws MetadataException
        {
            if(obj == null) return null;
            if(superRef.isJavaInheritance())
            {
                return obj;
            }
            else
            {
                return getObjectWithDeclaredSuperClass(obj);
            }
        }

        private Object getObjectWithDeclaredSuperClass(Object obj)
        {
            Object value = getFromFieldCache(obj);
            if(value == null)
            {
                ClassDescriptor baseCld = null;
                try
                {
                    baseCld = superRef.getClassDescriptor().getSuperClassDescriptor();
                    value = ClassHelper.buildNewObjectInstance(baseCld);
                }
                catch(Exception e)
                {
                    throw new MetadataException("Can't create new base class object for '"
                            + (baseCld != null ? baseCld.getClassNameOfObject() : null) + "'", e);
                }
                copyFields(baseCld, value, superRef.getClassDescriptor(), obj, true, false);
                putToFieldCache(obj, value);
            }
            return value;
        }

        void copyFields(ClassDescriptor targetCld, Object target, ClassDescriptor sourceCld, Object source, boolean targetIsSuper, boolean javaInheritance)
        {
            if(getLog().isDebugEnabled())
            {
                String msg = ("Copy fields from " + SystemUtils.LINE_SEPARATOR
                        + "source object '" + (source != null ? source.getClass().getName() : null) + "'" + SystemUtils.LINE_SEPARATOR
                        + "using source fields declared in '" + sourceCld.getClassNameOfObject() + "'" + SystemUtils.LINE_SEPARATOR
                        + "to target object '" + (target != null ? target.getClass().getName() : null) + "'" + SystemUtils.LINE_SEPARATOR
                        + "using target fields declared in '" + targetCld.getClassNameOfObject() + "'" + SystemUtils.LINE_SEPARATOR
                        + "the fields to copy are declared in '" + (targetIsSuper ? targetCld.getClassNameOfObject() : sourceCld.getClassNameOfObject()) + "' class" + SystemUtils.LINE_SEPARATOR
                        + "the used classes are associated by java inheritance: " + javaInheritance + SystemUtils.LINE_SEPARATOR);
                getLog().debug(msg);
            }
            /*
            arminw:
            If the target object is a super object of the source object, iterate all target object fields.
            If the source object is a super object of the target object, iterate all source object fields

            If java inheritance is used (target is super class of source or vice versa) we can use the same
            FieldDescriptor to copy the fields.
            If only a "declarative inheritance" is used (no class inheritance, only identical field names of the super class)
            we have to use the associated FieldDescriptor of target and source ClassDescriptor
            */
            FieldDescriptor[] fields = targetIsSuper ? targetCld.getFieldDescriptions() : sourceCld.getFieldDescriptions();
            for(int i = 0; i < fields.length; i++)
            {
                FieldDescriptor field = fields[i];
                if(!field.isAnonymous())
                {
                    performFieldCopy(target,  targetCld, source, sourceCld,
                                field.getPersistentField(), targetIsSuper, javaInheritance);
                }
            }
            List refs = targetIsSuper ? targetCld.getCollectionDescriptors() : sourceCld.getCollectionDescriptors();
            for(int i = 0; i < refs.size(); i++)
            {
                CollectionDescriptor col = (CollectionDescriptor) refs.get(i);
                PersistentField pf = col.getPersistentField();
                performFieldCopy(target,  targetCld, source, sourceCld, pf, targetIsSuper, javaInheritance);
            }

            refs = targetIsSuper ? targetCld.getObjectReferenceDescriptors() : sourceCld.getObjectReferenceDescriptors();
            for(int i = 0; i < refs.size(); i++)
            {
                ObjectReferenceDescriptor ord = (ObjectReferenceDescriptor) refs.get(i);
                PersistentField pf = ord.getPersistentField();
                performFieldCopy(target,  targetCld, source, sourceCld, pf, targetIsSuper, javaInheritance);
            }
        }

        private void performFieldCopy(Object target, ClassDescriptor targetCld, Object source,
                                 ClassDescriptor sourceCld, PersistentField pf, boolean targetIsSuper, boolean javaInheritance)
        {
            if(javaInheritance)
            {
                pf.set(target, pf.get(source));
            }
            else
            {
                if(targetIsSuper)
                {
                    if(pf instanceof SuperReferenceField)
                    {
                        log.error("Declared inheritance doesn't support nested super references, target '"
                                + targetCld.getClassNameOfObject() + "' has super reference");
                    }
                    else
                    {
                        PersistentField tmp = superRef.getDeclaredInheritanceField(sourceCld.getClassOfObject(), pf.getName());
                        pf.set(target, tmp.get(source));
                    }
                }
                else
                {
                    PersistentField tmp = superRef.getDeclaredInheritanceField(targetCld.getClassOfObject(), pf.getName());
                    tmp.set(target, pf.get(source));
                }
            }
        }
    }
}

