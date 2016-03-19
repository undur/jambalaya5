package jambalaya.interfaces;

import java.time.LocalDateTime;

public interface DateTimeStampedCreation {

	public abstract LocalDateTime creationDate();

	public abstract void setCreationDate( LocalDateTime t );
}