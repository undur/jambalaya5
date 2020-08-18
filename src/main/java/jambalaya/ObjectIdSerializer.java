package jambalaya;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.cayenne.Cayenne;
import org.apache.cayenne.DataObject;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.ObjectId;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.ObjEntity;

public class ObjectIdSerializer {

	private static final String PK_ELEMENT_SEPARATOR = "|";

	public static String serialize( ObjectId oid ) {
		Map<String, Object> idSnapshot = oid.getIdSnapshot();
		List<String> keys = new ArrayList<>( idSnapshot.keySet() );
		keys.sort( Comparator.naturalOrder() );

		StringBuilder b = new StringBuilder();

		int i = 0;

		for( String key : keys ) {
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

		return ObjectId.of( objEntityName, keyMap );
	}

	/**
	 * This method should really be called something else entirely (if it should even exist)
	 */
	@Deprecated
	public static DataObject eo( ObjectContext ec, String entityName, String pkString ) {
		return (DataObject)Cayenne.objectForPK( ec, entityName, pkString );
	}
}