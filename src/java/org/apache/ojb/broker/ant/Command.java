package org.apache.ojb.broker.ant;

/* Copyright 1999-2005 The Apache Software Foundation.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.ddlutils.Platform;
import org.apache.ddlutils.model.Database;
import org.apache.ojb.broker.metadata.DescriptorRepository;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

/**
 * Base class for commands that work with a database and repository model.
 * 
 * @author Thomas Dudziak
 * @version $Revision: 1.1 $
 */
public abstract class Command
{
    /** The platform. */
    private Platform _platform;
    /** Whether to stop execution upon an error. */
    private boolean _failOnError = true;

    /**
     * Returns the database type.
     * 
     * @return The database type
     */
    protected String getDatabaseType()
    {
        return _platform.getName();
    }

    /**
     * Sets the platform.
     * 
     * @param platform The platform
     */
    protected void setPlatform(Platform platform)
    {
        _platform = platform;
    }

    /**
     * Determines whether the command execution will be stopped upon an error.
     * Default value is <code>true</code>.
     *
     * @return <code>true</code> if the execution stops in case of an error
     */
    public boolean isFailOnError()
    {
        return _failOnError;
    }

    /**
     * Specifies whether the command execution will be stopped upon an error.
     *
     * @param failOnError <code>true</code> if the execution stops in case of an error
     */
    public void setFailOnError(boolean failOnError)
    {
        _failOnError = failOnError;
    }

    /**
     * Creates the platform for the configured database.
     * 
     * @return The platform
     */
    protected Platform getPlatform() throws BuildException
    {
        return _platform;
    }

    /**
     * Executes this command.
     * 
     * @param task     The executing task
     * @param dbModel  The database model
     * @param objModel The object model
     */
    public abstract void execute(Task task, Database dbModel, DescriptorRepository objModel) throws BuildException;
}
