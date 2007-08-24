package org.apache.ojb.broker.accesslayer.sql;

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

import org.apache.ojb.broker.util.factory.ConfigurableFactory;
import org.apache.ojb.broker.platforms.Platform;

import java.util.Map;
import java.util.HashMap;

/**
 * This factory creates SqlGenerator instances. it is a configurable factory and
 * can be used to generate user defined implementations too.
 * @author Thomas Mahler
 */
public class SqlGeneratorFactory extends ConfigurableFactory
{
	private static SqlGeneratorFactory instance = null;

    private Map generatorMap = new HashMap();

	/**
	 * @see org.apache.ojb.broker.util.factory.ConfigurableFactory#getConfigurationKey()
	 */
	protected String getConfigurationKey()
	{
		return "SqlGeneratorClass";
	}

	public SqlGenerator createSqlGenerator(Platform pf)
	{
		SqlGenerator gen = (SqlGenerator) generatorMap.get(pf.getClass().getName());
        if(gen == null)
        {
            gen = (SqlGenerator) this.createNewInstance(Platform.class, pf);
            generatorMap.put(pf.getClass(), gen);
        }
        return gen;
	}

	public static SqlGeneratorFactory getInstance()
	{
		if (instance == null)
		{
			instance = new SqlGeneratorFactory();
		}
		return instance;
	}
}
