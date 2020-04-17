package boomerang.scene;

public interface IfStatement {
  enum Evaluation {
    TRUE,
    FALSE,
    UNKOWN
  };

  Statement getTarget();

  Evaluation evaluate(Val val);

  boolean uses(Val val);
}
