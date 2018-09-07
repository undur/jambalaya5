package jambalaya.interfaces;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

import jambalaya.model.Artist;

public class TestUniqueIDStamped {

	@Test
	public void testUniqueIDAssignedAtObjectAdd() {
		Artist artist = Artist.createArtist( TestCore.newContext() );
		assertNotNull( artist.uniqueID() );
	}
}