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
package org.isihop.fr.isietl;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Properties;
import org.isihop.fr.isietl.jobs.IntegratorTools;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import java.util.logging.SimpleFormatter;


/**
 *
 * @author tondeur-h
 * 
 * NOTES:
 * =====
 * User logs messages are in English only.
 * Internal comments are in French and/or English.
 * Sorry purists!
 */


public class isietl 
{
    //VERSION
    private final String VERSION="0.1-202601";
    
    //logs
    private static final  Logger logger = Logger.getLogger(isietl.class.getName());
    
    //Global definition variables
    private String fileIntegratorPath="integrator.yml";
    private boolean displayParameters=false;
    private String programName;
    private String currentPath;
    
    /****************************
     * Main entry point
     * just call the constructor
     * @param args 
     ****************************/
    public static void main(String[] args) {new isietl(args);}

    
    /****************
     * Constructor 
     * @param args
     ****************/
    public isietl(String[] args) 
    {   
        //Get short program Name
        programName=getClass().getSimpleName();
        //current application Path
        currentPath=System.getProperty("user.dir");
        
        //initialize logs files, only one name for all services
        try 
            {
                logger.setUseParentHandlers(false);
                FileHandler fh = new FileHandler(programName+"%g.log", 0, 1, true);
                fh.setLevel(Level.ALL);
                fh.setFormatter(new SimpleFormatter());
                logger.addHandler(fh);
            } catch (IOException ioe) {
                Logger.getLogger(programName).log(Level.SEVERE, "Error logs initializer!", ioe.getMessage());
            }
        
        //Show version header
        print_version();
        
        //At work now!
        worker(args);
    }

    
    /*********************************
     * Job expander and run job
     *********************************/
    private void worker(String[] args)
    {
        //read CLI parameters first
        //if not read system properties isietl if present in jar folder
        //if not assign default values job file path =>program folder+integrator.xml, display=false
        read_properties(args);//get args list for testing CLI parameters
        
        //read the integration file.
        new IntegratorTools().read_job_file(fileIntegratorPath,displayParameters,logger);
    }
    
    
    /*******************************************
     * Read file properties
     * local properties, if args are not
     * are not defined, if args and
     * properties not defined then default
     * by default.
     * @param args
     ******************************************/
    public void read_properties(String[] args)
    {
        //if number of arguments greater than Zero
        //test if contains job path 
        if (!parse_cli_args(args))
        {
            //get local path and application name
            //the properties file must have the same
            //name as the application.
            FileInputStream is=null;
            try {
                is = new FileInputStream(currentPath+"/"+programName+".properties");
                Properties p=new Properties();
                try {
                    //load properties file
                    p.load(is);
                    //lecture des variables à définir...
                    fileIntegratorPath=p.getProperty("fileintegratorpath", currentPath+"/integrator.yml"); //par defaut le chemin de l'appli
                    displayParameters=Boolean.parseBoolean(p.getProperty("displayparameters","false"));
                } catch (IOException ex) 
                {
                    logger.log(Level.SEVERE, ex.getMessage());
                    //If there is a read error, the default values are retained.
                }
            } catch (FileNotFoundException ex) 
            {
                logger.log(Level.SEVERE, ex.getMessage());
                fileIntegratorPath=currentPath+"/integrator.yml"; //default definition
            } finally 
            {
                try {if (is!=null){is.close();}} catch (IOException ex) {logger.log(Level.SEVERE, ex.getMessage());}
            }
                //A LOGGING TOOL
                logger.log(Level.INFO, "CLI Parameter, fileintegratorpath={0}", fileIntegratorPath);
                logger.log(Level.INFO, "CLI Parameter, displayparameters={0}", displayParameters);
        }
    }
    
    
    /***************************************
     * Analyze CLI for use of
     * Args in priority
     * @param args
     * @return 
     **************************************/
    private boolean parse_cli_args(String[] args) 
    {
        boolean CLIOK=false;
        if(args.length==0) {return CLIOK;}
        else
        {
            //create short and long arguments.
            Option jobstemplate = Option.builder()
                                        .longOpt("jobstemplate")
                                        .option("jt")
                                        .desc("create a new job file YML template.")
                                        .build();
            Option displayparameters = Option.builder()
                                        .longOpt("displayparameters")
                                        .option("dp")
                                        .desc("Displays the parameters of the YAML 'JOB' file.")
                                        .build();
            Option fip = Option.builder("fileintegratorpath")
                             .argName("file")
                             .longOpt("fileintegratorpath")
                             .option("fip")
                             .hasArg()
                             .desc("Path of je 'JOB' file")
                             .build();
            Option help= Option.builder("help")
                    .longOpt("help")
                    .option("h")
                    .desc("Help on isiEtl")
                    .build();
            Options options = new Options();

            //add CLI options
            options.addOption(jobstemplate);
            options.addOption(displayparameters);
            options.addOption(fip);
            options.addOption(help);
            
            //create CLI parser
            CommandLineParser parser = new DefaultParser();
            CommandLine line ;
            try 
            {
                line = parser.parse(options, args);
            }
            catch (ParseException exp) 
            {
                System.err.println("CLI Parse Error : " + exp.getMessage());
                logger.log(Level.SEVERE, "CLI Parse error : {0}", exp.getMessage());
                return CLIOK;
            }
            
            //arguments analysis
            if (line.hasOption(jobstemplate)) 
            {
                print_template();
                System.exit(0);
            }
            if (line.hasOption(help)) 
            {
                new HelpFormatter().printHelp("isiEtl", options);
                System.exit(0);
            } //print "aids" and "quit"
            if (line.hasOption(fip)) 
            {
                fileIntegratorPath = line.getOptionValue("fileintegratorpath");
            }
            displayParameters = line.hasOption(displayparameters);
            logger.log(Level.INFO, "Parameter CLI, fileintegratorpath={0}", fileIntegratorPath);
            logger.log(Level.INFO, "Parameter CLI, displayparameters={0}", displayParameters);
            CLIOK=true;
        }        
        return CLIOK;
    }
    
    
    /*********************************
     * Application version
     *********************************/
    private void print_version() 
    {
        System.out.println("------------------------------------------------");
        System.out.println(programName.toUpperCase()+": Version "+VERSION);
        System.out.println("Copyright (c) TONDEUR Hervé (2025/05)");
        System.out.println("licence GNU GPLv3 : https://www.gnu.org/licenses/gpl-3.0.fr.html#license-text");
        System.out.println("https://github.com/isi-hop");
        System.out.println("------------------------------------------------");
        System.out.println("");
    }
    
    private void print_template()
    {
        PrintWriter pr=null;
        try {
            String template="""
                                        #
                                        # Copyright (C) 2025 tondeur-h
                                        #
                                        # This program is free software: you can redistribute it and/or modify
                                        # it under the terms of the GNU General Public License as published by
                                        # the Free Software Foundation, either version 3 of the License, or
                                        # (at your option) any later version.
                                        #
                                        # This program is distributed in the hope that it will be useful,
                                        # but WITHOUT ANY WARRANTY; without even the implied warranty of
                                        # MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
                                        # GNU General Public License for more details.
                                        #
                                        # You should have received a copy of the GNU General Public License
                                        # along with this program.  If not, see <http://www.gnu.org/licenses/>.
                                        #
                                        # Integration process name and description
                                        # Optional fields to describe the job
                                        #-------------HEADER---------------
                                        jobName: "MyFirst integrator process"
                                        jobDescription: "Get data from a CSV file to a DB postgresql"
                                        jobDateTime: "2025-05-20 12:00:00"

                                        #size defaut to 1 if not defined, must be GT 0, if not value is 1
                                        jobBatchSize: "200"
                                        forceIntermediateCommit: "true"
                            
                                        #----------------------------------
                                        # Description of the incoming connector
                                        # Type: CSV file or PostgreSQL database for now
                                        # For the template, all possible configurations have been placed here
                                        #------------INBOUND CONNECTOR-----------
                                        connectorInbound:
                                          #must be "file" or "database" see below
                                          connectortype:
                                            value: "file"
                                          filespath:
                                            value : "/home/herve/isietl/template"
                                          checkfiles:
                                            value: "true"
                                          backupdestinationpath:
                                            value: "/home/herve/isietl/template"
                                          exttype:
                                            value: "csv"

                                        #common to all types of InBound connector
                                          nbfields:
                                            value: "4"
                                          jumpheader:
                                            value: "1"

                                        #only for inBound database type
                                          #connectortype:
                                            #value: "database"
                                          dbdriver:
                                            value: "org.postgresql.Driver"
                                          dburl:
                                            value: "jdbc:postgresql://localhost:5432/mydb"
                                          dblogin:
                                            value: "postgres"
                                          dbpassword:
                                            value: "admin"
                                        #----------------------------------------
                            
                                        #------------OUTBOUND CONNECTOR----------
                                        # description of the outgoing connector to the postgresql database
                                        connectorOutbound:
                                          connectortype:
                                            value: "database"
                                          dbdriver:
                                            value: "org.postgresql.Driver"
                                          dburl:
                                            value: "jdbc:postgresql://localhost:5432/mydb"
                                          dblogin:
                                            value: "postgres"
                                          dbpassword:
                                            value: "admin"
                            
                                          targetTable:
                                            value: "tabletest"
                            
                                          #common value for outBound connectors
                                          #must be false or true
                                          ignoreErrors:
                                            value: "false"
                                          #ignore duplicates (true/false)
                                          ignoreDuplicates:
                                            value: "false"
                            
                                        # Description of the fields in the outgoing postgresql table.
                                        fieldsOut:
                                          id:
                                            defaultValue: ""
                                            size: "20"
                                            type: "int4"
                                          column1:
                                            defaultValue: "col1"
                                            size: "80"
                                            type: "varchar"
                                          column2:
                                            defaultValue: "col2"
                                            size: "50"
                                            type: "varchar"
                                          column3:
                                            defaultValue: "col3"
                                            size: "10"
                                            type: "varchar"
                                        #-------------------------------------
                            
                                        #-----------FMT PROCESSING------------
                                        # DSL script that allows you to execute
                                        # per-processing actions such as
                                        # Filtering, Mapping, and Transformation
                                        filteringScript: ""
                                        mappingScript: ""
                                        transformerScript: ""
                                        #-------------------------------------
                            
                                        #-----------POSTPROCESSING------------
                                        # SQL script that points to a valid SQL file
                                        # Allows these queries to be executed post-processed
                                        # If the destination is a database
                                        SQLPostProcessing: ""
                                        #-------------------------------------
                                        """;
            pr = new PrintWriter(currentPath+"/integrator_template.yml");
            pr.print(template);
            pr.flush();
            pr.close();
            System.out.println("Job Template created... See integrator_template.yml in this current path!");
        } catch (FileNotFoundException ex) {
            Logger.getLogger(isietl.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            pr.close();
        }
    }
    
}