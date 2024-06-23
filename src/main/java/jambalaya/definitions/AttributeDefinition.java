package jambalaya.definitions;

import org.apache.cayenne.exp.Property;

/**
 * Defines the display of an attribute.
 */

public class AttributeDefinition {

	private Property<?> _property;
	private String _name;
	private String _icelandicName;
	private String _text;
	private boolean _show;
	private Integer _sortOrder;

	public AttributeDefinition() {}

	public AttributeDefinition( String name ) {
		_name = name;
	}

	public AttributeDefinition( Integer sortOrder, Property<?> property, String icelandicName, boolean show ) {
		_sortOrder = sortOrder;
		_name = property.getName();
		_property = property;
		_icelandicName = icelandicName != null ? icelandicName : property.getName();
		_show = show;
	}

	public AttributeDefinition( Integer sortOrder, String propertyName, String icelandicName, boolean show ) {
		_sortOrder = sortOrder;
		_name = propertyName;
		_property = Property.create( propertyName, null );
		_icelandicName = icelandicName != null ? icelandicName : propertyName;
		_show = show;
	}

	public String name() {
		return _name;
	}

	public Property<?> property() {
		return _property;
	}

	public String icelandicName() {
		return _icelandicName;
	}

	public String text() {
		return _text;
	}

	public boolean show() {
		return _show;
	}

	public Integer sortOrder() {
		return _sortOrder;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((_icelandicName == null) ? 0 : _icelandicName.hashCode());
		result = prime * result + ((_name == null) ? 0 : _name.hashCode());
		result = prime * result + (_show ? 1231 : 1237);
		result = prime * result + ((_sortOrder == null) ? 0 : _sortOrder.hashCode());
		result = prime * result + ((_text == null) ? 0 : _text.hashCode());
		return result;
	}

	@Override
	public boolean equals( Object obj ) {
		if( this == obj ) {
			return true;
		}
		if( obj == null ) {
			return false;
		}
		if( getClass() != obj.getClass() ) {
			return false;
		}
		AttributeDefinition other = (AttributeDefinition)obj;
		if( _icelandicName == null ) {
			if( other._icelandicName != null ) {
				return false;
			}
		}
		else if( !_icelandicName.equals( other._icelandicName ) ) {
			return false;
		}
		if( _name == null ) {
			if( other._name != null ) {
				return false;
			}
		}
		else if( !_name.equals( other._name ) ) {
			return false;
		}
		if( _show != other._show ) {
			return false;
		}
		if( _sortOrder == null ) {
			if( other._sortOrder != null ) {
				return false;
			}
		}
		else if( !_sortOrder.equals( other._sortOrder ) ) {
			return false;
		}
		if( _text == null ) {
			if( other._text != null ) {
				return false;
			}
		}
		else if( !_text.equals( other._text ) ) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		return "AttributeDefinition [_name=" + _name + ", _icelandicName=" + _icelandicName + ", _text=" + _text + ", _show=" + _show + ", _sortOrder=" + _sortOrder + "]";
	}
}