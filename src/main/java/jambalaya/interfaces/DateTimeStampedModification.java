package jambalaya.interfaces;

import java.time.LocalDate;

public interface DateTimeStampedModification {

	public abstract LocalDate modificationDate();

	public abstract void setModificationDate( LocalDate t );
}