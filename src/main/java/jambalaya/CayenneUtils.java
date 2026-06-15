package jambalaya;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.PersistentObject;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.exp.parser.ASTEqual;
import org.apache.cayenne.exp.parser.ASTGreaterOrEqual;
import org.apache.cayenne.exp.parser.ASTLess;
import org.apache.cayenne.exp.parser.ASTLikeIgnoreCase;
import org.apache.cayenne.exp.parser.ASTObjPath;
import org.apache.cayenne.exp.property.Property;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.ObjRelationship;
import org.apache.cayenne.reflect.PropertyUtils;
import org.apache.cayenne.util.CayenneMapEntry;

import is.rebbi.core.util.DateUtilities;
import is.rebbi.core.util.StringUtilities;

/**
 * Various utility methods for database connectivity.
 */

public class CayenneUtils {

	/**
	 * Groups a collection of objects by the value of a property
	 *
	 * @param The list of objects to group
	 * @param property The property to group by
	 * @param includeNull Indicates if we want to include "the null group" in the map (containing all objects where invoking [property] resolves to null)
	 *
	 * @return The original list as a map, where keys are distinct values provided by invoking [property]
	 */
	public static <T, E extends PersistentObject> Map<T, List<E>> group( final Collection<E> collection, final Property<T> property, final boolean includeNull ) {
		Objects.requireNonNull( collection, "Collection can't be null" );
		Objects.requireNonNull( property, "Property can't be null" );

		final Map<T, List<E>> result = new HashMap<>();

		for( E object : collection ) {
			T value = (T)PropertyUtils.getProperty( object, property.getName() );

			if( value != null || includeNull ) {
				List<E> group = result.get( value );

				if( group == null ) {
					group = new ArrayList<>();
					result.put( value, group );
				}

				group.add( object );
			}
		}

		return result;
	}

	/**
	 * @return True if the string looks like an ISO-8601 compliant date string (without time)
	 */
	private static boolean isDateString( String string ) {
		if( string == null ) {
			return false;
		}

		return string.matches( "\\d\\d\\d\\d-\\d?\\d-\\d?\\d" );
	}

	public static boolean attributeIsBigDecimal( ObjAttribute attribute ) {
		return BigDecimal.class.isAssignableFrom( attribute.getJavaClass() );
	}

	public static boolean attributeIsUUID( ObjAttribute attribute ) {
		return UUID.class.isAssignableFrom( attribute.getJavaClass() );
	}

	public static boolean attributeIsLong( ObjAttribute attribute ) {
		return Long.class.isAssignableFrom( attribute.getJavaClass() );
	}

	public static boolean attributeIsInteger( ObjAttribute attribute ) {
		return Integer.class.isAssignableFrom( attribute.getJavaClass() );
	}

	public static boolean attributeIsDecimal( ObjAttribute attribute ) {
		return BigDecimal.class.isAssignableFrom( attribute.getJavaClass() );
	}

	public static boolean attributeIsString( ObjAttribute attribute ) {
		return String.class.isAssignableFrom( attribute.getJavaClass() );
	}

	public static boolean attributeIsDate( ObjAttribute attribute ) {
		return Date.class.isAssignableFrom( attribute.getJavaClass() );
	}

	public static boolean attributeIsLocalDate( ObjAttribute attribute ) {
		return LocalDate.class.isAssignableFrom( attribute.getJavaClass() );
	}

	public static boolean attributeIsLocalDateTime( ObjAttribute attribute ) {
		return LocalDateTime.class.isAssignableFrom( attribute.getJavaClass() );
	}

	public static boolean attributeIsLocalTime( ObjAttribute attribute ) {
		return LocalTime.class.isAssignableFrom( attribute.getJavaClass() );
	}

	public static boolean attributeIsData( ObjAttribute attribute ) {
		return byte[].class.isAssignableFrom( attribute.getJavaClass() );
	}

	public static boolean attributeIsBoolean( ObjAttribute currentAttribute ) {
		boolean isBooleanObject = Boolean.class.isAssignableFrom( currentAttribute.getJavaClass() );
		boolean isBooleanPrimitive = boolean.class.isAssignableFrom( currentAttribute.getJavaClass() );
		return isBooleanObject || isBooleanPrimitive;
	}

	/**
	 * Given an entity class and a list of keyPaths you plan to show on a page, this will
	 *
	 * @return a list of keyPaths you should prefetch
	 */
	public static List<String> keyPathsToPrefetch( ObjectContext oc, Class entityClass, List<String> keyPaths ) {
		Set<String> l = new HashSet<>();

		for( String keyPath : keyPaths ) {
			l.addAll( relationshipsInKeyPath( oc, entityClass, keyPath ) );
		}

		return new ArrayList<>( l );
	}

	/**
	 * @return A list of relationships in the given keyPath.
	 */
	private static List<String> relationshipsInKeyPath( ObjectContext oc, Class entityClass, String keyPath ) {
		EntityResolver entityResolver = oc.getEntityResolver();
		ObjEntity entity = entityResolver.getObjEntity( entityClass );

		List<String> relationships = new ArrayList<>();

		StringBuilder b = new StringBuilder();

		for( Iterator<CayenneMapEntry> it = entity.resolvePathComponents( keyPath ); it.hasNext(); ) {
			CayenneMapEntry next = it.next();

			if( next instanceof ObjRelationship ) {

				if( b.length() > 0 ) {
					b.append( "." );
				}

				b.append( next.getName() );
				relationships.add( b.toString() );
			}
		}

		return relationships;
	}

	/**
	 * @return An expression that searches all attributes in the given entity.
	 */
	public static Expression allExpression( ObjectContext oc, String searchString, Class<? extends PersistentObject> entityClass ) {

		// FIXME: Whould this check be here?
		if( searchString == null ) {
			return null;
		}

		final ObjEntity entity = oc.getEntityResolver().getObjEntity( entityClass );
		final List<Expression> expressions = new ArrayList<>();

		for( ObjAttribute attribute : entity.getAttributes() ) {
			final String keyPath = attribute.getName();

			final Expression e = expression( searchString, attribute, keyPath );

			if( e != null ) {
				expressions.add( e );
			}
		}

		// If no expressions were created, we're just returning false from here
		if( expressions.isEmpty() ) {
			return ExpressionFactory.expFalse();
		}

		return ExpressionFactory.or( expressions );
	}

	/**
	 * @return An expression that searches the given keyPaths in the given entity
	 */
	public static Expression allExpression( ObjectContext oc, String searchString, Class<? extends PersistentObject> entityClass, List<String> keyPaths ) {

		final ObjEntity entity = oc.getEntityResolver().getObjEntity( entityClass );
		final List<Expression> expressions = new ArrayList<>();

		for( String keyPath : keyPaths ) {
			keyPath = keyPath.replace( ".", "+." );

			CayenneMapEntry last = null;

			for( Iterator<CayenneMapEntry> it = entity.resolvePathComponents( keyPath ); it.hasNext(); ) {
				last = it.next();
			}

			if( last instanceof ObjAttribute attribute ) {
				final Expression e = expression( searchString, attribute, keyPath );

				if( e != null ) {
					expressions.add( e );
				}
			}
		}

		// If no expressions were created, we're just returning false from here
		if( expressions.isEmpty() ) {
			return ExpressionFactory.expFalse();
		}

		return ExpressionFactory.or( expressions );
	}

	private static Expression expression( String searchString, ObjAttribute attribute, String keyPath ) {
		Objects.requireNonNull( searchString );
		Objects.requireNonNull( attribute );
		Objects.requireNonNull( keyPath );

		if( attributeIsString( attribute ) ) {
			return new ASTLikeIgnoreCase( new ASTObjPath( keyPath ), "%" + searchString + "%" );
		}

		final boolean isNumeric = StringUtilities.isDigitsOnly( searchString );
		final boolean isNumericNegative = "-".equals( searchString.substring( 0, 1 ) ) && StringUtilities.isDigitsOnly( searchString.substring( 1, searchString.length() ) );

		if( isNumeric || isNumericNegative ) {
			if( attributeIsInteger( attribute ) ) {
				return new ASTEqual( new ASTObjPath( keyPath ), Integer.valueOf( searchString ) );
			}

			if( attributeIsLong( attribute ) ) {
				return new ASTEqual( new ASTObjPath( keyPath ), Long.valueOf( searchString ) );
			}

			if( attributeIsBigDecimal( attribute ) ) {
				return new ASTEqual( new ASTObjPath( keyPath ), new BigDecimal( searchString ) );
			}
		}

		if( isDateString( searchString ) ) {
			try {
				final LocalDate from = LocalDate.parse( searchString );
				final LocalDate to = from.plusDays( 1 );

				if( attributeIsDate( attribute ) ) {
					final List<Expression> l = new ArrayList<>();
					l.add( new ASTGreaterOrEqual( new ASTObjPath( keyPath ), DateUtilities.toDate( from ) ) );
					l.add( new ASTLess( new ASTObjPath( keyPath ), DateUtilities.toDate( to ) ) );
					return ExpressionFactory.and( l );
				}

				if( attributeIsLocalDate( attribute ) ) {
					return new ASTEqual( new ASTObjPath( keyPath ), from.atStartOfDay() );
				}

				if( attributeIsLocalDateTime( attribute ) ) {
					final List<Expression> l = new ArrayList<>();
					l.add( new ASTGreaterOrEqual( new ASTObjPath( keyPath ), from.atStartOfDay() ) );
					l.add( new ASTLess( new ASTObjPath( keyPath ), to.atStartOfDay() ) );
					return ExpressionFactory.and( l );
				}
			}
			catch( DateTimeParseException e ) {
				e.printStackTrace();
			}
		}

		if( attributeIsUUID( attribute ) ) {
			try {
				final UUID uuid = UUID.fromString( searchString );
				return new ASTEqual( new ASTObjPath( keyPath ), uuid );
			}
			catch( IllegalArgumentException e ) {
				// What happened here is that we don't have an exact match for the UUID.
				// Having a "like" (contains) for UUID types sould be nice though.
			}
		}

		return null;
	}

	/**
	 * @return True if the two given objects have equal object IDs
	 */
	public static boolean equalsByObjectID( Persistent p1, Persistent p2 ) {
		return p1 == p2 || (p1 != null && p2 != null && p1.getObjectId().equals( p2.getObjectId() ));
	}
}