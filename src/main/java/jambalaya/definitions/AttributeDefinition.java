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

	public AttributeDefinition( Integer sortOrder, Property<?> property, String icelandicName, boolean show ) {
		setSortOrder( sortOrder );
		setName( property.getName() );
		setProperty( property );
		setIcelandicName( icelandicName );
		setShow( show );
	}

	public AttributeDefinition( Integer sortOrder, String name, String icelandicName, boolean show ) {
		setSortOrder( sortOrder );
		setName( name );
		setProperty( Property.create( name, null ) );
		setIcelandicName( icelandicName );
		setShow( show );
	}

	public String name() {
		return _name;
	}

	public void setName( String value ) {
		_name = value;
	}

	public Property<?> property() {
		return _property;
	}

	public void setProperty( Property<?> value ) {
		_property = value;
	}

	public String icelandicName() {
		if( _icelandicName == null ) {
			_icelandicName = name();
		}

		return _icelandicName;
	}

	public void setIcelandicName( String value ) {
		_icelandicName = value;
	}

	public String text() {
		return _text;
	}

	public void setText( String value ) {
		_text = value;
	}

	public boolean show() {
		return _show;
	}

	public void setShow( boolean value ) {
		_show = value;
	}

	public Integer sortOrder() {
		return _sortOrder;
	}

	public void setSortOrder( Integer value ) {
		_sortOrder = value;
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