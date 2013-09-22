package org.cdi.ppm;

/** Encapsula un nodo del modelo PPM en el árbol de Huffman 
 * (tambien puede ser un nodo sin modelo)
 * 
 * @author Javier y Maldo
 */
public class HuffmanNode implements Comparable 
{
	/** El nodo del modelo PPM que encapsula */
	ModelNode node = null;
	/** La cuenta de apariciones del nodo en el modelo PPM */
	int count;
	
	/** El nodo Huffman padre a éste */
	HuffmanNode parent = null;
	
	/** El hijo izquierdo */
	HuffmanNode izq = null;
	/** El hijo derecho */
	HuffmanNode der = null;
	
	/** Crea un nuevo nodo del árbol de Huffman que es padre
	 * de dos nodos existentes
	 * @param izq el hijo izquierdo
	 * @param der el hijo derecho
	 */
	public HuffmanNode(HuffmanNode izq, HuffmanNode der)
	{
		this.izq = izq;
		this.der = der;
		
		izq.parent = this;
		der.parent = this;
		
		count = izq.count + der.count;
	}
	
	/** Crea un nuevo nodo del árbol de Huffman envolviendo
	 * a un nodo del modelo PPM
	 * @param node el nodo a envolver
	 */
	public HuffmanNode(ModelNode node)
	{
		this.node = node;
		this.count = node.count;
	}
	
	/** Compara dos nodos según su número de ocurrencias
	 * 
	 * @param arg la clase a comparar
	 * @return 0 si tienen las mismas apariciones
	 */
	public int compareTo(Object arg) {
		if (arg == null)
			throw new NullPointerException();
		
		HuffmanNode n2 = (HuffmanNode) arg;
		
		if (this.count < n2.count)
			return -1;
		
		if (this.count > n2.count)
			return 1;
		
		return 0;
	}
}
