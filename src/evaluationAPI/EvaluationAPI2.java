package evaluationAPI;

import lpsolve.LpSolve;
import lpsolve.LpSolveException;
import solverAPI.LpsolveAPI;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Random;


public class EvaluationAPI2 extends AbstractEvaluationAPI {

    public EvaluationAPI2(int n, double C, boolean verb) throws IOException, LpSolveException {
        super(n, C, verb);

        FileWriter myWriter = new FileWriter(this.evalFile, false);
        myWriter.write(initFile());
        myWriter.close();
        solver = new LpsolveAPI(evalFile, 0, verb);
        solver.createSolverFile();
    }

    /**
     * Place les bornes à ]-inf,+inf[, initialise n+1 contraintes bornant MRU, xOptimal=0, et x sur S(0,C)
     */
    public void initEvaluer() throws LpSolveException {
        int solvecode = 2;
        int boundcode_m = 3;
        int boundcode_p = 3;
        int cpt = 0;

        emptyInfBounds(solver.lpSolver);

        while (solvecode+boundcode_m+boundcode_p != 0){
            if (cpt>0){
                for (int i = n+1; i >= 1; i--){
                    solver.lpSolver.delConstraint(i);
                }
            }
            for(int i=1; i<=n+1; i++){
                double[] c = randomContrainte();
                solver.lpSolver.addConstraint(c, LpSolve.LE, c[n+1]);
                if (verb){
                    System.out.println("Contrainte n°" + i + " générée : " + strContrainte(c));
                }
            }
            double[] lastConstr = new double[n+1];
            Arrays.fill(lastConstr, 1);
            solver.lpSolver.addConstraint(lastConstr, LpSolve.EQ, C);
            solvecode = solver.lpSolver.solve();
            // initialise x comme la solution : dans MRU et à distance C
            x = solver.lpSolver.getPtrVariables();
            solver.lpSolver.delConstraint(n+2);
            boundcode_m = solver.lpSolver.solve();
            solver.lpSolver.setMaxim();
            boundcode_p = solver.lpSolver.solve();
            solver.lpSolver.setMinim();
            if (verb){
                System.out.println("code solvabilité : " + solvecode);
                System.out.println("codes bornes : " + boundcode_m + " " + boundcode_p + "\n");
            }
            cpt++;
        }
        System.out.println("Initialisation des contraintes OK (" + cpt + " essais)\n");
        System.out.println("x initialisé à " + Arrays.toString(x));
    }

    /**
     * @return une contrainte aléatoire
     */
    public double[] randomContrainte() {
        Random r = new Random();
        double[] c = new double[n+2];
        // les n coeffs entre -1 et 1...
        for (int i=1; i<=n; i++){
            c[i] = 2*r.nextDouble() - 1;
        }
        // ...plus le rhs entre 0 et 1 à la fin
        c[n+1]=r.nextDouble();
        return c;
    }

    /**
     * @param rand si oui coût random, si non pseudo-centroïde
     * @return une fonction de coût aléatoire dans MRU, ou le centre de masse du polytope délimité par MRU
     */
    public double[] getRandomOrCentroid(boolean rand) throws LpSolveException {
        LpSolve altSolver = solver.lpSolver.copyLp(); // solveur ayant pour contraintes le MRU
        emptyInfBounds(altSolver);
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

    private void emptyInfBounds(LpSolve s) throws LpSolveException {
        for (int i = 1; i <= s.getNcolumns(); i++) {
            s.setBounds(i, -s.getInfinite(), s.getInfinite());
        }
    }
}