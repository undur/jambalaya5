package jambalaya.interfaces;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

import jambalaya.model.Artist;

public class TestTimeStamped {

	@Test
	public void testCreationAndModificationDateAssignedAtObjectAdd() {
		Artist artist = Artist.createArtist( TestCore.newContext() );
		assertNotNull( artist.creationDate() );
		assertNotNull( artist.modificationDate() );
	}
}