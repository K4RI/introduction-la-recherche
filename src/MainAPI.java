import evaluationAPI.*;
import lpsolve.LpSolveException;
import solver.Lpsolve;
import solverAPI.LpsolveAPI;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Objects;

public class MainAPI {


    public static void main(String[] args) throws Exception {
        String resultFile = "src/evaluationAPI/results.txt";

        if(Objects.equals(args[0], "eval")) { // On lance la partie évaluation
            // "eval 1 5.0 4 25 false"
            int variante = Integer.parseInt(args[1]);

            double C = Double.parseDouble(args[2]);
            int n = Integer.parseInt(args[3]);
            int nbIter = Integer.parseInt(args[4]);
            boolean verb = Boolean.parseBoolean(args[5]);
            int nMax = 0;

            AbstractEvaluationAPI e = null;
            switch (variante) {
                case 0:
                    e = new EvaluationAPI(n, C, verb);
                    break;
                case 1:
                    e = new EvaluationAPI1(n, C, verb);
                    break;
                case 2:
                    e = new EvaluationAPI2(n, C, verb);
                    break;
                case 3:
                    nMax = Integer.parseInt(args[6]);
                    e = new EvaluationAPI3(n, C, verb, nMax);
                    break;
                case 4:
                    nMax = Integer.parseInt(args[6]);
                    e = new EvaluationAPI4(n, C, verb, nMax);
                    break;
                default:
                    System.err.println("Error unknown variant");
                    System.exit(0);
            }
            int lastIter=e.evaluer(nbIter);

            FileWriter myWriter = new FileWriter(resultFile, true);
            if (variante<=2){
                myWriter.write(C + " & " + n + " & " + lastIter + " & " + e.getPy() + " & " + e.getPz() + " \\\\ \n");
            } else if (variante==3) {
                myWriter.write(C + " & " + n + " & " + nMax + " & " + lastIter + " & " + ATSrounded(e.getx0(),2)+ " & " + ATSrounded(e.getx(),2) + " \\\\ \n");
            }else{
                myWriter.write(n + " & " + nMax + " & " + lastIter + " & " + ATSrounded(e.gety(),2) + " & " + e.getPy() + " & " + ATSrounded(e.getz(),2) + " & " + e.getPz() + " \\\\ \n");
            }
            myWriter.close();

        } else if(Objects.equals(args[0], "lp_solve")) { // résolution d'un fichier
            // "lp_solve ./src/test.lp 3" "eval 1 5.0 3 200 false"

            // les x=0 viennent de "...e+188" qui se glissent dans les matrices de résolution de 2.1

            // récupération du fichier lp
            String filePath = args[1];
            System.out.println("filepath : " + filePath);
            // option par défaut si aucune option n'est précisée
            int options = 0;
            if (args.length == 3) {
                options = Integer.parseInt(args[2]);
            }

            LpsolveAPI solver = new LpsolveAPI(filePath, options, true);

            // exécution du solveur
            solver.createSolverFile();
            solver.run();
            solver.parseOutput();

        } else {
            System.err.println("Error unknown solver : Please");
            System.exit(0);
        }
    }

    protected static String ATSrounded(double[] a, int n){
        double N = Math.pow(10,n);
        double[] b = new double[a.length];
        for (int i=0; i<a.length; i++){
            b[i]=Math.round(a[i]*N)/N;
        }
        return Arrays.toString(b);
    }
}