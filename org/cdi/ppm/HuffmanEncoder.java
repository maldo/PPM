package org.cdi.ppm;
import java.io.*;
import java.util.*;

import de.tivano.flash.swf.common.BitOutputStream;

/** Implementa un codificador Huffman para el modelo PPM
 * 
 * @author Javier y Maldo
 */
public class HuffmanEncoder extends Encoder
{
	/** Salida de bits
	 */
	protected BitOutputStream outb;
	
	/** Crea un nuevo codificador
	 * 
	 * @param out donde guardar los datos codificados
	 * @param order orden del modelo PPM
	 */
	public HuffmanEncoder(OutputStream out, int order)
	{
		super(out, order);
		outb = new BitOutputStream(out);
	}
	
	/** Codifica un carácter dada una tabla de probabilidades
	 * (debe ser sobrecargada)
	 * 
	 * @param l lista/tabla de probabilidades
	 * @param encode el carácter a codificar
	 * @throws IOException si hay un error de salida al escribir
	 */
	protected void encodeChar(List<ModelNode> l, ModelNode encode) throws IOException
	{
		HuffmanNode acodificar;
		PriorityQueue<HuffmanNode> q = new PriorityQueue<HuffmanNode>();

		acodificar = encapsular(l, q, encode);

		while (q.size() > 1)
		{
			q.add( new HuffmanNode(q.poll(), q.poll()) );
		}

		ascender(acodificar);

	}
	
	/** Ordena los elementos de una lista en una cola de prioridad
	 * 
	 * @param l lista de <code>ModelNode</code>
	 * @param c cola donde se añaden los <code>HuffmanNode</code>
	 * @param encode el nodo del model que representa el carácter a codificar
	 * @return el nodo de Huffman que representa el caracter a codificar
	 */
	protected HuffmanNode encapsular(List<ModelNode> l, PriorityQueue<HuffmanNode> c, ModelNode encode)
	{
		ListIterator<ModelNode> iterator;
		ModelNode node;
		HuffmanNode node2, acodificar = null;

		//Poner los nodos iniciales
		for (iterator = l.listIterator() ; iterator.hasNext(); )
		{
			node = iterator.next();
			node2 = new HuffmanNode(node);
			c.add( node2 );
			
			//Devolver el nodo que hay que acodificar, si lo hay
			if (node == encode)
				acodificar = node2;
		}

		return acodificar;
	}

	/** Asciende por el árbol de Huffman, y escribe los bits que 
	 * tocan
	 * 
	 * @param node el nodo desde el que comenzar la ascensión
	 * @throws IOException si hay algun problema de e/s
	 */
	protected void ascender(HuffmanNode node) throws IOException
	{
		if (node.parent == null)
			return;
		else
		{
			ascender(node.parent);
			if (node.parent.izq == node) outb.writeBit(false);
			if (node.parent.der == node) outb.writeBit(true);
		}

	}

    /**
     * Cierra el codificador, enviando los últimos bits.
	 *
     * @exception IOException if an  I/O error occurs.
     */
    public void close() throws IOException
    {
		super.close();
    	outb.close();
    	out.close();
    }
}
