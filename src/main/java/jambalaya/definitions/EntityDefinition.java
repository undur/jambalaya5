package jambalaya.definitions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.PersistentObject;
import org.apache.cayenne.access.DataDomain;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.runtime.CayenneRuntime;
import org.apache.cayenne.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jambalaya.Jambalaya;

public class EntityDefinition<E extends PersistentObject> {

	private static final Logger logger = LoggerFactory.getLogger( EntityDefinition.class );

	/**
	 * All registered definitions.
	 */
	private static Map<String, EntityDefinition> _definitions;

	/**
	 * The attributes of this entity
	 */
	private Map<String, AttributeDefinition> _attributeDefinitions;

	/**
	 * Name of the entity this definition defines. Equal to entityClass.getSimpleName()
	 */
	private String _name;

	/**
	 * The class that represents this entity. Can be null if the entity does not have a corresponding class.
	 */
	private Class<E> _entityClass;

	/**
	 * Icelandic name.
	 */
	private String _icelandicName;

	/**
	 * Icelandic plural name.
	 */
	private String _icelandicNamePlural;

	/**
	 * Icelandic description.
	 */
	private String _text;

	/**
	 * Name of the category
	 */
	private String _categoryName;

	/**
	 * Cached list of attributes to show.
	 */
	private List<AttributeDefinition> _attributesToShow;

	/**
	 * List of all objects that provide the system with EntityDefinitions.
	 */
	private static List<ProvidesEntityDefinitions> _entityDefinitionProviders = new ArrayList<>();

	private EntityDefinition() {
		setAttributeDefinitions( new HashMap<>() );
	}

	/**
	 * Define an entity.
	 */
	public static <E extends PersistentObject> EntityDefinition<E> create( final Class<E> entityClass, final String icelandicName, final String icelandicNamePlural, final String categoryName ) {
		final EntityDefinition<E> e = new EntityDefinition<>();
		e.setEntityClass( entityClass );

		final String name = Jambalaya.serverRuntime().getDataDomain().getEntityResolver().getObjEntity( entityClass ).getName();
		e.setName( name );
		e.setIcelandicName( icelandicName );
		e.setIcelandicNamePlural( icelandicNamePlural );
		e.setCategoryName( categoryName );
		return e;
	}

	private static List<ProvidesEntityDefinitions> entityDefinitionProviders() {
		return _entityDefinitionProviders;
	}

	public static void registerEntityDefinitionProvider( ProvidesEntityDefinitions provider ) {
		entityDefinitionProviders().add( provider );
		Collections.sort( _entityDefinitionProviders, Comparator.comparing( ProvidesEntityDefinitions::priority ) );
		invalidateCache();
	}

	public AttributeDefinition addAttributeDefinition( AttributeDefinition a ) {
		if( a.name() != null ) {
			attributeDefinitions().put( a.name(), a );
		}

		return a;
	}

	private static Map<String, EntityDefinition> definitions() {
		if( _definitions == null ) {
			_definitions = new HashMap<>();

			for( ProvidesEntityDefinitions provider : entityDefinitionProviders() ) {
				logger.debug( "Loading Entity View Definitions from {} - priority {}", provider.getClass(), provider.priority() );
				for( EntityDefinition e : provider.entityDefinitions() ) {
					e.register();
				}
			}
		}

		return _definitions;
	}

	private static Class<?> classForEntity( String entityName ) {
		Class<?> entityClass = null;

		CayenneRuntime serverRuntime = Jambalaya.serverRuntime();
		DataDomain dataDomain = serverRuntime.getDataDomain();
		EntityResolver entityResolver = dataDomain.getEntityResolver();
		ObjEntity entity = entityResolver.getObjEntity( entityName );

		if( entity != null ) {
			entityClass = getJavaClass( entity.getJavaClassName() );
		}

		return entityClass;
	}

	/**
	 * FIXME: We need to eliminate this method and just plain use the proper way Cayenne suggests ever since 4.0
	 */
	@Deprecated
	private static Class<?> getJavaClass( final String name ) {
		try {
			return Util.getJavaClass( name );
		}
		catch( ClassNotFoundException e ) {
			throw new CayenneRuntimeException( "Failed to doLoad class " + name + ": " + e.getMessage(), e );
		}
	}

	/**
	 * Define view definition for an entity.
	 */
	private void register() {
		logger.debug( "Defining view for: {}", name() );

		EntityDefinition e = get( name() );

		if( entityClass() != null ) {
			e.setEntityClass( entityClass() );
		}

		if( icelandicName() != null ) {
			e.setIcelandicName( icelandicName() );
		}

		if( icelandicNamePlural() != null ) {
			e.setIcelandicNamePlural( icelandicNamePlural() );
		}

		if( categoryName() != null ) {
			e.setCategoryName( categoryName() );
		}

		if( text() != null ) {
			e.setText( text() );
		}

		if( !attributeDefinitions().isEmpty() ) {
			e.setAttributeDefinitions( attributeDefinitions() );
		}
	}

	/**
	 * @return The definition for the given class
	 */
	public static <T> EntityDefinition get( Class<T> entityClass ) {
		return get( entityClass.getSimpleName() );
	}

	/**
	 * @return The definition for the given entityName
	 */
	public static EntityDefinition get( String entityName ) {

		if( entityName == null ) {
			throw new IllegalArgumentException( "[entityName] cannot be null" );
		}

		// FIXME: Remove the cast once we're done
		EntityDefinition e = definitions().get( entityName );

		if( e == null ) {
			e = new EntityDefinition();
			e.setName( entityName );
			definitions().put( entityName, e );
		}

		return e;
	}

	public String name() {
		return _name;
	}

	public void setName( String value ) {
		_name = value;
	}

	public Class<E> entityClass() {
		if( _entityClass == null ) {
			_entityClass = (Class<E>)classForEntity( name() );
		}

		return _entityClass;
	}

	public void setEntityClass( Class<E> value ) {
		_entityClass = value;
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

	public String icelandicNamePlural() {
		if( _icelandicNamePlural == null ) {
			_icelandicNamePlural = icelandicName();
		}

		return _icelandicNamePlural;
	}

	public void setIcelandicNamePlural( String value ) {
		_icelandicNamePlural = value;
	}

	public String text() {
		return _text;
	}

	public void setText( String value ) {
		_text = value;
	}

	public String categoryName() {
		return _categoryName;
	}

	public void setCategoryName( String value ) {
		_categoryName = value;
	}

	public ObjEntity entity() {
		return Jambalaya.newContext().getEntityResolver().getObjEntity( name() );
	}

	public AttributeDefinition attributeNamed( String attributeName ) {

		if( attributeName == null ) {
			return null;
		}

		AttributeDefinition result = attributeDefinitions().get( attributeName );

		if( result == null ) {
			result = new AttributeDefinition( attributeName );
			addAttributeDefinition( result );
		}

		return result;
	}

	public List<AttributeDefinition> attributes() {
		return new ArrayList<>( attributeDefinitions().values() );
	}

	public Map<String, AttributeDefinition> attributeDefinitions() {
		return _attributeDefinitions;
	}

	public void setAttributeDefinitions( final Map<String, AttributeDefinition> value ) {
		_attributeDefinitions = value;
	}

	public List<AttributeDefinition> attributesToShow() {
		if( _attributesToShow == null ) {
			_attributesToShow = new ArrayList<>( attributeDefinitions().values() );
			_attributesToShow = _attributesToShow
					.stream()
					.filter( AttributeDefinition::show )
					.sorted( Comparator
							.comparing( AttributeDefinition::name )
							.thenComparing( AttributeDefinition::sortOrder ) )
					.toList();
		}

		return _attributesToShow;
	}

	public static List<EntityDefinition> all() {
		List<EntityDefinition> all = new ArrayList<>();

		for( String entityName : allCayenneEntityNames() ) {
			all.add( EntityDefinition.get( entityName ) );
		}

		Collections.sort( all, Comparator.comparing( EntityDefinition::icelandicName ) );

		return all;
	}

	/**
	 * @return A list of all cayenne entities
	 *
	 * FIXME: We should really implement this by just registering all entities and attributes at application startup. We can then override/supplement the Cayenne data with other entity info providers
	 */
	private static List<String> allCayenneEntityNames() {
		// FIXME: MAXIMUM UGLYNESS!
		CayenneRuntime serverRuntime = Jambalaya.serverRuntime();

		if( serverRuntime == null ) {
			return new ArrayList<>();
		}

		return serverRuntime
				.getDataDomain()
				.getEntityResolver()
				.getObjEntities().stream()
				.map( ObjEntity::getName )
				.toList();
	}

	/**
	 * @return Icelandic name of object associated with the named entity.
	 */
	public static String icelandicName( String entityName ) {
		EntityDefinition type = get( entityName );

		if( type != null ) {
			String name = type.icelandicName();

			if( name != null ) {
				return name;
			}
		}

		return null;
	}

	/**
	 * @return Icelandic name of object associated with the named entity.
	 */
	public static String icelandicNamePlural( String entityName ) {
		EntityDefinition type = get( entityName );

		if( type != null ) {
			String name = type.icelandicNamePlural();

			if( name != null ) {
				return name;
			}
		}

		return null;
	}

	public static void invalidateCache() {
		logger.debug( "Invalidating EntityDefinition cache" );
		_definitions = null;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((_name == null) ? 0 : _name.hashCode());
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
		EntityDefinition other = (EntityDefinition)obj;
		if( _name == null ) {
			if( other._name != null ) {
				return false;
			}
		}
		else if( !_name.equals( other._name ) ) {
			return false;
		}
		return true;
	}
}