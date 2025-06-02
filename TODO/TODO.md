**Mettre une version a l'ETL**  

>Version paire => développement  
>version impair => stable
<hr>


**Contruire une documentation README.MD**
>En cours
<hr>

**Format d'entrée a ajouter**  

><u>File</u>: XML, HTML, json, Excel  
><u>Databases</u>: (mysql,mariadb,h2sql,javaDderby,sqlserver,oracle, Access)
<hr>

**Transformer les données d’entrées et adapter au format destination**  

>Date, boolean, entier, chaines, flottant (selon db de destination)  
>script de modification (si v1 alors v2)  
>Remplacement par valeur une par defaut si champ vide (option)  
<hr>

**Mapping des données entre entrée et sortie (fieldsIn & FieldsOut)**  
<hr>

**Option pour ignorer les lignes défectueuses d'un fichier**  
>en oposition avec l'option checkfile. (connectorOutbound.ignoreErrors.value=(true/false))  
<hr>

**Traitement des fichiers entrant en mode Batch**  
>plusieurs lignes lues, traitement en mémoire  
>baisser le nombre d'accés disque.  
>Traitement de l'integration database en mode Batch  
>- *jobBatchMode=(true/false)*  
>- *jobBatchSize(integer)*  
<hr>

**ajouter d'autres base de données de destinations.**  
>(mysql,mariadb,sqlserver,oracle,javaderby,h2sql)
<hr>

**Option qui autorise les doublons (pas de contrôles via hashcode)**  
>(connectorOutbound.ignoreDuplicates.value=(true/false))  
<hr>

**Post Traitement via requête dédié (option, uniquement si définie)**  
>Doit pointer vers un fichiers de scripts SQL qui sera éxécuté en fin d'intégration  
>-*(SQLPostProcessing=filePath)*  
>Post traitement via script Java DSL potentiellement  
>-*(DSLPostProcessing=filePath)*  
<hr>
