package jambalaya.listeners;

import java.util.UUID;

import org.apache.cayenne.BaseDataObject;
import org.apache.cayenne.annotation.PostAdd;

import jambalaya.interfaces.UUIDStamped;

/**
 * Looks at objects before they are committed. If they implement "UUIDStamped", the uniqueID field of the object will be updated on addition.
 */

public class UUIDStampedListener {

	@PostAdd({ BaseDataObject.class })
	public void handleAdd( BaseDataObject object ) {
		if( object instanceof UUIDStamped ) {
			((UUIDStamped)object).setUniqueID( UUID.randomUUID() );
		}
	}
}