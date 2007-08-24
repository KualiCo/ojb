package org.apache.ojb.broker.accesslayer.sql;

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

import org.apache.ojb.broker.metadata.ProcedureDescriptor;
import org.apache.ojb.broker.util.logging.Logger;

/**
 * Model a call to a stored procedure based on ProcedureDescriptors
 *
 * @see org.apache.ojb.broker.metadata.ProcedureDescriptor
 * @author <a href="mailto:rburt3@mchsi.com">Randall Burt</a>
 * @author <a href="mailto:rgallagh@bellsouth.net">Ron Gallagher</a>
 * @version $Id: SqlProcedureStatement.java,v 1.1 2007-08-24 22:17:39 ewestfal Exp $
 */

public class SqlProcedureStatement implements SqlStatement
{

    /**
     * The descriptor that defines the procedure to invoke.
     */
    private ProcedureDescriptor procedureDescriptor;

    /**
     * The logger to utilize
     */
    private Logger logger;

    /**
     * Create an instance of this object.
     *
     * @param procedureDescriptor the descriptor that defines the procedure to invoke.
     * @param logger the logger to utilize
     */
    public SqlProcedureStatement(ProcedureDescriptor procedureDescriptor, Logger logger)
    {
        // Save the values.
        this.procedureDescriptor = procedureDescriptor;
        this.logger = logger;
    }

    /**
     * Get the syntax that is required to invoke the procedure that is defined
     * by the <code>ProcedureDescriptor</code> that was passed to the
     * constructor of this class.
     *
     * @see SqlStatement#getStatement()
     */
    public String getStatement()
    {
        StringBuffer sb = new StringBuffer(512);
        int argumentCount = this.procedureDescriptor.getArgumentCount();
        if (this.procedureDescriptor.hasReturnValue())
        {
            sb.append("{ ?= call ");
        }
        else
        {
            sb.append("{ call ");
        }
        sb.append(this.procedureDescriptor.getName());
        sb.append("(");
        for (int i = 0; i < argumentCount; i++)
        {
            if (i == 0)
            {
                sb.append("?");
            }
            else
            {
                sb.append(",?");
            }
        }
        sb.append(") }");
        return sb.toString();
    }
}
