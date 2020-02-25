package boomerang;

import boomerang.scene.Statement;
import java.util.Collection;

public interface IContextRequester {
  public Collection<Context> getCallSiteOf(Context child);

  public Context initialContext(Statement stmt);
}
