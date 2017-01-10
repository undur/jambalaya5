package jambalaya;

import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.configuration.server.ServerRuntime;
import org.apache.cayenne.configuration.server.ServerRuntimeBuilder;

public class Jambalaya {

	/**
	 * The processes global ServerRuntime
	 */
	private static ServerRuntime _serverRuntime;

	public static void setServerRuntime( ServerRuntime runtime ) {
		_serverRuntime = runtime;
	}

	public static ServerRuntime serverRuntime() {
		if( _serverRuntime == null ) {
			setServerRuntime( new ServerRuntimeBuilder().build() );
		}

		return _serverRuntime;
	}

	public static ObjectContext newContext() {
		return serverRuntime().newContext();
	}
}