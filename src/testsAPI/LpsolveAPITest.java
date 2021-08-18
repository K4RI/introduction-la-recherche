package testsAPI;

import lpsolve.LpSolveException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.*;
import solverAPI.AbstractSolverAPI;
import solverAPI.LpsolveAPI;

import java.io.File;
import java.io.IOException;

class LpsolveAPITest {

    private AbstractSolverAPI solver1;
    private AbstractSolverAPI solver2;
    private AbstractSolverAPI solver3;
    private AbstractSolverAPI solver4;
    private AbstractSolverAPI solver5;
    private AbstractSolverAPI solver6;

    @BeforeEach
    void setUp() {
        solver1 = new LpsolveAPI("."+File.separatorChar+"src"+File.separatorChar+"testsAPI"+File.separatorChar+ "rightFunctionAPI.lp", 3);
        solver2 = new LpsolveAPI("."+File.separatorChar+"src"+File.separatorChar+"testsAPI"+File.separatorChar+ "wrongFunctionRightMRUAPI.lp", 3);
        solver3 = new LpsolveAPI("."+File.separatorChar+"src"+File.separatorChar+"testsAPI"+File.separatorChar+ "wrongMRUAPI.lp", 3);
        solver4 = new LpsolveAPI("."+File.separatorChar+"src"+File.separatorChar+"testsAPI"+File.separatorChar+"wrongExtensionAPI.mp3", 3);
        solver5 = new LpsolveAPI("."+File.separatorChar+"src"+File.separatorChar+"testsAPI"+File.separatorChar+"testWrongAPI.lp", 3);
        solver6 = new LpsolveAPI("."+File.separatorChar+"src"+File.separatorChar+"testsAPI"+File.separatorChar+"wrong_pathAPI.lp", 3);
    }

    @Test
    void testCreateSolverFileRight() throws IOException, LpSolveException {
        solver1.createSolverFile();
        solver2.createSolverFile();
        solver3.createSolverFile();
        solver4.createSolverFile();

        File file = new File(solver1.getSolverFile());
        File file2 = new File(solver2.getSolverFile());
        File file3 = new File(solver3.getSolverFile());
        File file4 = new File(solver4.getSolverFile());

        Assertions.assertTrue(file.exists());
        Assertions.assertTrue(file2.exists());
        Assertions.assertTrue(file3.exists());
        Assertions.assertTrue(file4.exists());
    }

    @Test
    void testRightFunction() throws IOException, LpSolveException {
        solver1.createSolverFile();
        solver1.run();
        solver1.parseOutput();

        Assertions.assertEquals(solver1.getStatut(), "right");
    }

    @Test
    void testWrongFunction() throws IOException, LpSolveException {
        solver2.createSolverFile();
        solver2.run();
        solver2.parseOutput();
        
        Assertions.assertEquals(solver2.getStatut(), "inf_coh");
        double[] res = new double[] {2.999, 2};
        Assertions.assertArrayEquals(solver2.getNouvelleFctCout(), res);
    }

    @Test
    void testWrongMRU() throws IOException, LpSolveException {
        solver3.createSolverFile();
        solver3.run();
        solver3.parseOutput();

        Assertions.assertEquals(solver3.getStatut(), "inf_incoh");
        Assertions.assertEquals(solver3.getNbContraintes(), 2);
    }

    @Test
    void testParseOutputWrongExtension() throws IOException, LpSolveException {
        solver4.createSolverFile();
        solver4.run();
        solver4.parseOutput();
    }

    @Test
    void testParseOutputEmptyTxt() {
        Assertions.assertThrows(LpSolveException.class, () ->
                        solver5.createSolverFile(),
                "read_LP returned NULL"
        );
    }

    @Test
    void testcreateSolverFileWrong() {
        Assertions.assertThrows(LpSolveException.class, () ->
                        solver6.createSolverFile(),
                "read_LP returned NULL"
        );
    }
}