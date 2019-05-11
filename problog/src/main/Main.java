package main;

import static java.nio.charset.StandardCharsets.UTF_8;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import com.google.errorprone.annotations.CheckReturnValue;
import com.google.errorprone.annotations.Var;
import engine.AdInference;
import engine.Inference;
import types.Fact;

@CheckReturnValue
public class Main {

  public static void main(String[] args) throws IOException {

    Scanner sc = new Scanner(System.in, UTF_8.name());

    while (true) {

      System.out.println(
          "please choose naive or semi_naive, if naive, please enter (n), semi_naive is (s)");
      String way = sc.nextLine().toLowerCase();
      System.out.println("please choose file, enter: ");
      String textName = sc.nextLine();
      System.out.println("do you want probability? enter(y/n): ");
      String pro = sc.nextLine().toLowerCase();
      System.out.println("do you want max function for disjunction? enter(y?): ");
      String max = sc.nextLine().toLowerCase();
      System.out.println("do you want product function for conjunction? enter(y?): ");
      String product = sc.nextLine().toLowerCase();

      @Var
      boolean hasProbability = false;

      if (pro.equals("y")) {
        hasProbability = true;
      }

      if (way.equals("s")) {
        testSemiNaive(textName, hasProbability, max, product);
      } else {
        testNaive(textName, hasProbability, max, product);
      }

      System.out.println("do you want continue(y?), enter: ");

      String exit = sc.nextLine().toLowerCase();
      if (!exit.equals("y")) {
        break;
      }
    }
  }

  private static void testNaive(String filename, boolean hasProbability, String max, String product)
      throws IOException {

    Inference inference = new Inference(File.separator + filename, hasProbability);
    if (max.equals("y")) {
      inference.useMax = true;
    }
    if (product.equals("y")) {
      inference.useProduct = true;
    }

    long currentTime1, currentTime2;
    currentTime1 = System.currentTimeMillis();
    inference.naive();
    currentTime2 = System.currentTimeMillis();

    System.out.println(inference.factMap);

    writeFile(inference.factMap);

    System.out.println("use time: " + (currentTime2 - currentTime1) + " ms");

    @Var
    int size = 0;
    for (Map.Entry<String, List<Fact>> entry : inference.factMap.entrySet()) {
      size = size + entry.getValue().size();
    }

    System.out.println("the size of all facts is: " + size);
  }

  private static void testSemiNaive(String filename, boolean hasProbability, String max,
      String product) throws IOException {

    AdInference inference = new AdInference(File.separator + filename, hasProbability);
    if (max.equals("y")) {
      inference.useMax = true;
    }
    if (product.equals("y")) {
      inference.useProduct = true;
    }

    long currentTime1, currentTime2;
    currentTime1 = System.currentTimeMillis();
    inference.semi_naive();
    currentTime2 = System.currentTimeMillis();

    System.out.println(inference.factMap);

    writeFile(inference.factMap);

    System.out.println("use time: " + (currentTime2 - currentTime1) + " ms");

    @Var
    int size = 0;
    for (Map.Entry<String, List<Fact>> entry : inference.factMap.entrySet()) {
      size = size + entry.getValue().size();
    }

    System.out.println("the siez of all facts is: " + size);
  }

  private static void writeFile(Map<String, List<Fact>> map) throws IOException {
    try (BufferedWriter bw = Files.newBufferedWriter(
        Paths.get(System.getProperty("user.dir") + File.separator + "output.text"), UTF_8)) {
      for (Map.Entry<String, List<Fact>> entry : map.entrySet()) {
        List<Fact> temp = entry.getValue();
        for (Fact fact : temp) {
          bw.write(fact.toString());
          bw.newLine();
        }
      }
    }
  }
}
