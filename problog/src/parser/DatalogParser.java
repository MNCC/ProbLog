package parser;

import static java.nio.charset.StandardCharsets.UTF_8;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import com.google.errorprone.annotations.CheckReturnValue;
import com.google.errorprone.annotations.Var;
import types.Fact;
import types.Literal;
import types.Rule;

@CheckReturnValue
public class DatalogParser {

  public final ArrayList<Fact> edb = new ArrayList<>();
  public final ArrayList<Rule> rules = new ArrayList<>();
  private final ArrayList<String> lines = new ArrayList<>();

  public void dataReader(String filename) throws IOException {
    try (BufferedReader br =
        Files.newBufferedReader(Paths.get(System.getProperty("user.dir") + filename), UTF_8)) {
      @Var
      String line = "";
      while ((line = br.readLine()) != null) {
        lines.add(line);
      }
    }
  }

  public Map<String, Fact> buildMap() {
    Map<String, Fact> map = new HashMap<>();
    for (Fact fact : edb) {
      if (!map.containsKey(fact.eString()))
        map.put(fact.eString(), fact);
    }
    return map;
  }

  private Literal literal(String str) {
    int left = str.indexOf("(");
    int right = str.indexOf(")");
    String predicate = str.substring(0, left);
    String[] variables = str.substring(left + 1, right).split(",");
    return new Literal(predicate, variables);
  }

  public void parse(boolean hasProbability) {
    for (int i = 0; i < lines.size(); i++) {

      String line = lines.get(i);
      if (!line.contains(".")) {
        continue;
      }

      try {
        if (line.contains(":-")) {
          parseRule(line.split(":-"), hasProbability);
        } else {
          parseFact(line.trim(), hasProbability);
        }
      } catch (Exception e) {
        System.out.println("Line " + (i + 1) + ": not a legal syntax rule");
        e.printStackTrace();
      }
    }
  }

  private void parseRule(String[] str, boolean hasProbability) {

    String head = str[0].trim();
    String body = str[1].trim();
    @Var
    int eol = body.indexOf(".");
    @Var
    double probability = 1.0d;

    if (hasProbability) {
      eol = body.indexOf(":");
      probability = Double.parseDouble(body.substring(eol + 1, body.indexOf(".") + 2).trim());
    }

    String[] bodAtomz = body.substring(0, eol).split(", ");
    Literal[] bodyAtoms = new Literal[bodAtomz.length];

    for (int i = 0; i < bodAtomz.length; i++) {
      bodyAtoms[i] = literal(bodAtomz[i]);
    }

    rules.add(new Rule(literal(head), bodyAtoms, probability));
  }

  private void parseFact(String str, boolean hasProbability) {

    int left = str.indexOf("(");
    int right = str.indexOf(")");
    int eol = str.indexOf(".");
    String predicate = str.substring(0, left);
    String[] constants = str.substring(left + 1, right).split(",");
    Fact fact = new Fact(predicate, constants);

    if (hasProbability) {
      fact.probability = Double.parseDouble(str.substring(str.indexOf(":") + 1, eol + 2).trim());
    }

    edb.add(fact);
  }
}
