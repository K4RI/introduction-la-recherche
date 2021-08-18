package solverAPI;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Objects;

import lpsolve.*;

public class LpsolveAPI extends AbstractSolverAPI {

    /**
     * Constructeur de solveur de programme linéaire pour LpSolve
     * @param filePath chemin du fichier
     * @param options  options du solveur
     */
    public LpsolveAPI(String filePath, int options) {
        super(filePath, options);
        extension = ".lp";
    }

    /**
     * Méthode permettant d'initialiser l'objet LpSolve à partir du fichier .lp
     */
    public void createSolverFile() throws IOException, LpSolveException {
        this.lpSolver = LpSolve.readLp(filePath, options, "Problème");
        this.nbVariables = lpSolver.getNcolumns();
        this.nbContraintes = lpSolver.getNrows();
        for (int i = 1; i <= getNbContraintes(); i++) {
            if (lpSolver.getConstrType(i)==LpSolve.LE){
                lpSolver.setRh(i, lpSolver.getRh(i)-epsilon);
            } else {
                lpSolver.setRh(i, lpSolver.getRh(i)+epsilon);}
        }

        lpSolver.writeLp(getSolverFile());
    }

    /**
     * Méthode permettant de récupérer les valeurs optimales et réalisables du programme linéaire
     */
    public void parseOutput() throws LpSolveException {
        valOptimal = lpSolver.getObjective();

        if (solvecode==0){
            lpSolver.printLp();
            System.out.println("La solution est dans MRU (cas 1)");
            if (!Objects.equals(statut, "inf_incoh")) {
                statut="right";
            }

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

        // fin de l'exécution
        lpSolver.writeLp(getNewSolverFile());
    }

    /**
     * Méthode qui détermine si le problème d'infaisabilité vient de la fonction de coût ou de la cohérence de MRU
     */
    public void retryLpFile() throws LpSolveException {
        lpSolverBis= lpSolver.copyLp();
        lpSolverBis.setLpName("Problème 2");
        // on retire la fonction de coût, pour ne garder que les contraintes et vérifier leur cohérence
        emptyBounds(lpSolverBis);
        int newStatut = lpSolverBis.solve();

        if(newStatut==0){
            System.out.println("MRU cohérent (cas 2.1)");
            if (!Objects.equals(statut, "inf_incoh")) {
                statut="inf_coh";
            }
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
        lpSolverTer = lpSolver.copyLp();
        lpSolverTer.setLpName("Problème 2.1");
        emptyBounds(lpSolverTer);

        // on ajoute les variables zi
        for (int i = 1; i <= getNbVariables(); i++) {
            lpSolverTer.addColumn(new double[getNbContraintes()]);
            lpSolverTer.setColName(i, "y"+i);
            lpSolverTer.setColName(getNbVariables() + i, "z"+i);
        }

        // on ajoute les contraintes -yi+zi>=-xi et yi+zi>=xi
        // càd zi >= |yi-xi| = diff absolue entre ancien et nouveau coût
        for (int i = 1; i <= getNbVariables(); i++) {
            double xi = lpSolver.getUpbo(i);
            lpSolverTer.addConstraint(new double[2*getNbVariables()], LpSolve.GE, -xi);
            lpSolverTer.setMat(lpSolverTer.getNrows(), i, -1);
            lpSolverTer.setMat(lpSolverTer.getNrows(), i+getNbVariables(), 1);
            lpSolverTer.addConstraint(new double[2*getNbVariables()], LpSolve.GE, xi);
            lpSolverTer.setMat(lpSolverTer.getNrows(), i, 1);
            lpSolverTer.setMat(lpSolverTer.getNrows(), i+getNbVariables(), 1);
        }

        // on modifie la fonction objectif en somme des zi
        double[] fctObj = new double[2*getNbVariables()+1];
        for (int i = getNbVariables()+1; i <= 2*getNbVariables(); i++) {
            fctObj[i]=1;
        }
        lpSolverTer.setObjFn(fctObj);
        lpSolverTer.solve();

        nouvelleFctCout = Arrays.copyOfRange(lpSolverTer.getPtrVariables(), 0, getNbVariables());
        printNewCout();
        lpSolverTer.printLp();
        lpSolverTer.writeLp(getCohSolverFile());
        updateFunction();
        lpSolver.printLp();
        valOptimal=lpSolverTer.getObjective();
    }

    /**
     * Méthode permettant de rendre cohérent un système de contraintes MRU incohérent (cas 2.2)
     */
    private void fixMRU() throws LpSolveException {
        LpSolve newMRU = lpSolver.copyLp();
        newMRU.setLpName("Problème 2.2");
        emptyBounds(newMRU);

        // à chaque contrainte on ajoute une même déviation b, qu'on cherche alors à minimiser
        newMRU.addColumn(new double[getNbContraintes()]);
        newMRU.setColName(getNbVariables() + 1, "b");
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

        newMRU.printLp();
        newMRU.writeLp(getIncohSolverFile1());
        newMRU.solve();
        //System.out.println(Arrays.toString(newMRU.getPtrVariables()));

        double[] constrs;
        while (newMRU.getObjective() != 0) {
            // on recherche la première contrainte active (="saturée")
            constrs = newMRU.getPtrConstraints();
            int i=1;
            while (constrs[i-1] != newMRU.getRh(i)){
                //System.out.println("valeur atteinte : " + constrs[i-1] + " / seuil : " + newMRU.getRh(i));
                i+=1;
            }
            // on la supprime puis on relance
            System.out.println("Contrainte supprimée :" + i);
            lpSolver.delConstraint(i);
            newMRU.delConstraint(i);
            nbContraintes--;
            newMRU.solve();
        }
        newMRU.printLp();
        newMRU.writeLp(getIncohSolverFile2());

        // une fois réglé le problème du MRU, on reprend du début (cas 1 ou 2.1)
        parseOutput();

    }

    /**
     * Méthode permettant de réinitialiser à 0 Inf les bornes de toutes les variables
     */
    private void emptyBounds(LpSolve s) throws LpSolveException {
        for (int i = 1; i <= s.getNcolumns(); i++) {
            s.setBounds(i, 0, s.getInfinite());
        }
    }

    /**
     * Méthode permettant de mettre à jour l'objet Lpsolve à partir de la fonction de coût
     */
    private void updateFunction() throws LpSolveException {
        emptyBounds(lpSolver);
        for (int i = 0; i < getNbVariables(); i++) {
            lpSolver.setBounds(i+1, nouvelleFctCout[i], nouvelleFctCout[i]);
        }
    }
}