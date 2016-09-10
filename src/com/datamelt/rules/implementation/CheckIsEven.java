package com.datamelt.rules.implementation;

/**
 * Checks if a given number is an even number
 * <p>
 * The first parameter of a given method is always the value of the field that shall be checked. The second parameter is either another field to check against 
 * or an expected value (fixed value) to check against the first value.
 * 
 * @author uwe geercken
 */
public class CheckIsEven extends GenericCheck
{
	
    /**
     * Evaluates if the given integer value is an even number.
     * 
     * @param value	the value for comparison
     * @return		indication if the value is an even number
     */
    public static boolean evaluate(int value)
    {
    	if (value % 2==0)
    	{
    		return true;
    	}
    	else
    	{
    		return false;
    	}
    }
    
    /**
     * Evaluates if the given long value is an even number.
     * 
     * @param value	the value for comparison
     * @return		indication if the value is an even number
     */
    public static boolean evaluate(long value)
    {
    	if (value % 2==0)
    	{
    		return true;
    	}
    	else
    	{
    		return false;
    	}
    }
}
