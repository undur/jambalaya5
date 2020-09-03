package jambalaya.interfaces;

import java.util.UUID;

public interface UUIDStamped {

	public UUID uniqueID();

	public void setUniqueID( UUID value );
}