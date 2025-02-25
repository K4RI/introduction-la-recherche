import evaluation.Evaluation;
import solver.AbstractSolver;
import solver.Lpsolve;

import java.io.IOException;

public class Main {

    public static void main(String[] args) throws IOException {

        //On lance la partie évaluation
        if (args.length <= 1) {
            Evaluation e = new Evaluation(2);
            e.evaluer(5);
        } else {

            // Récupération du nom de l'exécutable du solveur
            String solveur = args[0];
            // récupération du fichier texte

            String filePath = args[1];
            // option par défaut si aucune option n'est précisée
            String options = "";
            if (args.length == 3) {
                options = args[2];
            }

            AbstractSolver solver = null;

            switch (solveur) {
                case "lp_solve":
                    solver = new Lpsolve(filePath, options);
                    break;
                default:
                    System.err.println("Error unknown solver : Please");
                    System.exit(0);
            }

            // exécution du solveur
            try {
                solver.createSolverFile();
                solver.run();
                solver.display();
                solver.parseOutput();
            } catch (IOException e) {
                System.err.println("Error file not found : The file " + filePath + " was not found.");
            }
        }
    }

}
