package jambalaya.definitions;

import java.util.List;

public interface ProvidesEntityDefinitions {

	/**
	 * @return A list of EntityDefinitions provided by the implementing class
	 */
	public List<EntityDefinition> entityDefinitions();

	/**
	 * @return The priority of this definition. Higher numbers override lower numbers.
	 */
	public default int priority() {
		return 0;
	}
}