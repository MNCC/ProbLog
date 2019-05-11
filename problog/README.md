0. Build the program in `/opt/src/ProbLog` with `mvn clean package`
1. Run the program with :
```
cd /opt/src/ProbLog/dataSet && java -cp /opt/src/ProbLog/problog/target/problog-1.0-SNAPSHOT.jar main.Main
```
2. In the console, it would show "please choose naive or semi_naive, if naive, please enter (n), semi_naive is (s)", enter s for selecting semi_naive engine, enter n for for selecting naive engine( default is naive engine, case insensitivity)
3. After choose the engine , the console would show "please choose file, enter: ", you need to enter the file name( note the file should already in the project directory), for example, enter: clique10.cdl
4. After choosing the file , the console would should "do you want probability? enter(y/n):", enter y for certainty system, enter n for standard system( the enter is case insensitivity)
5. After that, it would show "do you want max function for disjunction? enter(y?): " , enter y for use max function for disjunction otherwise use independent for disjunction(enter is case insensitivity ).
6. Finally, the colsole would show "do you want production function for conjunction? enter(y?):", enter y for using produnction for conjunction otherwise would use min function(the enter is case insensitivity).
7. After that you can see the output in the console , if you want to see clear output, you can see the output from "output.txt" in project directory.
8. In the end , the console would show "do you want continue(y?), enter:", enter y for continue , otherwise the program would terminate.( enter is case insensitivity)