package org.cdi.ppm;
/** Mantiene una lista de bytes excluidos
 * (es decir, una variable booleana para cada byte
 * posible [0..255]) 
 * 
 * @author Javier y Maldo
 */
public class Excluder {
	
	/** El número de valores máximos posibles para un tipo Byte.
	 */
	protected static final int byteRange =  Byte.MAX_VALUE - Byte.MIN_VALUE + 1;
	/** Un array de booleanos.
	 * excluded[i] es cierto si el byte I está excluido
	 */
	protected boolean[] excluded = new boolean[byteRange];
	/** Cuenta el total de carácteres excluidos
	 */
	protected int exclusionsCount = 0;
	
	/** Limpia la lista de excluidos, olvidándose de ellos
	 * 
	 *
	 */
	public void clear()
	{
		if (exclusionsCount == 0) return;
		
		int i;
		for (i = 0; i < byteRange; i++)
			excluded[i] = false;
		
		exclusionsCount = 0;
	}
	
	/** Excluye un byte
	 * 
	 * @param b byte a excluir
	 */
	public void exclude(byte b)
	{
		short i = unsignByte(b);
		if (!excluded[i])
		{
			exclusionsCount++;
			excluded[i] = true;
		}		
	}
	
	/** Determina si un byte está excluido
	 * 
	 * @param b byte a determinar
	 * @return true si está excluido
	 */
	public boolean isExcluded(byte b)
	{
		short i = unsignByte(b);
		return excluded[i];
	}
	
	/** Función auxiliar para convertir un byte en un indice de array
	 * 
	 * @param b byte (signed)
	 * @return valor válido como indice de array
	 */
	private static short unsignByte(byte b)
	{
		short ret = 0;
		ret |= b & 0xFF;
		assert ret >= 0;
		return ret;
	}
}
