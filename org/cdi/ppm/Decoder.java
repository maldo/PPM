package org.cdi.ppm;
import java.io.*;
import java.util.*;

/** Clase base para un decompresor basado en el modelo PPM
 * 
 * @author Javier y Maldo
 */

public class Decoder extends FilterInputStream
{
	//protected PrintStream debug;
	
	/** Almacena el orden máximo
	 */
	protected final int maxOrder;
	
	/** El contexto actual
	 */
	protected final Context currentContext;
	
	/** El modelo usado
	 */
	protected final Model model;
	
	/** Indica si hemos recibido un carácter EOF
	 */
	protected boolean eofReached = false;

	/** Crea una nueva instancia del decompresor
	 * 
	 * @param in La stream donde está el archivo comprimido
	 * @param order Orden máximo de la predicción
	 */
	public Decoder(InputStream in, int order)
	{
		super(in);
		
		maxOrder = order;
		
		currentContext = new Context(order);
		model = new Model();
		
		//debug = System.out;
		/*try {
			debug = new PrintStream(new BufferedOutputStream(new FileOutputStream("debug.txt")), true);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			debug = null;
		}*/
	}

	/** Decodifica un byte
	 * 
	 * @return byte decodificado
	 * @throws IOException si ocurre un error de entrada/salida
	 */
	public int read() throws IOException
	{
		byte[] buf = new byte[1];
		int lenread = read(buf, 0, 1);
		if (lenread < 1) return -1;
		return buf[0];
	}
	
	/** Decodifica hasta <code>len</code> bytes en el array especificado
	 * 
	 * @param b el array de bytes
	 * @param off el índice del primer byte usable del búffer
	 * @param len la longitud del buffer
	 * @return el número de bytes decodificados
	 * @throws IOException si ocurre un error de entrada/salida
	 */
	public int read(byte[] b, int off, int len) throws IOException
	{
		Context testContext;
		Excluder exclusions = new Excluder();
		List<ModelNode> list;
		int pos, max, read = 0;
		
		pos = off;
		max = off + len;
		
		if (eofReached) return -1;
		
		while (pos < max)
		{
			ModelNode node;
			byte car;
			
			testContext = currentContext.clone();
			exclusions.clear();
			
			while (true)
			{		
				//debug.print("En orden" + testContext.order());
				
				list = model.getNodeListFor(testContext, exclusions);
				
				assert list != null && list.size() > 0: "No hay nodos?";
				
				node = decodeChar(list);
				
				if (node == null)
				{
					throw new IOException("Origen inválido");
				}
				
				if (model.isEscape(node))
				{
					//Ha habido un escape
					
					//Realizar exclusiones
					model.excludeAll(list, exclusions);
					
					//Escapar y reducir contexto en uno			
					if (testContext.order() == 0)
					{
						//Hemos llegado al orden -1 y nada
						//Leer carácter literal
						list = model.getAllCharsList(exclusions);
						
						node = decodeChar(list);
						
						if (model.isEscape(node))
						{
							throw new IOException("Llegado a orden -2");
						}
						else if (model.isEOF(node))
						{
							//Fin del archivo!!
							eofReached = true;
							return read;
						}
						else
						{
							car = node.car;
						}
						break;
					}
					else
					{
						//Reducir contexto
						//debug.print("drop");
						testContext.drop();
						
						continue;
					}
				}
				else
				{
					//OK, tenemos un carácter
					car = node.car;
					break;
				}
			}
			
			model.add(currentContext, car);
			currentContext.append(car);
			
			//Guardar carácter decodificado
			b[pos] = car;
			
			//debug.print("tengo: " + car);

			pos++;
			read++;
			//debug.println();
		}
		
		return read;
	}
	
	/** Decodifica un carácter, dada una lista con los carácteres posibles
	 * (debe ser sobrecargada por un decodificador)
	 * 
	 * @param l la lista con carácteres posibles
	 * @return el carácter encontrado
	 * @throws IOException si hay algun error de e/s
	 */
	protected ModelNode decodeChar(List<ModelNode> l) throws IOException
	{
		throw new ClassCastException();
	}
}