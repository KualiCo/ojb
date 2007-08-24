/*
 * Created by IntelliJ IDEA.
 * User: tom
 * Date: Aug 7, 2001
 * Time: 9:37:18 PM
 * To change template for new class use
 * Code Style | Class Templates options (Tools | IDE Options).
 */
package org.apache.ojb.broker;

import java.util.Map;

import org.apache.ojb.broker.accesslayer.RowReaderDefaultImpl;
import org.apache.ojb.broker.metadata.ClassDescriptor;

public class RowReaderTestImpl extends RowReaderDefaultImpl
{
    public RowReaderTestImpl(ClassDescriptor cld)
    {
        super(cld);
    }

    /**
     * materialize a single object of a type described by cld,
     * from the first row of the ResultSet rs.
     * the implementor of this class must not care for materialiing
     *  references or collection attributes, this is done later!
     */
    public Object readObjectFrom(Map row)
    {
        Object result = super.readObjectFrom(row);
        if (result instanceof ArticleWithStockDetail)
        {
            ArticleWithStockDetail art = (ArticleWithStockDetail) result;
            boolean sellout = art.isSelloutArticle;
            int minimum = art.minimumStock;
            int ordered = art.orderedUnits;
            int stock = art.stock;
            String unit = art.unit;
            StockDetail detail = new StockDetail(sellout, minimum, ordered, stock, unit, art);
            art.stockDetail = detail;
            return art;
        }
        else
        {
            return result;
        }
    }
}
