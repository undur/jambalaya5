package jambalaya;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.cayenne.DataObject;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.Property;
import org.apache.cayenne.query.ObjectSelect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Stores a cache of DataObjects. Stored objects can then be references from a set of the objects' values.
 *
 * Example use, storing a cache of people by their names and addresses:
 *
 * DataObjectCache cache = new DataObjectCache( Person.class, Person.NAME, Person.ADDRESS );
 *
 * -- Using an exact value: Map<Property<?>,Object> values = new HashMap<>(); values.put( Person.NAME, "Hugi" ); values.put( Person.ADDRESS, "Hraunteigur 23" ); cache.get( values );
 *
 * -- Using an Expression (returns the first matching object in the cached list) cache.get( Person.NAME.eq("Hugi").and( Person.ADDRESS.eq( "Hraunteigur 23" ) );
 *
 * FIXME: Allow consumer to specify if duplicate key values should be allowed.
 */

public class DataObjectCache<E extends DataObject> {

	private static final Logger logger = LoggerFactory.getLogger( DataObjectCache.class );

	/**
	 * The DataObject class this stores object list for.
	 */
	private Class<E> _entityClass;

	/**
	 * A list of properties that will be read for
	 */
	private Property<?>[] _properties;

	/**
	 * The ObjectContext to use when fetching.
	 */
	private ObjectContext _objectContext;

	private ObjectSelect<E> _query;

	/**
	 * The keys along with their corresponding objects.
	 */
	public Map<Map<Property<?>, Object>, E> _cache;

	/**
	 * @param objectContext The objectContext to use when fetching the cache and storing the objects.
	 * @param entityClass The class to store objects for.
	 * @param properties A list of properties to use as keys.
	 */
	public DataObjectCache( ObjectContext objectContext, Class<E> entityClass, Property<?>... properties ) {

		if( objectContext == null ) {
			throw new IllegalArgumentException( "You must specify an objectContext" );
		}

		if( entityClass == null ) {
			throw new IllegalArgumentException( "You must specify an entityClass" );
		}

		if( properties == null || properties.length == 0 ) {
			throw new IllegalArgumentException( "You must specify a list of properties to use as keys when generating the cache" );
		}

		_objectContext = objectContext;
		_entityClass = entityClass;
		_properties = properties;
	}

	/**
	 * @param objectContext The objectContext to use when fetching the cache and storing the objects.
	 * @param entityClass The class to store objects for.
	 * @param properties A list of properties to use as keys.
	 */
	public DataObjectCache( ObjectContext objectContext, ObjectSelect<E> query, Property<?>... properties ) {

		if( objectContext == null ) {
			throw new IllegalArgumentException( "You must specify an objectContext" );
		}

		if( query == null ) {
			throw new IllegalArgumentException( "You must specify a query" );
		}

		if( properties == null || properties.length == 0 ) {
			throw new IllegalArgumentException( "You must specify a list of properties to use as keys when generating the cache" );
		}

		_objectContext = objectContext;
		_query = query;
		_properties = properties;
	}

	/**
	 * @return An object containing exactly the values specified in the given key map. Null if no object matches the given criteria.
	 */
	public E get( Map<Property<?>, Object> key ) {
		return cache().get( key );
	}

	/**
	 * @return An object containing exactly the values specified in the given key map. Null if no object matches the given criteria.
	 */
	public E get( ObjectContext oc, Map<Property<?>, Object> key ) {
		E object = get( key );

		if( object == null ) {
			return null;
		}

		return oc.localObject( object );
	}

	/**
	 * @return The objects matching the given expression. Null if no match is found.
	 */
	public List<E> get( Expression e ) {
		return e.filterObjects( cache().values() );
	}

	/**
	 * @return The objects matching the given expression. Null if no match is found.
	 */
	public E getOne( Expression e ) {
		List<E> list = e.filterObjects( cache().values() );

		if( list.size() == 0 ) {
			return null;
		}

		if( list.size() > 1 ) {
			throw new IllegalStateException( "Matched more than one object" );
		}

		return list.get( 0 );
	}

	/**
	 * @return the object cache.
	 */
	private Map<Map<Property<?>, Object>, E> cache() {
		if( _cache == null ) {
			populateCache();
		}

		return _cache;
	}

	private ObjectSelect<E> query() {
		if( _query == null ) {
			_query = ObjectSelect.query( _entityClass );
		}

		return _query;
	}

	/**
	 * Populates the cache.
	 */
	private void populateCache() {
		logger.info( "Populating cache for entity" + _entityClass + "..." );

		List<E> list = _objectContext.select( query() );

		_cache = new HashMap<>();

		for( E object : list ) {
			Map<Property<?>, Object> key = new HashMap<>();

			for( Property<?> property : _properties ) {
				key.put( property, property.getFrom( object ) );
			}

			_cache.put( key, object );
		}

		logger.info( "Successfully populated cache for entity" + _entityClass + "..." );
	}

	/**
	 * Clears the object cache. Invoke if you expect the data source to have changes.
	 */
	public void clear() {
		_cache = null;
	}
}