package org.apache.ojb.jdori.sql;

import java.lang.reflect.Field;

import javax.jdo.JDOFatalInternalException;

import com.sun.jdori.StateManagerInternal;
import com.sun.jdori.common.model.jdo.JDOModelFactoryImpl;
import com.sun.jdori.common.model.runtime.RuntimeJavaModelFactory;
import com.sun.jdori.model.java.JavaModel;
import com.sun.jdori.model.java.JavaModelFactory;
import com.sun.jdori.model.jdo.JDOClass;
import com.sun.jdori.model.jdo.JDOModel;

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

/**
 * @author Thomas Mahler
 *
 * this is a helper class providing static convenience methods.
 */
class Helper
{
	/**
	 * this method looks up the appropriate JDOClass for a given persistent Class.
	 * It uses the JDOModel to perfom this lookup.
	 * @param c the persistent Class
	 * @return the JDOCLass object
	 */
	static JDOClass getJDOClass(Class c)
	{
		JDOClass rc = null;
		try
		{
			JavaModelFactory javaModelFactory = RuntimeJavaModelFactory.getInstance();
			JavaModel javaModel = javaModelFactory.getJavaModel(c.getClassLoader());
			JDOModel m = JDOModelFactoryImpl.getInstance().getJDOModel(javaModel);
			rc = m.getJDOClass(c.getName());
		}
		catch (RuntimeException ex)
		{
			throw new JDOFatalInternalException("Not a JDO class: " + c.getName()); 
		}
		return rc;
	}

	/**
	 * obtains the internal JDO lifecycle state of the input StatemanagerInternal.
	 * This Method is helpful to display persistent objects internal state.
	 * @param sm the StateManager to be inspected
	 * @return the LifeCycleState of a StateManager instance
	 */
	static Object getLCState(StateManagerInternal sm)
	{
		// unfortunately the LifeCycleState classes are package private.
		// so we have to do some dirty reflection hack to access them
		try
		{
			Field myLC = sm.getClass().getDeclaredField("myLC");
			myLC.setAccessible(true);
			return myLC.get(sm);
		}
		catch (NoSuchFieldException e)
		{
			return e;
		}
		catch (IllegalAccessException e)
		{
			return e;
		}	
	}

}
