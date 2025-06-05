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
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.isihop.fr.isietl.entities.Job;
import org.mozilla.universalchardet.UniversalDetector;

/**
 *
 * @author tondeur-h
 */
public class FSTools 
{
    
    //ouverture et lecture fichier
    private FileReader fr;
    private BufferedReader br;
    private final List<String> listFichiers=new ArrayList<>();
    private int numLigneEnCours=0;
    
    //logs
    public final Logger logger;
    
    /**********************
     * Constructeur
     * @param logs 
     **********************/
    public FSTools(Logger logs)
    {
        logger=logs;
    }

    
    /**************************
     * Fermer le fichier source
     **************************/
    public void close_file() 
    {
        try {
            br.close();
            fr.close();
        } catch (IOException ex) {
            logger.log(Level.SEVERE, ex.getMessage());
        }
    }

    /***********************
     * Statut de la lecture
     * @return 
     ***********************/
    public boolean get_read_file_status() 
    {
        boolean statut;
        try {
            statut=br.ready();
        } catch (IOException ex) {
            logger.log(Level.SEVERE, ex.getMessage());
            statut=false;
        }
        
        return statut;
    }
    
    
    /**********************
     * Lire une ligne du CSV
     * @return 
     **********************/
    public String read_line() 
    {
        String ligne="";
        try {
            ligne=br.readLine();
            numLigneEnCours++;
        } catch (IOException ex) {
            logger.log(Level.SEVERE, ex.getMessage());
        }
        if (ligne.isBlank() || ligne.isEmpty()) {ligne="vide";}
        
        return ligne;
    }

    
    /**********************
     * Lire une ligne du CSV
     * @param nbLignes 
     **********************/
    public void skip_header(int nbLignes) 
    {
        String ligne;
        try {
            for(int nbl=1;nbl<=nbLignes;nbl++)
            {
                ligne=br.readLine();
                logger.log(Level.INFO, "Ignore header : {0}", ligne);
                numLigneEnCours++;
            }
        } catch (IOException ex) {
            logger.log(Level.SEVERE, ex.getMessage());
        }
    }
    
    /*********************
     * Ouvrir fichier csv
     * prépa lecture
     * @param csvPath
     * @return 
     **********************/
    public boolean open_file(String csvPath) 
    {
        boolean okfile=false;
        try {
            File f=new File(csvPath);
            if (f.exists())
            {
                fr=new FileReader(csvPath);
                br=new BufferedReader(fr);
                okfile=true;
            }
        } catch (FileNotFoundException ex) {
            logger.log(Level.SEVERE, ex.getMessage());
        }
        return okfile;
    }

    
    /******************************
     * Lister tous les fichiers CSV
     * du dossier local fichiers
     * @param csvPath
     * @param extension
     * @return 
     ******************************/
    public List<String> list_files_in_path_with_ext(String csvPath, String extension) 
    {       
        File[] filesInDirectory = new File(csvPath).listFiles();
        for(File f : filesInDirectory)
        {
            String filePath = f.getAbsolutePath();
            String fileExtenstion = filePath.substring(filePath.lastIndexOf(".") + 1,filePath.length());
            if(extension.equals(fileExtenstion)){listFichiers.add(filePath);}
        }
        
        return listFichiers;
    }
    
    
   /*****************************
     * Supprimer le fichier traité.
     * @param cheminFichier 
     *****************************/
    public void delete_file(String cheminFichier) 
    {
        File f=new File(cheminFichier);
        f.delete();
    }
    
    
     
    /*****************************
     * déplacer le fichier tra ité 
     * @param cheminFichierSrc
     * @param cheminFichierDest
     *****************************/
    public void move_file_from_to(String cheminFichierSrc, String cheminFichierDest) 
    {
        try {            
            Files.move(Paths.get(cheminFichierSrc), Paths.get(cheminFichierDest),java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            logger.log(Level.SEVERE, ex.getMessage());
            logger.log(Level.SEVERE, "Error moving source file : {0}", cheminFichierSrc);
        }
    }

    
    /**********************************
     * Controle le format des fichiers
     * selon l'extension fournie et
     * dans le dossier fournie.
     * @param jobIntegrator 
     **********************************/
    public void check_files_format(Job jobIntegrator) 
    {
        //lire extension
        String extension=jobIntegrator.getConnectorInbound().get("exttype").getValue();
        //selon extension
        switch (extension.toUpperCase()) {
            case "CSV" -> 
            {
                System.out.println("CSV format detected.");
                check_CSV(jobIntegrator);
            }
            default -> {
                logger.log(Level.SEVERE, "Unknown extension!");
                System.exit(5);
            }
        }
    }
    

    /**************************
     * Check CSV File format
     * @param jobIntegrator 
     **************************/
    private void check_CSV(Job jobIntegrator) 
    {
        boolean hasError=false; //has error in file Flag
        
        //nombre de colonne attendues dans les fichiers CSV
        int nbAttenduCol=safeParseInt(jobIntegrator.getConnectorInbound().get("nbfields").getValue(),1);
        
        //lister tous les fichiers
        //pour chaque fichier, tester le format qui correspond à l'extension.        
        for (String fichier:list_files_in_path_with_ext(jobIntegrator.getConnectorInbound().get("filespath").getValue(),"csv"))
        {
            //check
            //doit avoir autant de colonne que défini séparé par des ; uniquement     
            open_file(fichier);

            //check chaque ligne...
            while (get_read_file_status())
            {
                int nbcol=read_line().split(";").length;
                
                if (nbcol!=nbAttenduCol) 
                {
                    logger.log(Level.SEVERE, "ERROR : File {0} does not conform in line N\u00b0 {1}", new Object[]{fichier, numLigneEnCours});
                    hasError=true;
                }
            }
            
            close_file();
            
            //ok with this file...
            System.out.println("Check "+numLigneEnCours+" Lines => File "+fichier+" is CHECK");
            logger.log(Level.INFO, "Check {0} Lines => File {1} is CHECK", new Object[]{numLigneEnCours, fichier});
            //next please...
        }
        
            //check errors
            if (!hasError)
            {
                System.out.println("End of file control, all files are ok");
                logger.log(Level.INFO, "End of file control, all files are ok");
            }
            else
            {
                //stop pipeline processing
                System.out.println("The process stopped with some error in files!");
                logger.log(Level.INFO, "The process stopped with some error in files!");
                System.exit(6);                
            }
    }
    
    
    /************************************
     *  Safe Parse Integer
     * @param str
     * @return 
     ************************************/
    private int safeParseInt(String str,int defaultValue) 
    {
        if (str == null) {return defaultValue;}
        try {
            //doit etre positif ou = à zero
            if (Integer.parseInt(str,10)>=0) {return Integer.parseInt(str,10);} else {return defaultValue;}
        } catch (NumberFormatException e) {return defaultValue;}
    }

    /***********************************
     * Count the Nb of lines in current file
     * @return 
     ***********************************/    
    public int get_Nb_Lines_In_This_File(String csvPath) 
    {
        BufferedReader reader = null;
        int lines = 0;
        try {
            reader = new BufferedReader(new FileReader(csvPath));
            while (reader.readLine() != null) lines++;
            reader.close();
        } catch (FileNotFoundException ex) {
            logger.log(Level.SEVERE, ex.getMessage());
        } catch (IOException ex) {
            logger.log(Level.SEVERE, ex.getMessage());
        } finally {
            try {
                reader.close();
            } catch (IOException ex) {
                logger.log(Level.SEVERE, ex.getMessage());
            }
        }
        
        return lines;
    }

    /**************************************
     * DETECT if file is in UTF-8 encoding!
     * @param jobIntegrator 
     **************************************/
    public void check_UTF8(Job jobIntegrator) {
        for (String fichier:list_files_in_path_with_ext(jobIntegrator.getConnectorInbound().get("filespath").getValue(),"csv"))
        {
            String encoding;
            try {
                encoding = UniversalDetector.detectCharset(new File(fichier));
                if (encoding != null) {
                    System.out.println("Detected encoding = " + encoding);
                } else 
                {
                    System.out.println("No encoding detected.");
                    encoding="UNKNOWN";
                }
                if (encoding.compareToIgnoreCase("UTF-8")!=0)
                {
                //stop pipeline processing
                System.out.println("The process stopped with some encoding error in files, "+fichier+" is not in UTF-8 charsets!");
                logger.log(Level.INFO, "The process stopped with some encoding error in files, {0} is not in UTF-8 charsets!", fichier);
                System.exit(8);                
                }
                
            } catch (IOException ex) {
                logger.log(Level.SEVERE, ex.getMessage());
                System.exit(7);
            }
        }
    }
    
}
