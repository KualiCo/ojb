// $ANTLR 2.7.5 (20050128): "oql-ojb.g" -> "OQLLexer.java"$

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


import java.io.InputStream;
import java.io.Reader;
import java.util.Hashtable;

import antlr.ANTLRHashString;
import antlr.ByteBuffer;
import antlr.CharBuffer;
import antlr.CharStreamException;
import antlr.CharStreamIOException;
import antlr.InputBuffer;
import antlr.LexerSharedInputState;
import antlr.NoViableAltForCharException;
import antlr.RecognitionException;
import antlr.Token;
import antlr.TokenStream;
import antlr.TokenStreamException;
import antlr.TokenStreamIOException;
import antlr.TokenStreamRecognitionException;
import antlr.collections.impl.BitSet;

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
 * @version $Id: OQLLexer.java,v 1.1 2007-08-24 22:17:36 ewestfal Exp $
 */
public class OQLLexer extends antlr.CharScanner implements OQLLexerTokenTypes, TokenStream
 {
public OQLLexer(InputStream in) {
	this(new ByteBuffer(in));
}
public OQLLexer(Reader in) {
	this(new CharBuffer(in));
}
public OQLLexer(InputBuffer ib) {
	this(new LexerSharedInputState(ib));
}
public OQLLexer(LexerSharedInputState state) {
	super(state);
	caseSensitiveLiterals = true;
	setCaseSensitive(true);
	literals = new Hashtable();
	literals.put(new ANTLRHashString("nil", this), new Integer(52));
	literals.put(new ANTLRHashString("group", this), new Integer(44));
	literals.put(new ANTLRHashString("between", this), new Integer(55));
	literals.put(new ANTLRHashString("date", this), new Integer(61));
	literals.put(new ANTLRHashString("select", this), new Integer(38));
	literals.put(new ANTLRHashString("timestamp", this), new Integer(63));
	literals.put(new ANTLRHashString("like", this), new Integer(58));
	literals.put(new ANTLRHashString("is_undefined", this), new Integer(56));
	literals.put(new ANTLRHashString("asc", this), new Integer(48));
	literals.put(new ANTLRHashString("or", this), new Integer(50));
	literals.put(new ANTLRHashString("in", this), new Integer(47));
	literals.put(new ANTLRHashString("list", this), new Integer(54));
	literals.put(new ANTLRHashString("from", this), new Integer(40));
	literals.put(new ANTLRHashString("desc", this), new Integer(49));
	literals.put(new ANTLRHashString("true", this), new Integer(59));
	literals.put(new ANTLRHashString("by", this), new Integer(43));
	literals.put(new ANTLRHashString("not", this), new Integer(53));
	literals.put(new ANTLRHashString("and", this), new Integer(51));
	literals.put(new ANTLRHashString("distinct", this), new Integer(39));
	literals.put(new ANTLRHashString("prefetch", this), new Integer(45));
	literals.put(new ANTLRHashString("false", this), new Integer(60));
	literals.put(new ANTLRHashString("time", this), new Integer(62));
	literals.put(new ANTLRHashString("order", this), new Integer(42));
	literals.put(new ANTLRHashString("where", this), new Integer(41));
	literals.put(new ANTLRHashString("exists", this), new Integer(46));
	literals.put(new ANTLRHashString("is_defined", this), new Integer(57));
}

public Token nextToken() throws TokenStreamException {
	Token theRetToken=null;
tryAgain:
	for (;;) {
		Token _token = null;
		int _ttype = Token.INVALID_TYPE;
		resetText();
		try {   // for char stream error handling
			try {   // for lexical error handling
				switch ( LA(1)) {
				case ')':
				{
					mTOK_RPAREN(true);
					theRetToken=_returnToken;
					break;
				}
				case '(':
				{
					mTOK_LPAREN(true);
					theRetToken=_returnToken;
					break;
				}
				case ',':
				{
					mTOK_COMMA(true);
					theRetToken=_returnToken;
					break;
				}
				case ';':
				{
					mTOK_SEMIC(true);
					theRetToken=_returnToken;
					break;
				}
				case ':':
				{
					mTOK_COLON(true);
					theRetToken=_returnToken;
					break;
				}
				case '|':
				{
					mTOK_CONCAT(true);
					theRetToken=_returnToken;
					break;
				}
				case '=':
				{
					mTOK_EQ(true);
					theRetToken=_returnToken;
					break;
				}
				case '+':
				{
					mTOK_PLUS(true);
					theRetToken=_returnToken;
					break;
				}
				case '*':
				{
					mTOK_STAR(true);
					theRetToken=_returnToken;
					break;
				}
				case '!':
				{
					mTOK_NE2(true);
					theRetToken=_returnToken;
					break;
				}
				case '[':
				{
					mTOK_LBRACK(true);
					theRetToken=_returnToken;
					break;
				}
				case ']':
				{
					mTOK_RBRACK(true);
					theRetToken=_returnToken;
					break;
				}
				case '\'':
				{
					mCharLiteral(true);
					theRetToken=_returnToken;
					break;
				}
				case '"':
				{
					mStringLiteral(true);
					theRetToken=_returnToken;
					break;
				}
				case '\t':  case '\r':  case ' ':
				{
					mWhiteSpace(true);
					theRetToken=_returnToken;
					break;
				}
				case '\n':
				{
					mNewLine(true);
					theRetToken=_returnToken;
					break;
				}
				default:
					if ((LA(1)=='-') && (LA(2)=='>')) {
						mTOK_INDIRECT(true);
						theRetToken=_returnToken;
					}
					else if ((LA(1)=='<') && (LA(2)=='=')) {
						mTOK_LE(true);
						theRetToken=_returnToken;
					}
					else if ((LA(1)=='>') && (LA(2)=='=')) {
						mTOK_GE(true);
						theRetToken=_returnToken;
					}
					else if ((LA(1)=='<') && (LA(2)=='>')) {
						mTOK_NE(true);
						theRetToken=_returnToken;
					}
					else if ((LA(1)=='/') && (LA(2)=='/')) {
						mCommentLine(true);
						theRetToken=_returnToken;
					}
					else if ((LA(1)=='/') && (LA(2)=='*')) {
						mMultiLineComment(true);
						theRetToken=_returnToken;
					}
					else if ((LA(1)=='.') && (true)) {
						mTOK_DOT(true);
						theRetToken=_returnToken;
					}
					else if ((LA(1)=='-') && (true)) {
						mTOK_MINUS(true);
						theRetToken=_returnToken;
					}
					else if ((LA(1)=='/') && (true)) {
						mTOK_SLASH(true);
						theRetToken=_returnToken;
					}
					else if ((LA(1)=='<') && (true)) {
						mTOK_LT(true);
						theRetToken=_returnToken;
					}
					else if ((LA(1)=='>') && (true)) {
						mTOK_GT(true);
						theRetToken=_returnToken;
					}
					else if ((LA(1)=='$') && (true)) {
						mTOK_DOLLAR(true);
						theRetToken=_returnToken;
					}
					else if ((_tokenSet_0.member(LA(1))) && (true)) {
						mIdentifier(true);
						theRetToken=_returnToken;
					}
					else if ((_tokenSet_1.member(LA(1))) && (true)) {
						mTOK_EXACT_NUMERIC_LITERAL(true);
						theRetToken=_returnToken;
					}
				else {
					if (LA(1)==EOF_CHAR) {uponEOF(); _returnToken = makeToken(Token.EOF_TYPE);}
				else {throw new NoViableAltForCharException(LA(1), getFilename(), getLine(), getColumn());}
				}
				}
				if ( _returnToken==null ) continue tryAgain; // found SKIP token
				_ttype = _returnToken.getType();
				_returnToken.setType(_ttype);
				return _returnToken;
			}
			catch (RecognitionException e) {
				throw new TokenStreamRecognitionException(e);
			}
		}
		catch (CharStreamException cse) {
			if ( cse instanceof CharStreamIOException ) {
				throw new TokenStreamIOException(((CharStreamIOException)cse).io);
			}
			else {
				throw new TokenStreamException(cse.getMessage());
			}
		}
	}
}

	public final void mTOK_RPAREN(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = TOK_RPAREN;
		int _saveIndex;
		
		match(')');
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	public final void mTOK_LPAREN(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = TOK_LPAREN;
		int _saveIndex;
		
		match('(');
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	public final void mTOK_COMMA(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = TOK_COMMA;
		int _saveIndex;
		
		match(',');
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	public final void mTOK_SEMIC(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = TOK_SEMIC;
		int _saveIndex;
		
		match(';');
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	public final void mTOK_COLON(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = TOK_COLON;
		int _saveIndex;
		
		match(':');
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	public final void mTOK_DOT(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = TOK_DOT;
		int _saveIndex;
		
		match('.');
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	public final void mTOK_INDIRECT(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = TOK_INDIRECT;
		int _saveIndex;
		
		match('-');
		match('>');
		_ttype = TOK_DOT;
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	public final void mTOK_CONCAT(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = TOK_CONCAT;
		int _saveIndex;
		
		match('|');
		match('|');
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	public final void mTOK_EQ(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = TOK_EQ;
		int _saveIndex;
		
		match('=');
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	public final void mTOK_PLUS(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = TOK_PLUS;
		int _saveIndex;
		
		match('+');
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	public final void mTOK_MINUS(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = TOK_MINUS;
		int _saveIndex;
		
		match('-');
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	public final void mTOK_SLASH(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = TOK_SLASH;
		int _saveIndex;
		
		match('/');
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	public final void mTOK_STAR(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = TOK_STAR;
		int _saveIndex;
		
		match('*');
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	public final void mTOK_LE(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = TOK_LE;
		int _saveIndex;
		
		match('<');
		match('=');
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	public final void mTOK_GE(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = TOK_GE;
		int _saveIndex;
		
		match('>');
		match('=');
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	public final void mTOK_NE(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = TOK_NE;
		int _saveIndex;
		
		match('<');
		match('>');
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	public final void mTOK_NE2(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = TOK_NE2;
		int _saveIndex;
		
		match('!');
		match('=');
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	public final void mTOK_LT(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = TOK_LT;
		int _saveIndex;
		
		match('<');
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	public final void mTOK_GT(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = TOK_GT;
		int _saveIndex;
		
		match('>');
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	public final void mTOK_LBRACK(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = TOK_LBRACK;
		int _saveIndex;
		
		match('[');
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	public final void mTOK_RBRACK(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = TOK_RBRACK;
		int _saveIndex;
		
		match(']');
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	public final void mTOK_DOLLAR(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = TOK_DOLLAR;
		int _saveIndex;
		
		match('$');
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	protected final void mNameFirstCharacter(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = NameFirstCharacter;
		int _saveIndex;
		
		{
		switch ( LA(1)) {
		case 'A':  case 'B':  case 'C':  case 'D':
		case 'E':  case 'F':  case 'G':  case 'H':
		case 'I':  case 'J':  case 'K':  case 'L':
		case 'M':  case 'N':  case 'O':  case 'P':
		case 'Q':  case 'R':  case 'S':  case 'T':
		case 'U':  case 'V':  case 'W':  case 'X':
		case 'Y':  case 'Z':
		{
			matchRange('A','Z');
			break;
		}
		case 'a':  case 'b':  case 'c':  case 'd':
		case 'e':  case 'f':  case 'g':  case 'h':
		case 'i':  case 'j':  case 'k':  case 'l':
		case 'm':  case 'n':  case 'o':  case 'p':
		case 'q':  case 'r':  case 's':  case 't':
		case 'u':  case 'v':  case 'w':  case 'x':
		case 'y':  case 'z':
		{
			matchRange('a','z');
			break;
		}
		case '_':
		{
			match('_');
			break;
		}
		case '.':
		{
			mTOK_DOT(false);
			break;
		}
		case '-':
		{
			mTOK_INDIRECT(false);
			break;
		}
		case '$':
		{
			mTOK_DOLLAR(false);
			break;
		}
		default:
		{
			throw new NoViableAltForCharException(LA(1), getFilename(), getLine(), getColumn());
		}
		}
		}
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	protected final void mNameCharacter(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = NameCharacter;
		int _saveIndex;
		
		{
		switch ( LA(1)) {
		case '$':  case '-':  case '.':  case 'A':
		case 'B':  case 'C':  case 'D':  case 'E':
		case 'F':  case 'G':  case 'H':  case 'I':
		case 'J':  case 'K':  case 'L':  case 'M':
		case 'N':  case 'O':  case 'P':  case 'Q':
		case 'R':  case 'S':  case 'T':  case 'U':
		case 'V':  case 'W':  case 'X':  case 'Y':
		case 'Z':  case '_':  case 'a':  case 'b':
		case 'c':  case 'd':  case 'e':  case 'f':
		case 'g':  case 'h':  case 'i':  case 'j':
		case 'k':  case 'l':  case 'm':  case 'n':
		case 'o':  case 'p':  case 'q':  case 'r':
		case 's':  case 't':  case 'u':  case 'v':
		case 'w':  case 'x':  case 'y':  case 'z':
		{
			mNameFirstCharacter(false);
			break;
		}
		case '0':  case '1':  case '2':  case '3':
		case '4':  case '5':  case '6':  case '7':
		case '8':  case '9':
		{
			matchRange('0','9');
			break;
		}
		default:
		{
			throw new NoViableAltForCharException(LA(1), getFilename(), getLine(), getColumn());
		}
		}
		}
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	public final void mIdentifier(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = Identifier;
		int _saveIndex;
		
		mNameFirstCharacter(false);
		{
		_loop29:
		do {
			if ((_tokenSet_2.member(LA(1)))) {
				mNameCharacter(false);
			}
			else {
				break _loop29;
			}
			
		} while (true);
		}
		_ttype = testLiteralsTable(_ttype);
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	protected final void mTOK_UNSIGNED_INTEGER(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = TOK_UNSIGNED_INTEGER;
		int _saveIndex;
		
		matchRange('0','9');
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	protected final void mTOK_APPROXIMATE_NUMERIC_LITERAL(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = TOK_APPROXIMATE_NUMERIC_LITERAL;
		int _saveIndex;
		
		match('e');
		{
		switch ( LA(1)) {
		case '+':
		{
			match('+');
			break;
		}
		case '-':
		{
			match('-');
			break;
		}
		case '0':  case '1':  case '2':  case '3':
		case '4':  case '5':  case '6':  case '7':
		case '8':  case '9':
		{
			break;
		}
		default:
		{
			throw new NoViableAltForCharException(LA(1), getFilename(), getLine(), getColumn());
		}
		}
		}
		{
		int _cnt34=0;
		_loop34:
		do {
			if (((LA(1) >= '0' && LA(1) <= '9'))) {
				matchRange('0','9');
			}
			else {
				if ( _cnt34>=1 ) { break _loop34; } else {throw new NoViableAltForCharException(LA(1), getFilename(), getLine(), getColumn());}
			}
			
			_cnt34++;
		} while (true);
		}
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	public final void mTOK_EXACT_NUMERIC_LITERAL(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = TOK_EXACT_NUMERIC_LITERAL;
		int _saveIndex;
		
		switch ( LA(1)) {
		case '.':
		{
			match('.');
			{
			int _cnt37=0;
			_loop37:
			do {
				if (((LA(1) >= '0' && LA(1) <= '9'))) {
					mTOK_UNSIGNED_INTEGER(false);
				}
				else {
					if ( _cnt37>=1 ) { break _loop37; } else {throw new NoViableAltForCharException(LA(1), getFilename(), getLine(), getColumn());}
				}
				
				_cnt37++;
			} while (true);
			}
			_ttype = TOK_EXACT_NUMERIC_LITERAL;
			{
			if ((LA(1)=='e')) {
				mTOK_APPROXIMATE_NUMERIC_LITERAL(false);
				_ttype = TOK_APPROXIMATE_NUMERIC_LITERAL;
			}
			else {
			}
			
			}
			break;
		}
		case '0':  case '1':  case '2':  case '3':
		case '4':  case '5':  case '6':  case '7':
		case '8':  case '9':
		{
			{
			int _cnt40=0;
			_loop40:
			do {
				if (((LA(1) >= '0' && LA(1) <= '9'))) {
					mTOK_UNSIGNED_INTEGER(false);
				}
				else {
					if ( _cnt40>=1 ) { break _loop40; } else {throw new NoViableAltForCharException(LA(1), getFilename(), getLine(), getColumn());}
				}
				
				_cnt40++;
			} while (true);
			}
			_ttype = TOK_UNSIGNED_INTEGER;
			{
			switch ( LA(1)) {
			case '.':
			{
				match('.');
				{
				_loop43:
				do {
					if (((LA(1) >= '0' && LA(1) <= '9'))) {
						mTOK_UNSIGNED_INTEGER(false);
					}
					else {
						break _loop43;
					}
					
				} while (true);
				}
				_ttype = TOK_EXACT_NUMERIC_LITERAL;
				{
				if ((LA(1)=='e')) {
					mTOK_APPROXIMATE_NUMERIC_LITERAL(false);
					_ttype = TOK_APPROXIMATE_NUMERIC_LITERAL;
				}
				else {
				}
				
				}
				break;
			}
			case 'e':
			{
				mTOK_APPROXIMATE_NUMERIC_LITERAL(false);
				_ttype = TOK_APPROXIMATE_NUMERIC_LITERAL;
				break;
			}
			default:
				{
				}
			}
			}
			break;
		}
		default:
		{
			throw new NoViableAltForCharException(LA(1), getFilename(), getLine(), getColumn());
		}
		}
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	public final void mCharLiteral(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = CharLiteral;
		int _saveIndex;
		
		_saveIndex=text.length();
		match('\'');
		text.setLength(_saveIndex);
		{
		_loop48:
		do {
			if ((LA(1)=='\'') && (LA(2)=='\'')) {
				match('\'');
				match('\'');
				text.setLength(_begin); text.append("'");
			}
			else if ((LA(1)=='\n')) {
				match('\n');
				newline();
			}
			else if ((_tokenSet_3.member(LA(1)))) {
				{
				match(_tokenSet_3);
				}
			}
			else {
				break _loop48;
			}
			
		} while (true);
		}
		_saveIndex=text.length();
		match('\'');
		text.setLength(_saveIndex);
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	public final void mStringLiteral(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = StringLiteral;
		int _saveIndex;
		
		_saveIndex=text.length();
		match('"');
		text.setLength(_saveIndex);
		{
		_loop52:
		do {
			if ((LA(1)=='\\') && (LA(2)=='"')) {
				match('\\');
				match('"');
				text.setLength(_begin); text.append("\"");
			}
			else if ((_tokenSet_4.member(LA(1))) && ((LA(2) >= '\u0003' && LA(2) <= '\ufffe'))) {
				{
				match(_tokenSet_4);
				}
			}
			else if ((LA(1)=='\n')) {
				match('\n');
				newline();
			}
			else {
				break _loop52;
			}
			
		} while (true);
		}
		_saveIndex=text.length();
		match('"');
		text.setLength(_saveIndex);
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	public final void mWhiteSpace(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = WhiteSpace;
		int _saveIndex;
		
		{
		switch ( LA(1)) {
		case ' ':
		{
			match(' ');
			break;
		}
		case '\t':
		{
			match('\t');
			break;
		}
		case '\r':
		{
			match('\r');
			break;
		}
		default:
		{
			throw new NoViableAltForCharException(LA(1), getFilename(), getLine(), getColumn());
		}
		}
		}
		_ttype = Token.SKIP;
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	public final void mNewLine(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = NewLine;
		int _saveIndex;
		
		match('\n');
		newline(); _ttype = Token.SKIP;
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	public final void mCommentLine(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = CommentLine;
		int _saveIndex;
		
		_saveIndex=text.length();
		match('/');
		text.setLength(_saveIndex);
		_saveIndex=text.length();
		match('/');
		text.setLength(_saveIndex);
		{
		_loop58:
		do {
			if ((_tokenSet_5.member(LA(1)))) {
				_saveIndex=text.length();
				matchNot('\n');
				text.setLength(_saveIndex);
			}
			else {
				break _loop58;
			}
			
		} while (true);
		}
		_saveIndex=text.length();
		match('\n');
		text.setLength(_saveIndex);
		newline(); _ttype = Token.SKIP;
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	public final void mMultiLineComment(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = MultiLineComment;
		int _saveIndex;
		
		match("/*");
		{
		_loop62:
		do {
			if (((LA(1)=='*') && ((LA(2) >= '\u0003' && LA(2) <= '\ufffe')))&&( LA(2)!='/' )) {
				match('*');
			}
			else if ((LA(1)=='\n')) {
				match('\n');
				newline();
			}
			else if ((_tokenSet_6.member(LA(1)))) {
				{
				match(_tokenSet_6);
				}
			}
			else {
				break _loop62;
			}
			
		} while (true);
		}
		match("*/");
		_ttype = Token.SKIP;
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	
	private static final long[] mk_tokenSet_0() {
		long[] data = new long[1025];
		data[0]=105621835743232L;
		data[1]=576460745995190270L;
		return data;
	}
	public static final BitSet _tokenSet_0 = new BitSet(mk_tokenSet_0());
	private static final long[] mk_tokenSet_1() {
		long[] data = new long[1025];
		data[0]=288019269919178752L;
		return data;
	}
	public static final BitSet _tokenSet_1 = new BitSet(mk_tokenSet_1());
	private static final long[] mk_tokenSet_2() {
		long[] data = new long[1025];
		data[0]=288054523010744320L;
		data[1]=576460745995190270L;
		return data;
	}
	public static final BitSet _tokenSet_2 = new BitSet(mk_tokenSet_2());
	private static final long[] mk_tokenSet_3() {
		long[] data = new long[2048];
		data[0]=-549755814920L;
		for (int i = 1; i<=1022; i++) { data[i]=-1L; }
		data[1023]=9223372036854775807L;
		return data;
	}
	public static final BitSet _tokenSet_3 = new BitSet(mk_tokenSet_3());
	private static final long[] mk_tokenSet_4() {
		long[] data = new long[2048];
		data[0]=-17179870216L;
		for (int i = 1; i<=1022; i++) { data[i]=-1L; }
		data[1023]=9223372036854775807L;
		return data;
	}
	public static final BitSet _tokenSet_4 = new BitSet(mk_tokenSet_4());
	private static final long[] mk_tokenSet_5() {
		long[] data = new long[2048];
		data[0]=-1032L;
		for (int i = 1; i<=1022; i++) { data[i]=-1L; }
		data[1023]=9223372036854775807L;
		return data;
	}
	public static final BitSet _tokenSet_5 = new BitSet(mk_tokenSet_5());
	private static final long[] mk_tokenSet_6() {
		long[] data = new long[2048];
		data[0]=-4398046512136L;
		for (int i = 1; i<=1022; i++) { data[i]=-1L; }
		data[1023]=9223372036854775807L;
		return data;
	}
	public static final BitSet _tokenSet_6 = new BitSet(mk_tokenSet_6());
	
	}
