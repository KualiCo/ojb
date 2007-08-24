package org.apache.ojb.broker;

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

import java.util.Collection;

import org.apache.ojb.broker.accesslayer.conversions.ConversionException;
import org.apache.ojb.broker.accesslayer.conversions.FieldConversion;
import org.apache.ojb.broker.query.Criteria;
import org.apache.ojb.broker.query.QueryByCriteria;
import org.apache.ojb.broker.query.QueryFactory;
import org.apache.ojb.junit.PBTestCase;

/**
 * Test extents having a different type of the same field.
 * the field 'price' is of type double in Article and String in BookArticle
 * 
 * @author <a href="mailto:jbraeuchi@gmx.ch">Jakob Braeuchi</a>
 * @version $Id: FieldTypeTest.java,v 1.1 2007-08-24 22:17:28 ewestfal Exp $
 */
public class FieldTypeTest extends PBTestCase
{
    public void testDifferentFieldTypes()
    {
        QueryByCriteria q;
        Collection result;
        Criteria c = new Criteria();

        q = QueryFactory.newQuery(AbstractArticle.class, c);
        result = broker.getCollectionByQuery(q);
        assertNotNull(result);        
    }
    
    
    public static void main(String[] args)
    {
        String[] arr = {FieldTypeTest.class.getName()};
        junit.textui.TestRunner.main(arr);
    }

    public static class AbstractArticle
    {
        /** maps to db-column "Artikel-Nr";INT;PrimaryKey*/
        private Integer articleId;
        /** maps to db-column Artikelname;CHAR*/
        private String articleName;
        /**
         * @return Returns the articleId.
         */
        
        public Integer getArticleId()
        {
            return articleId;
        }
        /**
         * @param articleId The articleId to set.
         */
        public void setArticleId(Integer articleId)
        {
            this.articleId = articleId;
        }
        /**
         * @return Returns the articleName.
         */
        public String getArticleName()
        {
            return articleName;
        }
        /**
         * @param articleName The articleName to set.
         */
        public void setArticleName(String articleName)
        {
            this.articleName = articleName;
        }

    }
    
    public static class Article extends AbstractArticle
    {
        /** maps to db-column Einzelpreis;DECIMAL*/
        private double price;

        /**
         * @return Returns the price.
         */
        public double getPrice()
        {
            return price;
        }

        /**
         * @param price The price to set.
         */
        public void setPrice(double price)
        {
            this.price = price;
        }

    }    
    public static class BookArticle extends AbstractArticle
    {
        /** books author*/
        private String author;
        /** ISBN No of Book*/
        private String isbn;

        /** maps to db-column Einzelpreis;DECIMAL*/
        private String price;

        /**
         * @return Returns the author.
         */
        public String getAuthor()
        {
            return author;
        }

        /**
         * @param author The author to set.
         */
        public void setAuthor(String author)
        {
            this.author = author;
        }

        /**
         * @return Returns the isbn.
         */
        public String getIsbn()
        {
            return isbn;
        }

        /**
         * @param isbn The isbn to set.
         */
        public void setIsbn(String isbn)
        {
            this.isbn = isbn;
        }

        /**
         * @return Returns the price.
         */
        public String getPrice()
        {
            return price;
        }

        /**
         * @param price The price to set.
         */
        public void setPrice(String price)
        {
            this.price = price;
        }

    }
    
    public static class DoubleToStringConversion implements FieldConversion
    {
        public Object javaToSql(Object source) throws ConversionException
        {
            if (source instanceof String)
            {
                return Double.valueOf((String)source);
            }
            else
            {
                return null;
            }    
        }

        public Object sqlToJava(Object source) throws ConversionException
        {
            if (source instanceof Double)
            {
                return source.toString();
            }
            else
            {
                return null;
            }
        }
    }

}

