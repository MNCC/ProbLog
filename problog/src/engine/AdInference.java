package engine;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.errorprone.annotations.CheckReturnValue;
import com.google.errorprone.annotations.Var;
import parser.DatalogParser;
import types.Fact;
import types.Literal;
import types.Rule;

@CheckReturnValue
class RuleTree {

  final Fact val;
  final ArrayList<RuleTree> child;
  RuleTree par = null;

  public RuleTree(Fact val) {
    this.val = val;
    this.child = new ArrayList<>();
  }

  @Override
  public String toString() {
    return val.toString();
  }
}


@CheckReturnValue
public class AdInference {

  private final DatalogParser parser;
  private final Map<String, Fact> database;
  private final ArrayList<Rule> rules;
  private final ArrayList<RuleTree> trees;
  private final HashMap<String, Fact> factCollections;
  public HashMap<String, ArrayList<Fact>> factMap;
  public boolean useMax = false;
  private boolean hasProbability;
  private ArrayList<Fact> preIDB;
  private ArrayList<Fact> curIDB;

  public AdInference(String textName, boolean hasProbability) throws IOException {
    parser = new DatalogParser();
    parser.dataReader(textName);
    this.hasProbability = hasProbability;
    parser.parse(this.hasProbability);
    database = parser.buildMap();
    rules = parser.rules;
    trees = new ArrayList<>();
    preIDB = new ArrayList<>();
    curIDB = new ArrayList<>();
    factCollections = new HashMap<>();
    init();
  }

  private void init() {
    factMap = new HashMap<>();
    for (Map.Entry<String, Fact> entry : database.entrySet()) {
      if (!factMap.containsKey(entry.getValue().predicate)) {
        ArrayList<Fact> facts = new ArrayList<>();
        facts.add(entry.getValue());
        factMap.put(entry.getValue().predicate, facts);
      } else {
        factMap.get(entry.getValue().predicate).add(entry.getValue());
      }
    }
    for (Fact fact : parser.edb) {
      String index = fact.eString() + ":";
      Fact factNew = new Fact(fact.predicate, fact.constants);
      if (hasProbability) {
        factNew.probability = fact.probability;
      }
      if (!factCollections.containsKey(index)) {
        factCollections.put(index, factNew);
      }
    }
  }

  private void getOnePath(@Var RuleTree tree, List<Fact> facts) {
    while (!tree.val.predicate.contains(":-")) {
      facts.add(tree.val);
      tree = tree.par;
    }
  }

  private void dfsTree(int depth, RuleTree tree, List<Fact> facts, Rule rule) {
    if (depth == rule.body.length) {
      getOnePath(tree, facts);
    } else {
      for (int i = 0; i < tree.child.size(); i++) {
        dfsTree(depth + 1, tree.child.get(i), facts, rule);
      }
    }
  }

  private void semiDfs(int depth, RuleTree tree, List<Fact> facts, Fact newFact, Rule rule) {
    if (depth < rule.body.length) {
      if (tree.val.eString().equals(newFact.eString())) {
        dfsTree(depth + 1, tree, facts, rule);
      } else {
        for (int i = 0; i < tree.child.size(); i++) {
          semiDfs(depth + 1, tree.child.get(i), facts, newFact, rule);
        }
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

    HashMap<String, String> model = new HashMap<>();

    for (int k = 0; k < rule.body.length; k++) {
      Literal literal = rule.body[k];
      for (int i = 0; i < literal.variables.length; i++) {
        if (!model.containsKey(literal.variables[i])) {
          model.put(literal.variables[i], facts.get(k).constants[i]);
        }
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
      double probability = useMax ? product(facts) : min(facts);
      fact.probability = probability * rule.probability;
    }

    updateCollection(fact, facts);
    return fact;
  }

  @CanIgnoreReturnValue
  private boolean updateCollection(Fact fact, List<Fact> facts) {

    StringBuilder sb = new StringBuilder();
    Fact factNew = new Fact(fact.predicate, fact.constants);

    if (hasProbability) {
      factNew.probability = fact.probability;
    }

    sb.append(fact.eString());
    sb.append(":");

    for (Fact e : facts) {
      sb.append(e.eString());
    }

    @Var
    boolean isUpdate = false;
    String key = sb.toString();

    if (!factCollections.containsKey(key)) {
      factCollections.put(key, factNew);
      isUpdate = true;
    } else {
      if (hasProbability) {
        if (factCollections.get(key).probability < fact.probability) {
          factCollections.get(key).probability = fact.probability;
          isUpdate = true;
        }
      }
    }
    return isUpdate;
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

  private List<List<Fact>> semiTree(Fact fact, Rule rule) {

    List<List<Fact>> res = new ArrayList<>();
    @Var
    RuleTree root = null;

    for (RuleTree tree : trees) {
      if (tree.val.predicate.equals(rule.toString())) {
        root = tree;
        break;
      }
    }

    int max = rule.body.length - 1;

    if (fact.predicate.equals(rule.body[0].predicate)) {
      if (hasProbability) {
        if (!database.containsKey(fact.eString())) {
          doUpdate(0, fact, root, rule);
        }
      } else {
        doUpdate(0, fact, root, rule);
      }
    }

    for (RuleTree tree : root.child) {
      List<Fact> facts = new ArrayList<>();
      if (hasProbability) {
        if (!database.containsKey(fact.eString())) {
          updateTree(1, fact, tree, max, rule);
        }
      } else {
        updateTree(1, fact, tree, max, rule);
      }
      semiDfs(0, tree, facts, fact, rule);
      res.add(facts);
    }
    return inferFacts(res, rule);
  }

  private List<List<Fact>> Tree(Rule rule) {

    String[] constants = new String[1];
    constants[0] = "root";
    Fact fact = new Fact(rule.toString(), constants);
    HashMap<String, String> model = new HashMap<>();
    RuleTree tree = new RuleTree(fact);
    buildTree(0, rule, model, tree);
    trees.add(tree);
    List<List<Fact>> collection = new ArrayList<>();

    for (int i = 0; i < tree.child.size(); i++) {
      List<Fact> facts = new ArrayList<>();
      dfsTree(1, tree.child.get(i), facts, rule);
      collection.add(facts);
    }
    return collection;
  }

  private void doUpdate(int depth, Fact fact, RuleTree tree, Rule rule) {

    RuleTree rt = new RuleTree(fact);
    tree.child.add(rt);
    rt.par = tree;

    if (depth < rule.body.length - 1) {

      Literal goal = rule.body[depth];
      HashMap<String, String> model = new HashMap<>();

      for (int i = 0; i < fact.constants.length; i++) {
        if (!model.containsKey(goal.variables[i].trim())) {
          model.put(goal.variables[i].trim(), fact.constants[i].trim());
        }
      }
      buildTree(depth + 1, rule, model, rt);
    }
  }

  private void updateTree(int depth, Fact fact, RuleTree tree, int depthMax, Rule rule) {
    if (depth <= depthMax) {

      Literal curGoal = rule.body[depth];

      if (curGoal.predicate.equals(fact.predicate)) {

        HashMap<String, String> model = new HashMap<>();
        Literal lastMatch = rule.body[depth - 1];
        Fact lastFact = tree.val;

        for (int i = 0; i < lastMatch.variables.length; i++) {
          if (!model.containsKey(lastMatch.variables[i].trim())) {
            model.put(lastMatch.variables[i].trim(), lastFact.constants[i].trim());
          }
        }

        @Var
        boolean isMatch = true;

        for (int i = 0; i < curGoal.variables.length; i++) {
          if (model.containsKey(curGoal.variables[i].trim())
              && !model.get(curGoal.variables[i].trim()).equals(fact.constants[i].trim())) {
            isMatch = false;
            break;
          }
        }

        if (isMatch) {
          doUpdate(depth, fact, tree, rule);
        } else {
          for (RuleTree t : tree.child) {
            updateTree(depth + 1, fact, t, depthMax, rule);
          }
        }
      }
    }
  }

  private void buildTree(int depth, Rule rule, HashMap<String, String> model, RuleTree tree) {
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

            HashMap<String, String> curModel = new HashMap<>();

            for (int i = 0; i < fact.constants.length; i++) {
              if (!curModel.containsKey(goal.variables[i].trim())) {
                curModel.put(goal.variables[i].trim(), fact.constants[i].trim());
              }
            }

            RuleTree node = new RuleTree(fact);
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
    double product = 1;
    for (Fact fact : facts) {
      product = product * fact.probability;
    }
    return product;
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

    Fact factNew = new Fact(facts.get(0).predicate, facts.get(0).constants);
    factNew.probability = sum;
    return factNew;
  }

  private ArrayList<Fact> dealIdb(List<Fact> idb) {

    HashMap<String, ArrayList<Fact>> map = new HashMap<>();

    for (Fact fact : idb) {
      if (!map.containsKey(fact.eString())) {
        ArrayList<Fact> facts = new ArrayList<>();
        map.put(fact.eString(), facts);
      }
      map.get(fact.eString()).add(fact);
    }

    ArrayList<Fact> facts = new ArrayList<>();

    for (Map.Entry<String, ArrayList<Fact>> entry : map.entrySet()) {
      facts.add(combineFacts(entry.getValue()));
    }
    return facts;
  }

  private void trim(Fact fact) {
    for (Rule rule : rules) {
      @Var
      RuleTree root = null;
      for (RuleTree tree : trees) {
        if (tree.val.predicate.equals(rule.toString())) {
          root = tree;
          break;
        }
      }
      if (rule.body[0].predicate.equals(fact.predicate)) {
        doUpdate(0, fact, root, rule);
      }
      for (RuleTree tree : root.child) {
        updateTree(1, fact, tree, rule.body.length - 1, rule);
      }
    }
  }

  private List<Fact> dupFactRemove(@Var List<Fact> idb) {

    Map<String, Fact> factsIndex = new HashMap<>();
    List<Fact> facts = new ArrayList<>();
    idb = dealIdb(idb);
    @Var
    List<Fact> factsNew = new ArrayList<>();

    for (Map.Entry<String, Fact> entry : factCollections.entrySet()) {
      Fact fact = new Fact(entry.getValue().predicate, entry.getValue().constants);
      if (hasProbability) {
        fact.probability = entry.getValue().probability;
      }
      factsNew.add(fact);
    }

    factsNew = dealIdb(factsNew);

    for (Fact fact : factsNew) {
      if (!factsIndex.containsKey(fact.eString())) {
        factsIndex.put(fact.eString(), fact);
      }
    }

    for (Fact fact : idb) {
      if (factsIndex.containsKey(fact.eString())) {
        facts.add(factsIndex.get(fact.eString()));
      }
    }
    return facts;
  }

  private boolean isUpdate(List<Fact> idb) {

    @Var
    boolean isOk = false;
    List<Fact> facts = new ArrayList<>();

    for (Fact fact : idb) {
      if (!database.containsKey(fact.eString())) {

        database.put(fact.eString(), fact);
        isOk = true;
        facts.add(fact);

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

            database.get(fact.eString()).probability = fact.probability;
            isOk = true;

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

    if (hasProbability) {
      for (Fact fact : facts) {
        trim(fact);
      }
    }
    return isOk;
  }

  private double calPro(double p1, double p2) {
    BigDecimal x1 = BigDecimal.valueOf(p1);
    BigDecimal x2 = BigDecimal.valueOf(p2);
    if (useMax) {
      return Math.max(x1.doubleValue(), x2.doubleValue());
    }
    return x1.add(x2).subtract(x1.multiply(x2)).doubleValue();
  }

  @CanIgnoreReturnValue
  private boolean semi_update() {

    @Var
    boolean isChange = false;
    List<Fact> idb = new ArrayList<>();
    Map<String, Fact> map = new HashMap<>();
    List<Fact> temp = (ArrayList<Fact>) dealIdb(curIDB).clone();

    for (Fact fact : preIDB) {
      if (!map.containsKey(fact.eString()))
        map.put(fact.eString(), fact);
    }

    for (Fact fact : temp) {
      if (!map.containsKey(fact.eString())) {
        idb.add(fact);
        isChange = true;
      } else {
        if (hasProbability) {
          if (map.get(fact.eString()).probability != fact.probability) {
            idb.add(fact);
            isChange = true;
          }
        }
      }
    }

    preIDB = (ArrayList<Fact>) curIDB.clone();
    curIDB = new ArrayList<>(dupFactRemove(idb));

    return isChange;

  }

  public void semi_naive() {

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

      if (count == 1) {
        for (Rule r : rules) {
          List<List<Fact>> temp = inferFacts(Tree(r), r);
          for (List<Fact> facts : temp) {
            idb.addAll(facts);
          }
        }
      } else {
        List<List<List<Fact>>> facts = new ArrayList<>();
        for (Fact fact : curIDB) {
          for (Rule rule : rules) {
            facts.add(semiTree(fact, rule));
          }
        }
        for (List<List<Fact>> temp : facts) {
          for (List<Fact> factz : temp) {
            idb.addAll(factz);
          }
        }
      }

      idb = dupFactRemove(idb);
      curIDB = new ArrayList<>(idb);

      semi_update();

      isUpdate = isUpdate(idb);
      count++;
    }
    System.out.println("The iteration time is: " + count);
  }
}
