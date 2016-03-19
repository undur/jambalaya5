package jambalaya.interfaces;

import java.time.LocalDate;

public interface DateTimeStampedCreation {

	public abstract LocalDate creationDate();

	public abstract void setCreationDate( LocalDate t );
}