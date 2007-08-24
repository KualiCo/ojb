package org.apache.ojb.broker.util.dbhandling;

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
import java.io.InputStream;
import org.apache.ojb.broker.metadata.JdbcConnectionDescriptor;
import org.apache.ojb.broker.platforms.PlatformException;

/**
 * Interface for classes providing basic database handling (drop, create, init).
 * 
 * @author Thomas Dudziak
 */
public interface DBHandling
{
    /**
     * Sets the working directory.
     * 
     * @param dir The directory
     * @throws IOException If the directory does not exist or cannot be written/read
     */
    public void setWorkDir(String dir) throws IOException;

    /**
     * Sets the jdbc connection to use.
     * 
     * @param jcd The connection to use
     * @throws PlatformException If the target database cannot be handled
     */
    public void setConnection(JdbcConnectionDescriptor jcd) throws PlatformException;

    /**
     * Returns the connection descriptor used by this handling object.
     * 
     * @return The connection descriptor
     */
    public JdbcConnectionDescriptor getConnection();

    /**
     * Adds db definition files to use.
     * 
     * @param srcDir          The directory containing the files
     * @param listOfFilenames The filenames in a comma-separated list
     */
    public void addDBDefinitionFiles(String srcDir, String listOfFilenames) throws IOException;

    /**
     * Adds an input streams containg part of the db definition to use.
     * 
     * @param schemataStreams Input streams
     */
    public void addDBDefinitionFile(InputStream inputStream) throws IOException;

    /**
     * Creates the database.
     * 
     * @throws PlatformException If some error occurred
     */
    public void createDB() throws PlatformException;

    /**
     * Creates the tables according to the schema files.
     * 
     * @throws PlatformException If some error occurred
     */
    public void initDB() throws PlatformException;
}
