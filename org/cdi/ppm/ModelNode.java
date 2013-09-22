package org.cdi.ppm;
/** Un nodo para el árbol de contextos PPM
 *
 * @author Javier y Maldo
 */
public class ModelNode
{
	/** El cáracter que representa este nodo */
	final byte car;
	
	/** El contexto padre (con un carácter menos) */
	ModelNode parent;
	/** El contexto hermano (con el mismo número de carácteres, variando el último) */
	ModelNode nextBrother = null;
	/** El contexto hijo (con un carácter más) */
	ModelNode firstSon = null;
	
	/** La cuenta de veces que se ha dado este contexto. */ 
	int count = 0;

	/** Construye un nuevo nodo para el carácter señalado
	 * 
	 * @param car el carácter indicado
	 */
	public ModelNode(byte car)
	{
		this.car = car;
	}
}
