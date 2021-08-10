package solverAPI;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

import lpsolve.*;

public abstract class AbstractSolverAPI {

    protected LpSolve lpSolver;
    protected LpSolve newlpSolver;
    protected LpSolve newnewlpSolver;
    protected final String filePath;
    protected String solverFile;
    protected final String newSolverFile;
    protected final int options;
    protected String solver;
    protected String output;
    protected int nbVariables;
    protected int nbContraintes;
    protected String extension;
    protected static double epsilon;
    protected double valOptimal;
    protected double[] nouvelleFctCout;
    protected int statut;
    protected int newStatut;
    protected int newnewStatut;

    /**
     * Constructeur général de solveur de programme linéaire
     * @param filePath chemin du fichier
     * @param options options du solveur
     */
    public AbstractSolverAPI(String filePath, int options) throws LpSolveException {
        this.filePath = filePath;
        this.options = options;
        this.solverFile = "";
        this.newSolverFile = "output"+File.separatorChar+"final_result";
        File outputRes = new File("output");
        epsilon = 0.001;
        outputRes.mkdir();
        System.out.println(filePath);
    }

    /**
     * Lance l'exécutable du solveur pour un fichier et des options spécifiques
     * @throws LpSolveException en cas d'erreur avec le package
     */
    public void run() throws LpSolveException {
        statut = lpSolver.solve();
        // System.out.println("STATUT: " + statut);
    }


    public void setStatutInfeasible() {
        this.statut = 2;
    }

    public void setStatutUnbounded() {
        this.statut = 3;
    }

    public void setStatutRight() {
        this.statut = 0;
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

    public String getSolverFile() {
        return solverFile;
    }

    public String getNewSolverFile() {
        return newSolverFile+extension;
    }

    public int getOptions() {
        return options;
    }

    public String getSolver() {
        return solver;
    }

    public String getOutput() {
        return output;
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

    public int getStatut() {
        return statut;
    }
}
