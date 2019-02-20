package jambalaya.listeners;

import java.util.Date;

import org.apache.cayenne.BaseDataObject;
import org.apache.cayenne.annotation.PostAdd;
import org.apache.cayenne.annotation.PreUpdate;

import jambalaya.interfaces.TimeStampedCreation;
import jambalaya.interfaces.TimeStampedModification;

/**
 * Looks at objects before they are committed. If they implement "Timestamped", the corresponding values of the objects will be updated on creation and update.
 */

public class TimestampedListener {

	@PostAdd( { BaseDataObject.class } )
	public void handleAdd( BaseDataObject object ) {
		if( object instanceof TimeStampedCreation ) {
			((TimeStampedCreation)object).setCreationDate( new Date() );
		}

		if( object instanceof TimeStampedModification ) {
			((TimeStampedModification)object).setModificationDate( new Date() );
		}
	}

	@PreUpdate( { BaseDataObject.class } )
	public void handleUpdate( BaseDataObject object ) {
		if( object instanceof TimeStampedModification ) {
			((TimeStampedModification)object).setModificationDate( new Date() );
		}
	}
}