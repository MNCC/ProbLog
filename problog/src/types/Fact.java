package types;

import com.google.errorprone.annotations.CheckReturnValue;

@CheckReturnValue
public class Fact {

  public final String predicate;
  public final String[] constants;
  public double probability = 1.0d;

  public Fact(String predicate, String[] constants) {
    this.predicate = predicate;
    this.constants = constants;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append(predicate);
    sb.append("(");
    for (int i = 0; i < constants.length - 1; i++) {
      sb.append(constants[i]);
      sb.append(",");
    }
    sb.append(constants[constants.length - 1]);
    sb.append(")");
    if (probability != 2) {
      sb.append(" :");
      sb.append(probability);
    }
    return sb.toString();
  }

  public String eString() {
    StringBuilder sb = new StringBuilder();
    sb.append(predicate);
    sb.append("(");
    for (int i = 0; i < constants.length - 1; i++) {
      sb.append(constants[i]);
      sb.append(",");
    }
    sb.append(constants[constants.length - 1]);
    sb.append(")");
    return sb.toString();
  }
}
