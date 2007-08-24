package org.apache.ojb.otm.copy;

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

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.ojb.broker.PersistenceBroker;
import org.apache.ojb.broker.util.IdentityMapFactory;

/**
 * User: matthew.baird
 * Date: Jul 7, 2003
 * Time: 3:05:22 PM
 */
public final class ReflectiveObjectCopyStrategy implements ObjectCopyStrategy
{
	private static final Set FINAL_IMMUTABLE_CLASSES;
	private static final Object[] EMPTY_OBJECT_ARRAY = new Object[0];
	private static final Class[] EMPTY_CLASS_ARRAY = new Class[0];
    private static final SerializeObjectCopyStrategy _serialize = new SerializeObjectCopyStrategy();

	static
	{
		FINAL_IMMUTABLE_CLASSES = new HashSet(17);
		FINAL_IMMUTABLE_CLASSES.add(String.class);
		FINAL_IMMUTABLE_CLASSES.add(Byte.class);
		FINAL_IMMUTABLE_CLASSES.add(Short.class);
		FINAL_IMMUTABLE_CLASSES.add(Integer.class);
		FINAL_IMMUTABLE_CLASSES.add(Long.class);
		FINAL_IMMUTABLE_CLASSES.add(Float.class);
		FINAL_IMMUTABLE_CLASSES.add(Double.class);
		FINAL_IMMUTABLE_CLASSES.add(Character.class);
		FINAL_IMMUTABLE_CLASSES.add(Boolean.class);
	}

	/**
	 * makes a deep clone of the object, using reflection.
	 * @param toCopy the object you want to copy
	 * @return
	 */
	public final Object copy(final Object toCopy, PersistenceBroker broker)
	{
		return clone(toCopy, IdentityMapFactory.getIdentityMap(), new HashMap());
	}

	/*
	 * class used to cache class metadata info
	 */
	private static final class ClassMetadata
	{
		Constructor m_noArgConstructor;
		Field[] m_declaredFields;
		boolean m_noArgConstructorAccessible;
		boolean m_fieldsAccessible;
		boolean m_hasNoArgConstructor = true;
	}

	private static Object clone(final Object toCopy, final Map objMap, final Map metadataMap)
	{
		/**
		 * first, check to make sure we aren't recursing to some object that we've already copied.
		 * if the toCopy is in the objMap, just return it.
		 */
		if (objMap.containsKey(toCopy)) return objMap.get(toCopy);
		final Class objClass = toCopy.getClass();
		final Object retval;
		if (objClass.isArray())
		{
			retval = handleArray(toCopy, objMap, objClass, metadataMap);
		}
		else if (FINAL_IMMUTABLE_CLASSES.contains(objClass))
		{
			objMap.put(toCopy, toCopy);
			retval = toCopy;
		}
		else
		{
			retval = handleObjectWithNoArgsConstructor(metadataMap, objClass, objMap, toCopy);
		}
		return retval;
	}

	private static Object handleObjectWithNoArgsConstructor(final Map metadataMap, final Class objClass, final Map objMap, final Object toCopy)
	{
		Object retval = null;
		ClassMetadata metadata = (ClassMetadata) metadataMap.get(objClass);
		if (metadata == null)
		{
			metadata = new ClassMetadata();
			metadataMap.put(objClass, metadata);
		}
		Constructor noArg = metadata.m_noArgConstructor;
		if (metadata.m_hasNoArgConstructor)
		{
			if (noArg == null)
			{
				try
				{
					noArg = objClass.getDeclaredConstructor(EMPTY_CLASS_ARRAY);
					metadata.m_noArgConstructor = noArg;
				}
				catch (Exception e)
				{
					metadata.m_hasNoArgConstructor = false;
	//				throw new ObjectCopyException("class [" + objClass.getName() + "] has no noArg constructor: " + e.toString(), e);
				}
			}
		}
		if (metadata.m_hasNoArgConstructor)
		{
			if (!metadata.m_noArgConstructorAccessible && (Modifier.PUBLIC & noArg.getModifiers()) == 0)
			{
				try
				{
					noArg.setAccessible(true);
				}
				catch (SecurityException e)
				{
					throw new ObjectCopyException("cannot access noArg constructor [" + noArg + "] of class [" + objClass.getName() + "]: " + e.toString(), e);
				}
				metadata.m_noArgConstructorAccessible = true;
			}
			try
			{
				/**
				 * create the return value via the default no argument constructor
				 */
				retval = noArg.newInstance(EMPTY_OBJECT_ARRAY);
				objMap.put(toCopy, retval);
			}
			catch (Exception e)
			{
				throw new ObjectCopyException("cannot instantiate class [" + objClass.getName() + "] using noArg constructor: " + e.toString(), e);
			}
			for (Class c = objClass; c != Object.class; c = c.getSuperclass())
			{
				copyClass(metadataMap, c, toCopy, retval, objMap);
			}
		}
        else
        {
            retval = _serialize.copy(toCopy, null);
        }
		return retval;
	}

	private static void copyClass(final Map metadataMap, final Class c, final Object obj, final Object retval, final Map objMap)
	{
		ClassMetadata metadata;
		metadata = (ClassMetadata) metadataMap.get(c);
		if (metadata == null)
		{
			metadata = new ClassMetadata();
			metadataMap.put(c, metadata);
		}
		Field[] declaredFields = metadata.m_declaredFields;
		if (declaredFields == null)
		{
			declaredFields = c.getDeclaredFields();
			metadata.m_declaredFields = declaredFields;
		}
		setFields(obj, retval, declaredFields, metadata.m_fieldsAccessible, objMap, metadataMap);
		metadata.m_fieldsAccessible = true;
	}

	private static Object handleArray(final Object obj, final Map objMap, final Class objClass, final Map metadataMap)
	{
		final Object retval;
		final int arrayLength = Array.getLength(obj);
		/**
		 * immutable
		 */
		if (arrayLength == 0)
		{
			objMap.put(obj, obj);
			retval = obj;
		}
		else
		{
			final Class componentType = objClass.getComponentType();
			/**
			 * even though arrays implicitly have a public clone(), it
			 * cannot be invoked reflectively, so need to do copy construction
			 */
			retval = Array.newInstance(componentType, arrayLength);
			objMap.put(obj, retval);
			if (componentType.isPrimitive() || FINAL_IMMUTABLE_CLASSES.contains(componentType))
			{
				System.arraycopy(obj, 0, retval, 0, arrayLength);
			}
			else
			{
				for (int i = 0; i < arrayLength; ++i)
				{
					/**
					 * recursively clone each array slot:
					 */
					final Object slot = Array.get(obj, i);
					if (slot != null)
					{
						final Object slotClone = clone(slot, objMap, metadataMap);
						Array.set(retval, i, slotClone);
					}
				}
			}
		}
		return retval;
	}

	/**
	 * copy all fields from the "from" object to the "to" object.
	 *
	 * @param from source object
	 * @param to from's clone
	 * @param fields fields to be populated
	 * @param accessible 'true' if all 'fields' have been made accessible during
	 * traversal
	 */
	private static void setFields(final Object from, final Object to,
	                              final Field[] fields, final boolean accessible,
	                              final Map objMap, final Map metadataMap)
	{
		for (int f = 0, fieldsLength = fields.length; f < fieldsLength; ++f)
		{
			final Field field = fields[f];
			final int modifiers = field.getModifiers();
			if ((Modifier.STATIC & modifiers) != 0) continue;
			if ((Modifier.FINAL & modifiers) != 0)
				throw new ObjectCopyException("cannot set final field [" + field.getName() + "] of class [" + from.getClass().getName() + "]");
			if (!accessible && ((Modifier.PUBLIC & modifiers) == 0))
			{
				try
				{
					field.setAccessible(true);
				}
				catch (SecurityException e)
				{
					throw new ObjectCopyException("cannot access field [" + field.getName() + "] of class [" + from.getClass().getName() + "]: " + e.toString(), e);
				}
			}
			try
			{
				cloneAndSetFieldValue(field, from, to, objMap, metadataMap);
			}
			catch (Exception e)
			{
				throw new ObjectCopyException("cannot set field [" + field.getName() + "] of class [" + from.getClass().getName() + "]: " + e.toString(), e);
			}
		}
	}

	private static void cloneAndSetFieldValue(final Field field, final Object src, final Object dest, final Map objMap, final Map metadataMap) throws IllegalAccessException
	{
		Object value = field.get(src);
		if (value == null)
		{
			/**
			 *  null is a valid type, ie the object may initialize this field to a different value,
			 * so we must explicitely set all null fields.
			 */
			field.set(dest, null);
		}
		else
		{
			final Class valueType = value.getClass();
			if (!valueType.isPrimitive() && !FINAL_IMMUTABLE_CLASSES.contains(valueType))
			{
				/**
				 * recursively call clone on value as it could be an object reference, an array,
				 * or some mutable type
				 */
				value = clone(value, objMap, metadataMap);
			}
			field.set(dest, value);
		}
	}
}
