package org.cdi.ppm;
import java.io.*;
import java.util.*;

import de.tivano.flash.swf.common.BitInputStream;

/** Implementa un decodificador Huffman para el modelo PPM
 * 
 * @author Javier y Maldo
 */
public class HuffmanDecoder extends Decoder
{
	/** La fuente de entrada de bits
	 */
	protected BitInputStream inb;
	
	/** Crea un nuevo decodificador
	 * 
	 * @param in de donde leer los datos codificados
	 * @param order orden del modelo PPM
	 */
	public HuffmanDecoder(InputStream in, int order)
	{
		super(in, order);
		inb = new BitInputStream(in);
	}
	
	/** Decodifica un carácter, dada una lista con los carácteres posibles
	 * 
	 * @param l la lista con carácteres posibles
	 * @return el carácter encontrado
	 * @throws IOException si hay algun error de e/s
	 */
	public ModelNode decodeChar(List<ModelNode> l) throws IOException
	{
		PriorityQueue<HuffmanNode> q = new PriorityQueue<HuffmanNode>();
		
		encapsular(l, q);
		
		while (q.size() > 1)
		{
			q.add( new HuffmanNode(q.poll(), q.poll()) );
		}

		HuffmanNode arbol = q.poll();
		HuffmanNode decodificado = descender(arbol);
		
		assert decodificado.node != null;
		
		return decodificado.node; 
	}
	
	/** Ordena los elementos de una lista en una cola de prioridad
	 * 
	 * @param l lista de <code>ModelNode</code>
	 * @param c cola donde se añaden los <code>HuffmanNode</code>
	 */
	protected void encapsular(List<ModelNode> l, PriorityQueue<HuffmanNode> c)
	{
		ListIterator<ModelNode> iterator;
		ModelNode node;
		HuffmanNode node2;

		//Poner los nodos iniciales
		for (iterator = l.listIterator() ; iterator.hasNext(); )
		{
			node = iterator.next();
			node2 = new HuffmanNode(node);
			c.add( node2 );
		}
	}
	
	/** Desciende por el árbol de Huffman segun los bits de la entrada
	 * 
	 * @param padre el nodo desde el que comenzar a descender
	 * @throws IOException si hay algun problema de e/s
	 */
	protected HuffmanNode descender(HuffmanNode padre) throws IOException
	{
		boolean bit;
		
		inb.mark(1); //Solo "probar" el estado del bit, sin leer.
		
		try
		{
			bit = inb.readBit();
		}
		catch (EOFException e)
		{
			//Fin del archivo!
			if (padre.node == null)
			{
				throw new IOException("Final del archivo inesperado");
			}
			return padre;
		}
		
		if (bit)
		{
			if (padre.der == null)
			{
				//Hemos acabado este carácter
				inb.reset();
				if (padre.node == null)
				{
					throw new IOException("Final del archivo inesperado");
				}
				return padre;
			}
			//debug.print("-");
			return descender(padre.der);
		}
		else
		{
			if (padre.izq == null)
			{
				//Hemos acabado este carácter
				inb.reset();
				if (padre.node == null)
				{
					throw new IOException("Final del archivo inesperado");
				}
				return padre;
			}
			//debug.print("-");
			return descender(padre.izq);
		}
	}
	
    /**
     * Cierra el decodificador.
	 *
     * @exception IOException if an I/O error occurs.
     */
    public void close() throws IOException
    {
    	inb.close();
    	super.close();
    }
}
