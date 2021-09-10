# Linear Programming Solver Interface

Ce projet est une interface faisant le lien entre un programme linéaire et un solveur. Il a été créé dans le cadre d'un projet visant à réestimer des coûts de transformations de requête.  

En entrée est fournie une fonction de coût qui attribue une valeur à chaque transformation de requête, ainsi qu'un multi-ensemble d'inégalités (aussi appelé MRU). Elles correspondent à des contraintes d'utilisateur(s) sur l'ordre de priorité des types de modifications d'une requête.  

Le but est de trouver une fonction de coût satisfaisant toutes ces contraintes, quitte à réduire l'ensemble d'inégalités, tout en restant proche de celle fournie en entrée. La nouvelle configuration (fonction de coût + inégalités) est alors réécrite dans un fichier dont la syntaxe est similaire à l'entrée.  

### Prérequis

Logiciel(s) nécessaires au fonctionnement du projet :

```
Lp Solve *(étapes d'installation dans "Notes techniques.doc")*  
JUnit 5.7.0  
```

Version du SDK utilisé :

```
Java 1.8
```

### Solveurs disponibles

Liste des différents solveurs compatibles avec l'interface :

* lp_solve version 5.5.2.11

## Exécution

### Ligne de commande pour lire un fichier :

```
java -jar jarName.jar [solver] [file_path] [options]
```

- **[solver]** doit être remplacé par un des solveurs de la liste "Solveurs disponibles".  
- **[file_path]** est le chemin vers le fichier décrivant la configuration    
- **[options]** décrit la verbose de lp_solve en un nombre entier situé entre 0 et 6 inclus *(0=NEUTRAL, 1=CRITICAL, 2=SEVERE, 3=IMPORTANT, 4=NORMAL, 5=DETAILED, 6=FULL)*

### Ligne de commande pour lancer une évaluation :

```
java -jar jarName.jar eval [typeEval] [distInit] [nbreVariables] [nbreIter] [verbose]
```

- **[typeEval]** doit être remplacé par la variante souhaitée, entre 0 et 4 inclus *(0=procédure standard, 1=variables non-bornées, 2=x\* non-nul, 3=coefficients entiers et RHS nul, 4=variante 3 normalisée)*   
- **[distInit]** est la distance au coût optimal à laquelle x0 est initialisé  
- **[nbreVariables]** est le nombre de colonnes du problème  
- **[nbreIter]** est le nombre maximal d'itérations à prévoir
- **[verbose]** est un booléen "true" ou "false" pour choisir si l'on veut détailler le processus en console  


## Format de fichier
Le fichier doit être un fichier .lp et respecter cette syntaxe pour être utilisé avec lp_solve :
```
min: x1 +...+ xp // Resp. max, le type d'optimisation à effectuer

ci: ai1*x1 +...+ aip*xp >= ai; // Resp. <= ai, selon le type de contrainte
// à répéter pour toutes les contraintes du multi-ensemble

xj = bj;
// à répéter pour toutes les variables de la fonction de coût
```

## Explication des programmes

La classe AbstractSolverAPI et son extension LpsolveAPI regroupent les méthodes de détection de cas *(parseOutput() sur la cohérence de la fonction, retryLpFile() sur la cohérence de MRU)* et leur résolution *(findShortestDistance() réestime un coût, fixMRU() réduit un multi-ensemble incohérent)*.  

LpsolveAPITest regroupe les tests unitaires.  

EvaluationAPI contient une classe abstraite et six procédures d'évaluation différentes les unes des autres. Elle compare la méthode de lp_solve avec d'autres.  

Les classes ne contenant pas le mot "API" proviennent d'une ancienne version du dépôt, dont les seules modifications depuis ont été les corrections de bugs.  


## Exemple d'exécution

Exemple d'exécution sur le fichier test.lp avec le serveur lp_solve :
```
min: x1 + x2;

c1: x1 >= 5;
c2: x1 >= 5;
c3: x1 <= 3;
c4: x2 >= 5;
c5: x2 <= 3;

x1 = 5;
x2 = 2;
```

On exécute la commande ``java -jar jarName.jar lp_solve ./src/test.lp 0``, 
ce qui donne en sortie : 
```
min: x1 + x2;

c3: x1 <= 3;
c5: x2 <= 3;

x1 = 3;
x2 = 2;
```

On détecte que le MRU donné dans le fichier test.lp n'est pas cohérent donc corrigé, puis la fonction de coût est réestimée.

## Documentation

* [LpSolve](http://web.mit.edu/lpsolve_v5520/doc/Java/docs/api/index.html) - routines Java pour LpSolve
* [LpSolve](http://lpsolve.sourceforge.net/5.5/Java/docs/reference.html) - correspondance avec les routines C
* [LpSolve](http://lpsolve.sourceforge.net/5.5/lp_solveAPIreference.htm) - description détaillée des routines C
