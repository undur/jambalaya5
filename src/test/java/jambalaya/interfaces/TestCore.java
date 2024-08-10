package jambalaya.interfaces;

import java.util.UUID;

import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.access.dbsync.CreateIfNoSchemaStrategy;
import org.apache.cayenne.access.dbsync.SchemaUpdateStrategy;
import org.apache.cayenne.runtime.CayenneRuntime;
import org.apache.cayenne.runtime.CayenneRuntimeBuilder;

import jambalaya.listeners.DateTimestampedListener;
import jambalaya.listeners.UniqueIDStampedListener;

/**
 * Systemwide database logic.
 */

public class TestCore {

	private static CayenneRuntime _serverRuntime;

	public static CayenneRuntime serverRuntime() {
		if( _serverRuntime == null ) {
			CayenneRuntimeBuilder b = CayenneRuntime.builder();
			b.addConfig( "cayenne-project.xml" );
			b = b.addModule( binder -> binder.bind( SchemaUpdateStrategy.class ).to( CreateIfNoSchemaStrategy.class ) );
			b = b.jdbcDriver( "org.h2.Driver" );
			b = b.url( "jdbc:h2:mem:" + UUID.randomUUID().toString() );

			_serverRuntime = b.build();

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