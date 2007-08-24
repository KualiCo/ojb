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

import org.apache.ojb.broker.PBKey;
import org.apache.ojb.broker.PersistenceBrokerFactory;

import javax.resource.spi.ConnectionRequestInfo;

/**
 *
 * @author <a href="mailto:mattbaird@yahoo.com">Matthew Baird<a>
 */

public class OTMConnectionRequestInfo
		implements ConnectionRequestInfo
{
	private PBKey m_pbKey;

	public OTMConnectionRequestInfo(PBKey pbkey)
	{
		Util.log("In OTMConnectionRequestInfo");
		m_pbKey = pbkey;
	}

	public PBKey getPbKey()
	{
		if (m_pbKey == null)
			return PersistenceBrokerFactory.getDefaultKey();
		else
			return m_pbKey;
	}

	public boolean equals(Object o)
	{
		if (this == o) return true;
		if (!(o instanceof OTMConnectionRequestInfo)) return false;

		final OTMConnectionRequestInfo otmConnectionRequestInfo = (OTMConnectionRequestInfo) o;

		if (m_pbKey != null ? !m_pbKey.equals(otmConnectionRequestInfo.m_pbKey) : otmConnectionRequestInfo.m_pbKey != null) return false;

		return true;
	}

	public int hashCode()
	{
		return (m_pbKey != null ? m_pbKey.hashCode() : 0);
	}
}
