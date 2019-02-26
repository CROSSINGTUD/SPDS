package boomerang;

import java.util.Collection;

import boomerang.jimple.Statement;

public interface IContextRequester {
    public Collection<Context> getCallSiteOf(Context child);

    public Context initialContext(Statement stmt);
}