/* Objective function */
min: x1 + x2 + x3;

/* Constraints - MRU */
c1: x1 + x2 >= x3;
c2: x1 <= 3;
c3: x2 >= 1;
c4: x2 <= 3;

/* Variable bounds - Fonction de coût */
x1 = 2;
x2 = 2;
x3 = 1;