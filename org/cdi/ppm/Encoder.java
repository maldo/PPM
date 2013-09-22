package org.cdi.ppm;
import java.io.*;
import java.util.*;

/** Clase base para un compresor basado en el modelo PPM
 * 
 * @author Javier y Maldo
 */

public class Encoder extends FilterOutputStream
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

	/** Crea una nueva instancia del compresor PPM
	 * 
	 * @param out La stream donde se escribirá el archivo resultante
	 * @param order Orden máximo la predicción
	 */
	public Encoder(OutputStream out, int order)
	{
		super(out);
		
		maxOrder = order;
		
		currentContext = new Context(order);
		model = new Model();
		
		/*try {
			debug = new PrintStream(new BufferedOutputStream(new FileOutputStream("debug.txt")), true);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			debug = null;
		}*/
	}

	/** Codifica el byte especificado
	 * 
	 * @param b el byte
	 * @throws IOException si ocurre un error de entrada/salida
	 */
	public void write(int b) throws IOException
	{
		byte[] buf = new byte[1];
		buf[0] = (byte) b;
		write(buf, 0, 1);
	}
	
	/** Codifica hasta <code>len</code> bytes del array especificado
	 * 
	 * @param b el array de bytes
	 * @param off el índice del primer byte del array a codificar
	 * @param len el número de bytes a codificar
	 * @throws IOException si ocurre un error de entrada/salida
	 */
	public void write(byte[] b, int off, int len) throws IOException
	{
		Context testContext;
		Excluder exclusions = new Excluder();
		List<ModelNode> list;
		ModelNode toEncode;
		int pos, max;
		
		pos = off;
		max = off + len;
		
		while (pos < max)
		{
			byte car = b[pos];

			//debug.print((char) car + ": ");
			
			testContext = currentContext.clone();
			exclusions.clear();
			
			while (true)
			{
				//debug.print(", contexto " + testContext.order() + " ");
				
				//Obtiene la lista de carácteres para el contexto actual
				list = model.getNodeListFor(testContext, exclusions);
				//Busca el carácter a comprimir en esa lista
				toEncode = model.searchNode(list, car);
				
				if (toEncode == null)
				{
					//Carácter no está en el contexto actual
					
					//Excluir todos los carácteres del contexto actual
					model.excludeAll(list, exclusions);
					
					//Escapar y reducir contexto en uno
					//debug.print("\\ " + Math.round(model.escape(testContext) * 100) + " % - ");
					toEncode = model.searchEscapeNode(list);
					assert toEncode != null: "El carácter de escape siempre está";
					encodeChar(list, toEncode);
					
					if (testContext.order() == 0)
					{
						//Hemos llegado al orden -1 y nada
						//Escribir carácter literal (1/todos los carácteres)
						//debug.print("Literal");
						list = model.getAllCharsList(exclusions);
						
						toEncode = model.searchNode(list, car);
						assert toEncode != null: "Todos los carácteres debe contener todos los cars";
						encodeChar(list, toEncode);
						break;
					}
					else
					{
						//Reducir contexto
						//debug.print("Drop");
						testContext.drop();
						
						continue;
					}
				}
				else
				{
					//Hay compresión
					//debug.print("Comprimido, " + Math.round(prob * 100) + " %");
					encodeChar(list, toEncode);
					break;
				}
			}
			
			//debug.println("");
			
			model.add(currentContext, car);
			currentContext.append(car);

			pos++;
		}
	}
	
	/** Codifica un carácter dada una tabla de probabilidades
	 * (debe ser sobrecargada)
	 * 
	 * @param l la tabla/lista de probabilidades
	 * @param encode el carácter a codificar
	 * @throws IOException si hay un error de salida al escribir
	 */
	protected void encodeChar(List<ModelNode> l, ModelNode encode) throws IOException
	{
		throw new ClassCastException();
	}
	
	/** Termina la compresión, enviando el carácter de fin de archivo
	 * (no cierra el fichero de salida, debe hacerlo la subclase)
	 */
	public void close() throws IOException
	{
		//Hay que enviar un EOF
		// para hacer eso necesitamos bajar al contexto literal, orden -1
		Excluder exclusions = new Excluder();
		List<ModelNode> list;
		ModelNode toEncode;
		
		//Reducir el contexto a orden -1
		while ( true )
		{
			//Obtiene la lista de carácteres para el contexto actual
			list = model.getNodeListFor(currentContext, exclusions);
			//Busca el carácter a comprimir en esa lista
			toEncode = model.searchEscapeNode(list);
			
			//Excluir todos los carácteres del contexto actual
			model.excludeAll(list, exclusions);
			//	Todos, así reducimos el tamaño de la lista de carácteres
			
			//Enviar un escape
			encodeChar(list, toEncode);
			
			//Y bajar el contexto, si aún se puede físicamente
			if ( currentContext.order() > 0 )
				currentContext.drop();
			else
				break; //Salir si hemos llegado a -1
		}
		
		//Ahora el orden es -1
		
		//Enviar EOF, literal
		list = model.getAllCharsList(exclusions);
		
		toEncode = model.searchEOFNode(list);
		assert toEncode != null: "Todo contexto debe contener a EOF";
		encodeChar(list, toEncode);
	}
}
