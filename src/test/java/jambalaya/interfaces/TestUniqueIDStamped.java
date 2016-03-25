package jambalaya.interfaces;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import jambalaya.model.Artist;

public class TestUniqueIDStamped {

	@Test
	public void testUniqueIDAssignedAtObjectAdd() {
		Artist artist = Artist.createArtist( TestCore.newContext() );
		assertNotNull( artist.uniqueID() );
	}
}