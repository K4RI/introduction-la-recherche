package evaluationAPI;

import lpsolve.LpSolve;
import lpsolve.LpSolveException;
import solverAPI.LpsolveAPI;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Random;

/**
 * le plus proche de la vraie étude, identique à EvaluationAPI4 mais normalisé
 * init : restreindre TOUS les coûts dans S(0,1) = "x1+...+xn=1"
 * à chaque iter : coeffs entiers relatifs non-tous positifs, RHS nul
 * MRU est l'intersection d'un cône et de la sphère unité, càd une surface ""incurvée"" de dimension n-1
 */
public class EvaluationAPI4 extends AbstractEvaluationAPI {

    protected final int nMax;

    public EvaluationAPI4(int n, double C, boolean verb, int nMax) throws IOException, LpSolveException {
        super(n, C, verb);

        FileWriter myWriter = new FileWriter(this.evalFile, false);
        myWriter.write(initFile());
        myWriter.close();
        solver = new LpsolveAPI(evalFile, 0, verb);
        solver.createSolverFile();
        this.nMax = nMax;
    }

    /**
     * 1 contrainte pour borner MRU, n contraintes pour le cône polyhédral, et x initialisé dans l'intersection cône-sphère
     */
    public void initEvaluer() throws LpSolveException {
        Random r = new Random();

        for (int i = 1; i<=n; i++) { // initialiser xOptimal
            xOptimal[i-1]=1./n;
        }
        System.out.println("xOptimal initialisé à " + Arrays.toString(xOptimal));

        double[] c = new double[n+2]; // contrainte x1+...+xn=1 pour borner x sur la sphère
        Arrays.fill(c, 1);
        solver.lpSolver.addConstraint(c, LpSolve.EQ, c[n+1]);
        if (verb){
            System.out.println("Contrainte n°1 générée : " + strContrainte(c));
        }

        int solvecode = 2;
        int cpt = 0;

        while (solvecode!=0){
            if (cpt>0){
                for (int i = n+1; i >= 2; i--){
                    solver.lpSolver.delConstraint(i);
                }
            }
            boolean check=false;
            for(int i=1; i<=n; i++){
                while(!check){
                    c = randomContrainte();
                    check=checkConstr(c, xOptimal); // CONDITION xOptimal \in MRU
                }
                solver.lpSolver.addConstraint(c, LpSolve.LE, 0);
                if (verb){
                    System.out.println("Contrainte n°" + (i+1) + " générée : " + strContrainte(c));
                }
                check=false;
            }

            solvecode = solver.lpSolver.solve();
            // initialise x dans MRU : sur l'hypersphère et dans les contraintes (cône)
            x = solver.lpSolver.getPtrVariables();

            LpSolve altSolver = solver.lpSolver.copyLp();
            solver.emptyBounds(altSolver);
            double[] corner = new double[n];
            for(int i=0; i<=n-1; i++){ // CONDITION 2 : aucun coin de l'hypercube n'est dans MRU
                corner[i]=1;
                if (altSolver.isFeasible(corner, 0)){solvecode=2;break;}
                corner[i]=0;
            }
            cpt++;

        }
        System.out.println("Initialisation des contraintes OK (" + cpt + " essais)\n");
        System.out.println("x initialisé à " + Arrays.toString(x));
        x0 = x;
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
     * identique à la méthode de la classe abstraite, mais lance une exception après trop de boucles
     */
    public void addContrainte() throws Exception {
        boolean b = true;
        double[] c = new double[n + 2];
        int cpt = 0;
        while (b) {
            c = randomContrainte();
            b = (checkConstr(c, x) || !(checkConstr(c, xOptimal)));
            cpt++;
            if (cpt>5e5){
                throw new Exception("Boucle infinie");
            }
        }
        solver.lpSolver.addConstraint(c, LpSolve.LE, c[n + 1]);
        System.out.println("Contrainte n°" + solver.lpSolver.getNrows() + " générée en " + cpt + " essais : " + strContrainte(c));
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