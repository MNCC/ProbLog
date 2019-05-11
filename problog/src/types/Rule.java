package types;

import com.google.errorprone.annotations.CheckReturnValue;

@CheckReturnValue
public class Rule {

  public final Literal head;
  public final Literal[] bodys;
  public final double probability;

  public Rule(Literal head, Literal[] bodys, double probability) {
    this.head = head;
    this.bodys = bodys;
    this.probability = probability;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append(head.toString());
    sb.append(" :-");
    for (int i = 0; i < bodys.length - 1; i++) {
      sb.append(bodys[i].toString());
      sb.append(" ,");
    }
    sb.append(bodys[bodys.length - 1]);
    if (probability != 1) {
      sb.append(" :");
      sb.append(probability);
    }
    return sb.toString();
  }
}
