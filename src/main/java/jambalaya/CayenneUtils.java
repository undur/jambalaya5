package jambalaya;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.cayenne.DataObject;
import org.apache.cayenne.DataRow;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.exp.Property;
import org.apache.cayenne.exp.parser.ASTEqual;
import org.apache.cayenne.exp.parser.ASTGreaterOrEqual;
import org.apache.cayenne.exp.parser.ASTLess;
import org.apache.cayenne.exp.parser.ASTLikeIgnoreCase;
import org.apache.cayenne.exp.parser.ASTObjPath;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.ObjRelationship;
import org.apache.cayenne.query.EJBQLQuery;
import org.apache.cayenne.query.ObjectSelect;
import org.apache.cayenne.query.SQLTemplate;
import org.apache.cayenne.util.CayenneMapEntry;

import is.rebbi.core.kvc.KVC;
import is.rebbi.core.util.DateUtilities;
import is.rebbi.core.util.StringUtilities;

/**
 * Various utility methods for database connectivity.
 */

public class CayenneUtils {

	/**
	 * @return A list of distinct values of the specified property
	 *
	 * @param oc The ObjectContext to fetch into
	 * @param entityClass Class of Cayenne entity to fetch
	 * @param expression Qualifier for the query
	 * @param properties List of properties to fetch values for
	 */
	public static <E> List<E> distinct( ObjectContext oc, Class<? extends DataObject> entityClass, Expression expression, Property<E> property ) {
		StringBuilder b = new StringBuilder();
		b.append( "SELECT distinct a." );
		b.append( property.getName() );
		b.append( " FROM " );
		b.append( oc.getEntityResolver().getObjEntity( entityClass ).getName() );
		b.append( " a" );

		EJBQLQuery query = queryByApplyingExpression( b.toString(), expression );
		return oc.performQuery( query );
	}

	/**
	 * @return A list of distinct value combinations of the specified properties.
	 *
	 * @param oc The ObjectContext to fetch into
	 * @param entityClass Class of Cayenne entity to fetch
	 * @param expression Qualifier for the query
	 * @param properties List of properties to fetch values for
	 */
	public static List<Map<String, Object>> distinctValues( ObjectContext oc, Class<? extends DataObject> entityClass, Expression expression, Property<?>... properties ) {
		StringBuilder b = new StringBuilder();
		b.append( "SELECT distinct" );

		for( int i = 0 ; i < properties.length ; i++ ) {
			if( i > 0 ) {
				b.append( "," );
			}

			String attributeName = properties[i].getName();
			b.append( " a." + attributeName );
		}

		b.append( " FROM " );
		b.append( oc.getEntityResolver().getObjEntity( entityClass ).getName() );
		b.append( " a" );

		EJBQLQuery query = queryByApplyingExpression( b.toString(), expression );

		List<Object[]> fetchedObjects = oc.performQuery( query );

		List<Map<String, Object>> results = new ArrayList<>();

		for( Object[] fetchedObject : fetchedObjects ) {
			Map<String, Object> resultObject = new HashMap<>();

			for( int i = 0 ; i < properties.length ; i++ ) {
				String key = properties[i].getName();
				Object value = fetchedObject[i];
				resultObject.put( key, value );
			}

			results.add( resultObject );
		}

		return results;
	}

	/**
	 * @return The result of executing the given aggregate function on the named property of the given entityClass.
	 */
	private static <E> E executeAggregateFunction( ObjectContext oc, Class<? extends DataObject> entityClass, String functionName, Property<E> property, Expression expression ) {
		StringBuilder b = new StringBuilder();
		b.append( "SELECT " );
		b.append( functionName );
		b.append( "(a." + property.getName() + ")" );
		b.append( " FROM " );
		b.append( oc.getEntityResolver().getObjEntity( entityClass ).getName() );
		b.append( " a" );

		EJBQLQuery query = queryByApplyingExpression( b.toString(), expression );
		List result = oc.performQuery( query );

		for( Object object : result ) {
			return (E)object;
		}

		throw new IllegalStateException( "Execution should never reach here (count is returned in the above loop)" );
	}

	/**
	 * @return an EJBQLQuery with the given expression applied to it.
	 */
	public static EJBQLQuery queryByApplyingExpression( String ejbqlString, Expression expression ) {
		List<Object> parameters = new ArrayList<>();

		StringBuilder b = new StringBuilder( ejbqlString );

		if( expression != null ) {
			b.append( " WHERE " );
			b.append( expression.toEJBQL( parameters, "a" ) );
		}

		String queryString = b.toString();

		EJBQLQuery query = new EJBQLQuery( queryString );

		for( int i = 0 ; i < parameters.size() ; i++ ) {
			query.setParameter( i + 1, parameters.get( i ) );
		}

		return query;
	}

	/**
	 * @return The number of rows matching the given expression.
	 */
	public static long countDistinct( ObjectContext oc, Class<? extends DataObject> entityClass, Property<?> property, Expression expression ) {
		StringBuilder b = new StringBuilder();
		b.append( "SELECT count" );
		b.append( "(distinct a." + property.getName() + ")" );
		b.append( " FROM " );
		b.append( oc.getEntityResolver().getObjEntity( entityClass ).getName() );
		b.append( " a" );

		EJBQLQuery query = queryByApplyingExpression( b.toString(), expression );
		List result = oc.performQuery( query );

		for( Object object : result ) {
			return (Long)object;
		}

		throw new IllegalStateException( "Execution should never reach here (count is returned in the above loop)" );
	}

	/**
	 * @return The result of executing [ejbqlString]
	 */
	public static <E> List<E> ejbqlQuery( ObjectContext oc, Class clazz, String ejbqlString ) {
		EJBQLQuery query = new EJBQLQuery( ejbqlString );
		return oc.performQuery( query );
	}

	/**
	 * @return The number of rows matching the given expression.
	 */
	public static long count( ObjectContext oc, Class<? extends DataObject> entityClass, Expression expression ) {
		return ObjectSelect
				.query( entityClass )
				.column( Property.COUNT )
				.where( expression )
				.selectOne( oc );
	}

	/**
	 * @return Max value of the [property] matching [expression]
	 */
	public static <E> E max( ObjectContext oc, Class<? extends DataObject> entityClass, Property<E> property, Expression expression ) {
		return executeAggregateFunction( oc, entityClass, "max", property, expression );
	}

	/**
	 * @return Max value of the [property] in the data set matching [expression]
	 */
	public static <E> E min( ObjectContext oc, Class<? extends DataObject> entityClass, Property<E> property, Expression expression ) {
		return executeAggregateFunction( oc, entityClass, "min", property, expression );
	}

	/**
	 * @return Min value of the [property] in the data set matching [expression]
	 */
	public static Number avg( ObjectContext oc, Class<? extends DataObject> entityClass, Property<?> property, Expression expression ) {
		return (Number)executeAggregateFunction( oc, entityClass, "avg", property, expression );
	}

	/**
	 * @return Sum of values in [property] in the data set matching [expression]
	 */
	public static Number sum( ObjectContext oc, Class<? extends DataObject> entityClass, Property<?> property, Expression expression ) {
		return (Number)executeAggregateFunction( oc, entityClass, "sum", property, expression );
	}

	/**
	 * Group a list of DataObject by a property.
	 *
	 * @param The list of objects to group
	 * @param property The property to group by
	 * @param includeNulls Indicates if we want to include a null group in the map, containing all objects where invoking [property] resolves to null
	 *
	 * @return The original list as a map, where keys are distinct values provided by invoking [property]
	 */
	public static <T, E extends DataObject> Map<T, List<E>> group( Collection<E> collection, Property<T> property, boolean includeNulls ) {

		if( collection == null ) {
			throw new IllegalArgumentException( "List can't be null" );
		}

		Map<T, List<E>> map = new HashMap<>();

		for( E object : collection ) {
			T value = (T)KVC.valueForKey( object, property.getName() );

			if( value != null || includeNulls ) {
				List<E> group = map.get( value );

				if( group == null ) {
					group = new ArrayList<>();
					map.put( value, group );
				}

				group.add( object );
			}
		}

		return map;
	}

	/**
	 * @return An expression that searches all attributes in the given entity.
	 */
	public static Expression allQualifier( ObjectContext oc, String searchString, Class<? extends DataObject> entityClass ) {

		if( searchString == null ) {
			return null;
		}

		List<Expression> expressions = new ArrayList<>();

		ObjEntity entity = oc.getEntityResolver().getObjEntity( entityClass );

		for( ObjAttribute attribute : entity.getAttributes() ) {
			if( attributeIsString( attribute ) ) {
				expressions.add( new ASTLikeIgnoreCase( new ASTObjPath( attribute.getName() ), "%" + searchString + "%" ) );
			}

			if( attributeIsInteger( attribute ) && StringUtilities.isDigitsOnly( searchString ) ) {
				expressions.add( new ASTEqual( new ASTObjPath( attribute.getName() ), Integer.valueOf( searchString ) ) );
			}

			if( attributeIsLong( attribute ) && StringUtilities.isDigitsOnly( searchString ) ) {
				expressions.add( new ASTEqual( new ASTObjPath( attribute.getName() ), Long.valueOf( searchString ) ) );
			}

			if( attributeIsBigDecimal( attribute ) && StringUtilities.isDigitsOnly( searchString ) ) {
				expressions.add( new ASTEqual( new ASTObjPath( attribute.getName() ), Long.valueOf( searchString ) ) );
			}

			if( attributeIsDate( attribute ) && isDateString( searchString ) ) {
				Date from = null;

				try {
					from = new SimpleDateFormat( "yyyy-MM-dd" ).parse( searchString );
				}
				catch( ParseException e1 ) {
					e1.printStackTrace();
				}

				Date to = DateUtilities.dateByAddingGregorianUnits( from, 0, 0, 1, 0, 0, 0 );

				List<Expression> betweenInclusiveLower = new ArrayList<>();
				betweenInclusiveLower.add( new ASTGreaterOrEqual( new ASTObjPath( attribute.getName() ), from ) );
				betweenInclusiveLower.add( new ASTLess( new ASTObjPath( attribute.getName() ), to ) );
				Expression e = ExpressionFactory.and( betweenInclusiveLower );
				expressions.add( e );
			}
		}

		return ExpressionFactory.or( expressions );
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
		return java.math.BigDecimal.class.isAssignableFrom( attribute.getJavaClass() );
	}

	public static boolean attributeIsLong( ObjAttribute attribute ) {
		return java.lang.Long.class.isAssignableFrom( attribute.getJavaClass() );
	}

	public static boolean attributeIsInteger( ObjAttribute attribute ) {
		return java.lang.Integer.class.isAssignableFrom( attribute.getJavaClass() );
	}

	public static boolean attributeIsDecimal( ObjAttribute attribute ) {
		return java.math.BigDecimal.class.isAssignableFrom( attribute.getJavaClass() );
	}

	public static boolean attributeIsString( ObjAttribute attribute ) {
		return String.class.isAssignableFrom( attribute.getJavaClass() );
	}

	public static boolean attributeIsDate( ObjAttribute attribute ) {
		return Date.class.isAssignableFrom( attribute.getJavaClass() );
	}

	public static boolean attributeIsData( ObjAttribute attribute ) {
		return false;
		// FIXME: Implement;
		// return Data.class.equals( attribute.getJavaClass() );
	}

	public static boolean attributeIsBoolean( ObjAttribute currentAttribute ) {
		return java.lang.Boolean.class.isAssignableFrom( currentAttribute.getJavaClass() );
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

		for( Iterator<CayenneMapEntry> it = entity.resolvePathComponents( keyPath ) ; it.hasNext() ; ) {
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
	public static Expression allExpression( ObjectContext oc, String searchString, Class<? extends DataObject> entityClass, List<String> keyPaths ) {

		ObjEntity entity = oc.getEntityResolver().getObjEntity( entityClass );

		List<Expression> expressions = new ArrayList<>();

		for( String keyPath : keyPaths ) {

			CayenneMapEntry last = null;

			for( Iterator<CayenneMapEntry> it = entity.resolvePathComponents( keyPath ) ; it.hasNext() ; ) {
				last = it.next();
			}

			if( last instanceof ObjAttribute ) {
				ObjAttribute attribute = (ObjAttribute)last;

				if( attributeIsString( attribute ) ) {
					expressions.add( new ASTLikeIgnoreCase( new ASTObjPath( keyPath ), "%" + searchString + "%" ) );
				}

				if( attributeIsInteger( attribute ) && StringUtilities.isDigitsOnly( searchString ) ) {
					expressions.add( new ASTEqual( new ASTObjPath( keyPath ), Integer.valueOf( searchString ) ) );
				}

				if( attributeIsLong( attribute ) && StringUtilities.isDigitsOnly( searchString ) ) {
					expressions.add( new ASTEqual( new ASTObjPath( keyPath ), Long.valueOf( searchString ) ) );
				}

				if( attributeIsBigDecimal( attribute ) && StringUtilities.isDigitsOnly( searchString ) ) {
					expressions.add( new ASTEqual( new ASTObjPath( keyPath ), new BigDecimal( searchString ) ) );
				}

				if( attributeIsDate( attribute ) && isDateString( searchString ) ) {
					Date from = null;

					try {
						from = new SimpleDateFormat( "yyyy-MM-dd" ).parse( searchString );
					}
					catch( ParseException e1 ) {
						e1.printStackTrace();
					}

					Date to = DateUtilities.dateByAddingGregorianUnits( from, 0, 0, 1, 0, 0, 0 );

					List<Expression> betweenInclusiveLower = new ArrayList<>();
					betweenInclusiveLower.add( new ASTGreaterOrEqual( new ASTObjPath( keyPath ), from ) );
					betweenInclusiveLower.add( new ASTLess( new ASTObjPath( keyPath ), to ) );
					Expression e = ExpressionFactory.and( betweenInclusiveLower );
					expressions.add( e );
				}
			}
		}

		return ExpressionFactory.or( expressions );
	}

	/**
	 * @return The same expression as before, with every keypath converted to an outer join.
	 */
	public static Expression convertToOuter( Expression e ) {
		String expressionString = e.toString();
		expressionString = StringUtilities.replace( expressionString, ".", "+." );
		return ExpressionFactory.exp( expressionString );
	}

	/**
	 * Perform an SQL query and @return the result as a list of objects of the type <E>.
	 *
	 * @param oc The ObjectContext used to perform the query.
	 * @param targetClass Class of the resulting objects. Must contain public fields with same names as fields in the sql resultset.
	 * @param sql The SQL string to execute
	 *
	 *            FIXME: Needs to specify a root entity class.
	 */
	public static <E> List<E> selectCustomObjects( ObjectContext oc, Class<E> targetClass, String sql ) {
		SQLTemplate query = new SQLTemplate( targetClass, sql );
		query.setFetchingDataRows( true );

		List<DataRow> dataRows = oc.performQuery( query );
		List<E> result = new ArrayList<>();

		try {
			for( DataRow dataRow : dataRows ) {
				E o = targetClass.newInstance();

				Set<Entry<String, Object>> entrySet = dataRow.entrySet();

				for( Entry<String, Object> entry : entrySet ) {
					String key = entry.getKey();
					Object value = entry.getValue();
					o.getClass().getField( key ).set( o, value );
				}

				result.add( o );
			}
		}
		catch( InstantiationException | IllegalAccessException | IllegalArgumentException | NoSuchFieldException | SecurityException e ) {
			throw new RuntimeException( e );
		}

		return result;
	}
}