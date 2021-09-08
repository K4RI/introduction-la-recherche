package evaluationAPI;

import lpsolve.LpSolve;
import lpsolve.LpSolveException;
import solverAPI.LpsolveAPI;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Random;

/**
 * le 2ème plus proche de la vraie étude
 * init : 1 seule contrainte à coeffs positifs pour bornesup la distance, xOptimal =/= 0
 * à chaque iter : coeffs entiers relatifs, RHS nul
 * MRU est un cône convexe polyhédral
 */
public class EvaluationAPI3 extends AbstractEvaluationAPI {

    private final int nMax; // nombre maximal des coefficients entiers (càd des applications des règles de transformation)

    public EvaluationAPI3(int n, double C, boolean verb, int nMax) throws IOException, LpSolveException {
        super(n, C, verb);

        FileWriter myWriter = new FileWriter(this.evalFile, false);
        myWriter.write(initFile());
        myWriter.close();
        solver = new LpsolveAPI(evalFile, 0, verb);
        solver.createSolverFile();
        this.nMax = nMax;
    }

    /**
     * 1 contrainte pour borner MRU, xOptimal aléatoire dans l'hypercube unité, et x sur S(xOptimal,C)
     */
    public void initEvaluer() throws LpSolveException {
        Random r = new Random();

        for (int i = 1; i<=n; i++) { // initialiser xOptimal
            xOptimal[i-1]=r.nextDouble();
            solver.lpSolver.setLowbo(i, xOptimal[i-1]);
        }
        System.out.println("xOptimal initialisé à " + Arrays.toString(xOptimal));

        // créer n contraintes d'entiers relatifs telles que chacune ait un coef >0 et un coef <0, MRU borné, et x \in MRU

        int solvecode = 2;
        int cpt = 0;

        while (solvecode!=0){
            if (cpt>0){
                for (int i = n+1; i >= 1; i--){
                    solver.lpSolver.delConstraint(i);
                }
            }

            double[] c = new double[n+2]; // contrainte à coeffs tous positifs pour borner MRU
            boolean check=false;
            while (!check){
                for (int i=1; i<=n+1; i++){
                    c[i] = r.nextDouble();
                }
                check=checkConstr(c, xOptimal); // CONDITION 0 : xOptimal est dans MRU
            }
            solver.lpSolver.addConstraint(c, LpSolve.LE, c[n+1]);
            if (verb){
                System.out.println("Contrainte n°1 générée : " + strContrainte(c));
            }
            check=false;

            for(int i=1; i<=n; i++){
                while(!check){
                    c = randomContrainte();
                    check=checkConstr(c, xOptimal); // CONDITION 0
                }
                solver.lpSolver.addConstraint(c, LpSolve.LE, 0);
                if (verb){
                    System.out.println("Contrainte n°" + (i+1) + " générée : " + strContrainte(c));
                }
                check=false;
            }

            Arrays.fill(c, 1);
            double sum = 0; for (int i=0;i<n;i++){sum+=xOptimal[i];}
            solver.lpSolver.addConstraint(c, LpSolve.EQ, C+sum); // x sur la sphère S(xOptimal, C)
            // (x1+...+xn=C+n <-> (x1-1)+...+(xn-1)=C & xi>1) ---> somme des xi-1 = C

            solvecode = solver.lpSolver.solve(); // CONDITION 1 : MRU ^ S(xOptimal, C) est non-vide
            // initialise x comme la solution : dans MRU et à distance C
            x = solver.lpSolver.getPtrVariables();
            solver.lpSolver.delConstraint(n+2);

            LpSolve altSolver = solver.lpSolver.copyLp();
            solver.emptyBounds(altSolver);
            double[] corner = new double[n+1];
            for(int i=0; i<=n-1; i++){ // CONDITION 2 : aucun coin de l'hypercube n'est dans MRU
                corner[i]=1;
                if (altSolver.isFeasible(corner, 0)){solvecode=2; break;}
                corner[i]=0;
            }
            cpt++;

        }
        System.out.println("Initialisation des contraintes OK (" + cpt + " essais)\n");
        System.out.println("x initialisé à " + Arrays.toString(x));
        // le MRU est un cône polyhédral borné
    }

    /**
     * @return une contrainte aléatoire de coefficients entiers entre -10 et 10 et de RHS nul
     */
    public double[] randomContrainte() {
        Random r = new Random();
        double[] c = new double[n+2];
        boolean isThereNeg=false;
        boolean isTherePos=false;
        while(!(isThereNeg & isTherePos)){ // possède un coef >0 et un coef <0
            isThereNeg=false;
            isTherePos=false;
            for (int i=1; i<=n; i++){
                c[i] = r.nextInt(2*nMax+1) - nMax;
                if (c[i]>0){isTherePos=true;}
                if (c[i]<0){isThereNeg=true;}
            }
        }
        c[n+1]=0;

        return c;
    }

    /**
     * @param rand si oui coût random, si non pseudo-centroïde
     * @return une fonction de coût aléatoire dans MRU, ou le centre de masse du polytope délimité par MRU
     */
    public double[] getRandomOrCentroid(boolean rand) throws LpSolveException {
        LpSolve altSolver = solver.lpSolver.copyLp(); // solveur ayant pour contraintes le MRU
        solver.emptyBounds(altSolver);
        double[] cout = new double[n];
        double bornInf;
        double bornSup;
        double c;
        altSolver.setObjFn(new double[n+1]);

        for (int i=1; i<=n; i++){
            // System.out.println(":::::génération coût, variable n°" + i);
            altSolver.setMat(0, i, 1); // l'objectif est sur xi
            if (i>1){
                altSolver.setMat(0, i-1, 0); // mais pas sur les autres composantes
                altSolver.setBounds(i-1, cout[i-2], cout[i-2]); // avec une condition sur les composantes déjà calculées
            }
            altSolver.setMinim(); // objectif min xi
            altSolver.solve();
            bornInf = altSolver.getPtrVariables()[i-1];
            altSolver.setMaxim(); // objectif max xi
            altSolver.solve();
            bornSup = altSolver.getPtrVariables()[i-1];
            if (rand) {
                Random r = new Random(); c = r.nextDouble();
            } else {
                c = 0.5; }
            cout[i-1]=bornInf+c*(bornSup-bornInf); // xi au hasard ou médian entre ces deux bornes
        }

        if (rand){
            System.out.println("Coût aléatoire généré");
        } else {
            System.out.println("Centroïde généré");
        }
        return cout;
    }
}