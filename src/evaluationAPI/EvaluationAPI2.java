package evaluationAPI;

import lpsolve.LpSolve;
import lpsolve.LpSolveException;
import solverAPI.LpsolveAPI;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Random;

/**
 * Identique à EvaluationAPI, mais xOptimal = (1, ..., 1)
 * adapter les générations de contraintes
 * init : 1 seule contrainte à coeffs positifs pour bornesup la distance
 * à chaque iter : contrainte vérifiée par x* mais pas par x, coeffs non-tous négatifs
 */
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
     * Initialise les contraintes de départ et xOptimal si nécessaire
     */
    public void initEvaluer() throws LpSolveException {
        // initialise les n contraintes du MRU dans solver, plus une contrainte x1+...+xn=C
        int code = 2;
        int cpt = 0;

        while (code != 0){
            if (cpt>0){
                for (int i = n+1; i >= 1; i--){
                    solver.lpSolver.delConstraint(i);
                }
            }
            for(int i=1; i<=n; i++){
                double[] c = randomContrainte();
                solver.lpSolver.addConstraint(c, LpSolve.LE, c[n+1]);
                System.out.println("Contrainte n°" + i + " générée : " + strContrainte(c));
            }
            double[] lastConstr = new double[n+1];
            Arrays.fill(lastConstr, 1);
            solver.lpSolver.addConstraint(lastConstr, LpSolve.EQ, C);
            code = solver.lpSolver.solve();
            System.out.println("code solvabilité : " + code);
            cpt++;
        }
        System.out.println("Initialisation des contraintes OK (" + cpt + " essais)\n");
    }

    /**
     * @return une contrainte aléatoire
     */
    public double[] randomContrainte() {
        Random r = new Random();
        double[] c = new double[n+2];
        // les n coeffs...
        for (int i=1; i<=n; i++){
            c[i] = 2*r.nextDouble() - 1;
        }
        // ...plus le terme rhs à la fin
        c[n+1]=r.nextDouble();
        return c;
    }

    /**
     * @param numContrainte nombre de contraintes à ce stade dans MRU
     * @param rand si oui coût random, si non pseudo-centroïde
     * @return une fonction de coût aléatoire dans MRU, ou le centre de masse du polytope délimité par MRU
     */
    public double[] getRandomOrCentroid(int numContrainte, boolean rand) throws LpSolveException {
        LpSolve altSolver = solver.lpSolver.copyLp(); // solveur ayant pour contraintes le MRU
        int p = altSolver.getNrows(); // nombre de contraintes à ce stade dans MRU
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