package solverAPI;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;

import lpsolve.*;

public class LpsolveAPI extends AbstractSolverAPI {

    /**
     * Constructeur de solveur de programme linéaire pour LpSolve
     * @param filePath chemin du fichier
     * @param options  options du solveur
     */
    public LpsolveAPI(String filePath, int options) throws LpSolveException {
        super(filePath, options);
        extension = ".lp";
        this.solverFile = "output"+File.separatorChar+"user_solution";
        this.lpSolver = LpSolve.readLp(filePath, options, "Problème");
        this.nbVariables = lpSolver.getNcolumns();
        this.nbContraintes = lpSolver.getNrows();
        for (int i = 1; i <= getNbContraintes(); i++) {
            if (lpSolver.getConstrType(i)==LpSolve.LE){
                lpSolver.setRh(i, lpSolver.getRh(i)-epsilon);
            } else {
                lpSolver.setRh(i, lpSolver.getRh(i)+epsilon);}
        }
    }

    /**
     * Méthode permettant d'initialiser l'objet LpSolve à partir du fichier .lp
     */
    public void createSolverFile() throws IOException, LpSolveException {
        lpSolver.writeLp(getSolverFile());
    }

    /**
     * Méthode permettant de récupérer les valeurs optimales et réalisables du programme linéaire
     */
    public void parseOutput() throws LpSolveException {
        valOptimal = lpSolver.getObjective();

        if (solvecode==0){
            System.out.println("La solution est dans MRU (cas 1)");
            statut="right";
        } else if(solvecode==2){
            System.out.println("Le problème est infaisable (cas 2)");
            retryLpFile();
        } else if (solvecode==3) {
            System.out.println("Le problème n'est pas borné");
            statut="unbounded";
        } else {
            System.out.println("Code inconnu : " + solvecode);
            statut=Integer.toString(solvecode);
        }
    }

    /**
     * Méthode qui détermine si le problème d'infaisabilité vient de la fonction de coût ou de la cohérence de MRU
     */
    public void retryLpFile() throws LpSolveException {
        newlpSolver= lpSolver.copyLp();
        newlpSolver.setLpName("Problème 2");
        // on retire la fonction de coût, pour ne garder que les contraintes et vérifier leur cohérence
        emptyBounds(newlpSolver);
        int newStatut = newlpSolver.solve();

        if(newStatut==0){
            System.out.println("MRU cohérent (cas 2.1)");
            statut="inf_coh";
            findShortestDistance();
        } else if (newStatut==2) {
            System.out.println("MRU incohérent (cas 2.2)");
            statut="inf_incoh";
            fixMRU();
        } else if (newStatut==3){
            System.out.println("Le problème n'est pas borné");
            statut="unbounded";
        } else {
            System.out.println("Code inconnu : " + solvecode);
            statut=Integer.toString(solvecode);
        }
    }

    /**
     * Méthode permettant de retrouver la plus petite distance entre une fonction de coût et MRU (cas 2.1)
     */
    private void findShortestDistance() throws LpSolveException {
        newnewlpSolver = lpSolver.copyLp();
        newnewlpSolver.setLpName("Problème 2.1");
        emptyBounds(newnewlpSolver);

        // on ajoute les variables zi
        for (int i = 1; i <= getNbVariables(); i++) {
            newnewlpSolver.addColumn(new double[getNbContraintes()]);
        }

        // on ajoute les contraintes -yi+zi>=-xi et yi+zi>=xi
        for (int i = 1; i <= getNbVariables(); i++) {
            double xi = lpSolver.getUpbo(i);
            newnewlpSolver.addConstraint(new double[2*getNbVariables()], LpSolve.GE, -xi);
            newnewlpSolver.setMat(newnewlpSolver.getNrows(), i, -1);
            newnewlpSolver.setMat(newnewlpSolver.getNrows(), i+getNbVariables(), 1);
            newnewlpSolver.addConstraint(new double[2*getNbVariables()], LpSolve.GE, xi);
            newnewlpSolver.setMat(newnewlpSolver.getNrows(), i, 1);
            newnewlpSolver.setMat(newnewlpSolver.getNrows(), i+getNbVariables(), 1);
        }

        // on modifie la fonction objectif en somme des zi
        double[] fctObj = new double[2*getNbVariables()+1];
        for (int i = getNbVariables()+1; i <= 2*getNbVariables(); i++) {
            fctObj[i]=1;
        }
        newnewlpSolver.setObjFn(fctObj);
        newnewlpSolver.solve();

        nouvelleFctCout = Arrays.copyOfRange(newnewlpSolver.getPtrVariables(), 0, getNbVariables());
        for (int i = 0; i < getNbVariables(); i++) {
            System.out.println("Nouvelle valeur de x" + (i+1) + " = " + nouvelleFctCout[i]);
        }
        newnewlpSolver.writeLp(getNewSolverFile());
    }

    /**
     * Méthode permettant de rendre cohérent un système de contraintes MRU incohérent (cas 2.2)
     */
    private void fixMRU() throws LpSolveException {
        LpSolve newMRU = lpSolver.copyLp();
        newMRU.setLpName("Problème 2.2");
        emptyBounds(newMRU);
        newMRU.addColumn(new double[getNbContraintes()]);
        for (int i = 1; i <= getNbContraintes(); i++) {
            if (newMRU.getConstrType(i) == LpSolve.LE) {
                newMRU.setMat(i, getNbVariables() + 1, -1);
            } else {
                newMRU.setMat(i, getNbVariables() + 1, 1);
            }
        }
        for (int i = 1; i <= getNbVariables(); i++) {
            newMRU.setMat(0, i, 0);
        }
        newMRU.setMat(0, getNbVariables() + 1, 1);

        double b = 1;
        /* while (b != 0) {
            newMRU.solve();
            // déterminer les contraintes saturées
            newMRU.delConstraint(...)
        }
        newMRU.printLp(); */
    }


    private void emptyBounds(LpSolve s) throws LpSolveException {
        for (int i = 1; i <= s.getNcolumns(); i++) {
            s.setBounds(i, 0, s.getInfinite());
        }
    }
}