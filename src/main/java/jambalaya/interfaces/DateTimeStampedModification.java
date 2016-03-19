package jambalaya.interfaces;

import java.time.LocalDateTime;

public interface DateTimeStampedModification {

	public abstract LocalDateTime modificationDate();

	public abstract void setModificationDate( LocalDateTime t );
}