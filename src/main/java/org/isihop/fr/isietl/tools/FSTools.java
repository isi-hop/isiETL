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
package org.isihop.fr.isietl.tools;

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

/**
 *
 * @author tondeur-h
 */
public class FSTools {
    
    //ouverture et lecture fichier
    FileReader fr;
    BufferedReader br;
    List<String> listCsv=new ArrayList<>();
    int numLigneEnCours=0;
    
    //logs
    private static final  Logger logger = Logger.getLogger(FSTools.class.getName());
    
    
    /**************************
     * Fermer le fichier source
     **************************/
    private void fermer_fichier() {
        try {
            br.close();
            fr.close();
        } catch (IOException ex) {
            logger.log(Level.SEVERE, ex.getMessage());
        }
    }

    
    /**********************
     * Lire une ligne du CSV
     * @return 
     **********************/
    private String lecture_ligne() {
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
    private boolean ouvrir_fichier(String csvPath) {
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
     ******************************/
    private void lister_les_fichiers_csv(String csvPath) {       
    File[] filesInDirectory = new File(csvPath).listFiles();
    for(File f : filesInDirectory)
    {
        String filePath = f.getAbsolutePath();
        String fileExtenstion = filePath.substring(filePath.lastIndexOf(".") + 1,filePath.length());
        if("csv".equals(fileExtenstion)){listCsv.add(filePath);}
    }       
    }
    
    
   /*****************************
     * Supprimer le fichier traité.
     * @param cheminFichier 
     *****************************/
    private void supprimer_fichier(String cheminFichier) {
        File f=new File(cheminFichier);
        f.delete();
    }
    
    
     
    /*****************************
     * déplacer le fichier tra ité
     * @param cheminFichier 
     *****************************/
    private void deplacer_fichier(String cheminFichierSrc, String cheminFichierDest) {
        try {            
            Files.move(Paths.get(cheminFichierSrc), Paths.get(cheminFichierDest),java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            logger.log(Level.SEVERE, ex.getMessage());
            logger.log(Level.SEVERE, "Erreur de d\u00e9placement du fichier source : {0}", cheminFichierSrc);
        }
    }
    
}
