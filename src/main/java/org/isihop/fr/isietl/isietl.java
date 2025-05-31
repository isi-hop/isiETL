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
 * User log messages are in English only.
 * Internal comments are in French and/or English.
 * Sorry purists!
 */


public class isietl 
{
    //VERSION
    private final String VERSION="0.1-202505";
    
    //logs
    private static final  Logger logger = Logger.getLogger(isietl.class.getName());
    
    //variables globales
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
        afficher_version();
        
        //At work now!
        worker(args);
    }

    
    /*********************************
     * Job expander and run job
     *********************************/
    private void worker(String[] args)
    {
        //lire les paramètres de la CLI en priorité
        //sinon lire les properties du système isietl si present dans le dossier du jar
        //sinon affecte des valeurs par defaut path du fichier job =>dossier du prog+integrator.xml, display=false
        lire_properties(args);//get args list for testing CLI parameters
        
        //lire le fichier d'integration.
        new IntegratorTools().lire_fichier_jobs(fileIntegratorPath,displayParameters,logger);
    }
    
    
    /*******************************************
     * Lire les properties du fichier
     * properties local, si les args ne
     * sont pas définis, si args et 
     * properties non définis alors valeurs
     * par defaut.
     * @param args
     ******************************************/
    public void lire_properties(String[] args)
    {
        //si nombre d'arguments supértieur à Zero
        //tester si contient le path du job 
        if (!analyse_cli_args(args))
        {
            //recuperer le chemin local et le nom de l'application
            //le fichier properties doit comporter le même
            //nom que l'application.
            FileInputStream is=null;
            try {
                is = new FileInputStream(currentPath+"/"+programName+".properties");
                Properties p=new Properties();
                try {
                    //charger le fichier properties
                    p.load(is);
                    //lecture des variables à définir...
                    fileIntegratorPath=p.getProperty("fileintegratorpath", currentPath+"/integrator.yml"); //par defaut le chemin de l'appli
                    displayParameters=Boolean.parseBoolean(p.getProperty("displayparameters","false"));
                } catch (IOException ex) {
                    logger.log(Level.SEVERE, ex.getMessage());
                    //si erreur de lecture on arde les valeurs par défaut.
                }
            } catch (FileNotFoundException ex) 
            {
                logger.log(Level.SEVERE, ex.getMessage());
                fileIntegratorPath=currentPath+"/integrator.yml"; //définition par defaut
            } finally 
            {
                try {if (is!=null){is.close();}} catch (IOException ex) {logger.log(Level.SEVERE, ex.getMessage());}
            }
                //un peut de journalisation
                logger.log(Level.INFO, "CLI Parameter, fileintegratorpath={0}", fileIntegratorPath);
                logger.log(Level.INFO, "CLI Parameter, displayparameters={0}", displayParameters);
        }
    }
    
    
    /***************************************
     * Analyser la CLI pour utilisation des
     * Args en priorité
     * @param args
     * @return 
     **************************************/
    private boolean analyse_cli_args(String[] args) 
    {
        boolean CLIOK=false;
        if(args.length==0) {return CLIOK;}
        else
        {
            //creer les arguments court et long
            Option jobstemplate = Option.builder()
                                        .longOpt("jobstemplate")
                                        .option("jt")
                                        .desc("create a job YML template.")
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

            //ajout des options 
            options.addOption(jobstemplate);
            options.addOption(displayparameters);
            options.addOption(fip);
            options.addOption(help);
            
            //creation du parser CLI
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
            
            //analyse des arguments
            if (line.hasOption(jobstemplate)) 
            {
                imprimer_template();
                System.exit(0);
            }
            if (line.hasOption(help)) 
            {
                new HelpFormatter().printHelp("isiEtl", options);
                System.exit(0);
            } //afficher aide et quitter
            if (line.hasOption(fip)) 
            {
                fileIntegratorPath = line.getOptionValue("fileintegratorpath");
            }
            displayParameters = line.hasOption(displayparameters);
            logger.log(Level.INFO, "Parametre CLI, fileintegratorpath={0}", fileIntegratorPath);
            logger.log(Level.INFO, "Parametre CLI, displayparameters={0}", displayParameters);
            CLIOK=true;
        }        
        return CLIOK;
    }
    
    
    /*********************************
     * Version de l'application
     *********************************/
    private void afficher_version() 
    {
        System.out.println("------------------------------------------------");
        System.out.println(programName.toUpperCase()+": Version "+VERSION);
        System.out.println("Copyright (c) TONDEUR Hervé (2025/05)");
        System.out.println("licence GNU GPLv3 : https://www.gnu.org/licenses/gpl-3.0.fr.html#license-text");
        System.out.println("https://github.com/isi-hop");
        System.out.println("------------------------------------------------");
        System.out.println("");
    }
    
    private void imprimer_template()
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
                                        # nom de process d'integration et sa description
                                        # champs optionnels pour décrire le JOB
                                        #-------------HEADER---------------
                                        jobName: "MyFirst integrator process"
                                        jobDescription: "Get data from a CSV file to a DB postgresql"
                                        jobDateTime: "2025-05-20 12:00:00"
                                        #this 2 parameters works together
                                        jobBatchMode: "false"
                                        #size defaut to 1 if not defined, must be GT 0, if not value is 1
                                        jobBatchSize: "20"
                                        #----------------------------------
                                                         # description du connecteur entrant
                                        # type file csv ou database postgresql pour l'instant'
                                        # pour la demo, toutes les configurations possible on été placé ici
                                        #------------INBOUND CONNECTOR-----------
                                        connectorInbound:
                                          #must be ""file"" or ""database""
                                          connectortype:
                                            value: "file"
                                          filespath:
                                            value : "c:/users/herve/documents/netbeansprojects/isietl/test"
                                          checkfiles:
                                            value: "true"
                                          backupdestinationpath:
                                            value: "c:/users/herve/documents/netbeansprojects/isietl/test/"
                                          exttype:
                                            value: "csv"
                                        #commun a tous les type de connecteur InBound
                                          nbfields:
                                            value: "4"
                                                         #seulement pour le type database inBound
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
                                        # description du connecteur sortant vers la database postgresql
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
                                          #valeur commune aux connecteurs outBound
                                          #prend false ou true
                                          ignoreErrors:
                                            value: "false"
                                          #ignorer les doublons (true/false)
                                          ignoreDuplicates:
                                            value: "false"
                                        # description des champs de la table sortant postgresql.
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
                                        # Script DSL qui permet d'éxécuter en
                                        # per processing les actions de
                                        # Filtrage, Mapping, Trnsformation
                                        filteringScript: ""
                                        mappingScript: ""
                                        transformerScript: ""
                                        #-------------------------------------
                                        #-----------POSTPROCESSING------------
                                        # Script sql qui pointe vers un fichier SQL valide
                                        # permet d'eéxécuter en post processing ces requêtes
                                        # si destination est une BDD
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