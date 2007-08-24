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


import java.util.ArrayList;
import java.util.Collection;
import java.util.Vector;

import org.apache.ojb.broker.metadata.ClassDescriptor;
import org.apache.ojb.broker.metadata.MetadataManager;
import org.apache.ojb.broker.metadata.ObjectReferenceDescriptor;
import org.apache.ojb.broker.query.Criteria;
import org.apache.ojb.broker.query.Query;
import org.apache.ojb.broker.query.QueryByCriteria;
import org.apache.ojb.broker.query.QueryFactory;
import org.apache.ojb.broker.util.ClassHelper;

import antlr.NoViableAltException;
import antlr.ParserSharedInputState;
import antlr.RecognitionException;
import antlr.Token;
import antlr.TokenBuffer;
import antlr.TokenStream;
import antlr.TokenStreamException;
import antlr.collections.impl.BitSet;

public class OQLParser extends antlr.LLkParser       implements OQLLexerTokenTypes
 {

protected OQLParser(TokenBuffer tokenBuf, int k) {
  super(tokenBuf,k);
  tokenNames = _tokenNames;
}

public OQLParser(TokenBuffer tokenBuf) {
  this(tokenBuf,3);
}

protected OQLParser(TokenStream lexer, int k) {
  super(lexer,k);
  tokenNames = _tokenNames;
}

public OQLParser(TokenStream lexer) {
  this(lexer,3);
}

public OQLParser(ParserSharedInputState state) {
  super(state,3);
  tokenNames = _tokenNames;
}

	public final Query  buildQuery() throws RecognitionException, TokenStreamException {
		Query query = null;
		
		
		try {      // for error handling
			query=selectQuery();
			{
			if ((LA(1)==TOK_SEMIC)) {
				match(TOK_SEMIC);
			}
			else if ((LA(1)==EOF)) {
			}
			else {
				throw new NoViableAltException(LT(1), getFilename());
			}
			
			}
		}
		catch (RecognitionException ex) {
			reportError(ex);
			recover(ex,_tokenSet_0);
		}
		return query;
	}
	
	public final QueryByCriteria  selectQuery() throws RecognitionException, TokenStreamException {
		QueryByCriteria query = null;
		
		
		try {      // for error handling
			
			Class clazz = null;
			Criteria criteria = new Criteria();
			String[] projectionAttrs;
			boolean distinct = false;
			
			match(LITERAL_select);
			{
			if ((LA(1)==LITERAL_distinct)) {
				match(LITERAL_distinct);
				
				distinct = true;
				
			}
			else if ((LA(1)==TOK_STAR||LA(1)==Identifier)) {
			}
			else {
				throw new NoViableAltException(LT(1), getFilename());
			}
			
			}
			projectionAttrs=projectionAttributes();
			match(LITERAL_from);
			clazz=fromClause();
			{
			if ((LA(1)==LITERAL_where)) {
				match(LITERAL_where);
				whereClause(criteria);
			}
			else if ((_tokenSet_1.member(LA(1)))) {
			}
			else {
				throw new NoViableAltException(LT(1), getFilename());
			}
			
			}
			
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
			
			{
			if ((LA(1)==LITERAL_order)) {
				match(LITERAL_order);
				match(LITERAL_by);
				orderClause(query);
			}
			else if ((_tokenSet_2.member(LA(1)))) {
			}
			else {
				throw new NoViableAltException(LT(1), getFilename());
			}
			
			}
			{
			if ((LA(1)==LITERAL_group)) {
				match(LITERAL_group);
				match(LITERAL_by);
				groupClause(query);
			}
			else if ((_tokenSet_3.member(LA(1)))) {
			}
			else {
				throw new NoViableAltException(LT(1), getFilename());
			}
			
			}
			{
			if ((LA(1)==LITERAL_prefetch)) {
				match(LITERAL_prefetch);
				prefetchClause(query);
			}
			else if ((LA(1)==EOF||LA(1)==TOK_RPAREN||LA(1)==TOK_SEMIC)) {
			}
			else {
				throw new NoViableAltException(LT(1), getFilename());
			}
			
			}
		}
		catch (RecognitionException ex) {
			reportError(ex);
			recover(ex,_tokenSet_4);
		}
		return query;
	}
	
	public final String[]  projectionAttributes() throws RecognitionException, TokenStreamException {
		String[] projectionAttrs = null;
		
		Token  id = null;
		Token  id1 = null;
		
		try {      // for error handling
			
			String first = null;
			ArrayList list = null;
			
			{
			if ((LA(1)==Identifier)) {
				id = LT(1);
				match(Identifier);
				{
				
				first = id.getText();
				
				}
				{
				_loop80:
				do {
					if ((LA(1)==TOK_COMMA)) {
						match(TOK_COMMA);
						id1 = LT(1);
						match(Identifier);
						{
						
						if (list == null)
						{
						list = new ArrayList();
						list.add(first);
						}
						list.add(id1.getText());
						
						}
					}
					else {
						break _loop80;
					}
					
				} while (true);
				}
			}
			else if ((LA(1)==TOK_STAR)) {
				match(TOK_STAR);
			}
			else {
				throw new NoViableAltException(LT(1), getFilename());
			}
			
			}
			
			if (list == null)
			{
			projectionAttrs = new String[] {first};
			}
			else
			{
			projectionAttrs = (String[]) list.toArray(new String[list.size()]);
			}
			
			
		}
		catch (RecognitionException ex) {
			reportError(ex);
			recover(ex,_tokenSet_5);
		}
		return projectionAttrs;
	}
	
	public final Class  fromClause() throws RecognitionException, TokenStreamException {
		Class clazz = null;
		
		Token  id = null;
		
		try {      // for error handling
			id = LT(1);
			match(Identifier);
			
			try {
			clazz = ClassHelper.getClass(id.getText());
			} catch (Exception e) {
			}
			
		}
		catch (RecognitionException ex) {
			reportError(ex);
			recover(ex,_tokenSet_6);
		}
		return clazz;
	}
	
	public final void whereClause(
		Criteria criteria
	) throws RecognitionException, TokenStreamException {
		
		
		try {      // for error handling
			orExpr(criteria);
		}
		catch (RecognitionException ex) {
			reportError(ex);
			recover(ex,_tokenSet_7);
		}
	}
	
	public final void orderClause(
		QueryByCriteria query
	) throws RecognitionException, TokenStreamException {
		
		
		try {      // for error handling
			sortCriterion(query);
			{
			_loop83:
			do {
				if ((LA(1)==TOK_COMMA)) {
					match(TOK_COMMA);
					sortCriterion(query);
				}
				else {
					break _loop83;
				}
				
			} while (true);
			}
		}
		catch (RecognitionException ex) {
			reportError(ex);
			recover(ex,_tokenSet_2);
		}
	}
	
	public final void groupClause(
		QueryByCriteria query
	) throws RecognitionException, TokenStreamException {
		
		
		try {      // for error handling
			groupCriterion(query);
			{
			_loop89:
			do {
				if ((LA(1)==TOK_COMMA)) {
					match(TOK_COMMA);
					groupCriterion(query);
				}
				else {
					break _loop89;
				}
				
			} while (true);
			}
		}
		catch (RecognitionException ex) {
			reportError(ex);
			recover(ex,_tokenSet_3);
		}
	}
	
	public final void prefetchClause(
		QueryByCriteria query
	) throws RecognitionException, TokenStreamException {
		
		
		try {      // for error handling
			prefetchCriterion(query);
			{
			_loop93:
			do {
				if ((LA(1)==TOK_COMMA)) {
					match(TOK_COMMA);
					prefetchCriterion(query);
				}
				else {
					break _loop93;
				}
				
			} while (true);
			}
		}
		catch (RecognitionException ex) {
			reportError(ex);
			recover(ex,_tokenSet_4);
		}
	}
	
	public final Query  existsQuery() throws RecognitionException, TokenStreamException {
		Query query = null;
		
		
		try {      // for error handling
			
			Class clazz = null;
			Criteria criteria = new Criteria();
			
			match(LITERAL_exists);
			projectionAttributes();
			match(LITERAL_in);
			clazz=fromClause();
			{
			if ((LA(1)==TOK_COLON)) {
				match(TOK_COLON);
				whereClause(criteria);
			}
			else if ((_tokenSet_7.member(LA(1)))) {
			}
			else {
				throw new NoViableAltException(LT(1), getFilename());
			}
			
			}
			
			if (clazz != null) {
			query = QueryFactory.newQuery(clazz, criteria);
			}
					
		}
		catch (RecognitionException ex) {
			reportError(ex);
			recover(ex,_tokenSet_7);
		}
		return query;
	}
	
	public final void orExpr(
		Criteria criteria
	) throws RecognitionException, TokenStreamException {
		
		
		try {      // for error handling
			andExpr(criteria);
			{
			_loop97:
			do {
				if ((LA(1)==LITERAL_or) && (_tokenSet_8.member(LA(2))) && (_tokenSet_9.member(LA(3)))) {
					match(LITERAL_or);
					Criteria orCriteria = new Criteria();
					andExpr(orCriteria);
					criteria.addOrCriteria(orCriteria);
				}
				else {
					break _loop97;
				}
				
			} while (true);
			}
		}
		catch (RecognitionException ex) {
			reportError(ex);
			recover(ex,_tokenSet_7);
		}
	}
	
	public final void sortCriterion(
		QueryByCriteria query
	) throws RecognitionException, TokenStreamException {
		
		Token  id = null;
		
		try {      // for error handling
			boolean descending = false;
			id = LT(1);
			match(Identifier);
			{
			if ((LA(1)==LITERAL_asc||LA(1)==LITERAL_desc)) {
				{
				if ((LA(1)==LITERAL_asc)) {
					match(LITERAL_asc);
					descending = false;
				}
				else if ((LA(1)==LITERAL_desc)) {
					match(LITERAL_desc);
					descending = true;
				}
				else {
					throw new NoViableAltException(LT(1), getFilename());
				}
				
				}
			}
			else if ((_tokenSet_10.member(LA(1)))) {
			}
			else {
				throw new NoViableAltException(LT(1), getFilename());
			}
			
			}
			
			if (descending) {
			query.addOrderByDescending(id.getText());
			} else {
			query.addOrderByAscending(id.getText());
			}
			
		}
		catch (RecognitionException ex) {
			reportError(ex);
			recover(ex,_tokenSet_10);
		}
	}
	
	public final void groupCriterion(
		QueryByCriteria query
	) throws RecognitionException, TokenStreamException {
		
		Token  id = null;
		
		try {      // for error handling
			id = LT(1);
			match(Identifier);
			
			query.addGroupBy(id.getText());
			
		}
		catch (RecognitionException ex) {
			reportError(ex);
			recover(ex,_tokenSet_11);
		}
	}
	
	public final void prefetchCriterion(
		QueryByCriteria query
	) throws RecognitionException, TokenStreamException {
		
		Token  id = null;
		
		try {      // for error handling
			id = LT(1);
			match(Identifier);
			
			query.addPrefetchedRelationship(id.getText());
			
		}
		catch (RecognitionException ex) {
			reportError(ex);
			recover(ex,_tokenSet_12);
		}
	}
	
	public final void andExpr(
		Criteria criteria
	) throws RecognitionException, TokenStreamException {
		
		
		try {      // for error handling
			quantifierExpr(criteria);
			{
			_loop100:
			do {
				if ((LA(1)==LITERAL_and) && (_tokenSet_8.member(LA(2))) && (_tokenSet_9.member(LA(3)))) {
					match(LITERAL_and);
					Criteria andCriteria = new Criteria();
					quantifierExpr(andCriteria);
					criteria.addAndCriteria(andCriteria);
				}
				else {
					break _loop100;
				}
				
			} while (true);
			}
		}
		catch (RecognitionException ex) {
			reportError(ex);
			recover(ex,_tokenSet_7);
		}
	}
	
	public final void quantifierExpr(
		Criteria criteria
	) throws RecognitionException, TokenStreamException {
		
		
		try {      // for error handling
			switch ( LA(1)) {
			case TOK_LPAREN:
			{
				match(TOK_LPAREN);
				orExpr(criteria);
				match(TOK_RPAREN);
				break;
			}
			case LITERAL_is_undefined:
			case LITERAL_is_defined:
			{
				undefinedExpr(criteria);
				break;
			}
			case LITERAL_exists:
			case LITERAL_not:
			{
				existsExpr(criteria);
				break;
			}
			default:
				if ((LA(1)==Identifier) && (_tokenSet_13.member(LA(2)))) {
					equalityExpr(criteria);
				}
				else if ((LA(1)==Identifier) && (LA(2)==LITERAL_not||LA(2)==LITERAL_like) && (_tokenSet_14.member(LA(3)))) {
					likeExpr(criteria);
				}
				else if ((LA(1)==Identifier) && (LA(2)==LITERAL_not||LA(2)==LITERAL_between) && (_tokenSet_15.member(LA(3)))) {
					betweenExpr(criteria);
				}
				else if ((LA(1)==Identifier) && (LA(2)==LITERAL_in||LA(2)==LITERAL_not) && (LA(3)==TOK_LPAREN||LA(3)==LITERAL_in||LA(3)==LITERAL_list)) {
					inExpr(criteria);
				}
			else {
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
		}
		catch (RecognitionException ex) {
			reportError(ex);
			recover(ex,_tokenSet_7);
		}
	}
	
	public final void equalityExpr(
		Criteria criteria
	) throws RecognitionException, TokenStreamException {
		
		Token  id = null;
		
		try {      // for error handling
			
				Object value = null;
			
			id = LT(1);
			match(Identifier);
			{
			{
			switch ( LA(1)) {
			case TOK_EQ:
			{
				match(TOK_EQ);
				{
				if ((LA(1)==LITERAL_nil)) {
					match(LITERAL_nil);
					criteria.addIsNull(id.getText());
				}
				else if ((_tokenSet_16.member(LA(1)))) {
					value=literal();
					criteria.addEqualTo(id.getText(), value);
				}
				else {
					throw new NoViableAltException(LT(1), getFilename());
				}
				
				}
				break;
			}
			case TOK_NE:
			{
				match(TOK_NE);
				{
				if ((LA(1)==LITERAL_nil)) {
					match(LITERAL_nil);
					criteria.addNotNull(id.getText());
				}
				else if ((_tokenSet_16.member(LA(1)))) {
					value=literal();
					criteria.addNotEqualTo(id.getText(), value);
				}
				else {
					throw new NoViableAltException(LT(1), getFilename());
				}
				
				}
				break;
			}
			case TOK_NE2:
			{
				match(TOK_NE2);
				{
				if ((LA(1)==LITERAL_nil)) {
					match(LITERAL_nil);
					criteria.addNotNull(id.getText());
				}
				else if ((_tokenSet_16.member(LA(1)))) {
					value=literal();
					criteria.addNotEqualTo(id.getText(), value);
				}
				else {
					throw new NoViableAltException(LT(1), getFilename());
				}
				
				}
				break;
			}
			case TOK_LT:
			{
				match(TOK_LT);
				value=literal();
				criteria.addLessThan(id.getText(), value);
				break;
			}
			case TOK_GT:
			{
				match(TOK_GT);
				value=literal();
				criteria.addGreaterThan(id.getText(), value);
				break;
			}
			case TOK_LE:
			{
				match(TOK_LE);
				value=literal();
				criteria.addLessOrEqualThan(id.getText(), value);
				break;
			}
			case TOK_GE:
			{
				match(TOK_GE);
				value=literal();
				criteria.addGreaterOrEqualThan(id.getText(), value);
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
			}
			}
		}
		catch (RecognitionException ex) {
			reportError(ex);
			recover(ex,_tokenSet_7);
		}
	}
	
	public final void likeExpr(
		Criteria criteria
	) throws RecognitionException, TokenStreamException {
		
		Token  id = null;
		
		try {      // for error handling
			
			boolean negative = false;
			Object value = null;
			
			id = LT(1);
			match(Identifier);
			{
			if ((LA(1)==LITERAL_not)) {
				match(LITERAL_not);
				negative = true;
			}
			else if ((LA(1)==LITERAL_like)) {
			}
			else {
				throw new NoViableAltException(LT(1), getFilename());
			}
			
			}
			match(LITERAL_like);
			value=literal();
			
			if (negative) {
			criteria.addNotLike(id.getText(), value);
			} else {
			criteria.addLike(id.getText(), value);
			}
			
		}
		catch (RecognitionException ex) {
			reportError(ex);
			recover(ex,_tokenSet_7);
		}
	}
	
	public final void undefinedExpr(
		Criteria criteria
	) throws RecognitionException, TokenStreamException {
		
		Token  id = null;
		
		try {      // for error handling
			boolean negative = false;
			{
			if ((LA(1)==LITERAL_is_undefined)) {
				match(LITERAL_is_undefined);
				negative = false;
			}
			else if ((LA(1)==LITERAL_is_defined)) {
				match(LITERAL_is_defined);
				negative = true;
			}
			else {
				throw new NoViableAltException(LT(1), getFilename());
			}
			
			}
			match(TOK_LPAREN);
			id = LT(1);
			match(Identifier);
			match(TOK_RPAREN);
			
			if (negative) {
			criteria.addNotNull(id.getText());
			} else {
			criteria.addIsNull(id.getText());
			}
			
		}
		catch (RecognitionException ex) {
			reportError(ex);
			recover(ex,_tokenSet_7);
		}
	}
	
	public final void betweenExpr(
		Criteria criteria
	) throws RecognitionException, TokenStreamException {
		
		Token  id = null;
		
		try {      // for error handling
			
			boolean negative = false;
			Object lower = null;
			Object upper = null;
			
			id = LT(1);
			match(Identifier);
			{
			if ((LA(1)==LITERAL_not)) {
				match(LITERAL_not);
				negative = true;
			}
			else if ((LA(1)==LITERAL_between)) {
			}
			else {
				throw new NoViableAltException(LT(1), getFilename());
			}
			
			}
			match(LITERAL_between);
			lower=literal();
			match(LITERAL_and);
			upper=literal();
			
			if (negative) {
			criteria.addNotBetween(id.getText(), lower, upper);
			} else {
			criteria.addBetween(id.getText(), lower, upper);
			}
			
		}
		catch (RecognitionException ex) {
			reportError(ex);
			recover(ex,_tokenSet_7);
		}
	}
	
	public final void inExpr(
		Criteria criteria
	) throws RecognitionException, TokenStreamException {
		
		Token  id = null;
		
		try {      // for error handling
			
				 boolean negative = false;
				 Collection coll;
			
			id = LT(1);
			match(Identifier);
			{
			if ((LA(1)==LITERAL_not)) {
				match(LITERAL_not);
				negative = true;
			}
			else if ((LA(1)==LITERAL_in)) {
			}
			else {
				throw new NoViableAltException(LT(1), getFilename());
			}
			
			}
			match(LITERAL_in);
			{
			if ((LA(1)==LITERAL_list)) {
				match(LITERAL_list);
			}
			else if ((LA(1)==TOK_LPAREN)) {
			}
			else {
				throw new NoViableAltException(LT(1), getFilename());
			}
			
			}
			coll=argList();
			
			if (negative) {
			criteria.addNotIn(id.getText(), coll);
			} else {
			criteria.addIn(id.getText(), coll);
			}
			
		}
		catch (RecognitionException ex) {
			reportError(ex);
			recover(ex,_tokenSet_7);
		}
	}
	
	public final void existsExpr(
		Criteria criteria
	) throws RecognitionException, TokenStreamException {
		
		
		try {      // for error handling
			
			Query subQuery = null;
			boolean negative = false;
			
			{
			if ((LA(1)==LITERAL_not)) {
				match(LITERAL_not);
				negative = true;
			}
			else if ((LA(1)==LITERAL_exists)) {
			}
			else {
				throw new NoViableAltException(LT(1), getFilename());
			}
			
			}
			{
			if ((LA(1)==LITERAL_exists) && (LA(2)==TOK_LPAREN)) {
				match(LITERAL_exists);
				match(TOK_LPAREN);
				subQuery=selectQuery();
				match(TOK_RPAREN);
			}
			else if ((LA(1)==LITERAL_exists) && (LA(2)==TOK_STAR||LA(2)==Identifier)) {
				subQuery=existsQuery();
			}
			else {
				throw new NoViableAltException(LT(1), getFilename());
			}
			
			}
			
			if (negative) {
			criteria.addNotExists(subQuery);
			} else {
			criteria.addExists(subQuery);
			}
			
		}
		catch (RecognitionException ex) {
			reportError(ex);
			recover(ex,_tokenSet_7);
		}
	}
	
	public final Object  literal() throws RecognitionException, TokenStreamException {
		Object value = null;
		
		Token  tokInt = null;
		Token  tokADbl = null;
		Token  tokEDbl = null;
		Token  tokChar = null;
		Token  tokStr = null;
		Token  tokDate = null;
		Token  tokTime = null;
		Token  tokTs = null;
		
		try {      // for error handling
			switch ( LA(1)) {
			case TOK_DOLLAR:
			{
				match(TOK_DOLLAR);
				match(TOK_UNSIGNED_INTEGER);
				value = null;
				break;
			}
			case LITERAL_true:
			{
				match(LITERAL_true);
				value = Boolean.TRUE;
				break;
			}
			case LITERAL_false:
			{
				match(LITERAL_false);
				value = Boolean.FALSE;
				break;
			}
			case TOK_UNSIGNED_INTEGER:
			{
				tokInt = LT(1);
				match(TOK_UNSIGNED_INTEGER);
				
					try
					{
						value = Integer.valueOf(tokInt.getText());
					}
					catch (NumberFormatException ignored)
					{
						value = Long.valueOf(tokInt.getText());
					}
				
				break;
			}
			case TOK_APPROXIMATE_NUMERIC_LITERAL:
			{
				tokADbl = LT(1);
				match(TOK_APPROXIMATE_NUMERIC_LITERAL);
				value = Double.valueOf(tokADbl.getText());
				break;
			}
			case TOK_EXACT_NUMERIC_LITERAL:
			{
				tokEDbl = LT(1);
				match(TOK_EXACT_NUMERIC_LITERAL);
				value = Double.valueOf(tokEDbl.getText());
				break;
			}
			case CharLiteral:
			{
				tokChar = LT(1);
				match(CharLiteral);
				value = new Character(tokChar.getText().charAt(0));
				break;
			}
			case StringLiteral:
			{
				tokStr = LT(1);
				match(StringLiteral);
				value = tokStr.getText();
				break;
			}
			case LITERAL_date:
			{
				match(LITERAL_date);
				tokDate = LT(1);
				match(StringLiteral);
				value = java.sql.Date.valueOf(tokDate.getText());
				break;
			}
			case LITERAL_time:
			{
				match(LITERAL_time);
				tokTime = LT(1);
				match(StringLiteral);
				value = java.sql.Time.valueOf(tokTime.getText());
				break;
			}
			case LITERAL_timestamp:
			{
				match(LITERAL_timestamp);
				tokTs = LT(1);
				match(StringLiteral);
				value = java.sql.Timestamp.valueOf(tokTs.getText());
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
		}
		catch (RecognitionException ex) {
			reportError(ex);
			recover(ex,_tokenSet_17);
		}
		return value;
	}
	
	public final Collection  argList() throws RecognitionException, TokenStreamException {
		Collection coll = null;
		
		
		try {      // for error handling
			
					Collection temp = new Vector();
				    Object val;
				
			match(TOK_LPAREN);
			{
			if ((_tokenSet_16.member(LA(1)))) {
				val=literal();
				if (val != null) {temp.add(val);}
				{
				_loop124:
				do {
					if ((LA(1)==TOK_COMMA)) {
						match(TOK_COMMA);
						val=literal();
						if (val != null) {temp.add(val);}
					}
					else {
						break _loop124;
					}
					
				} while (true);
				}
			}
			else if ((LA(1)==TOK_RPAREN)) {
			}
			else {
				throw new NoViableAltException(LT(1), getFilename());
			}
			
			}
			match(TOK_RPAREN);
			
					if (!temp.isEmpty()) {coll = temp;}
					
		}
		catch (RecognitionException ex) {
			reportError(ex);
			recover(ex,_tokenSet_7);
		}
		return coll;
	}
	
	
	public static final String[] _tokenNames = {
		"<0>",
		"EOF",
		"<2>",
		"NULL_TREE_LOOKAHEAD",
		"right parenthesis",
		"left parenthesis",
		"comma",
		"semicolon",
		"TOK_COLON",
		"dot",
		"dot",
		"operator",
		"comparison operator",
		"operator",
		"operator",
		"operator",
		"operator",
		"comparison operator",
		"comparison operator",
		"comparison operator",
		"comparison operator",
		"comparison operator",
		"comparison operator",
		"left bracket",
		"right bracket",
		"TOK_DOLLAR",
		"NameFirstCharacter",
		"NameCharacter",
		"Identifier",
		"TOK_UNSIGNED_INTEGER",
		"TOK_APPROXIMATE_NUMERIC_LITERAL",
		"numeric value",
		"character string",
		"character string",
		"WhiteSpace",
		"NewLine",
		"CommentLine",
		"MultiLineComment",
		"\"select\"",
		"\"distinct\"",
		"\"from\"",
		"\"where\"",
		"\"order\"",
		"\"by\"",
		"\"group\"",
		"\"prefetch\"",
		"\"exists\"",
		"\"in\"",
		"\"asc\"",
		"\"desc\"",
		"\"or\"",
		"\"and\"",
		"\"nil\"",
		"\"not\"",
		"\"list\"",
		"\"between\"",
		"\"is_undefined\"",
		"\"is_defined\"",
		"\"like\"",
		"\"true\"",
		"\"false\"",
		"\"date\"",
		"\"time\"",
		"\"timestamp\""
	};
	
	private static final long[] mk_tokenSet_0() {
		long[] data = { 2L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_0 = new BitSet(mk_tokenSet_0());
	private static final long[] mk_tokenSet_1() {
		long[] data = { 57174604644498L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_1 = new BitSet(mk_tokenSet_1());
	private static final long[] mk_tokenSet_2() {
		long[] data = { 52776558133394L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_2 = new BitSet(mk_tokenSet_2());
	private static final long[] mk_tokenSet_3() {
		long[] data = { 35184372088978L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_3 = new BitSet(mk_tokenSet_3());
	private static final long[] mk_tokenSet_4() {
		long[] data = { 146L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_4 = new BitSet(mk_tokenSet_4());
	private static final long[] mk_tokenSet_5() {
		long[] data = { 141836999983104L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_5 = new BitSet(mk_tokenSet_5());
	private static final long[] mk_tokenSet_6() {
		long[] data = { 3437073348428178L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_6 = new BitSet(mk_tokenSet_6());
	private static final long[] mk_tokenSet_7() {
		long[] data = { 3434874325172370L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_7 = new BitSet(mk_tokenSet_7());
	private static final long[] mk_tokenSet_8() {
		long[] data = { 225250350381137952L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_8 = new BitSet(mk_tokenSet_8());
	private static final long[] mk_tokenSet_9() {
		long[] data = { 549650261048496160L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_9 = new BitSet(mk_tokenSet_9());
	private static final long[] mk_tokenSet_10() {
		long[] data = { 52776558133458L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_10 = new BitSet(mk_tokenSet_10());
	private static final long[] mk_tokenSet_11() {
		long[] data = { 35184372089042L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_11 = new BitSet(mk_tokenSet_11());
	private static final long[] mk_tokenSet_12() {
		long[] data = { 210L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_12 = new BitSet(mk_tokenSet_12());
	private static final long[] mk_tokenSet_13() {
		long[] data = { 8261632L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_13 = new BitSet(mk_tokenSet_13());
	private static final long[] mk_tokenSet_14() {
		long[] data = { -288230359475159040L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_14 = new BitSet(mk_tokenSet_14());
	private static final long[] mk_tokenSet_15() {
		long[] data = { -540431938607906816L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_15 = new BitSet(mk_tokenSet_15());
	private static final long[] mk_tokenSet_16() {
		long[] data = { -576460735626870784L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_16 = new BitSet(mk_tokenSet_16());
	private static final long[] mk_tokenSet_17() {
		long[] data = { 3434874325172434L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_17 = new BitSet(mk_tokenSet_17());
	
	}
