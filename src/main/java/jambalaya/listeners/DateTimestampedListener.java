package jambalaya.listeners;

import java.time.LocalDateTime;

import org.apache.cayenne.CayenneDataObject;
import org.apache.cayenne.annotation.PostAdd;
import org.apache.cayenne.annotation.PostUpdate;

import jambalaya.interfaces.DateTimeStampedCreation;
import jambalaya.interfaces.DateTimeStampedModification;

/**
 * Looks at objects before they are committed. If they implement "Timestamped", the corresponding values of the objects will be updated on creation and update.
 */

public class DateTimestampedListener {

	@PostAdd( { CayenneDataObject.class } )
	public void handleAdd( CayenneDataObject object ) {
		if( object instanceof DateTimeStampedCreation ) {
			((DateTimeStampedCreation)object).setCreationDate( LocalDateTime.now() );
		}

		if( object instanceof DateTimeStampedModification ) {
			((DateTimeStampedModification)object).setModificationDate( LocalDateTime.now() );
		}
	}

	@PostUpdate( { CayenneDataObject.class } )
	public void handleUpdate( CayenneDataObject object ) {
		if( object instanceof DateTimeStampedModification ) {
			((DateTimeStampedModification)object).setModificationDate( LocalDateTime.now() );
		}
	}
}