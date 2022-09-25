package jambalaya;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.ObjectId;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.ObjEntity;

public class ObjectIdSerializer {

	private static final String PK_ELEMENT_SEPARATOR = "|";

	public static String serialize( ObjectId oid ) {
		final Map<String, Object> idSnapshot = oid.getIdSnapshot();
		final List<String> keys = new ArrayList<>( idSnapshot.keySet() );
		keys.sort( Comparator.naturalOrder() );

		final StringBuilder b = new StringBuilder();

		int i = 0;

		for( final String key : keys ) {
			if( i++ > 0 ) {
				b.append( PK_ELEMENT_SEPARATOR );
			}

			b.append( idSnapshot.get( key ) );
		}

		return b.toString();
	}

	public static ObjectId deserialize( final ObjectContext oc, final String objEntityName, final String identifier ) {
		final ObjEntity objEntity = oc.getEntityResolver().getObjEntity( objEntityName );
		final Collection<DbAttribute> primaryKeyAttributes = objEntity.getDbEntity().getPrimaryKeys();
		final String[] components = identifier.split( "\\|" );

		final Map<String, Object> keyMap = new HashMap<>();

		int i = 0;

		for( DbAttribute attribute : primaryKeyAttributes ) {
			keyMap.put( attribute.getName(), components[i++] );
		}

		// FIXME: This is here to bridge the API gap between Cayenne 4.1 and 4.2. Remove once everything is on 4.2 // Hugi 2020-09-17
		// First we try the 4.1 method of using a constructor
		try {
			final Constructor<ObjectId> constructor = ObjectId.class.getConstructor( String.class, Map.class );
			final ObjectId objectId = constructor.newInstance( objEntityName, keyMap );
			return objectId;
		}
		catch( NoSuchMethodException | SecurityException | InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e ) {
			// If that failed, we try doing it the 4.2 way
			try {
				final Method method = ObjectId.class.getMethod( "of", String.class, Map.class );
				final ObjectId objectId = (ObjectId)method.invoke( null, objEntityName, keyMap );
				return objectId;
			}
			catch( NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e1 ) {
				throw new RuntimeException( "Failed to construct an ObjectId", e );
			}
		}
		//		return new ObjectId( objEntityName, keyMap );
		//		return ObjectId.of( objEntityName, keyMap ); // FIXME: For Cayenne 4.2
	}
}