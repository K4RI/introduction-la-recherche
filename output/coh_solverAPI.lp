/* Problème 2.1 */

/* Objective function */
min: +z1 +z2;

/* Constraints */
c1: +y1 >= 1.001;
c2: +y1 <= 2.999;
c3: +y2 >= 1.001;
c4: +y2 <= 2.999;
-y1 +z1 >= -5;
+y1 +z1 >= 5;
-y2 +z2 >= -2;
+y2 +z2 >= 2;
