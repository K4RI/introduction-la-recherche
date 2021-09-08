package evaluationAPI;

import lpsolve.LpSolve;
import lpsolve.LpSolveException;
import solverAPI.AbstractSolverAPI;
import sun.awt.SunToolkit;

import java.util.Arrays;

public abstract class AbstractEvaluationAPI {
    protected final int n;
    protected final double C;
    protected final boolean verb;
    protected final double[] xOptimal;
    protected double[] x0;
    protected double[] x;
    protected double[] y;
    protected double[] z;
    protected double[] dx;
    protected double[] dy;
    protected double[] dz;
    protected double Py;
    protected double Pz;
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
    public int evaluer(int nbIter) throws Exception {
        dx = new double[nbIter];
        dy = new double[nbIter];
        dz = new double[nbIter];
        double[] xprec;

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
                e.printStackTrace();
                System.err.println("\n\nErreur: Génération de nouvelle contrainte impossible");
                nbIter=i;
                break;
            }
            // reprint tout MRU à chaque fois ?
            solver.run();
            try{
                solver.parseOutput(); // nécessairement cas 2.1
            } catch(Exception e){
                e.printStackTrace();
                System.err.println("\n\nErreur: optimisation de x");
                nbIter=i;
                break;
            }

            xprec = x;
            x = solver.getNouvelleFctCout();
            if(xprec==x){
                System.err.println("^^^^IDENTIQUE^^^^");
                nbIter=i;
                break;
            }
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
            if (solver.isNull(x)) {
                System.err.println("\n\nErreur: x est nul, suite du protocole impossible");
                nbIter=i;
                break;
            }
            /*if (dx[i-1]<1e-15 || dy[i-1]<1e-15 || dz[i-1]<1e-15 || Arrays.equals(xprec, x)) {
                System.err.println("\n\nLes distances des coûts à xOptimal stagnent, continuation du protocole inutile");
                nbIter=i;
                break;
            }*/
        }
        conclEvaluer(cpt_xy, cpt_xz, nbIter);
        return nbIter;
    }

    /**
     * Affiche les résultats de l'évaluation
     */
    private void conclEvaluer(int cpt_xy, int cpt_xz, int nbIter){

        System.out.println("\n\n::::::::::::::::: FIN DES ITERATIONS\n\nDistances x : " + Arrays.toString(dx));
        System.out.println("Distances y : " + Arrays.toString(dy));
        System.out.println("Distances z : " + Arrays.toString(dz));

        // afficher les fréquences de dx[i]<dy[i] et de dx[i]<dz[i]
        Py = Math.round((double) 100 * cpt_xy / nbIter) / 100.;
        Pz = Math.round((double) 100 * cpt_xz / nbIter) / 100.;

        System.out.println("\nFréquences dx<dy : " + cpt_xy + "/" + nbIter + " (" + 100 * Py + "%)");
        System.out.println("Fréquences dx<dz : " + cpt_xz + "/" + nbIter + " (" + 100 * Pz + "%)");
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
    public boolean checkConstr(double[] c, double[] arr) {
        double sum = 0;
        for (int i = 0; i < n; i++) {
            sum += c[i + 1] * arr[i];
        }
        return (sum <= c[n + 1]);
    }

    public double[] getx0(){ return x0; }
    public double[] getx(){ return x; }
    public double[] gety(){ return y; }
    public double[] getz(){ return z; }
    public double getPy(){ return Py; }
    public double getPz(){ return Pz; }

}
