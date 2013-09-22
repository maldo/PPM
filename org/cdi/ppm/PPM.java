package org.cdi.ppm;
import java.io.*;

/** Gestiona la comunicación con el usuario y llama al Encoder y al Decoder
 * 
 * @author Javier y Maldo
 */

public class PPM
{
	/** Tamaño del búfer para operaciones de I/O */
	private final static int buf_len = 1024;
	
	/** Acciones que puede realizar este programa */
	private static enum ActionType
	{
		COMPRESS, DECOMPRESS
	}
	
	//Parámetros:
	/** El nombre de archivo origen */
	private static String archivo = "";
	/** El orden máximo */
	private static int order = -1;
	/** La acción a realizar */
	private static ActionType action;
	
	/** Punto de entrada del programa PPM
	 * 
	 * @param args parámetros de línea de comandos
	 */
	public static void main(String[] args) {

		InputStream in;
		OutputStream out;
		
		leer_parametros(args);
		
		if (archivo.equals("-"))
		{
			in = System.in;
			out = System.out;
		}
		else
		{
			FileInputStream inFile;
			FileOutputStream outFile;
			
			try
			{
				inFile = new FileInputStream(archivo);
			}
			catch (FileNotFoundException e)
			{
				System.err.println("Archivo no encontrado");
				System.exit(1);
				return;
			}
			
			String archivo_salida;
			switch (action)
			{
				case COMPRESS:
					archivo_salida = archivo + ".ppm";
				break;
				case DECOMPRESS:
					archivo_salida = archivo + ".ppmdec";
				break;
				default:
					throw new IllegalArgumentException("Nada que hacer");
			}
			
			try
			{
				outFile = new FileOutputStream(archivo_salida);
			} catch (FileNotFoundException e) {
				System.err.println("Problema al crear el archivo de salida");
				System.exit(1);
				return;
			}
			
			in = new BufferedInputStream(inFile);
			out = new BufferedOutputStream(outFile);
		}
		
		try
		{
			switch (action)
			{
				case COMPRESS:
					compress(in, out, order);
				break;
				case DECOMPRESS:
					decompress(in, out, order);
				break;
			}
		}
		catch (IOException e) {
			System.err.println("Problema de entrada/salida");
			e.printStackTrace();
		}
		
		try
		{
			in.close();
			out.close();
		}
		catch (IOException e) {
			System.err.println("Problema al cerrar archivos");
			e.printStackTrace();
		}
	}
	
	/** Lee los parámetros enviados al programa (desde linea de comandos o modo interactivo)
	 * 
	 * @param args parámetros de línea de comandos
	 */
	public static void leer_parametros(String[] args)
	{
		String accion;
		String orden;
		
		if (args.length < 3 || args[1] == "i")
		{
			//Modo interactivo

			//Leer el nombre del archivo de entrada
			System.out.print("Acción (C/D): ");
			try
			{

				BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
				
				accion = br.readLine();

			}
			catch (Exception e)
			{
				System.err.println("Problema al leer la acción");
				e.printStackTrace();
				System.exit(1);
				return;
			}
			
			if (accion == null || accion.length() == 0)
				return; //Salir
			
			//Leer el nombre del archivo de entrada
			System.out.print("Archivo de entrada: ");
			try
			{

				BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
				
				archivo = br.readLine();

			}
			catch (Exception e)
			{
				System.err.println("Problema al leer el nombre del archivo");
				e.printStackTrace();
				System.exit(1);
				return;
			}
			
			if (archivo.length() == 0) return; //Salir
			
			
			//Leer el orden
			System.out.print("Orden: ");
			try
			{

				BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
				
				orden = br.readLine();

			}
			catch (Exception e)
			{
				System.err.println("Problema al leer el orden");
				e.printStackTrace();
				System.exit(1);
				return;
			}
			
			if (orden == null || orden.length() == 0)
				return; //Salir
		}
		else
		{
			//Modo no interactivo
			accion = args[0];
			archivo = args[1];
			orden = args[2];
			
		}
		
		//Convertir la accion a algo usable
		accion = accion.toLowerCase();
		
		if (accion.indexOf('c') >= 0)
		{
			action = ActionType.COMPRESS;
		}
		if (accion.indexOf('d') >= 0 | accion.indexOf('x') >= 0)
		{
			action = ActionType.DECOMPRESS;
		}
		
		//Convertir el orden a valor numérico
		try
		{
			order = Integer.parseInt(orden);
		}
		catch (Exception e)
		{
			System.err.println("Problema al leer el orden");
			e.printStackTrace();
			System.exit(1);
		}
		
		if (order < 0) 
		{
			throw new IllegalArgumentException("Orden negativo");
		}
	}
	
	/** Comprime
	 * 
	 * @param in desde donde
	 * @param out hacia donde
	 * @param order orden máximo
	 * @throws IOException si hay un problema de entrada/salida o defecto con el archivo
	 */
	public static void compress(InputStream in, OutputStream out, int order) throws IOException
	{
		Encoder enc;
		enc = new HuffmanEncoder(out, order);

		
		byte[] buf = new byte[buf_len];
		int read;
		read = in.read(buf);
		while (read > 0)
		{
			enc.write(buf, 0, read);
			read = in.read(buf);
		};
		enc.close();
	}

	/** Descomprime
	 * 
	 * @param in desde donde
	 * @param out hacia donde
	 * @param order orden máximo
	 * @throws IOException si hay un problema de entrada/salida o defecto con el archivo
	 */
	public static void decompress(InputStream in, OutputStream out, int order) throws IOException
	{
		Decoder dec;
		dec = new HuffmanDecoder(in, order);

		
		byte[] buf = new byte[buf_len];
		int read;
		
		read = dec.read(buf);
		while (read > 0)
		{
			out.write(buf, 0, read);
			read = dec.read(buf);
		}
		dec.close();
	}

}
