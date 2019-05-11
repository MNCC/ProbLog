package types;

import com.google.errorprone.annotations.CheckReturnValue;

@CheckReturnValue
public class Rule {

  public final Literal head;
  public final Literal[] body;
  public final double probability;

  public Rule(Literal head, Literal[] body, double probability) {
    this.head = head;
    this.body = body;
    this.probability = probability;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append(head.toString());
    sb.append(" :-");
    for (int i = 0; i < body.length - 1; i++) {
      sb.append(body[i].toString());
      sb.append(" ,");
    }
    sb.append(body[body.length - 1]);
    if (probability != 1) {
      sb.append(" :");
      sb.append(probability);
    }
    return sb.toString();
  }
}
