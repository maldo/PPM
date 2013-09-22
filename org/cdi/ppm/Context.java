package org.cdi.ppm;
/** Representa un contexto determinado.
 * Se parece a un <code>ByteBuffer</code>.
 * (es decir, sencillamente almacena unos cuantos bytes
 * de forma dinámica)
 *
 * @author Javier
 */
public class Context implements Cloneable {
	/** Buffer con los n bytes
	 */
	protected byte[] buf;
	/** Siguiente índice del array <code>buf</code> libre
	 */
	protected int pos;
	
	/** Constructor que crea un nuevo contexto vacio
	 * 
	 * @param maxOrder orden máximo, para efectos de reservar memoria
	 */
	public Context(int maxOrder)
	{
		buf = new byte[maxOrder];
		pos = 0;
	}
	
	/** Devuelve el orden actual del contexto
	 * 
	 * @return valor entre 0 y orden máximo que indica el orden actual de este contexto
	 */
	public int order()
	{
		return pos;
	}
	
	/** Reduce el orden actual del contexto en 1
	 * 
	 * @throws IndexOutOfBoundsException si el contexto es de orden 0
	 */
	public void drop() throws IndexOutOfBoundsException
	{
		if (buf.length == 0) return;
		if (pos == 0)
			throw new IndexOutOfBoundsException();
		
		int i;
		int max = pos - 1;
		for (i = 0; i < max; i++)
		{
			buf[i] = buf[i + 1];
		}
		pos--;
	}
	
	/** Vacía el contexto
	 */
	public void clear()
	{
		pos = 0;
	}
	
	/** Añade un byte a este contexto, aumentando su orden actual
	 * 
	 * @param a el byte a añadir
	 */
	public void append(byte a)
	{
		if (buf.length == 0) return;
		if (pos == buf.length)
		{
			int i;
			int max = pos - 1;
			for (i = 0; i < max; i++)
			{
				buf[i] = buf[i + 1];
			}
			buf[max] = a;
		} 
		else
		{
			buf[pos++] = a;
		}
	}
	
	/** Devuelve el byte en la posición especificada
	 * 
	 * @param p el índice del byte a obtener
	 * @return el byte en la posición p
	 * @throws IndexOutOfBoundsException si el indice no está en el contexto
	 */
	public byte getByteAt(int p)
	{
		if (p >= pos)
			throw new IndexOutOfBoundsException();
		
		return buf[p];
	}
	
	/** Devuelve la posición de la primera aparición del byte
	 * 
	 * @param b byte a buscar
	 * @return posición del byte en el contexto, o -1 si no encontrado.
	 */
	public int indexOf(byte b)
	{
		int i;
		for (i = 0; i < pos; i++)
		{
			if (b == buf[i])
				return i;
		}
		
		return -1;
	}
	
	/** Clona el contexto actual, devolviendo una copia independiente
	 * 
	 * @return una copia del contexto actual
	 */
	public Context clone()
	{
		Context c = new Context(buf.length);
		c.buf = buf.clone();
		c.pos = pos;
		
		return c;
	}
	
	/** Devuelve una string con el contexto actual
	 * (para depuración)
	 * 
	 * @return string con el contexto actual
	 */
	public String toString() {
			return new String(buf, 0, pos);
    }
	
}