package jambalaya.interfaces;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

import jambalaya.model.Artist;

public class TestUniqueIDStamped {

	@Test
	public void testUniqueIDAssignedAtObjectAdd() {
		Artist artist = TestCore.newContext().newObject( Artist.class );
		assertNotNull( artist.uniqueID() );
	}
}