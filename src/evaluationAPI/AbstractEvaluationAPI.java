package evaluationAPI;

import lpsolve.LpSolve;
import lpsolve.LpSolveException;
import solverAPI.*;
import sun.awt.SunToolkit;

import java.util.Arrays;

public abstract class AbstractEvaluationAPI {
    protected final int n;
    protected final double C;
    protected final boolean verb;
    protected final double[] xOptimal;
    protected double[] x;
    protected double[] y;
    protected double[] z;
    protected double[] dx;
    protected double[] dy;
    protected double[] dz;
    protected AbstractSolverAPI solver;
    protected String evalFile = "src/evaluationAPI/evalAPI.lp";

    public AbstractEvaluationAPI(int n, double C, boolean verb) {
        this.n = n;
        this.C = C;
        this.verb = verb;
        xOptimal = new double[n];
        x = new double[n];
        y = new double[n];
        z = new double[n];
    }

    /**
     * Initialise les contraintes de départ, x, et xOptimal si nécessaire
     */
    public abstract void initEvaluer() throws LpSolveException;

    /**
     * Démarre l'évaluation de la méthode
     *
     * @param nbIter nombre d'itérations sur lesquelles vont être effectuées l'évaluation
     */
    public void evaluer(int nbIter) throws Exception {
        dx = new double[nbIter];
        dy = new double[nbIter];
        dz = new double[nbIter];

        initEvaluer(); // varie selon les variantes du protocole

        for (int i = 1; i <= n; i++) {
            solver.lpSolver.setBounds(i, x[i - 1], x[i - 1]);
        }
        solver.lpSolver.printLp();

        int cpt_xy = 0;
        int cpt_xz = 0;

        for (int i = 1; i <= nbIter; i++) {
            System.out.println("\n\n///////////////// ITERATION N°" + i + " /////////////////");
            try {
                addContrainte();
            }
            catch(Exception e){
                conclEvaluer(cpt_xy, cpt_xz, i);
                throw new Exception("Génération de nouvelle contrainte impossible");
            }
            // reprint tout MRU à chaque fois ?
            solver.run();
            solver.parseOutput(); // nécessairement cas 2.1
            x = solver.getNouvelleFctCout();

            y = getRandomOrCentroid(true);
            z = getRandomOrCentroid(false);

            dx[i - 1] = distanceManhattan(x);
            dy[i - 1] = distanceManhattan(y);
            dz[i - 1] = distanceManhattan(z);
            if (dx[i - 1] <= dy[i - 1]) {
                cpt_xy++;
            }
            if (dx[i - 1] <= dz[i - 1]) {
                cpt_xz++;
            }
            System.out.println("x = " + Arrays.toString(x));
            System.out.println("y = " + Arrays.toString(y));
            System.out.println("z = " + Arrays.toString(z));
            if (isNull(x)) {
                conclEvaluer(cpt_xy, cpt_xz, i);
                throw new NullPointerException("\n\nx est nul, suite du protocole impossible");
            }
            if (dx[i-1]<1e-15) {
                conclEvaluer(cpt_xy, cpt_xz, i);
                throw new NullPointerException("\n\nLes distances des coûts à xOptimal stagnent, continuation du protocole inutile");
            }

        }
        conclEvaluer(cpt_xy, cpt_xz, nbIter);
    }

    /**
     * Affiche les résultats de l'évaluation
     */
    private void conclEvaluer(int cpt_xy, int cpt_xz, int nbIter){

        System.out.println("\n\n::::::::::::::::: FIN DES ITERATIONS\n\nDistances x : " + Arrays.toString(dx));
        System.out.println("Distances y : " + Arrays.toString(dy));
        System.out.println("Distances z : " + Arrays.toString(dz));

        // afficher les fréquences de dx[i]<dy[i] et de dx[i]<dz[i]
        System.out.println("\nFréquences dx<dy : " + cpt_xy + "/" + nbIter + " (" + 100 * cpt_xy / nbIter + "%)");
        System.out.println("Fréquences dx<dz : " + cpt_xz + "/" + nbIter + " (" + 100 * cpt_xz / nbIter + "%)");
    }

    /**
     * Ajoute une nouvelle contrainte aléatoire à MRU telle que xOptimal est dans MRU mais pas x
     */
    public void addContrainte() throws LpSolveException {
        boolean b = true;
        double[] c = new double[n + 2];
        int cpt = 0;
        while (b) {
            c = randomContrainte();
            b = (checkConstr(c, x) || !(checkConstr(c, xOptimal)));
            cpt++;
            if (cpt>1e6){
                throw new SunToolkit.InfiniteLoop();
            }
        }
        solver.lpSolver.addConstraint(c, LpSolve.LE, c[n + 1]);
        System.out.println("Contrainte n°" + solver.lpSolver.getNrows() + " générée en " + cpt + " essais : " + strContrainte(c));
    }

    /**
     * @return une contrainte aléatoire
     */
    public abstract double[] randomContrainte();

    /**
     * @return une chaîne de caractères représentant une contrainte
     */
    public String strContrainte(double[] s) {
        StringBuilder t = new StringBuilder();
        for (int i = 1; i < n; i++) {
            t.append(s[i]).append("x").append(i).append(" + ");
        }
        t.append(s[n]).append("x").append(n).append(" <= ").append(s[n + 1]);
        return t.toString();
    }

    /**
     * @param rand si oui coût random, si non pseudo-centroïde
     * @return une fonction de coût aléatoire dans MRU, ou le centre de masse du polytope délimité par MRU
     */
    public abstract double[] getRandomOrCentroid(boolean rand) throws LpSolveException;

    /**
     * Calcule la distance de Manhattan entre la solution optimale et une fonction de coût
     *
     * @param w une fonction de coût
     */
    public double distanceManhattan(double[] w) {
        double res = 0;
        for (int i = 0; i < n; i++) {
            res += Math.abs(xOptimal[i] - w[i]);
        }
        return res;
    }

    /**
     * @return les premières lignes du fichier de travail
     */
    public String initFile() {
        StringBuilder s = new StringBuilder("/* Objective function */\nmin: ");
        for (int i = 1; i < n; i++) {
            s.append("x").append(i).append(" + ");
        }
        s.append("x").append(n).append(";\n");
        return s.toString();
    }


    /**
     * @return vrai si un tableau est rempli de zéros, faux sinon
     */
    private boolean isNull(double[] a) {
        for (double b : a) {
            if (b != 0) {
                return false;
            }
        }
        return true;
    }

    /**
     * @return vrai si un tableau est rempli de zéros, faux sinon
     */
    public boolean checkConstr(double[] c, double[] arr) {
        double sum = 0;
        for (int i = 0; i < n; i++) {
            sum += c[i + 1] * arr[i];
        }
        return (sum <= c[n + 1]);
    }
}
