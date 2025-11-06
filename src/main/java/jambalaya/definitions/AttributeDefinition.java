package jambalaya.definitions;

import org.apache.cayenne.exp.property.Property;

/**
 * Defines the display of an attribute.
 */

public record AttributeDefinition( Property<?> property, String name, String icelandicName, String text, boolean show, Integer sortOrder ) {

	@Deprecated
	public AttributeDefinition( String name ) {
		this( null, name, name, null, false, null );
	}

	@Deprecated
	public AttributeDefinition( Integer sortOrder, Property<?> property, String icelandicName, boolean show ) {
		this( property, property.getName(), icelandicName, null, show, sortOrder );
	}
}