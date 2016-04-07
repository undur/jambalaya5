package jambalaya.interfaces;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import jambalaya.model.Painting;

public class TestDateTimeStamped {

	@Test
	public void testCreationAndModificationDateAssignedAtObjectAdd() {
		Painting painting = Painting.createPainting( TestCore.newContext() );
		assertNotNull( painting.creationDate() );
		assertNotNull( painting.modificationDate() );
	}
}