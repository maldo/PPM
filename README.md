PPM
===

Prediction by partial matching

El programa se distribuye empaquetado en un archivo JAR. Para usarlo es necesario contar con la JRE 1.5 o superior. 
Algunos ejemplos para usarlo (suponiendo que PPM.jar está en el directorio actual y que la Java VM está adecuadamente instalada y en el path):

	java -jar PPM.jar c fichero.txt 5

Comprime el archivo fichero.txt usando un modelo de orden 5. Creará el resultado en fichero.txt.ppm

	java -jar PPM.jar d fichero.txt.ppm 5

Descomprime el archivo fichero.txt.ppm con un modelo de orden 5. Escribirá en fichero.txt.ppm

¡Hay que usar el mismo orden para comprimir y descomprimir!

	java -jar PPM.jar i

Entra en modo interactivo (cada dato necesario (orden, archivo...) se pide al usuario).
Durante la compresión o descompresión no hay ninguna indicación de progreso; cuando se termine la acción el programa terminará.
