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
    public void fermer_fichier() 
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
    public boolean lecture_statut() 
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
    public String lecture_ligne() 
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

    
    /*********************
     * Ouvrir fichier csv
     * prépa lecture
     * @param csvPath
     * @return 
     **********************/
    public boolean ouvrir_fichier(String csvPath) 
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
    public List<String> lister_les_fichiers(String csvPath, String extension) 
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
    public void supprimer_fichier(String cheminFichier) 
    {
        File f=new File(cheminFichier);
        f.delete();
    }
    
    
     
    /*****************************
     * déplacer le fichier tra ité 
     * @param cheminFichierSrc
     * @param cheminFichierDest
     *****************************/
    public void deplacer_fichier(String cheminFichierSrc, String cheminFichierDest) 
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
        //nombre de colonne attendues dans les fichiers CSV
        int nbAttenduCol=Integer.parseInt(jobIntegrator.getConnectorInbound().get("nbfields").getValue(),10);
        
        //lister tous les fichiers
        //pour chaque fichier, tester le format qui correspond à l'extension.        
        for (String fichier:lister_les_fichiers(jobIntegrator.getConnectorInbound().get("filespath").getValue(),"csv"))
        {
            //verifier
            //doit avoir autant de colonne que défini séparé par des ; uniquement     
            ouvrir_fichier(fichier);
            
            //check chaque ligne...
            while (lecture_statut())
            {
                int nbcol=lecture_ligne().split(";").length;
                
                if (nbcol!=nbAttenduCol) 
                {
                    logger.log(Level.SEVERE, "ERROR : File {0} does not conform to line N\u00b0 {1}", new Object[]{fichier, numLigneEnCours});
                    System.exit(6);
                }
            }
            
            fermer_fichier();
            System.out.println("File "+fichier+" : PASS");
        }
        
        System.out.println("End of file control");
    }
    
}
