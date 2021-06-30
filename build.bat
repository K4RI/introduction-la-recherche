javac --release 8 -sourcepath src src\Main.java
cd src
jar cfe jarName.jar Main Main.class
java -jar jarName.jar lp_solve test.txt
cd ..