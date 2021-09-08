package evaluationAPI;

import lpsolve.LpSolve;
import lpsolve.LpSolveException;
import solverAPI.LpsolveAPI;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Random;


public class EvaluationAPI extends AbstractEvaluationAPI {

    public EvaluationAPI(int n, double C, boolean verb) throws IOException, LpSolveException {
        super(n, C, verb);

        FileWriter myWriter = new FileWriter(this.evalFile, false);
        myWriter.write(initFile());
        myWriter.close();
        solver = new LpsolveAPI(evalFile, 0, verb);
        solver.createSolverFile();
    }

    /**
     * Initialise 1 contrainte pour borner MRU, xOptimal=0, et x sur S(0,C)
     */
    public void initEvaluer() throws LpSolveException {
        int solvecode = 2;
        int cpt = 0;

        while (solvecode!=0){
            if (cpt>0){
                solver.lpSolver.delConstraint(1);
            }

            Random r = new Random();
            double[] c = new double[n+2]; // contrainte à coeffs tous positifs pour borner MRU
            for (int i=1; i<=n+1; i++){
                c[i] = r.nextDouble();
            }
            solver.lpSolver.addConstraint(c, LpSolve.LE, c[n+1]);
            if (verb){
                System.out.println("Contrainte n°1 générée : " + strContrainte(c));
            }

            double[] lastConstr = new double[n+1];
            Arrays.fill(lastConstr, 1);
            solver.lpSolver.addConstraint(lastConstr, LpSolve.EQ, C);
            solvecode = solver.lpSolver.solve();
            // enlève la dernière contrainte x1+...+xn=C
            solver.lpSolver.delConstraint(2);
            cpt++;
        }
        System.out.println("Initialisation des contraintes OK (" + cpt + " essais)\n");
        // initialise x comme la solution : dans MRU et à distance C
        x = solver.lpSolver.getPtrVariables();
        System.out.println("x initialisé à " + Arrays.toString(x));
    }

    /**
     * @return une contrainte aléatoire de coefficients entre -1 et 1 et de RHS entre 0 et 1
     */
    public double[] randomContrainte() {
        Random r = new Random();
        double[] c = new double[n+2];
        // les n coeffs...
        for (int i=1; i<=n; i++){
            c[i] = 2*r.nextDouble() - 1;
        }
        // ...plus le terme rhs à la fin
        c[n+1]=r.nextDouble()*distanceManhattan(x);
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
        double binf;
        double bsup;
        double c;
        altSolver.setObjFn(new double[n+1]);

        for (int i=1; i<=n; i++){
            // System.out.println(":::::génération coût, variable n°" + i);
            altSolver.setMat(0, i, 1); // objectif xi
            if (i>1){
                altSolver.setMat(0, i-1, 0);
                altSolver.setBounds(i-1, cout[i-2], cout[i-2]);
            }
            altSolver.setMinim(); // objectif min
            altSolver.solve();
            binf = altSolver.getPtrVariables()[i-1];
            altSolver.setMaxim(); // objectif max
            altSolver.solve();
            bsup = altSolver.getPtrVariables()[i-1];
            // altSolver.printLp();
            if (rand) {
                Random r = new Random(); c = r.nextDouble();
            } else {
                c = 0.5; }
            cout[i-1]=binf+c*(bsup-binf);
        }

        if (rand){
            System.out.println("Coût aléatoire généré");
        } else {
            System.out.println("Centroïde généré");
        }
        return cout;
    }
}