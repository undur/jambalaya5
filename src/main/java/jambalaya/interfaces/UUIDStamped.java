package jambalaya.interfaces;

import java.util.UUID;

import org.apache.cayenne.Persistent;

public interface UUIDStamped extends Persistent {

	public UUID uniqueID();

	public void setUniqueID( UUID value );
}