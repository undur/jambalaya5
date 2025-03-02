package jambalaya;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;

import org.apache.cayenne.ObjectId;
import org.junit.jupiter.api.Test;

import jambalaya.interfaces.TestCore;

public class TestObjectIdSerializer {

	@Test
	public void serializeSingleKey() {
		var oid = ObjectId.of( "NoEntity", Map.of( "id", 5 ) );
		var serialized = ObjectIdSerializer.serialize( oid );
		assertEquals( "5", serialized );
	}

	@Test
	public void serializeTwoStringKey() {
		var oid = ObjectId.of( "NoEntity", Map.of(
				"name", "Hugi",
				"id", 5 ) );

		var serialized = ObjectIdSerializer.serialize( oid );
		assertEquals( "5|Hugi", serialized );
	}

	@Test
	public void deserializeSingleKey() {
		var oid = ObjectId.of( "Artist", Map.of( "id", "5" ) );
		assertEquals( oid, ObjectIdSerializer.deserialize( TestCore.newContext(), "Artist", "5" ) );
	}

	// FIXME: We're missing a test for multiple PK attributes
}