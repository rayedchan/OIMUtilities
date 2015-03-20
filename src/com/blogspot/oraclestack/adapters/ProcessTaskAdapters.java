package com.blogspot.oraclestack.adapters;

import java.util.Date;

/**
 *
 * @author rayedchan
 */
public class ProcessTaskAdapters 
{
    public static String returnCurrentDate()
    {
        return new Date().toString();
    }
}
