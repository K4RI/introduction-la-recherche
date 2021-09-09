# Linear Programming Solver Interface

Ce projet est une interface faisant le lien entre un programme linéaire et un solveur.
Il a été créé dans le cadre d'un projet visant à réestimer des coûts de transformations de requête. 
En entrée est fournie une fonction de coût qui attribue une valeur à chaque transformation de requête, ainsi qu'un ensemble d'inégalités. Elles correspondent à des contraintes d'utilisateur(s) sur l'ordre de priorité des types de modifications d'une requête.
Le but est de trouver une fonction de coût satisfaisant toutes ces contraintes, quitte à réduire l'ensemble d'inégalités, tout en restant proche de celle fournie en entrée. La nouvelle configuration (fonction de coût + inégalité) est alors réécrite dans un fichier dont la syntaxe est similaire à l'entrée.

### Prérequis

Logiciel(s) nécessaires au fonctionnement du projet :

```
Lp Solve
```

Version du sdk utilisé :

```
Java 1.8
```

### Solveurs disponibles

Liste des différents solveurs compatibles avec l'interface :

* lp_solve version 5.5.2.11

## Exécution

Ligne de commande pour lancer l'archive jar :

```
java -jar jarName.jar solver file_path verbose
```

*[solver] doit être remplacé par un des solveurs de la liste "Solveurs disponibles".*
*[verbose] est un nombre entier situé entre 0 et 6 inclus*


Ligne de commande pour lancer l'évaluation :

```
java -jar jarName.jar eval typeEval distInit nbreVariables nbreIter verbose
```

*[typeEval] doit être remplacé par la variante souhaitée, entre 0 et 4 inclus*


## Format de fichier
Le fichier doit être un fichier texte au format pour être utilisé avec lp_solve :
```
min: x1+[...]+xp // Resp. max, le type d'optimisation à effectuer

ci: xj >= ai; // Resp. <= ai, selon le type de contrainte
// à répéter pour toutes les contraintes du multi-ensemble

xj = bj;
// à répéter pour toutes les variables de la fonction de coût
```

## Explication des programmes

La classe AbstractSolver permet de regrouper les méthodes communes aux différents solveurs,
comme la méthode.
La classe Lpsolve contient les méthodes plus spécifiques au solveur lp_solve. Cette classe
contient les méthodes permettant de créer un fichier lp avec les informations de base. Elle
contient aussi les méthodes permettant d'analyser les différents fichiers, de calculer la plus
courte distance, de rendre cohérent un multi-ensemble de contraintes.


## Exemple d'exécution

Exemple d'éxécution sur le fichier test.txt avec le serveur lp_solve :
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

On exécute la commande ``java -jar jarName.jar test.txt 0``, 
ce qui donne en sortie : 
```
min: x1 + x2;

c1: x1 >= 1;
c2: x1 <= 3;
c3: x2 >= 1;
c4: x2 <= 3;

x1 = 5;
x2 = 2;
```

On détecte que la fonction de coût donnée dans le fichier test.txt ne satisfait pas les contraintes utilisateur.
Il est alors calculé une nouvelle fonction de coût, qui correspond à l'ensemble des variables y<sub>i</sub>.

## Documentation

* [Lp Solve](http://lpsolve.sourceforge.net/5.5/lp_solve.htm) - options pour Lp Solve
