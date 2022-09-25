import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;

import org.apache.cayenne.ObjectId;
import org.junit.jupiter.api.Test;

import jambalaya.ObjectIdSerializer;

public class TestObjectIdSerializer {

	@Test
	public void serializeSingleKey() {
		var oid = new ObjectId( "NoEntity", Map.of( "id", 5 ) );
		var serialized = ObjectIdSerializer.serialize( oid );
		assertEquals( "5", serialized );
	}

	@Test
	public void serializeTwoStringKey() {
		var singleObjectId = new ObjectId( "NoEntity", Map.of(
				"name", "Hugi",
				"id", 5 ) );

		var serialized = ObjectIdSerializer.serialize( singleObjectId );
		assertEquals( "5|Hugi", serialized );
	}
}