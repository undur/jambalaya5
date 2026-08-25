package jambalaya.interfaces;

import java.util.UUID;

import javax.sql.DataSource;

import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.configuration.DataNodeDescriptor;
import org.apache.cayenne.datasource.CayenneDataSource;
import org.apache.cayenne.runtime.CayenneRuntime;

import jambalaya.listeners.DateTimestampedListener;
import jambalaya.listeners.UniqueIDStampedListener;

/**
 * Cayenne setup for tests
 */

public class TestCore {

	private static CayenneRuntime _serverRuntime;

	public static CayenneRuntime serverRuntime() {
		if( _serverRuntime == null ) {
			final DataSource dataSource = CayenneDataSource.of( "jdbc:h2:mem:" + UUID.randomUUID().toString() )
					.driverClass( "org.h2.Driver" )
					.build();

			final DataNodeDescriptor dnd = DataNodeDescriptor
					.of( "testnode" )
					.dataSource( dataSource )
					.createSchemaIfNeeded()
					.build();

			_serverRuntime = CayenneRuntime
					.of()
					.addConfig( "cayenne-project.xml" )
					.build();

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