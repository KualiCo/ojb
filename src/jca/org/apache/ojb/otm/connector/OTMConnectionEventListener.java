package org.apache.ojb.otm.connector;

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

import javax.resource.spi.ConnectionEvent;
import javax.resource.spi.ConnectionEventListener;
import java.util.Vector;

/**
 *
 * @author <a href="mailto:mattbaird@yahoo.com">Matthew Baird<a>
 */

public class OTMConnectionEventListener implements ConnectionEventListener
{
	private Vector m_listeners = null;

	public OTMConnectionEventListener()
	{
		m_listeners = new Vector();
	}

	void sendEvent(ConnectionEvent ce)
	{
		Vector list = (Vector) m_listeners.clone();
		int size = list.size();
		for (int i = 0; i < size; i++)
		{
			ConnectionEventListener l =
					(ConnectionEventListener) list.elementAt(i);
			switch (ce.getId())
			{
				case ConnectionEvent.CONNECTION_CLOSED:
					l.connectionClosed(ce);
					break;
				case ConnectionEvent.LOCAL_TRANSACTION_STARTED:
					l.localTransactionStarted(ce);
					break;
				case ConnectionEvent.LOCAL_TRANSACTION_COMMITTED:
					l.localTransactionCommitted(ce);
					break;
				case ConnectionEvent.LOCAL_TRANSACTION_ROLLEDBACK:
					l.localTransactionRolledback(ce);
					break;
				case ConnectionEvent.CONNECTION_ERROR_OCCURRED:
					l.connectionErrorOccurred(ce);
					break;
				default:
					throw new IllegalArgumentException("Illegal eventType: " + ce.getId());
			}
		}
	}

	void addConnectorListener(ConnectionEventListener l)
	{
		m_listeners.addElement(l);
	}

	void removeConnectorListener(ConnectionEventListener l)
	{
		m_listeners.removeElement(l);
	}

	public void connectionClosed(ConnectionEvent event)
	{
		// do nothing. The event is sent by the ConnectionImpl class
	}

	public void connectionErrorOccurred(ConnectionEvent event)
	{
		sendEvent(event);
	}

	public void localTransactionCommitted(ConnectionEvent event)
	{
		sendEvent(event);
	}

	public void localTransactionStarted(ConnectionEvent event)
	{
		sendEvent(event);
	}

	public void localTransactionRolledback(ConnectionEvent event)
	{
		sendEvent(event);
	}
}
