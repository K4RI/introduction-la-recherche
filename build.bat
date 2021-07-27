cd %CD%
javac --release 8 -sourcepath src src\Main.java
cd src
jar cfe jarName.jar Main Main.class evaluation\Evaluation.class solver\AbstractSolver.class solver\Lpsolve.class
java -jar jarName.jar lp_solve test.txt
java Main lp_solve test.txt -S1
cd ..