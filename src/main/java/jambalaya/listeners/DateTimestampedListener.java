package jambalaya.listeners;

import java.time.LocalDateTime;
import java.util.Objects;

import org.apache.cayenne.DataRow;
import org.apache.cayenne.ObjectId;
import org.apache.cayenne.PersistentObject;
import org.apache.cayenne.access.DataContext;
import org.apache.cayenne.annotation.PostAdd;
import org.apache.cayenne.annotation.PreUpdate;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.ObjEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jambalaya.interfaces.DateTimeStampedCreation;
import jambalaya.interfaces.DateTimeStampedModification;

/**
 * Looks at objects before they are committed. If they implement "Timestamped", the corresponding values of the objects will be updated on creation and update.
 */

public class DateTimestampedListener {

	private static final Logger logger = LoggerFactory.getLogger( DateTimestampedListener.class );

	@PostAdd({ PersistentObject.class })
	public void handleAdd( PersistentObject object ) {
		if( object instanceof DateTimeStampedCreation ) {
			((DateTimeStampedCreation)object).setCreationDate( LocalDateTime.now() );
		}

		if( object instanceof DateTimeStampedModification ) {
			((DateTimeStampedModification)object).setModificationDate( LocalDateTime.now() );
		}
	}

	@PreUpdate({ PersistentObject.class })
	public void handleUpdate( PersistentObject object ) {
		if( object instanceof DateTimeStampedModification ) {
			if( hasChangesToOwnData( object ) ) {
				((DateTimeStampedModification)object).setModificationDate( LocalDateTime.now() );
			}
		}
	}

	/**
	 * @return True if the object has changes to "itself" (as in, changes to attributes or FKs (to-one relationships)
	 *
	 * Meant to prevent marking objects that only have changes in "to-many" relationships as modified.
	 *
	 * FIXME: This does not currently take relationships into account, only direct changes to attributes // Hugi 2020-07-13
	 */
	public static boolean hasChangesToOwnData( final PersistentObject dataObject ) {
		final DataContext dc = (DataContext)dataObject.getObjectContext();
		final ObjectId objectId = dataObject.getObjectId();
		final ObjEntity entity = dc.getEntityResolver().getObjEntity( objectId.getEntityName() );
		final DataRow snapshot = dc.getObjectStore().getSnapshot( objectId );

		if( snapshot != null ) {
			for( final ObjAttribute objAttribute : entity.getAttributes() ) {
				final String dbAttributeName = objAttribute.getDbAttributeName();
				final Object originalValue = snapshot.get( dbAttributeName );
				final Object currentValue = dataObject.readPropertyDirectly( objAttribute.getName() );

				if( !Objects.equals( originalValue, currentValue ) ) {
					return true;
				}
			}
		}
		else {
			// FIXME: This should never happen but it does. Need to find out why // Hugi 2022-05-26
			logger.error( "Snapshot is null for oid: " + objectId );
		}

		return false;
	}
}