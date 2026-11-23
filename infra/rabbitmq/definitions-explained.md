# Guia de definitions.json

JSON no admite comentarios. Esta guia acompana al archivo importado por RabbitMQ.

- **vhosts**: espacio logico / donde se declaran los recursos.
- **exchanges**: notifications es durable y de tipo topic: enruta por patrones de routing key. No es un topic Kafka.
- **queues**: new_orders_queue recibe pedidos; sms_queue y push_queue reciben tareas de notificacion.
- **durable: true**: conserva la definicion tras reinicios. La persistencia del mensaje se configura por separado.
- **auto_delete: false**: desactiva la eliminacion automatica al desaparecer consumidores (o bindings, en exchanges).
- **bindings**: notify.client.* conecta notifications con sms_queue; notify.driver.* lo conecta con push_queue.
- **Comodin asterisco**: coincide con exactamente una palabra separada por puntos. notify.client.sms coincide con notify.client.*.
- **arguments vacio**: no hay argumentos adicionales; aqui no se configura dead-letter para la cola principal.
- **Exchange predeterminado**: RabbitOrderCommandAdapter publica con exchange vacio y nombre de cola como routing key; no necesita un binding explicito en este archivo.

Varias instancias consumiendo una cola se reparten entregas; puede haber reentregas ante fallos.
Declarar una DLQ en RabbitConfig no basta: la cola original necesita argumentos o una politica de dead-letter para enviarle rechazos.
