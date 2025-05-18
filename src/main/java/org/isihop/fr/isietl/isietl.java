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
 */


public class isietl 
{
    //logs
    private static final  Logger logger = Logger.getLogger(isietl.class.getName());
    
    //variables globales
    private String fileIntegratorPath="integrator.yml";
    private boolean displayParameters=false;

    public static void main(String[] args) {new isietl(args);}

    
    /****************
     * Constructeur 
     * @param args
     ****************/
    public isietl(String[] args) 
    {   
        
        //mise en place des logs isietl basé sur logback dans l'ensemble des services
        try 
            {
                logger.setUseParentHandlers(false);
                FileHandler fh = new FileHandler("isietl%g.log", 0, 1, true);
                fh.setLevel(Level.ALL);
                fh.setFormatter(new SimpleFormatter());
                logger.addHandler(fh);
            } catch (IOException ioe) {
                Logger.getLogger(isietl.class.getName()).log(Level.SEVERE, null, ioe);
            }
        
        //au travail maintenant.
        worker(args);
    }

    
    /*********************************
     * Executer du processus des jobs
     *********************************/
    private void worker(String[] args)
    {
        //lire les paramètres de la CLI en priorité
        //sinon lire les properties du système isietl si present dans le dossier du jar
        //sinon affecte des valeurs par defaut path du fichier job dossier du prog, nom integrator.xml, display=false
        lire_properties(this.getClass().getSimpleName(),args);//recuper nom du programme principal
        
        //lire le fichier d'integration.
        new IntegratorTools().lire_fichier_jobs(fileIntegratorPath,displayParameters,logger);
    }
    
    
    /*******************************************
     * Lire les properties du fichier
     * properties local, si les args ne
     * sont pas définis, si args et 
     * properties non définis alors valeurs
     * par defaut.
     * @param programName
     * @param args
     ******************************************/
    public void lire_properties(String programName, String[] args)
    {
        //si nombre d'arguments supértieur à Zero
        //tester si contient le path du job 
        if (!analyse_cli_args(args))
        {
            //recuperer le chemin local et le nom de l'application
            //le fichier properties doit comporter le même
            //nom que l'application.
            String currentPath=System.getProperty("user.dir");
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
                }
            } catch (FileNotFoundException ex) {
                logger.log(Level.SEVERE, ex.getMessage());
                fileIntegratorPath=currentPath+"/integrator.yml"; //définition par defaut
            } finally 
            {
                try {if (is!=null){is.close();}} catch (IOException ex) {logger.log(Level.SEVERE, ex.getMessage());}
            }
                logger.log(Level.INFO, "Parametre CLI, fileintegratorpath={0}", fileIntegratorPath);
                logger.log(Level.INFO, "Parametre CLI, displayparameters={0}", displayParameters);
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
            //creer les arguments.
            Option displayparameters = Option.builder()
                                        .longOpt("displayparameters")
                                        .option("dp")
                                        .desc("Affiche les paramètres du fichier Jobs")
                                        .build();
            Option fip = Option.builder("fileintegratorpath")
                             .argName("fichier")
                             .longOpt("fileintegratorpath")
                             .option("fip")
                             .hasArg()
                             .desc("Chemin du fichier jobs")
                             .build();
            Option help= Option.builder("help")
                    .longOpt("help")
                    .option("h")
                    .desc("Aide sur isiEtl")
                    .build();
            Options options = new Options();

            options.addOption(displayparameters);
            options.addOption(fip);
            options.addOption(help);
            
            //creation du parser
            CommandLineParser parser = new DefaultParser();
            CommandLine line ;
            try 
            {
                line = parser.parse(options, args);
            }
            catch (ParseException exp) 
            {
                System.err.println("Erreur de parse : " + exp.getMessage());
                logger.log(Level.SEVERE, "Erreur de parse : {0}", exp.getMessage());
                return CLIOK;
            }
            
            //analyse des arguments
            if (line.hasOption(help)) {new HelpFormatter().printHelp("isiEtl", options);System.exit(0);} //afficher aide et quitter
            if (line.hasOption(fip)) {fileIntegratorPath = line.getOptionValue("fileintegratorpath");}
            displayParameters = line.hasOption(displayparameters);
            logger.log(Level.INFO, "Parametre CLI, fileintegratorpath={0}", fileIntegratorPath);
            logger.log(Level.INFO, "Parametre CLI, displayparameters={0}", displayParameters);
            CLIOK=true;
        }        
        return CLIOK;
    }
    
}