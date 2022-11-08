package jambalaya.interfaces;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

import jambalaya.model.Painting;

public class TestDateTimeStamped {

	@Test
	public void testCreationAndModificationDateAssignedAtObjectAdd() {
		Painting painting = TestCore.newContext().newObject( Painting.class );
		assertNotNull( painting.creationDate() );
		assertNotNull( painting.modificationDate() );
	}
}