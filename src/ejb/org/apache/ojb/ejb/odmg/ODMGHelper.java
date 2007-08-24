package org.apache.ojb.ejb.odmg;

import org.apache.ojb.odmg.OJB;
import org.odmg.Database;
import org.odmg.Implementation;
import org.odmg.ODMGException;

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

/**
 * Helper class that provide access to the OJB main/access classes.
 * Nevertheless it is recommended to bind OJB main classes to JNDI and to
 * lookup the {@link org.odmg.Implementation} instance via JNDI.
 *
 * @author <a href="mailto:arminw@apache.org">Armin Waibel</a>
 * @version $Id: ODMGHelper.java,v 1.1 2007-08-24 22:17:30 ewestfal Exp $
 */
public class ODMGHelper
{
    public static final String DEF_DATABASE_NAME = "default";
    private static Implementation odmg;
    private static Database db;

    static
    {
        odmg = OJB.getInstance();
        db = odmg.newDatabase();
        try
        {
            System.out.println("[ODMG] Open new database " + db + " using databaseName name " + DEF_DATABASE_NAME);
            db.open(DEF_DATABASE_NAME, Database.OPEN_READ_WRITE);
        }
        catch (ODMGException e)
        {
            e.printStackTrace();
        }
    }

    public static Implementation getODMG()
    {
        return odmg;
    }

    static Database getDatabase()
    {
        return db;
    }
}
