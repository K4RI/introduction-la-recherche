package evaluationAPI;

import lpsolve.LpSolve;
import lpsolve.LpSolveException;
import solverAPI.AbstractSolverAPI;
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
public class EvaluationAPI2 {

    private final int n;
    private final double C;
    private final double[] xOptimal;
    private double [] x;
    private double[] y;
    private double[] z;
    private double [] dx;
    private double [] dy;
    private double [] dz;
    private AbstractSolverAPI solver;
    private String evalFile = "src/evaluationAPI/evalAPI.lp";

    public EvaluationAPI2(int n, double C) throws IOException, LpSolveException {
        this.n = n;
        this.C = C;
        xOptimal = new double[n];
        x = new double[n];
        y = new double[n];
        z = new double[n];

        FileWriter myWriter = new FileWriter(evalFile, false);
        myWriter.write(initFile());
        myWriter.close();
        solver = new LpsolveAPI(evalFile, 0);
        solver.createSolverFile();
    }

    /**
     * Démarre l'évaluation de la méthode
     * @param nbIter nombre d'itérations sur lesquelles vont être effectuées l'évaluation
     */
    public void evaluer(int nbIter) throws LpSolveException {
        dx = new double[nbIter];
        dy = new double[nbIter];
        dz = new double[nbIter];

        // initialise les n contraintes du MRU dans solver, plus une contrainte x1+...+xn=C
        int code = 2;
        int cpt = 0;

        // TODO: : 1 seule contrainte, telle que max x1+...+xn n'est pas unbounded PUIS que x1+...+xn=C faisable

        // TODO : en vrai repenser le bullshit "x*=0", un optimal dans un coin c'est totalement biaisé en défaveur du x au bord
        // TODO : un x au centre d'un MRU de n+1 contraintes pré-initialisées ? ou juste en (1,..., 1) ?
        // TODO : dans ce cas faire de vrais checks pour la génération des contraintes, car - évident que x*=0

        while (code != 0){
            if (cpt>0){
                for (int i = 1+1; i >= 1; i--){
                    solver.lpSolver.delConstraint(i);
                }
            }
            for(int i=1; i<=1; i++){
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

        // initialise x comme la solution : dans MRU et à distance C
        x = solver.lpSolver.getPtrVariables();
        // enlève la dernière contrainte x1+...+xn=C
        solver.lpSolver.delConstraint(n+1);
        solver.lpSolver.printLp();


        int cpt_xy = 0;
        int cpt_xz = 0;

        for(int i=1; i<=nbIter; i++){
            addContrainte(n+i);
            // reprint tout MRU à chaque fois ?
            solver.run();
            solver.parseOutput(); // nécessairement cas 2.1
            x = solver.getNouvelleFctCout();
            y = randomCout(n+i);
            z = getCentroid();

            dx[i]=distanceManhattan(x);
            dy[i]=distanceManhattan(y);
            dz[i]=distanceManhattan(z);
            if (dx[i]<=dy[i]){
                cpt_xy++;
            }
            if (dx[i]<=dz[i]){
                cpt_xz++;
            }
            System.out.println("ITERATION N°" + i);
            System.out.println("x = " + Arrays.toString(x));
            System.out.println("y = " + Arrays.toString(y));
            System.out.println("z = " + Arrays.toString(z));
            System.out.println("_________\n");
        }
        System.out.println("Distances x : " + Arrays.toString(dx));
        System.out.println("Distances y : " + Arrays.toString(dy));
        System.out.println("Distances z : " + Arrays.toString(dz));

        // afficher les fréquences de dx[i]<dy[i] et de dx[i]<dz[i]
        System.out.println("Fréquences dx<dy : " + cpt_xy + "/" + nbIter + "(" + 100*cpt_xy/nbIter + "%)");
        System.out.println("Fréquences dx<dz : " + cpt_xz + "/" + nbIter + "(" + 100*cpt_xz/nbIter + "%)");
    }

    /**
     * Ajoute une nouvelle contrainte aléatoire à MRU telle que xOptimal est dans MRU mais pas x
     */
    private void addContrainte(int numContrainte) throws LpSolveException {
        boolean b = true;
        double[] c = new double[n+2];
        int cpt = 0;
        while (b){
            // c0x0 + ... + cn-1xn-1 <= cn, vrai si x=0 et cn>=0
            c = randomContrainte();
            double sum = 0;
            for (int i=0; i<n; i++){
                sum += c[i+1]*x[i];
            }
            b = (sum<=c[n+1]);
            cpt++;
        }
        // on s'est arrêté avec c.x>cn, donc x ne respectant pas la contrainte
        solver.lpSolver.addConstraint(c, LpSolve.LE, c[n]);
        System.out.println("Contrainte n°" + numContrainte + " générée en " + cpt + " essais : " + strContrainte(c));
    }

    /**
     * @return une contrainte aléatoire
     */
    private double[] randomContrainte() {
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
     * @return une chaîne de caractères représentant une contrainte
     */
    private String strContrainte(double[] s){
        StringBuilder t = new StringBuilder();
        for (int i=1; i<n; i++){
            t.append(s[i]).append("x").append(i).append(" + ");
        }
        t.append(s[n]).append("x").append(n).append(" <= ").append(s[n+1]);
        return t.toString();
    }

    /**
     * @param numContrainte nombre de contraintes à ce stade dans MRU
     * @return une fonction de coût aléatoire qui satisfait MRU
     */
    private double[] randomCout(int numContrainte) throws LpSolveException {
        LpSolve ysolver = solver.lpSolver.copyLp(); // solveur ayant pour contraintes le MRU
        double[] randCout = new double[n];
        double borne_inf;
        double borne_sup;



        // TODO : min x1, max x1 -> y[0] entre ces bornes

        // TODO : pour i de 2 à numContrainte :
            // TODO : ajouter en contrainte la composante choisie juste avant : "xi-1=y[i-2]"
            // TODO : min xi, max xi -> y[i] entre ces bornes

        return randCout;
    }

    /**
     * @return le centre de masse du polytope délimité par MRU
     */
    private double[] getCentroid(){
        int cpt = 0;
        // TODO :  cf. les nombreuses méthodes très compliquées, ou pseudo-centroïde

        System.out.println("Coût aléatoire généré au bout de "+cpt+" essais");
        return new double[n];
    }

    /**
     * Calcule la distance de Manhattan entre la solution optimale et une fonction de coût
     * @param w une fonction de coût
     */
    private double distanceManhattan(double[] w){
        double res = 0;
        for(int i=0; i<n; i++){
            res += Math.abs(xOptimal[i]-w[i]);
        }
        return res;
    }

    /**
     * @return les premières lignes du fichier de travail
     */
    private String initFile(){
        StringBuilder s = new StringBuilder("/* Objective function */\nmin: ");
        for (int i=1; i<n; i++){
            s.append("x").append(i).append(" + ");
        }
        s.append("x").append(n).append(";\n");
        return s.toString();
    }

}
