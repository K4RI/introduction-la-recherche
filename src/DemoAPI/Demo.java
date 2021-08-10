package DemoAPI;

import lpsolve.*;

public class Demo {

    public static void main(String[] args) {
        try {
            // Create a problem with 4 variables and 0 constraints
            LpSolve solver = LpSolve.makeLp(0, 4);

            // add constraints
            solver.strAddConstraint("3 2 2 1", LpSolve.LE, 4); // 3x1 + 2x2 + 2x3 + x4 <= 4
            solver.strAddConstraint("0 4 3 1", LpSolve.GE, 3); // 4x2 + 3x3 + x4 >= 3

            // set objective function
            solver.strSetObjFn("2 3 -2 3"); // minimiser 2x1 + 3x2 -2x3 + 3x4

            // solve the problem

            solver.writeLp("WriteLp.txt");

            LpSolve solver2 = LpSolve.readLp("WriteLp.txt", 0, "Nom");
            solver.printLp();
            solver2.printLp();
            solver2.solve();


            // print solution
            System.out.println("Value of objective function: " + solver2.getObjective());
            double[] var = solver2.getPtrVariables();
            for (int i = 0; i < var.length; i++) {
                System.out.println("Value of var[" + i + "] = " + var[i]);
            }

               // delete the problem and free memory

            solver2.deleteLp();
        }
        catch (LpSolveException e) {
            e.printStackTrace();
        }
    }

}