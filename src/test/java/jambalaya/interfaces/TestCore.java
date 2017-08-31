package jambalaya.interfaces;

import java.util.UUID;

import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.access.dbsync.CreateIfNoSchemaStrategy;
import org.apache.cayenne.access.dbsync.SchemaUpdateStrategy;
import org.apache.cayenne.configuration.server.ServerRuntime;
import org.apache.cayenne.configuration.server.ServerRuntimeBuilder;

import jambalaya.listeners.DateTimestampedListener;
import jambalaya.listeners.TimestampedListener;
import jambalaya.listeners.UniqueIDStampedListener;

/**
 * Systemwide database logic.
 */

public class TestCore {

	private static ServerRuntime _serverRuntime;

	public static ServerRuntime serverRuntime() {
		if( _serverRuntime == null ) {
			ServerRuntimeBuilder b = ServerRuntime.builder();
			b.addConfig( "cayenne-project.xml" );
			b = b.addModule( binder -> binder.bind( SchemaUpdateStrategy.class ).to( CreateIfNoSchemaStrategy.class ) );
			b = b.jdbcDriver( "org.h2.Driver" );
			b = b.url( "jdbc:h2:mem:" + UUID.randomUUID().toString() );

			_serverRuntime = b.build();

			_serverRuntime.getDataDomain().addListener( new TimestampedListener() );
			_serverRuntime.getDataDomain().addListener( new DateTimestampedListener() );
			_serverRuntime.getDataDomain().addListener( new UniqueIDStampedListener() );
		}

		return _serverRuntime;
	}

	public static void reset() {
		_serverRuntime = null;
	}

	/**
	 * @return A new ObjectContext.
	 */
	public static ObjectContext newContext() {
		return serverRuntime().newContext();
	}
}