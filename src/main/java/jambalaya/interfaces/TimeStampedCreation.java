package jambalaya.interfaces;

import java.util.Date;

public interface TimeStampedCreation {

	public abstract Date creationDate();

	public abstract void setCreationDate( Date t );
}