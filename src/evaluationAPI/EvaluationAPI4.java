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
 * à chaque iter : coeffs entiers relatifs non-tous positifs, RHS nul
 * MRU est un cône convexe polyhédral
 */
public class EvaluationAPI4 extends AbstractEvaluationAPI {

    public EvaluationAPI4(int n, double C, boolean verb) throws IOException, LpSolveException {
        super(n, C, verb);

        FileWriter myWriter = new FileWriter(this.evalFile, false);
        myWriter.write(initFile());
        myWriter.close();
        solver = new LpsolveAPI(evalFile, 0, verb);
        solver.createSolverFile();
    }

    /**
     * Initialise les contraintes de départ, et xOptimal si nécessaire
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
        double[] cout = new double[n];
        double borne_inf;
        double borne_sup;

        // TODO : min x1, max x1 -> y[0] entre ces bornes (random ou médian)

        // TODO : pour i de 2 à numContrainte :
        //  ajouter en contrainte la composante choisie juste avant : "xi-1=y[i-2]"
        //  min xi, max xi -> y[i] entre ces bornes (random ou médian)

        if (rand){
            System.out.println("Coût aléatoire généré");
        } else {
            System.out.println("Centroïde généré");
        }

        return cout;
    }

}
