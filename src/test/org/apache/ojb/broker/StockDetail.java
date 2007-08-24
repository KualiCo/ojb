package org.apache.ojb.broker;

public class StockDetail implements java.io.Serializable
{

    protected boolean isSelloutArticle;

    protected int minimumStock;

    protected int orderedUnits;

    protected int stock;

    protected String unit;

    private ArticleWithStockDetail myArticle;

	public StockDetail()
	{
		
	}

    public StockDetail(boolean sellout, int minimum, int ordered, int stock, String unit, ArticleWithStockDetail parent)
    {
        isSelloutArticle = sellout;
        minimumStock = minimum;
        orderedUnits = ordered;
        this.stock = stock;
        this.unit = unit;
        myArticle = parent;
    }


    public int getStock()
    {
        return stock;
    }

    public void setStock(int newStock)
    {
        stock = newStock;
        // we must keep the detail object in sync with the parent:
        if (myArticle != null)
        {
        	myArticle.stock = newStock;
        }
    }
    /**
     * Returns the isSelloutArticle.
     * @return boolean
     */
    public boolean isSelloutArticle()
    {
        return isSelloutArticle;
    }

    /**
     * Returns the minimumStock.
     * @return int
     */
    public int getMinimumStock()
    {
        return minimumStock;
    }

    /**
     * Returns the myArticle.
     * @return ArticleWithStockDetail
     */
    public ArticleWithStockDetail getMyArticle()
    {
        return myArticle;
    }

    /**
     * Returns the orderedUnits.
     * @return int
     */
    public int getOrderedUnits()
    {
        return orderedUnits;
    }

    /**
     * Returns the unit.
     * @return String
     */
    public String getUnit()
    {
        return unit;
    }

    /**
     * Sets the isSelloutArticle.
     * @param isSelloutArticle The isSelloutArticle to set
     */
    public void setIsSelloutArticle(boolean isSelloutArticle)
    {
        this.isSelloutArticle = isSelloutArticle;
    }

    /**
     * Sets the minimumStock.
     * @param minimumStock The minimumStock to set
     */
    public void setMinimumStock(int minimumStock)
    {
        this.minimumStock = minimumStock;
    }

    /**
     * Sets the myArticle.
     * @param myArticle The myArticle to set
     */
    public void setMyArticle(ArticleWithStockDetail myArticle)
    {
        this.myArticle = myArticle;
    }

    /**
     * Sets the orderedUnits.
     * @param orderedUnits The orderedUnits to set
     */
    public void setOrderedUnits(int orderedUnits)
    {
        this.orderedUnits = orderedUnits;
    }

    /**
     * Sets the unit.
     * @param unit The unit to set
     */
    public void setUnit(String unit)
    {
        this.unit = unit;
    }

}
