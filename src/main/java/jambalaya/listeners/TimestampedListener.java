package jambalaya.listeners;

import java.util.Date;

import org.apache.cayenne.CayenneDataObject;
import org.apache.cayenne.annotation.PostAdd;
import org.apache.cayenne.annotation.PostUpdate;

import jambalaya.interfaces.TimeStampedCreation;
import jambalaya.interfaces.TimeStampedModification;

/**
 * Looks at objects before they are committed. If they implement "Timestamped", the corresponding values of the objects will be updated on creation and update.
 */

public class TimestampedListener {

	@PostAdd( { CayenneDataObject.class } )
	public void handleAdd( CayenneDataObject object ) {
		if( object instanceof TimeStampedCreation ) {
			((TimeStampedCreation)object).setCreationDate( new Date() );
		}

		if( object instanceof TimeStampedModification ) {
			((TimeStampedModification)object).setModificationDate( new Date() );
		}
	}

	@PostUpdate( { CayenneDataObject.class } )
	public void handleUpdate( CayenneDataObject object ) {
		if( object instanceof TimeStampedModification ) {
			((TimeStampedModification)object).setModificationDate( new Date() );
		}
	}
}