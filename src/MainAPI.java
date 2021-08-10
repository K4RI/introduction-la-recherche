import evaluation.Evaluation;
import lpsolve.LpSolveException;
import solverAPI.AbstractSolverAPI;
import solverAPI.LpsolveAPI;

import java.io.File;
import java.io.IOException;

public class MainAPI {

    public static void main(String[] args) throws IOException, LpSolveException {
        // récupération du fichier texte
        String filePath = args[0];
        // option par défaut si aucune option n'est précisée
        int options = 0;
        if(args.length == 2) {
            options = Integer.parseInt(args[1]);
        }

        LpsolveAPI solver = new LpsolveAPI(filePath, options);

        // exécution du solveur
        solver.createSolverFile();
        solver.run();
        solver.parseOutput();
    }
}

