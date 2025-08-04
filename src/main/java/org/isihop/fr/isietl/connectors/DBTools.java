/*
 * Copyright (C) 2025 tondeur-h
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.isihop.fr.isietl.connectors;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author tondeur-h
 */
public class DBTools 
{
    
    //logs
    private final Logger logger;
    
    //database
    private Connection conn;
    private Statement stmt;
    private ResultSet rst;
    private Map<String,String> metadataMap;

    
    /**************************
     * Constructor
     * @param logs 
     **************************/
    public DBTools(Logger logs) 
    {
        logger=logs;
    }

    
    /****************************
     * Getter for DB connection
     * @return 
     ****************************/
    public Connection getConn() 
    {
        return conn;
    }

    /****************************
     * Getter for Statement
     * @return 
     ****************************/
    public Statement getStmt() 
    {
        return stmt;
    }
    
    
    /***********************************
     * connect RDBMS
     * @param dbdriver
     * @param dburl
     * @param dblogin
     * @param dbpassword
     * @return boolean
     **********************************/
    public boolean connect_db(String dbdriver, String dburl, String dblogin, String dbpassword) 
    {
        boolean isconnected=false;
        try {
            Class.forName(dbdriver);
       
                conn = DriverManager.getConnection(dburl,dblogin,dbpassword);
                isconnected=true;
                stmt=conn.createStatement();
                
        } catch (ClassNotFoundException | SQLException ex) {
            logger.log(Level.SEVERE, ex.getMessage());
        }
    return isconnected;  
    }

    
    /*********************************
     * Close RDBMS
     *********************************/
    public void close_db() 
    {
        try {
            stmt.close();
            conn.close();
        } catch (SQLException ex) {
            logger.log(Level.SEVERE, ex.getMessage());
        }
    }
    
    
    /***************************
     *Convert date format
     * JJ/MM/AA => AAAA-MM-JJ
     * @param string
     * @return 
     **************************/
    private String cvtTopgDate(String strDate) 
    {
        return "20"+strDate.substring(6)+"-"+strDate.substring(3,5)+"-"+strDate.substring(0, 2);
    }

    /***********************************
     * Post Processing SQL
     * @param SQLPostProcessing
     * @return 
     ***********************************/
    public boolean SQLPostProcessing(String SQLPostProcessing) 
    {
        String sql="";    
        boolean finalise=false;
        try {
            //Construct query...
            System.out.println("Begin of SQL Post Processing");
            logger.log(Level.INFO, "Begin of SQL Post Processing");
            stmt=conn.createStatement();
            BufferedReader brsql=new BufferedReader(new FileReader(SQLPostProcessing));
            while (brsql.ready())
            {
                sql=brsql.readLine();
                System.out.println("Running : "+sql);
                logger.log(Level.INFO, "Running : {0}", sql);
                stmt.executeUpdate(sql);
                conn.commit(); //force commit
            }
            System.out.println("End of SQL Post Processing");
            logger.log(Level.INFO, "End of SQL Post Processing");
            finalise=true; //Everything is OK
        } catch (SQLException ex) 
        {
             System.out.println("( KO : "+sql);
             logger.log(Level.SEVERE, "( KO : {0}", sql);
             logger.log(Level.SEVERE, "{0} ---- {1}", new Object[]{ex.getMessage(), ex.getSQLState()});
        } catch (FileNotFoundException ex) {
            System.out.println(ex.getMessage());
            logger.log(Level.SEVERE, ex.getMessage());
        } catch (IOException ex) {
            System.out.println(ex.getMessage());
            logger.log(Level.SEVERE, ex.getMessage());
        }
        return finalise;
    }
    
    
        /***********************************
     * Post Processing SQL
     * @param SQLPreProcessing
     * @return 
     ***********************************/
    public boolean SQLPreProcessing(String SQLPreProcessing) 
    {
        String sql="";    
        boolean finalise=false;
        try {
            //Construct query...
            System.out.println("Begin of SQL Pre Processing");
            logger.log(Level.INFO, "Begin of SQL Pre Processing");
            stmt=conn.createStatement();
            BufferedReader brsql=new BufferedReader(new FileReader(SQLPreProcessing));
            while (brsql.ready())
            {
                sql=brsql.readLine();
                System.out.println("Running : "+sql);
                logger.log(Level.INFO, "Running : {0}", sql);
                stmt.executeUpdate(sql);
                conn.commit(); //force commit
            }
            System.out.println("End of SQL Pre Processing");
            logger.log(Level.INFO, "End of SQL Pre Processing");
            finalise=true; //Everything is OK
        } catch (SQLException ex) 
        {
             System.out.println("( KO : "+sql);
             logger.log(Level.SEVERE, "( KO : {0}", sql);
             logger.log(Level.SEVERE, "{0} ---- {1}", new Object[]{ex.getMessage(), ex.getSQLState()});
        } catch (FileNotFoundException ex) {
            System.out.println(ex.getMessage());
            logger.log(Level.SEVERE, ex.getMessage());
        } catch (IOException ex) {
            System.out.println(ex.getMessage());
            logger.log(Level.SEVERE, ex.getMessage());
        }
        return finalise;
    }
    
    
    /**********************************
     * Make fetch data from DB
     * @param query 
     * @return  
     **********************************/
    public ResultSet SQLFetch(String query)
    {
        try {
            stmt=conn.createStatement();
            rst=stmt.executeQuery(query);
            System.out.println("Data extracted!");
            logger.log(Level.INFO, "Data extracted!");
            //get metadata list
            ResultSetMetaData metaData = rst.getMetaData();
            metadataMap=new HashMap<>();
            for (int cols=1;cols<=metaData.getColumnCount();cols++)
            {
                metadataMap.put(metaData.getColumnName(cols),metaData.getColumnTypeName(cols));
            }
            System.out.println("Metadata extracted!");
            logger.log(Level.INFO, "Metadata extracted!");
                        
        } catch (SQLException ex) {
            logger.log(Level.SEVERE, ex.getMessage());
        }
        return rst; //return resulset
    }
    
    /************************************
     * return metaData from Fetch
     * @return 
     ***********************************/
    public Map<String,String> getMetadataMap()
    {
        return metadataMap;
    }
    
}
