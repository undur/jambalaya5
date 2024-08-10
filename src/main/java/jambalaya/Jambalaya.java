package jambalaya;

import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.runtime.CayenneRuntime;

public class Jambalaya {

	/**
	 * The processes global ServerRuntime
	 */
	private static CayenneRuntime _runtime;

	public static void setServerRuntime( CayenneRuntime runtime ) {
		_runtime = runtime;
	}

	public static CayenneRuntime serverRuntime() {

		if( _runtime == null ) {
			throw new IllegalStateException( "A server runtime has not been set" );
		}

		return _runtime;
	}

	public static ObjectContext newContext() {
		return serverRuntime().newContext();
	}

	public static ObjectContext newContext( ObjectContext oc ) {
		return serverRuntime().newContext( oc );
	}
}