package jambalaya.listeners;

import java.util.UUID;

import org.apache.cayenne.PersistentObject;
import org.apache.cayenne.annotation.PostAdd;

import jambalaya.interfaces.UniqueIDStamped;

/**
 * Looks at objects before they are committed. If they implement "UniqueIDStamped", the uniqueID field of the object will be updated on addition.
 */

public class UniqueIDStampedListener {

	@PostAdd({ PersistentObject.class })
	public void handleAdd( PersistentObject object ) {
		if( object instanceof UniqueIDStamped ) {
			((UniqueIDStamped)object).setUniqueID( UUID.randomUUID().toString() );
		}
	}
}