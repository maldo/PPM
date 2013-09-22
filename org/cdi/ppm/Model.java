package org.cdi.ppm;
import java.util.*;

/** Implementa el árbol de contextos y conteo de carácteres
 * 
 * @author Javier y Maldo
 */

public class Model
{	
	/**
	 * La raíz del arbol de búsqueda
	 */
	protected final ModelNode trie;
	
	/**
	 * Una referencia al nodo que representa el carácter de escape
	 */
	protected final ModelNode escape;
	/**
	 * Una referencia al nodo que representa el carácter de fin de archivo
	 */
	protected final ModelNode eof;
	
	/**
	 * Crea un nuevo modelo con el árbol vacio (salvo escape y EOF)
	 */
	public Model()
	{
		trie = new ModelNode((byte) 0);
		trie.parent = null;
		
		escape = new ModelNode((byte) 0);
		escape.count = 1;
		escape.parent = null;
		
		eof = new ModelNode((byte) 0);
		eof.count = 1;
		eof.parent = null;
	}

	/** Tener en cuenta la aparición del carácter indicado en el contexto especificado
	 * 
	 * @param context el contexto que ha aparecido
     */
	public void add(Context context, byte car)
	{
		int i;
		int max = context.order();
		for (i = 0; i <= max; i++) //Tener un cuenta un orden más para añadir el carácter
		{
			addSubContext(context, i, max, car);
		}
	}
	
	/** (Interno) Tener en cuenta la aparición del contexto especificado <em>solo</em>
	 * en el orden en el que aparece tal contexto
	 * 
	 * @param context el contexto que ha aparecido
	 * @param off cuantos carácteres reducir el contexto para llegar al orden deseado
	 * @param len interpretar como contexto de qué orden
	 * @see #add(Context, byte) add
     */
	protected void addSubContext(Context context, int off, int len, byte car)
	{
		ModelNode cur = trie;
		ModelNode next;
		int i;
		byte contextCar;

		//Buscar y añadir todos los carácteres que forman el (sub)contexto
		for (i = off; i < len; i++)
		{
			contextCar = context.getByteAt(i);
			
			next = searchSonsOf(cur, contextCar);
			
			if (next == null) {
				//Añadir el carácter al árbol (primera aparación)
				next = addSonNodeTo(cur, contextCar);
			}
			
			cur = next;
		}
		
		//Ahora añadir el carácter
		next = searchSonsOf(cur, car);
		
		if (next == null) {
			//Añadir el carácter al árbol (primera aparación)
			next = addSonNodeTo(cur, car);
		}
		
		next.count++;
		
		//Comprobación
		assert next.count < Integer.MAX_VALUE : "integer overflow";
	}
	
	/** Obtiene una lista con todos los carácteres posibles,
	 * independientemente del contexto.
	 * La probabilidad será 1/(num carácteres posibles).
	 * 
	 * @param exclusions una lista de exclusiones que no se devolverán en la lista
	 * @return la tabla de probabilidades de todos los carácteres
	 */
	public List<ModelNode> getAllCharsList(Excluder exclusions)
	{
		LinkedList<ModelNode> l = new LinkedList<ModelNode>();
		ModelNode node;
		
		//Un bucle con todos los bytes posibles
		int i;
		for (i = Byte.MIN_VALUE; i <= Byte.MAX_VALUE; i++)
		{
			byte b = (byte) i;
			
			if (exclusions.isExcluded(b))
				continue; //Carácter excluido
			
			node = new ModelNode(b);
			node.count = 1;
			node.parent = null;
			
			l.add(node);
		}
	
		l.add(escape);
		l.add(eof);
		
		return l;
	}
	
	/** Devolver la tabla de nodos para un contexto determinado
	 * 
	 * @param context	el contexto donde buscar
	 * @param exclusions una lista de exclusiones que no se devolverán en la lista
	 * @return La tabla de nodos, o null si el carácter no estaba.
     */
	public List<ModelNode> getNodeListFor(Context context, Excluder exclusions)
	{
		ModelNode cur = trie;
		ModelNode next;
		
		int i;
		int max = context.order();
		for (i = 0; i < max; i++)
		{
			next = searchSonsOf(cur, context.getByteAt(i));
			
			if (next == null) {
				//¡Pues este contexto no está, que quieres que te diga...!
				return null;
			}

			cur = next;
		}
		
		return getListFromSonsOf(cur, exclusions);
	}
	
	/** Buscar un hijo del nodo especificado, 
	 * con el carácter especificado
	 * 
	 * @param parent	El nodo entre cuyos hijos buscar
	 * @param car	El carácter a buscar
	 * @return El nodo que tiene el carácter especificado, o null si no se encuentra
     */
	protected ModelNode searchSonsOf(ModelNode parent, byte car)
	{
		ModelNode node = parent.firstSon;
		
		while (node != null)
		{
			if (node.car == car)
				return node;
			node = node.nextBrother;
		}
		
		return null;
	}
	
	/** Añadir un nodo hijo con el carácter especificado al nodo especificado.
	 * Se supone que el nodo no está ya presente
	 * 
	 * @param parent	El nodo entre cuyos hermanos buscar
	 * @param car		El carácter a buscar
	 * @return El nodo añadido
     */
	protected ModelNode addSonNodeTo(ModelNode parent, byte car)
	{
		ModelNode ant, sig;
		ant = parent.firstSon;
		
		if (ant == null)
		{
			//No tiene hijos
			sig = new ModelNode(car);
			
			sig.parent = parent;
			
			parent.firstSon = sig;
			
			return sig;
		}

		sig = ant.nextBrother;
		while (sig != null)
		{			
			ant = sig;
			sig = ant.nextBrother;
		}
		
		//Aquí sig es null, ant es el último de la lista
		sig = new ModelNode(car);
		
		sig.parent = parent;
		
		ant.nextBrother = sig;
		
		return sig;

	}
	
	/** Devuelve una lista con los hijos del nodo parent, incluye el nodo de escape
	 * 
	 * @param parent el nodo del cual obtener los hijos
	 * @param exclusions un contexto con bytes a excluir
	 * @return la lista de los hijos de parent sin incluir los bytes en <code>exclusions</code>
	 */
	protected List<ModelNode> getListFromSonsOf(ModelNode parent, Excluder exclusions)
	{
		LinkedList<ModelNode> l = new LinkedList<ModelNode>();
		ModelNode node = parent.firstSon;

		while (node != null)
		{
			if (!exclusions.isExcluded(node.car))
			{
				//Si no está excluido, añadir a la lista
				l.add(node);
			}
			
			node = node.nextBrother;
		}
		
		//Añadir el nodo de "escape"
		l.add(escape);
		
		return l;
	}
	
	/** Busca, entre los nodos de una lista, el carácter especificado
	 * 
	 * @param l la lista donde buscar
	 * @param car carácter a buscar
	 * @return el nodo del modelo que tiene el carácter especificado
	 */
	public ModelNode searchNode(List<ModelNode> l, byte car)
	{
		ModelNode node;
		ListIterator<ModelNode> iterator;
		
		for (iterator = l.listIterator() ; iterator.hasNext(); )
		{
			node = iterator.next();
			if (node != escape && node != eof && node.car == car)
				return node;
		}
		
		return null;
	}
	
	/** Busca, entre los nodos de una lista, el carácter de escape
	 * 
	 * @param l la lista donde buscar
	 * @return el nodo del modelo que tiene el carácter especificado
	 */
	public ModelNode searchEscapeNode(List<ModelNode> l)
	{
		//Siempre es el mismo
		return escape;
	}
	
	/** Busca, entre los nodos de una lista, el carácter de EOF
	 * 
	 * @param l la lista donde buscar
	 * @return el nodo del modelo que tiene el carácter especificado
	 */
	public ModelNode searchEOFNode(List<ModelNode> l)
	{
		//Siempre es el mismo
		return eof;
	}
	
	/** Excluye todos los bytes de una lista de carácteres
	 * 
	 * @param l la lista de nodos a excluir
	 * @param exclusions donde poner las exclusiones
	 */
	public void excludeAll(List<ModelNode> l, Excluder exclusions)
	{
		ModelNode node;
		ListIterator<ModelNode> iterator;
		
		for (iterator = l.listIterator() ; iterator.hasNext(); )
		{
			node = iterator.next();
			if (node != escape && node != eof)
				exclusions.exclude(node.car);
		}
	}
	
	/** Indica si un nodo es el nodo de escape
	 * 
	 * @param node el nodo a comprobar
	 * @return true si node es nodo de escape
	 */
	public boolean isEscape(ModelNode node)
	{
		return (node == escape);
	}
	
	/** Indica si un nodo es el nodo de fin de archivo
	 * 
	 * @param node el nodo a comprobar
	 * @return true si node es nodo de fin de archivo
	 */
	public boolean isEOF(ModelNode node)
	{
		return (node == eof);
	}
}
