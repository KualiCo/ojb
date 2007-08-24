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

import java.io.*;
import org.apache.ojb.broker.PersistenceBroker;

/**
 * Does in-memory serialization to achieve a copy of the object graph.
 *
 * @author matthew.baird
 * @see ObjectCopyStrategy
 */
public final class SerializeObjectCopyStrategy implements ObjectCopyStrategy
{
	/**
	 * This implementation will probably be slower than the metadata
	 * object copy, but this was easier to implement.
	 * @see org.apache.ojb.otm.copy.ObjectCopyStrategy#copy(Object)
	 */
	public Object copy(final Object obj, PersistenceBroker broker)
			throws ObjectCopyException
	{
		ObjectOutputStream oos = null;
		ObjectInputStream ois = null;
		try
		{
			final ByteArrayOutputStream bos = new ByteArrayOutputStream();
			oos = new ObjectOutputStream(bos);
			// serialize and pass the object
			oos.writeObject(obj);
			oos.flush();
			final ByteArrayInputStream bin =
					new ByteArrayInputStream(bos.toByteArray());
			ois = new ObjectInputStream(bin);
			// return the new object
			return ois.readObject();
		}
		catch (Exception e)
		{
			throw new ObjectCopyException(e);
		}
		finally
		{
			try
			{
				if (oos != null)
				{
					oos.close();
				}
				if (ois != null)
				{
					ois.close();
				}
			}
			catch (IOException ioe)
			{
				// ignore
			}
		}
	}
}
