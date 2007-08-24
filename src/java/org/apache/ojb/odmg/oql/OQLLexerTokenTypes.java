// $ANTLR 2.7.5 (20050128): "oql-ojb.g" -> "OQLParser.java"$

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


public interface OQLLexerTokenTypes {
	int EOF = 1;
	int NULL_TREE_LOOKAHEAD = 3;
	int TOK_RPAREN = 4;
	int TOK_LPAREN = 5;
	int TOK_COMMA = 6;
	int TOK_SEMIC = 7;
	int TOK_COLON = 8;
	int TOK_DOT = 9;
	int TOK_INDIRECT = 10;
	int TOK_CONCAT = 11;
	int TOK_EQ = 12;
	int TOK_PLUS = 13;
	int TOK_MINUS = 14;
	int TOK_SLASH = 15;
	int TOK_STAR = 16;
	int TOK_LE = 17;
	int TOK_GE = 18;
	int TOK_NE = 19;
	int TOK_NE2 = 20;
	int TOK_LT = 21;
	int TOK_GT = 22;
	int TOK_LBRACK = 23;
	int TOK_RBRACK = 24;
	int TOK_DOLLAR = 25;
	int NameFirstCharacter = 26;
	int NameCharacter = 27;
	int Identifier = 28;
	int TOK_UNSIGNED_INTEGER = 29;
	int TOK_APPROXIMATE_NUMERIC_LITERAL = 30;
	int TOK_EXACT_NUMERIC_LITERAL = 31;
	int CharLiteral = 32;
	int StringLiteral = 33;
	int WhiteSpace = 34;
	int NewLine = 35;
	int CommentLine = 36;
	int MultiLineComment = 37;
	int LITERAL_select = 38;
	int LITERAL_distinct = 39;
	int LITERAL_from = 40;
	int LITERAL_where = 41;
	int LITERAL_order = 42;
	int LITERAL_by = 43;
	int LITERAL_group = 44;
	int LITERAL_prefetch = 45;
	int LITERAL_exists = 46;
	int LITERAL_in = 47;
	int LITERAL_asc = 48;
	int LITERAL_desc = 49;
	int LITERAL_or = 50;
	int LITERAL_and = 51;
	int LITERAL_nil = 52;
	int LITERAL_not = 53;
	int LITERAL_list = 54;
	int LITERAL_between = 55;
	int LITERAL_is_undefined = 56;
	int LITERAL_is_defined = 57;
	int LITERAL_like = 58;
	int LITERAL_true = 59;
	int LITERAL_false = 60;
	int LITERAL_date = 61;
	int LITERAL_time = 62;
	int LITERAL_timestamp = 63;
}
