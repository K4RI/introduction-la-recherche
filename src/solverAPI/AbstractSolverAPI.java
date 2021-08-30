package solverAPI;

import java.io.File;
import java.io.IOException;

import lpsolve.*;

public abstract class AbstractSolverAPI {

    public LpSolve lpSolver;
    protected LpSolve lpSolverBis;
    protected LpSolve lpSolverTer;
    protected final String filePath;
    protected final int options;
    protected String solverFile; // configuration initiale
    protected final String newSolverFile; // configuration finale
    protected final String cohSolverFile; // problème intermédiaire (2.1)
    protected final String incohSolverFile1; // problème intermédiaire (2.2 non-réduit)
    protected final String incohSolverFile2; // problème intermédiaire (2.2 réduit)
    protected String extension;
    protected String solver;
    protected int nbVariables;
    protected int nbContraintes;
    protected static double epsilon;
    protected double valOptimal;
    protected double[] nouvelleFctCout;
    protected int solvecode;
    protected String statut;

    /**
     * Constructeur général de solveur de programme linéaire
     * @param filePath chemin du fichier
     * @param options options du solveur
     */
    public AbstractSolverAPI(String filePath, int options){
        this.filePath = filePath;
        this.options = options;
        this.solverFile = "output"+File.separatorChar+"initial_solutionAPI";
        this.newSolverFile = "output"+File.separatorChar+"final_resultAPI";
        this.cohSolverFile = "output"+File.separatorChar+"coh_solverAPI";
        this.incohSolverFile1 = "output"+File.separatorChar+"incoh_solver1API";
        this.incohSolverFile2 = "output"+File.separatorChar+"incoh_solver2API";
        epsilon = 0.001;
        File outputRes = new File("output");
        outputRes.mkdir();
    }

    /**
     * Lance l'exécutable du solveur pour un fichier et des options spécifiques
     * @throws LpSolveException en cas d'erreur avec le package
     */
    public void run() throws LpSolveException {
        solvecode = lpSolver.solve();
    }

    /**
     * Méthode permettant de récupérer les valeurs optimales et réalisables du programme linéaire
     */
    public abstract void parseOutput() throws LpSolveException;

    /**
     * Méthode permettant de créer un fichier lp depuis un fichier texte
     */
    public abstract void createSolverFile() throws IOException, LpSolveException;

    public String getFilePath() {
        return filePath;
    }

    public String getSolverFile() { return solverFile+extension; }

    public String getNewSolverFile() { return newSolverFile+extension; }

    public String getCohSolverFile() { return cohSolverFile+extension; }

    public String getIncohSolverFile1() { return incohSolverFile1+extension; }

    public String getIncohSolverFile2() { return incohSolverFile2+extension; }

    public int getOptions() {
        return options;
    }

    public String getSolver() {
        return solver;
    }

    public int getNbVariables() {
        return nbVariables;
    }

    public int getNbContraintes() { return nbContraintes; }

    public double getValOptimal() {
        return valOptimal;
    }

    public double[] getNouvelleFctCout() {
        return nouvelleFctCout;
    }

    public String getStatut() {
        return statut;
    }

    public void printNewCout(){
        for (int i = 0; i < getNbVariables(); i++) {
            System.out.println("Nouvelle valeur de x" + (i+1) + " = " + nouvelleFctCout[i]);
        }
    }
}
