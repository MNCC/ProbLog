package engine;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.google.errorprone.annotations.CheckReturnValue;
import com.google.errorprone.annotations.Var;
import parser.DatalogParser;
import types.Fact;
import types.Literal;
import types.Rule;

@CheckReturnValue
class AnwserTree {

  final Fact answer;
  final ArrayList<AnwserTree> child;
  AnwserTree par = null;

  public AnwserTree(Fact answer) {
    this.answer = answer;
    this.child = new ArrayList<>();
  }

  @Override
  public String toString() {
    return answer.toString();
  }
}


@CheckReturnValue
public class Inference {

  private final DatalogParser parser;
  private final Map<String, Fact> database;
  private final List<Rule> rules;
  public Map<String, List<Fact>> factMap;
  public boolean useMax = false;
  public boolean useProduct = false;
  private boolean hasProbability;

  public Inference(String filename, boolean hasProbability) throws IOException {
    parser = new DatalogParser();
    parser.dataReader(filename);
    this.hasProbability = hasProbability;
    parser.parse(this.hasProbability);
    database = parser.buildMap();
    rules = parser.rules;
    init();
  }

  private void init() {
    factMap = new HashMap<>();
    for (Map.Entry<String, Fact> entry : database.entrySet()) {
      if (!factMap.containsKey(entry.getValue().predicate)) {
        ArrayList<Fact> facts = new ArrayList<>();
        facts.add(entry.getValue());
        factMap.put(entry.getValue().predicate, facts);
      } else
        factMap.get(entry.getValue().predicate).add(entry.getValue());
    }
  }

  private void getOnePath(@Var AnwserTree tree, List<Fact> facts) {
    while (!tree.answer.predicate.equals("root")) {
      facts.add(tree.answer);
      tree = tree.par;
    }
  }

  private void dfsTree(int depth, AnwserTree tree, List<Fact> facts, Rule rule) {
    if (depth == rule.body.length) {
      getOnePath(tree, facts);
    } else {
      for (int i = 0; i < tree.child.size(); i++) {
        dfsTree(depth + 1, tree.child.get(i), facts, rule);
      }
    }
  }

  private List<List<Fact>> inferFacts(List<List<Fact>> collection, Rule rule) {
    List<List<Fact>> res = new ArrayList<>();
    for (List<Fact> facts : collection) {
      if (!facts.isEmpty()) {
        res.add(inferTheFact(rule, facts));
      }
    }
    return res;
  }

  private Fact infer(Rule rule, List<Fact> facts) {

    Map<String, String> model = new HashMap<>();

    for (int k = 0; k < rule.body.length; k++) {

      Literal literal = rule.body[k];

      for (int i = 0; i < literal.variables.length; i++) {
        if (!model.containsKey(literal.variables[i]))
          model.put(literal.variables[i], facts.get(k).constants[i]);
      }
    }

    Literal head = rule.head;
    String predicate = head.predicate;
    String[] constants = new String[head.variables.length];

    for (int i = 0; i < head.variables.length; i++) {
      if (model.containsKey(head.variables[i])) {
        constants[i] = model.get(head.variables[i]);
      } else {
        constants[i] = model.get(rule.body[rule.body.length - 1].variables[i]);
      }
    }

    Fact fact = new Fact(predicate, constants);

    if (hasProbability) {
      double p = useProduct ? product(facts) : min(facts);
      fact.probability = p * rule.probability;
    }
    return fact;
  }

  private List<Fact> inferTheFact(Rule rule, List<Fact> facts) {

    List<Fact> res = new ArrayList<>();
    @Var
    int count = facts.size() - 1;

    while (count >= 0) {
      List<Fact> temp = new ArrayList<>();
      for (int i = 0; i < rule.body.length; i++) {
        temp.add(facts.get(count - i));
      }
      res.add(infer(rule, temp));
      count = count - rule.body.length;
    }
    return res;
  }

  private List<List<Fact>> Tree(Rule rule) {

    String[] constants = new String[1];
    constants[0] = "root";
    Fact fact = new Fact("root", constants);
    Map<String, String> model = new HashMap<>();
    AnwserTree tree = new AnwserTree(fact);
    buildTree(0, rule, model, tree);
    List<List<Fact>> collection = new ArrayList<>();

    for (int i = 0; i < tree.child.size(); i++) {
      List<Fact> temp = new ArrayList<>();
      dfsTree(1, tree.child.get(i), temp, rule);
      collection.add(temp);
    }
    return collection;
  }

  private void buildTree(int depth, Rule rule, Map<String, String> model, AnwserTree tree) {
    if (depth < rule.body.length) {

      Literal goal = rule.body[depth];

      if (factMap.containsKey(goal.predicate)) {
        for (Fact fact : factMap.get(goal.predicate)) {

          @Var
          boolean canMatch = true;

          for (int i = 0; i < fact.constants.length; i++) {
            if (model.containsKey(goal.variables[i].trim())) {
              if (!model.get(goal.variables[i].trim()).equals(fact.constants[i].trim())) {
                canMatch = false;
                break;
              }
            }
          }

          if (canMatch) {

            Map<String, String> curModel = new HashMap<>();

            for (int i = 0; i < fact.constants.length; i++) {
              if (!curModel.containsKey(goal.variables[i].trim())) {
                curModel.put(goal.variables[i].trim(), fact.constants[i].trim());
              }
            }

            AnwserTree node = new AnwserTree(fact);
            tree.child.add(node);
            node.par = tree;
            buildTree(depth + 1, rule, curModel, node);
          }
        }
      }
    }
  }

  private double min(List<Fact> facts) {
    @Var
    double min = 1;
    for (Fact fact : facts) {
      if (fact.probability < min) {
        min = fact.probability;
      }
    }
    return min;
  }

  private double product(List<Fact> facts) {
    @Var
    double probability = 1;
    for (Fact fact : facts) {
      probability = probability * fact.probability;
    }
    return probability;
  }

  private Fact combineFacts(List<Fact> facts) {
    if (facts.size() == 1 || !hasProbability) {
      return facts.get(0);
    }

    @Var
    double sum = 0;

    for (Fact fact : facts) {
      sum = calPro(sum, fact.probability);
    }

    Fact fact = new Fact(facts.get(0).predicate, facts.get(0).constants);
    fact.probability = sum;
    return fact;
  }

  private List<Fact> dealIdb(List<Fact> idb) {

    Map<String, List<Fact>> map = new HashMap<>();

    for (Fact fact : idb) {
      if (!map.containsKey(fact.eString())) {
        ArrayList<Fact> facts = new ArrayList<>();
        map.put(fact.eString(), facts);
      }
      map.get(fact.eString()).add(fact);
    }

    List<Fact> facts = new ArrayList<>();

    for (Map.Entry<String, List<Fact>> entry : map.entrySet()) {
      facts.add(combineFacts(entry.getValue()));
    }
    return facts;
  }

  private boolean isUpdate(@Var List<Fact> idb, int count) {

    @Var
    boolean res = false;
    idb = dealIdb(idb);

    for (Fact fact : idb) {
      if (!database.containsKey(fact.eString())) {

        database.put(fact.eString(), fact);
        res = true;

        if (!factMap.containsKey(fact.predicate)) {
          ArrayList<Fact> temp = new ArrayList<>();
          temp.add(fact);
          factMap.put(fact.predicate, temp);
        } else {
          factMap.get(fact.predicate).add(fact);
        }
      } else {
        if (hasProbability) {
          if (database.get(fact.eString()).probability != fact.probability) {
            if (count == 1) {
              double probability = database.get(fact.eString()).probability;
              database.get(fact.eString()).probability = calPro(probability, fact.probability);
              res = true;
            } else if (database.get(fact.eString()).probability < fact.probability) {
              database.get(fact.eString()).probability = fact.probability;
              res = true;
            } else
              res = false;
            if (res) {
              for (Fact f : factMap.get(fact.predicate)) {
                if (f.eString().equals(fact.eString())) {
                  f.probability = database.get(fact.eString()).probability;
                  break;
                }
              }
            }
          }
        }
      }
    }
    return res;
  }

  private double calPro(double p1, double p2) {
    BigDecimal x1 = BigDecimal.valueOf(p1);
    BigDecimal x2 = BigDecimal.valueOf(p2);
    if (useMax) {
      return Math.max(x1.doubleValue(), x2.doubleValue());
    }
    return x1.add(x2).subtract(x1.multiply(x2)).doubleValue();
  }

  public void naive() {

    @Var
    boolean isUpdate = true;

    System.out.println("edb: " + factMap);
    System.out.println("rules is " + rules);

    @Var
    int count = 1;

    while (isUpdate) {

      isUpdate = false;
      @Var
      List<Fact> idb = new ArrayList<>();

      for (Rule rule : rules) {
        List<List<Fact>> temp = inferFacts(Tree(rule), rule);
        for (List<Fact> facts : temp) {
          idb.addAll(facts);
        }
      }

      isUpdate = isUpdate(idb, count);
      count++;
    }
    System.out.println("The iteration time is: " + count);
  }
}

