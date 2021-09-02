import evaluationAPI.*;
import lpsolve.LpSolveException;
import solverAPI.LpsolveAPI;

import java.io.IOException;

public class MainAPI {

    public static void main(String[] args) throws IOException, LpSolveException {

        // On lance la partie évaluation
        // Ici 2 règles et 5 itérations
        if(args.length <= 1) {
            AbstractEvaluationAPI e = new EvaluationAPI3(4, 3.0, false);
            e.evaluer(25);
        } else {

            // récupération du fichier lp
            String filePath = args[0];
            // option par défaut si aucune option n'est précisée
            int options = 0;
            if (args.length == 2) {
                options = Integer.parseInt(args[1]);
            }

            LpsolveAPI solver = new LpsolveAPI(filePath, options, true);

            // exécution du solveur
            solver.createSolverFile();
            solver.run();
            solver.parseOutput();
        }
    }
}

