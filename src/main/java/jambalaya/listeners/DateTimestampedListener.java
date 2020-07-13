package jambalaya.listeners;

import java.time.LocalDateTime;
import java.util.Objects;

import org.apache.cayenne.BaseDataObject;
import org.apache.cayenne.DataObject;
import org.apache.cayenne.DataRow;
import org.apache.cayenne.ObjectId;
import org.apache.cayenne.access.DataContext;
import org.apache.cayenne.annotation.PostAdd;
import org.apache.cayenne.annotation.PreUpdate;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.ObjEntity;

import jambalaya.interfaces.DateTimeStampedCreation;
import jambalaya.interfaces.DateTimeStampedModification;

/**
 * Looks at objects before they are committed. If they implement "Timestamped", the corresponding values of the objects will be updated on creation and update.
 */

public class DateTimestampedListener {

	@PostAdd({ BaseDataObject.class })
	public void handleAdd( BaseDataObject object ) {
		if( object instanceof DateTimeStampedCreation ) {
			((DateTimeStampedCreation)object).setCreationDate( LocalDateTime.now() );
		}

		if( object instanceof DateTimeStampedModification ) {
			((DateTimeStampedModification)object).setModificationDate( LocalDateTime.now() );
		}
	}

	@PreUpdate({ BaseDataObject.class })
	public void handleUpdate( BaseDataObject object ) {
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
	public static boolean hasChangesToOwnData( final DataObject dataObject ) {
		final DataContext dc = (DataContext)dataObject.getObjectContext();
		final ObjectId objectId = dataObject.getObjectId();
		final ObjEntity entity = dc.getEntityResolver().getObjEntity( objectId.getEntityName() );
		final DataRow snapshot = dc.getObjectStore().getSnapshot( objectId );

		for( final ObjAttribute objAttribute : entity.getAttributes() ) {
			final Object originalValue = snapshot.get( objAttribute.getDbAttributeName() );
			final Object currentValue = dataObject.readPropertyDirectly( objAttribute.getName() );

			if( !Objects.equals( originalValue, currentValue ) ) {
				return true;
			}
		}

		return false;
	}
}