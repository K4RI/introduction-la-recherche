package solverAPI;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Scanner;

import lpsolve.*;
import solver.Lpsolve;

public class LpsolveAPI extends AbstractSolverAPI {

    /**
     * Constructeur de solveur de programme linéaire pour LpSolve
     * @param filePath chemin du fichier
     * @param options  options du solveur
     */
    public LpsolveAPI(String filePath, int options) throws LpSolveException {
        super(filePath, options);
        solverFile = "output"+File.separatorChar+"user_solution.lp";
        this.lpSolver = LpSolve.readLp(filePath, options, "");
        this.nbVariables = lpSolver.getNcolumns();
        this.nbContraintes = lpSolver.getNrows();
        for (int i = 0; i < getNbContraintes(); i++) {
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
        lpSolver.writeLp(solverFile);
    }

    /**
     * Méthode permettant de récupérer les valeurs optimales et réalisables du programme linéaire
     */
    public void parseOutput() throws LpSolveException {
        valOptimal = lpSolver.getObjective();

        /*System.out.println("Value of objective function: " + lpSolver.getObjective());
        double[] var = lpSolver.getPtrVariables();
        for (int i = 0; i < var.length; i++) {
            System.out.println("Value of var[" + i + "] = " + var[i]);
        } */

        if (statut==0){
            System.out.println("La solution est dans MRU (cas 1)");
        } else if(statut==2){
            System.out.println("Le problème est infaisable (cas 2)");
            retryLpFile();
        } else if (statut==3) {
            System.out.println("Le problème n'est pas borné");
        } else {
            System.out.println("Code inconnu : " + statut);
        }
    }

    /**
     * Méthode qui détermine si le problème d'infaisabilité vient de la fonction de coût ou de la cohérence de MRU
     */
    public void retryLpFile() throws LpSolveException {
        newlpSolver= lpSolver.copyLp();
        // on retire la fonction de coût, pour garder que les contraintes et vérifier leur cohérence
        for (int i = 0; i < getNbVariables(); i++) {
            newlpSolver.setBounds(i, 0, newlpSolver.getInfinite());
        }
        newStatut = newlpSolver.solve();

        if(newStatut==0){
            System.out.println("MRU cohérent (cas 2.1)");
            findShortestDistance();
        } else if (newStatut==2) {
            System.out.println("MRU incohérent (cas 2.2)");
            fixMRU();
        } else if (newStatut==3){
            System.out.println("Le problème n'est pas borné");
        } else {
            System.out.println("Code inconnu : " + statut);
        }
    }

    /**
     * Méthode permettant de retrouver la plus petite distance entre une fonction de coût et MRU (cas 2.1)
     */
    private void findShortestDistance() throws LpSolveException {
        newnewlpSolver = lpSolver.copyLp();

        // on ajoute les variables zi
        for (int i = 0; i < getNbVariables(); i++) {
            newnewlpSolver.addColumn(new double[getNbContraintes()]);
        }

        // on ajoute les contraintes -yi+zi>=-xi et yi+zi>=xi
        for (int i = 0; i < getNbVariables(); i++) {
            double xi = lpSolver.getUpbo(i);
            newnewlpSolver.addConstraint(new double[2*getNbVariables()], LpSolve.LE, -xi);
            newnewlpSolver.setMat(newnewlpSolver.getNrows(), i, -1);
            newnewlpSolver.setMat(newnewlpSolver.getNrows(), i+getNbVariables(), 1);
            newnewlpSolver.addConstraint(new double[2*getNbVariables()], LpSolve.LE, xi);
            newnewlpSolver.setMat(newnewlpSolver.getNrows(), i, 1);
            newnewlpSolver.setMat(newnewlpSolver.getNrows(), i+getNbVariables(), 1);
        }

        // on modifie la fonction objectif en somme des zi
        double[] fctObj = new double[2*getNbVariables()];
        for (int i = getNbVariables(); i < 2*getNbVariables(); i++) {
            fctObj[i]=1;
        }
        newnewlpSolver.setObjFn(fctObj);

        // on retire les bornes (=valeurs de la fonction de coût) pour les yi
        for (int i = 0; i < getNbVariables(); i++) {
            newnewlpSolver.setBounds(i, 0, newnewlpSolver.getInfinite());
        }
        newnewStatut = newnewlpSolver.solve();
        nouvelleFctCout = Arrays.copyOfRange(newnewlpSolver.getPtrVariables(), 0, getNbVariables());
        for (int i = 0; i < getNbVariables(); i++) {
            System.out.println("Valeur de y" + i + " = " + nouvelleFctCout[i]);
        }
    }

    private void fixMRU() throws LpSolveException {
        LpSolve newMRU = LpSolve.makeLp(0,getNbVariables());
        newMRU.strSetObjFn("2"); // minimiser beta
        ;
    }
}