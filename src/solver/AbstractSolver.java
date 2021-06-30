package solver;

import java.io.*;

public abstract class AbstractSolver {

    protected final String filePath;
    protected String solverFile;
    protected final String newSolverFile;
    protected final String options;
    protected String solver;
    private Process proc;
    protected String output;
    protected int nbVariables;
    protected String extension;
    protected static double epsilon;
    protected float valOptimal;
    protected float[] nouvelleFctCout;
    protected String statut;

    /**
     * Constructeur général de solveur de programme linéaire
     * @param filePath chemin du fichier
     * @param options options du solveur
     */
    public AbstractSolver(String filePath, String options) {
        this.statut = "right";
        this.filePath = filePath;
        this.options = options;
        this.solverFile = "";
        this.newSolverFile = "output"+File.separatorChar+"final_result";
        this.nbVariables = 0;
        File outputRes = new File("output");
        epsilon = 0.001;
        outputRes.mkdir();
    }

    /**
     * Lance l'exécutable du solveur pour un fichier et des options spécifiques
     * @throws IOException IOException si le fichier n'est pas trouvé
     */
    public void run() {
        Runtime rt = Runtime.getRuntime();
        // command pour exécuter lpsolve
        String[] commands = {solver, options, solverFile};
        // exécution de la commande
        try {
            proc = rt.exec(commands);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Permet l'affichage la sortie standard du solveur
     * @throws IOException IOException si le fichier n'est pas trouvé
     */
    public void display() throws IOException {

        BufferedReader stdInput = new BufferedReader(new InputStreamReader(proc.getInputStream()));
        BufferedReader stdError = new BufferedReader(new InputStreamReader(proc.getErrorStream()));

        // lecture de la sortie de la commande
        String s;
        this.output = "";
        while ((s = stdInput.readLine()) != null) {
            output += s;
            output += "\n";
        }

        if((s = stdError.readLine()) != null) {
            // lecture de toutes les erreurs relevées par la commande
            System.out.println("Standard error :\n" + s);
            while ((s = stdError.readLine()) != null) {
                System.out.println(s);
            }
        }
    }

    public void setStatutInfeasible() {
        this.statut = "infeasible";
    }

    public void setStatutUnbounded() {
        this.statut = "unbounded";
    }

    public void setStatutRight() {
        this.statut = "right";
    }

    /**
     * Méthode permettant de récupérer les valeurs optimales et réalisables du programme linéaire
     */
    public abstract void parseOutput();

    /**
     * Méthode permettant de créer un fichier lp depuis un fichier texte
     */
    public abstract void createSolverFile() throws IOException;

    public String getFilePath() {
        return filePath;
    }

    public String getSolverFile() {
        return solverFile;
    }

    public String getNewSolverFile() {
        return newSolverFile+extension;
    }

    public String getOptions() {
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

    public float getValOptimal() {
        return valOptimal;
    }

    public float[] getNouvelleFctCout() {
        return nouvelleFctCout;
    }

    public String getStatut() {
        return statut;
    }
}
