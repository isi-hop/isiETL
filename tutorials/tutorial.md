<center><h2>IsiETL TUTORIALS</h2></center>  

**First example** 

We're going to go to the `$ISIETL_HOME/tutorials/tuto_1` folder, which contains the first example of a csv file we're going to integrate into a PosgreSQL database.

This csv file is fairly basic, containing dummy data in 4 columns and 12 rows with no header.

_Extract from file_  
`tuto_1_no_head_4_col.csv`  

|C1|C2|C3|C4|
|-----|-----|-----|-----|
|col11|col12|col13|col14|
|col21|col22|col23|col24|
|col31|col32|col33|col34|
|col41|col42|col43|col44|
|col51|col52|col53|col54|
|col61|col62|col63|col64|
|col71|col72|col73|col74|
|col81|col82|col83|col84|
|col91|col92|col93|col94|
|col101|col102|col103|col104|
|col111|col112|col113|col114|
|col121|col122|col123|col124|  

The aim of this tutorial is to automatically integrate the data from columns C1 to C4 into a table we'll call `tabletest`, using the default varchar type for each column, with a maximum length of 6 characters.

**Let's get started!**  

Let's start by creating a job file template.  

on the CLI, run the command 
$> `isietl.sh -jt`.

You'll get the file `ìntegrator_template.yml` in the local folder, rename it `integrator_tuto_1.yml`.  

$> `mv integrator_template.yml integrator_tuto_1.yml`  

> - In what follows, we'll assume that you've installed IsiETL in the `$HOME\isietl` folder. In the files that follow, you'll replace the $HOME path with the absolute path of your personal installation.  
> - We'll also assume that we've launched a postgresql database on the workstation, ideally under docker, see the following link on how to launch a postgresql database under docker .  
> - We'll also assume that when we start postgresql, we'll have created a database named `tuto1`  
> - Finally, to set up your database login and password, you'll need to adapt the following information to your configuration.  

[How to use the docker Postgresql official image](https://www.docker.com/blog/how-to-use-the-postgres-docker-official-image/)  

Edit the `integrator_tuto_1.yml` file with your favorite text file editor, like this : 

``` yaml  
#-------------HEADER---------------
jobName: "tuto_1 integrator process"
jobDescription: "Get data from a CSV file : push a DB postgresql"
jobDateTime: "2025-06-11 07:45"
jobBatchSize: "6"
forceIntermediateCommit: "true"
#----------------------------------------
#------------INBOUND CONNECTOR-----------
connectorInbound:
  connectortype: 
    value: "file"
  filespath: 
    value : "$HOME/isiETL/tutorial/tuto_1"
  checkfiles: 
    value: "true"
  backupdestinationpath:
    value: "$HOME/isiETL/tutorial/backup"
  exttype:
    value: "csv"    
  nbfields:
    value: "4"
#----------------------------------------
#------------OUTBOUND CONNECTOR----------
connectorOutbound: 
  connectortype:
    value: "database"
  dbdriver:
    value: "org.postgresql.Driver"
  dburl:
    value: "jdbc:postgresql://localhost:5432/tuto1"
  dblogin:
    value: "postgres"
  dbpassword:
    value: "admin"
  targetTable:
    value: "tabletest"    
  ignoreErrors: 
    value: "false"
  ignoreDuplicates:
    value: "false"

fieldsOut:
   colname1:
    defaultValue: ""
    size: "6"
    type: "varchar"
  colname2:
    defaultValue: ""
    size: "6"
    type: "varchar"
  colname3:
    defaultValue: ""
    size: "6"
    type: "varchar"
  colname4:
    defaultValue: ""
    size: "6"
    type: "varchar"
#-------------------------------------
#-----------FMT PROCESSING------------
filteringScript: ""
mappingScript: ""
transformerScript: ""
#-------------------------------------

#-----------POSTPROCESSING------------
SQLPostProcessing: ""
#-------------------------------------
```  

**Let's unravel the file above!**  

**_First block of information, the header_**  

``` YAML
#-------------HEADER---------------
jobName: "tuto_1 integrator process"
jobDescription: "Get data from a CSV file : push a DB postgresql"
jobDateTime: "2025-06-11 07:45"
jobBatchSize: "6"
forceIntermediateCommit: "true"
#----------------------------------------
```  

The variables, `jobName, jobDescription, jobDateTime` have no impact on the operation of your job and are variables that will force you to comment on your job.  

On these variables you assign the information you feel is relevant to describe the job.

The `jobBatchSize` variable is imperative and will enable you to adjust the number of lines per processing group written to the database.  

This value must be adjusted according to the potential size of the source file. Sometimes the number of source lines is fixed, sometimes it's quite variable; ideally, this value should be for a fixed number of lines <= the maximum number of lines.  

For large files, you need to choose the number of lines according to your commit requirements, bearing in mind that the more commits there are during processing, the longer the process will take, so 
you need to find the right compromise.  

Be careful, this value must be within the range [1..65535].  

Finding the right value is often a matter of trial and error.  

In our case, we'll set it to 6, which will cause 2 commits on this job, which isn't much.  

If you want 1 commit per integration, in this case you need to set this value to 1.  

Here, it's often a matter of trial and error to find the right value.  
In our case, we're going to set it to 6, which will result in 2 commits on this job, which isn't much.  
If you want 1 commit by integration, then you need to set this value to 1.  

The `forceIntermediateCommit` variable takes the value true or false. A true value will force intermediate commits.  

A false value means no intermediate commits, in other words, a single commit is made in this case at the end of the integration of all lines in batch mode. It's like setting the jobBatchSize value to the number of lines in the source file.  

🚧️Under construction🚧️  

**_Incoming connector in CSV format_**  


**_Outgoing connector in BDD postgresql format_**  


**_Destination fields description_**  


**FMT processing_**  


**_POST SQL processing_**





