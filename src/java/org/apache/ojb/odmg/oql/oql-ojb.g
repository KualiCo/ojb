/* ====================================================================
 * OQL Sample Grammar for Object Data Management Group (ODMG)
 *
 * Copyright (c) 1999 Micro Data Base Systems, Inc. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. All advertising materials mentioning features or use of this
 *    software must display the following acknowledgment:
 *    "This product includes software developed by Micro Data Base Systems, Inc.
 *    (http://www.mdbs.com) for the use of the Object Data Management Group
 *    (http://www.odmg.org/)."
 *
 * 4. The names "mdbs" and "Micro Data Base Systems" must not be used to
 *    endorse or promote products derived from this software without
 *    prior written permission. For written permission, please contact
 *    info@mdbs.com.
 *
 * 5. Products derived from this software may not be called "mdbs"
 *    nor may "mdbs" appear in their names without prior written
 *    permission of Micro Data Base Systems, Inc.
 *
 * 6. Redistributions of any form whatsoever must retain the following
 *    acknowledgment:
 *    "This product includes software developed by Micro Data Base Systems, Inc.
 *    (http://www.mdbs.com) for the use of the Object Data Management Group
 *    (http://www.odmg.org/)."
 *
 * THIS SOFTWARE IS PROVIDED BY MICRO DATA BASE SYSTEMS, INC. ``AS IS'' AND ANY
 * EXPRESSED OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL MICRO DATA BASE SYSTEMS, INC. OR
 * ITS ASSOCIATES BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 */

/* mkalen: Notice from the original mdbs oql.g (do not touch Antlr version): */
/*
**  oql.g
**
** Built with Antlr 2.5
**   java antlr.Tool ojb-oql.g
*/
header{
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

package org.apache.ojb.odmg.oql;

}

/**
 * This OQL grammar has been derived from a OQL sample grammar from the ODMG
 * WebSite. The original grammar is copyright protected by MicroData Base
 * Systems: Copyright (c) 1999 Micro Data Base Systems, Inc. All rights
 * reserved.
 *
 * The original grammar has been modified to fit into the OJB
 * Persistence Managment System.
 *
 * Modifications done by Ch. Rath, Th. Mahler, S. Harris and many others.
 *
 * This grammar can be used to build an OQL Parser with the ANTLR Parser
 * construction set.
 * The grammar defines a Parser that translates valid OQL Strings
 * into ojb.broker.query.Query Objects. These query objects can be used
 * to perform database queries by means of the OJB PersistenceBroker.
 * @see org.apache.ojb.odmg.oql.OQLQueryImpl for implementation details.
 * @version $Id: oql-ojb.g,v 1.1 2007-08-24 22:17:36 ewestfal Exp $
 */
class OQLLexer extends Lexer ;
options {
    k = 2;
    charVocabulary = '\u0003'..'\uFFFE';
    testLiterals=false;     // don't automatically test for literals
    caseSensitive=true;
    caseSensitiveLiterals=true;
}

        /* punctuation */
TOK_RPAREN
    options {
        paraphrase = "right parenthesis";
    }                       :       ')'             ;
TOK_LPAREN
    options {
        paraphrase = "left parenthesis";
    }                       :       '('             ;
TOK_COMMA
    options {
        paraphrase = "comma";
    }                       :       ','             ;
TOK_SEMIC
    options {
        paraphrase = "semicolon";
    }                       :       ';'             ;

TOK_COLON   :   ':' ;
// protected - removed to fix bug with '.' 10/4/99
TOK_DOT
    options {
        paraphrase = "dot";
    }                       :       '.'             ;
TOK_INDIRECT
    options {
        paraphrase = "dot";
    }                       :       '-' '>'         { $setType(TOK_DOT); } ;
TOK_CONCAT
    options {
        paraphrase = "operator";
    }                       :       '|' '|'         ;
TOK_EQ
    options {
        paraphrase = "comparison operator";
    }                       :       '='             ;
TOK_PLUS
    options {
        paraphrase = "operator";
    }                       :       '+'             ;
TOK_MINUS
    options {
        paraphrase = "operator";
    }                       :       '-'             ;
TOK_SLASH
    options {
        paraphrase = "operator";
    }                       :       '/'             ;
TOK_STAR
    options {
        paraphrase = "operator";
    }                       :       '*'             ;
TOK_LE
    options {
        paraphrase = "comparison operator";
    }                       :       '<' '='         ;
TOK_GE
    options {
        paraphrase = "comparison operator";
    }                       :       '>' '='         ;
TOK_NE
    options {
        paraphrase = "comparison operator";
    }                       :       '<' '>'         ;
TOK_NE2
    options {
        paraphrase = "comparison operator";
    }                       :       '!' '='         ;
TOK_LT
    options {
        paraphrase = "comparison operator";
    }                       :       '<'             ;
TOK_GT
    options {
        paraphrase = "comparison operator";
    }                       :       '>'             ;
TOK_LBRACK
    options {
        paraphrase = "left bracket";
    }                       :       '['             ;
TOK_RBRACK
    options {
        paraphrase = "right bracket";
    }                       :       ']'             ;
TOK_DOLLAR
    :       '$' ;

/*
 * Names
 */

// BRJ: support -> and $
protected
NameFirstCharacter:
        ( 'A'..'Z' | 'a'..'z' | '_' | TOK_DOT | TOK_INDIRECT | TOK_DOLLAR );

// BRJ: use NameFirstCharacter as base
protected
NameCharacter:
        ( NameFirstCharacter | '0'..'9' );

Identifier
        options {testLiterals=true;}
            :

        NameFirstCharacter
        ( NameCharacter )*
    ;

/* Numbers */
protected
TOK_UNSIGNED_INTEGER :
        '0'..'9'
    ;

// a couple protected methods to assist in matching floating point numbers
protected
TOK_APPROXIMATE_NUMERIC_LITERAL
        :       'e' ('+'|'-')? ('0'..'9')+
	;


// a numeric literal
TOK_EXACT_NUMERIC_LITERAL
    options {
        paraphrase = "numeric value";
    }                       :

            '.'
            (
                TOK_UNSIGNED_INTEGER
            )+
            { _ttype = TOK_EXACT_NUMERIC_LITERAL; }

            (
                TOK_APPROXIMATE_NUMERIC_LITERAL
                { _ttype = TOK_APPROXIMATE_NUMERIC_LITERAL; }
            )?

        |   (
                TOK_UNSIGNED_INTEGER
            )+
            { _ttype = TOK_UNSIGNED_INTEGER; }

            // only check to see if it's a float if looks like decimal so far
            (
                '.'
                (
                    TOK_UNSIGNED_INTEGER
                )*
                { _ttype = TOK_EXACT_NUMERIC_LITERAL; }
                (TOK_APPROXIMATE_NUMERIC_LITERAL { _ttype = TOK_APPROXIMATE_NUMERIC_LITERAL; } )?

            |    TOK_APPROXIMATE_NUMERIC_LITERAL { _ttype = TOK_APPROXIMATE_NUMERIC_LITERAL; }
            )? // cristi, 20001027, ? was missing
	;


/*
** Define a lexical rule to handle strings.
*/
CharLiteral
    options {
        paraphrase = "character string";
    }                       :
        '\''!
        (
            '\'' '\''           { $setText("'"); }
        |   '\n'                { newline(); }
        |   ~( '\'' | '\n' )
        )*
        '\''!
    ;

StringLiteral
    options {
        paraphrase = "character string";
    }                       :
        '"'!
        (
            '\\' '"'            { $setText("\""); }
        |   '\n'                { newline(); }
        |   ~( '\"' | '\n' )   // cristi, 20001028, was '\'' instead of '\"'
        )*
        '"'!
    ;



/*
** Define white space so that we can throw out blanks
*/
WhiteSpace :
        (
            ' '
        |   '\t'
        |   '\r'
        )   { $setType(Token.SKIP); }
    ;

NewLine :
        '\n'    { newline(); $setType(Token.SKIP); }
    ;

/*
** Define a lexical rule to handle line comments.
*/
CommentLine :
        '/'! '/'!
        (
            ~'\n'!
        )*
        '\n'!   { newline(); $setType(Token.SKIP); }
    ;

/*
** Define a lexical rule to handle block comments.
*/
MultiLineComment :
        "/*"
        (
            { LA(2)!='/' }? '*'
        |   '\n' { newline(); }
        |   ~('*'|'\n')
        )*
        "*/"
        { $setType(Token.SKIP); }
    ;

{
import java.util.ArrayList;
import java.util.Collection;
import java.util.Vector;

import org.apache.ojb.broker.metadata.ClassDescriptor;
import org.apache.ojb.broker.metadata.MetadataManager;
import org.apache.ojb.broker.metadata.ObjectReferenceDescriptor;
import org.apache.ojb.broker.util.ClassHelper;
import org.apache.ojb.broker.query.Criteria;
import org.apache.ojb.broker.query.Query;
import org.apache.ojb.broker.query.QueryByCriteria;
import org.apache.ojb.broker.query.QueryFactory;
}
class OQLParser extends Parser ;
options {
    k = 3;
    codeGenMakeSwitchThreshold = 3;
    codeGenBitsetTestThreshold = 4;
}

buildQuery returns [Query query = null] :

        query = selectQuery ( TOK_SEMIC )?
    ;


selectQuery returns [QueryByCriteria query = null] :

        {
            Class clazz = null;
            Criteria criteria = new Criteria();
            String[] projectionAttrs;
            boolean distinct = false;
        }

        "select"

        (
            options {
                warnWhenFollowAmbig = false;
            } :

            "distinct"
            {
                 distinct = true;
            }
        )?

        projectionAttrs = projectionAttributes

        "from" clazz = fromClause
        ( "where" whereClause[criteria] )?
        {
            if (clazz != null)
            {
                if (projectionAttrs[0].indexOf('.') < 0)
                {
                    query = QueryFactory.newQuery(clazz, criteria, distinct);
                }
                else
                {
                    ClassDescriptor cld = MetadataManager.getInstance().getRepository().getDescriptorFor(clazz);
                    for (int i = 0; i < projectionAttrs.length; i++)
                    {
                        projectionAttrs[i] = projectionAttrs[i].substring(projectionAttrs[i].indexOf('.') + 1);
                    }

                    ArrayList descs = cld.getAttributeDescriptorsForPath(projectionAttrs[0]);
                    int pathLen = descs.size();

                    if ((pathLen > 0) && (descs.get(pathLen - 1) instanceof ObjectReferenceDescriptor))
                    {
                        ObjectReferenceDescriptor ord =
                                ((ObjectReferenceDescriptor) descs.get(pathLen - 1));
                        query = QueryFactory.newQuery(clazz, criteria, distinct);
                        query.setObjectProjectionAttribute(projectionAttrs[0],
                                                           ord.getItemClass());
                    }
                    else
                    {
                        query = QueryFactory.newReportQuery(clazz, projectionAttrs, criteria, distinct);
                    }
                }
            }
        }

        ( "order" "by" orderClause[query] )?
        ( "group" "by" groupClause[query] )?
        ( "prefetch" prefetchClause[query] )?

    ;


existsQuery returns [Query query = null] :

        {
            Class clazz = null;
            Criteria criteria = new Criteria();
        }

        "exists"

        // We also ignore the projection
        projectionAttributes

        "in" clazz = fromClause
        ( TOK_COLON whereClause[criteria] )?
        {
            if (clazz != null) {
                query = QueryFactory.newQuery(clazz, criteria);
            }
		}
    ;

fromClause returns [Class clazz = null] :

        id:Identifier
        {
            try {
                clazz = ClassHelper.getClass(id.getText());
            } catch (Exception e) {
            }
        }
    ;

whereClause[Criteria criteria] :

        orExpr[criteria]
    ;

projectionAttributes returns [String[] projectionAttrs = null] :
    {
        String first = null;
        ArrayList list = null;
    }

    (
        id:Identifier
        (
            {
                first = id.getText();
            }
        )
        (
            TOK_COMMA
            id1:Identifier
            (
                {
                    if (list == null)
                    {
                        list = new ArrayList();
                        list.add(first);
                    }
                    list.add(id1.getText());
                }
            )
        )*
        | TOK_STAR
    )

    {
        if (list == null)
        {
            projectionAttrs = new String[] {first};
        }
        else
        {
            projectionAttrs = (String[]) list.toArray(new String[list.size()]);
        }

    }
  ;

orderClause[QueryByCriteria query] :

        sortCriterion[query]
        (
            TOK_COMMA sortCriterion[query]
        )*
    ;

sortCriterion[QueryByCriteria query] :

        { boolean descending = false; }

	    id:Identifier
		// BRJ: asc or desc or nothing
        (
        	(
            	"asc" 	{ descending = false; }
        	|   "desc"	{ descending = true; }
        	)
        )?

        {
            if (descending) {
                query.addOrderByDescending(id.getText());
            } else {
                query.addOrderByAscending(id.getText());
            }
        }
    ;

groupClause[QueryByCriteria query] :

        groupCriterion[query]
        (
            TOK_COMMA groupCriterion[query]
        )*
    ;

groupCriterion[QueryByCriteria query] :

	    id:Identifier
        {
            query.addGroupBy(id.getText());
        }
    ;


// start prefetch

prefetchClause[QueryByCriteria query] :

        prefetchCriterion[query]
        (
            TOK_COMMA prefetchCriterion[query]
        )*
    ;

prefetchCriterion[QueryByCriteria query] :

	    id:Identifier
        {
            query.addPrefetchedRelationship(id.getText());
        }
    ;

// end prefetch


orExpr[Criteria criteria] :

        andExpr[criteria]
        (
            "or"
            { Criteria orCriteria = new Criteria(); }

            andExpr[orCriteria]
            { criteria.addOrCriteria(orCriteria); }
        )*
    ;

andExpr[Criteria criteria] :

        quantifierExpr[criteria]
        (
            "and"
            { Criteria andCriteria = new Criteria(); }

            quantifierExpr[andCriteria]
            { criteria.addAndCriteria(andCriteria); }
        )*
    ;

quantifierExpr[Criteria criteria] :
            TOK_LPAREN orExpr[criteria] TOK_RPAREN
        |   equalityExpr[criteria]
        |   likeExpr[criteria]
        |   undefinedExpr[criteria]
        |   betweenExpr[criteria]
        |   inExpr[criteria]
        |   existsExpr[criteria]
    ;



equalityExpr[Criteria criteria] :

        {
        	Object value = null;
        }

        id:Identifier
        (
            (
                TOK_EQ
                (
	                "nil" {criteria.addIsNull(id.getText());}
	                | value = literal { criteria.addEqualTo(id.getText(), value); }
                )

            |   TOK_NE
                (
	                "nil" {criteria.addNotNull(id.getText());}
	                | value = literal { criteria.addNotEqualTo(id.getText(), value); }
                )
            |   TOK_NE2
                (
	                "nil" {criteria.addNotNull(id.getText());}
	                | value = literal { criteria.addNotEqualTo(id.getText(), value); }
                )

            |   TOK_LT value = literal
                { criteria.addLessThan(id.getText(), value); }
            |   TOK_GT value = literal
                { criteria.addGreaterThan(id.getText(), value); }
            |   TOK_LE value = literal
                { criteria.addLessOrEqualThan(id.getText(), value); }
            |   TOK_GE value = literal
                { criteria.addGreaterOrEqualThan(id.getText(), value); }
            )
        )
    ;

inExpr[Criteria criteria] :

        {
        	 boolean negative = false;
        	 Collection coll;
        }

        id:Identifier
        (
            "not"
            { negative = true; }
        )?
        "in"
        (
            "list"
            {}
        )?
        coll = argList

        {
            if (negative) {
                criteria.addNotIn(id.getText(), coll);
            } else {
                criteria.addIn(id.getText(), coll);
            }
        }
    ;



betweenExpr[Criteria criteria] :

        {
            boolean negative = false;
            Object lower = null;
            Object upper = null;
        }

        id:Identifier
        (
            "not"
            { negative = true; }
        )?
        "between" lower = literal "and" upper = literal
        {
            if (negative) {
                criteria.addNotBetween(id.getText(), lower, upper);
            } else {
                criteria.addBetween(id.getText(), lower, upper);
            }
        }
    ;

undefinedExpr[Criteria criteria] :

        { boolean negative = false; }
        (
            "is_undefined"
            { negative = false; }
        |   "is_defined"
            { negative = true; }
        )
        TOK_LPAREN
        id:Identifier
        TOK_RPAREN

        {
            if (negative) {
                criteria.addNotNull(id.getText());
            } else {
                criteria.addIsNull(id.getText());
            }
        }

    ;

likeExpr[Criteria criteria] :

        {
            boolean negative = false;
            Object value = null;
        }

        id:Identifier
        (
            "not"
            { negative = true; }
        )?
        "like" value = literal
        {
            if (negative) {
                criteria.addNotLike(id.getText(), value);
            } else {
                criteria.addLike(id.getText(), value);
            }
        }
    ;

existsExpr[Criteria criteria] :

        {
            Query subQuery = null;
            boolean negative = false;
        }

        (
            "not"
            { negative = true; }
        )?
        (
            "exists" TOK_LPAREN subQuery = selectQuery TOK_RPAREN
        |   subQuery = existsQuery
        )
        {
            if (negative) {
                criteria.addNotExists(subQuery);
            } else {
                criteria.addExists(subQuery);
            }
        }
    ;

literal returns [Object value = null] :

            // Bind parameter
            // BRJ: does not create new Object
            TOK_DOLLAR TOK_UNSIGNED_INTEGER
			{ value = null; }

            // Boolean
        |   "true"
            { value = Boolean.TRUE; }
        |   "false"
            { value = Boolean.FALSE; }

           // Integer
        |   tokInt:TOK_UNSIGNED_INTEGER
            {
            	try
             	{
            		value = Integer.valueOf(tokInt.getText());
            	}
            	catch (NumberFormatException ignored)
            	{
            		value = Long.valueOf(tokInt.getText());
            	}
            }
            // Doubles
        |   tokADbl:TOK_APPROXIMATE_NUMERIC_LITERAL
            { value = Double.valueOf(tokADbl.getText()); }
        |   tokEDbl:TOK_EXACT_NUMERIC_LITERAL
            { value = Double.valueOf(tokEDbl.getText()); }

            // Character
        |   tokChar:CharLiteral
            { value = new Character(tokChar.getText().charAt(0)); }

            // String
        |   tokStr:StringLiteral
            { value = tokStr.getText(); }

            // Date
        |   "date" tokDate:StringLiteral
            // Format yyyy-mm-dd
            { value = java.sql.Date.valueOf(tokDate.getText()); }

            // Time
        |   "time" tokTime:StringLiteral
            // Format hh:mm:ss
            { value = java.sql.Time.valueOf(tokTime.getText()); }

            // Timestamp
        |   "timestamp" tokTs:StringLiteral
            // Format yyyy-mm-dd hh:mm:ss.fffffffff
            { value = java.sql.Timestamp.valueOf(tokTs.getText()); }
    ;

argList returns [Collection coll = null] :
// return a collection of parameters or null if we only have $1,$2...
	    {
		Collection temp = new Vector();
	    Object val;
	    }

        TOK_LPAREN
        (
            val = literal

            // BRJ: do not add null objects
            {if (val != null) {temp.add(val);} }
            (
                TOK_COMMA
	            val = literal
	            {if (val != null) {temp.add(val);} }
            )*
        )?
        TOK_RPAREN

		{
		if (!temp.isEmpty()) {coll = temp;}
		}
    ;