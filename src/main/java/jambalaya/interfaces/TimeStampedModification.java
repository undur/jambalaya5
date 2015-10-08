package jambalaya.interfaces;

import java.util.Date;

public interface TimeStampedModification {

	public abstract Date modificationDate();

	public abstract void setModificationDate( Date t );
}