<h1>Robot para tests de integración SADE</h1>
<p>Este comando permite verificar si uno de los ambientes de SADE -o algún
módulo en particular- se encuentran operativos (opción <code>-t oper</code>), o ejecutar
operaciones (opción <code>-t perf</code>) para medir el tiempo de respuesta.</p>
<p>Dadas las características de todas las aplicaciones web modernas y el 
uso extensivo de JavaScript y frameworks dinámicos como ZK, simular las
acciones de un usuario y su navegador es complejo y sujeto a problemas de
timing.</p>
<p>Ante un resultado negativo el comando reintenta la operación varias veces
para eliminar los falsos negativos, lo cual incrementa el tiempo total de la
prueba. El tiempo de respuesta indicado es el de la operación exitosa, sin
incluir los reintentos.</p>
<p>Si de todas formas si el resultado indicara que alguno de los módulos no
está operativo, se recomienda realizar una verificación manual para descartar
cualquier tipo de duda.</p>
<hr>
<h2>Uso:
<code>sadetester -a <env|hml|prd> [-d] [-h] [-m <arg>] -p <string> -t <perf|oper> -u <string>

	-a,--ambiente <env|hml|prd>   ambiente donde realizar el test
	-d,--debug                    imprime información para debug (default = no)
	-h,--help                     imprime esta ayuda
	-m,--modulo <gedo|ccoo|...>   modulo a testear (default = todo el ambiente)
	-p,--password <string>        password para el login a SADE
	-t,--tipo <perf|oper>         tipo de test a realizar 
	-u,--usuario <string>         usuario para el login a SADE
</code>