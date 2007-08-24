package org.apache.ojb.broker.ant;

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

import java.io.IOException;
import java.io.Writer;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;

import org.apache.commons.beanutils.DynaBean;
import org.apache.ddlutils.Platform;
import org.apache.ddlutils.model.Database;

/**
 * Encapsulates the data objects read by Digester.
 * 
 * @author Thomas Dudziak
 */
public class DataSet
{
    /** The data objects (dyna beans) */
    private ArrayList _beans = new ArrayList();

    /**
     * Adds a data object.
     * 
     * @param bean The data object
     */
    public void add(DynaBean bean)
    {
        _beans.add(bean);
    }

    /**
     * Generates and writes the sql for inserting the currently contained data objects.
     * 
     * @param model    The database model
     * @param platform The platform
     * @param writer   The output stream
     */
    public void createInsertionSql(Database model, Platform platform, Writer writer) throws IOException
    {
        for (Iterator it = _beans.iterator(); it.hasNext();)
        {
            writer.write(platform.getInsertSql(model, (DynaBean)it.next()));
            if (it.hasNext())
            {
                writer.write("\n");
            }
        }
    }

    /**
     * Inserts the currently contained data objects into the database.
     *  
     * @param platform  The (connected) database platform for inserting data 
     * @param model     The database model
     * @param batchSize The batch size; use 1 for not using batch mode
     */
    public void insert(Platform platform, Database model, int batchSize) throws SQLException
    {
        if (batchSize <= 1)
        {
            for (Iterator it = _beans.iterator(); it.hasNext();)
            {
                platform.insert(model, (DynaBean)it.next());
            }
        }
        else
        {
            for (int startIdx = 0; startIdx < _beans.size(); startIdx += batchSize)
            {
                platform.insert(model, _beans.subList(startIdx, startIdx + batchSize));
            }
        }
    }
}
