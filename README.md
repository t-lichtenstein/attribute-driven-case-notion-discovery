# Attribute-driven Case Notion Discovery

Discovers a case notion for an unlabeled event log using optional attributes and produces an XES-file that can be used for process mining purposes.

Run the precompiled JAR-executable by calling:
```
java -jar entity-aware-case-notion-detection.jar -i [INPUT EVENT LOG AS CSV-FILE] -s [CSV COLUMN SEPARATOR] -o [OUTPUT DIRECTORY]
```

Each line of the unlabeled event log taken as input is treated as an event. The first column of the CSV file of the event log must contain a UNIX timestamp (milliseconds) for each event, while the second column must contain the corresponding activity identifier of the event. All following columns are treated as optional attributes without any specific requirements. The first line of the CSV file is not parsed; thus, the file can contain a header.

The tool displays artifacts during the execution, for example, a structure for the optional artifacts:

```
ObjectClass_0:
  - subject_id
  - gender
  - dod

ObjectClass_1: 
  - admission_location
  - hadm_id
```

Each object class represents an entity class that is described with the attributes assigned to it. Additionally, life cycles graphs are generated for the object classes in the form of adjacency matrices, e.g.:

```
o: START
1: Started Transferring Patient
2: Stopped Transferring Patient
x: END

      o    1    2    x  
    --------------------
o  |  -   1.00  -    -  
1  |  -    -   0.75 0.25
2  |  -    -    -   1.00
x  |  -    -    -    -  
```

A matrix displays the transition probability (0 to 1, while 0 is displayed as "-") between each pair of activities (including a special START and END stage) from the left column attribute to the top row attribute.

The tool will ask for a root object class that determines the traces, which can be entered using the terminal. A selection of possible root object classes will be displayed. The object class life cycles can be used to determine a suitable root object class. Finally, the XES-event log file is generated in the specified directory.

## Requirements

For the tool to run, a working installation of <mark>Java 8</mark> is required. The execution benefits from the presence of multiple CPU cores. For development, an installation of Maven is mandatory, and the openXES library has to be included (a JAR file is provided in the resources directory). We recommend using IntelliJ IDEA 2021.1 or newer.
