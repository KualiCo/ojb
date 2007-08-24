package org.apache.ojb.broker.accesslayer;

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
 * Constants used to denote the type of SQL syntax of JOINs
 *
 * @author <a href="mailto:on@ibis.odessa.ua">Oleg Nitz</a>
 * @version $Id: JoinSyntaxTypes.java,v 1.1 2007-08-24 22:17:30 ewestfal Exp $
 */
public interface JoinSyntaxTypes
{
    /**
     * SQL-92 syntax for joins: <br>
     * SELECT ... FROM A INNER JOIN B ON A.PK=B.FK <br>
     * SELECT ... FROM A LEFT OUTER JOIN B ON A.PK=B.FK <br>
     * Nested joins:<br>
     * SELECT ... FROM A INNER JOIN (B INNER JOIN C ON B.PK=C.FK) ON A.PK=B.FK <br>
     * SELECT ... FROM A LEFT OUTER JOIN (B LEFT OUTER JOIN C ON B.PK=C.FK) ON A.PK=B.FK <br>
     */
    public final byte SQL92_JOIN_SYNTAX = 0;

    /**
     * SQL-92 without parenthesis syntax for joins: <br>
     * SELECT ... FROM A INNER JOIN B ON A.PK=B.FK <br>
     * SELECT ... FROM A LEFT OUTER JOIN B ON A.PK=B.FK <br>
     * Nested joins:<br>
     * SELECT ... FROM A INNER JOIN ON A.PK=B.FK INNER JOIN C ON B.PK=C.FK <br>
     * SELECT ... FROM A LEFT OUTER JOIN ON A.PK=B.FK LEFT OUTER JOIN C ON B.PK=C.FK<br>
     */
    public final byte SQL92_NOPAREN_JOIN_SYNTAX = 1;

    /**
     * Oracle syntax for joins: <br>
     * SELECT ... FROM A, B WHERE A.PK=B.FK <br>
     * SELECT ... FROM A, B WHERE A.PK=B.FK(+) <br>
     */
    public final byte ORACLE_JOIN_SYNTAX = 2;

    /**
     * Sybase ASE syntax for joins: <br>
     * SELECT ... FROM A, B WHERE A.PK=B.FK <br>
     * SELECT ... FROM A, B WHERE A.PK*=B.FK <br>
     */
    public final byte SYBASE_JOIN_SYNTAX = 3;
}

