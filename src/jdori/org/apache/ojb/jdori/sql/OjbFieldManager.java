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

import javax.jdo.spi.PersistenceCapable;

import org.apache.ojb.broker.PersistenceBroker;
import org.apache.ojb.broker.metadata.AttributeDescriptorBase;
import org.apache.ojb.broker.metadata.ClassDescriptor;

import com.sun.jdori.FieldManager;
import com.sun.jdori.model.jdo.JDOClass;
import com.sun.jdori.model.jdo.JDOField;

/**
 * @author Thomas Mahler
 */
public class OjbFieldManager implements FieldManager
{

    private PersistenceCapable pc;

    private PersistenceBroker broker;

    /**
     * Constructor for OjbFieldManager.
     */
    public OjbFieldManager()
    {
        super();
    }

    /**
     * Constructor for OjbFieldManager.
     */
    public OjbFieldManager(PersistenceCapable pPc)
    {
        pc = pPc;
    }

    /**
     * Constructor for OjbFieldManager.
     */
    public OjbFieldManager(PersistenceCapable pPc, PersistenceBroker pBroker)
    {
        pc = pPc;
        broker = pBroker;
    }

    /**
     * @see com.sun.jdori.FieldManager#storeBooleanField(int, boolean)
     */
    public void storeBooleanField(int fieldNum, boolean value)
    {
    }

    /**
     * @see com.sun.jdori.FieldManager#fetchBooleanField(int)
     */
    public boolean fetchBooleanField(int fieldNum)
    {
        Boolean value = (Boolean) getValue(fieldNum);
        return value.booleanValue();
    }

    /**
     * @see com.sun.jdori.FieldManager#storeCharField(int, char)
     */
    public void storeCharField(int fieldNum, char value)
    {
    }

    /**
     * @see com.sun.jdori.FieldManager#fetchCharField(int)
     */
    public char fetchCharField(int fieldNum)
    {
        Character value = (Character) getValue(fieldNum);
        return value.charValue();
    }

    /**
     * @see com.sun.jdori.FieldManager#storeByteField(int, byte)
     */
    public void storeByteField(int fieldNum, byte value)
    {
    }

    /**
     * @see com.sun.jdori.FieldManager#fetchByteField(int)
     */
    public byte fetchByteField(int fieldNum)
    {
        Byte value = (Byte) getValue(fieldNum);
        return value.byteValue();
    }

    /**
     * @see com.sun.jdori.FieldManager#storeShortField(int, short)
     */
    public void storeShortField(int fieldNum, short value)
    {
    }

    /**
     * @see com.sun.jdori.FieldManager#fetchShortField(int)
     */
    public short fetchShortField(int fieldNum)
    {
        Short value = (Short) getValue(fieldNum);
        return value.shortValue();
    }

    /**
     * @see com.sun.jdori.FieldManager#storeIntField(int, int)
     */
    public void storeIntField(int fieldNum, int value)
    {
    }

    /**
     * @see com.sun.jdori.FieldManager#fetchIntField(int)
     */
    public int fetchIntField(int fieldNum)
    {
        Integer value = (Integer) getValue(fieldNum);
        return value.intValue();
    }

    /**
     * @see com.sun.jdori.FieldManager#storeLongField(int, long)
     */
    public void storeLongField(int fieldNum, long value)
    {
    }

    /**
     * @see com.sun.jdori.FieldManager#fetchLongField(int)
     */
    public long fetchLongField(int fieldNum)
    {
        Long value = (Long) getValue(fieldNum);
        return value.longValue();
    }

    /**
     * @see com.sun.jdori.FieldManager#storeFloatField(int, float)
     */
    public void storeFloatField(int fieldNum, float value)
    {
    }

    /**
     * @see com.sun.jdori.FieldManager#fetchFloatField(int)
     */
    public float fetchFloatField(int fieldNum)
    {
        Float value = (Float) getValue(fieldNum);
        return value.floatValue();
    }

    /**
     * @see com.sun.jdori.FieldManager#storeDoubleField(int, double)
     */
    public void storeDoubleField(int fieldNum, double value)
    {
    }

    /**
     * @see com.sun.jdori.FieldManager#fetchDoubleField(int)
     */
    public double fetchDoubleField(int fieldNum)
    {
        Double value = (Double) getValue(fieldNum);
        return value.doubleValue();
    }

    /**
     * @see com.sun.jdori.FieldManager#storeStringField(int, String)
     */
    public void storeStringField(int fieldNum, String value)
    {
    }

    /**
     * @see com.sun.jdori.FieldManager#fetchStringField(int)
     */
    public String fetchStringField(int fieldNum)
    {
        String value = (String) getValue(fieldNum);
        return value;
    }

    /**
     * @see com.sun.jdori.FieldManager#storeObjectField(int, Object)
     */
    public void storeObjectField(int fieldNum, Object value)
    {
    }

    /**
     * @see com.sun.jdori.FieldManager#fetchObjectField(int)
     */
    public Object fetchObjectField(int fieldNum)
    {
        Object value = getValue(fieldNum);
        return value;
    }

    /**
     * Returns the pc.
     * @return PersistenceCapable
     */
    public PersistenceCapable getPc()
    {
        return pc;
    }

    /**
     * Sets the pc.
     * @param pc The pc to set
     */
    public void setPc(PersistenceCapable pc)
    {
        this.pc = pc;
    }

    String getAttributeName(int fieldNum)
    {
        JDOClass jdoClass = Helper.getJDOClass(pc.getClass());
        JDOField jdoField = jdoClass.getField(fieldNum);
        String attributeName = jdoField.getName();
        return attributeName;
    }

	/**
	 * retrieve the value of attribute[fieldNum] from the object.
	 * @return Object the value of attribute[fieldNum]
	 */
    Object getValue(int fieldNum)
    {
        String attributeName = getAttributeName(fieldNum);
        ClassDescriptor cld = broker.getClassDescriptor(pc.getClass());
        
        // field could be a primitive typed attribute...
        AttributeDescriptorBase fld = cld.getFieldDescriptorByName(attributeName);
        // field could be a reference attribute...
        if (fld == null) 
        {
			fld = cld.getObjectReferenceDescriptorByName(attributeName);
		}
		// or it could be a collection attribute:
        if (fld == null) 
        {
			fld = cld.getCollectionDescriptorByName(attributeName);
		}        
        Object value = fld.getPersistentField().get(pc);
        return value;
    }
}
