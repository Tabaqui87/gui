# gui
## Descrizione
gui è un'applicazione ad interfaccia grafica con lo scopo di monitorare via JMX la memoria heap utilizzata da una JVM remota, esponendone i valori letti attraverso un grafico e notificando l'utente nel caso in cui venga superato un "valore soglia".
## Parametri
I parametri di configurazione verranno letti dalla risorsa **configuration.properties** e potranno essere sovrascritti tramite riga di comando con la sintassi **chiave=valore**. I parametri gestiti sono i seguenti:
### tabaqui.host
Nome host della JVM remota alla quale collegarsi. Default: **localhost**
### tabaqui.port
Porta della JVM remota alla quale collegarsi. Default: **9999**
### tabaqui.refresh-rate
Intervallo in millisecondi tra una lettura e la successiva. Default: **250**
### tabaqui.time-window
Intervallo temporale in millisecondi che verrà visualizzato in grafico. Default: **un minuto**
### tabaqui.alert-value
Valore soglia in byte della memoria heap oltre il quale l'utente verrà avvisato. Default: **50 MB**
## Tecnologie e note
L'applicazione gui è stata sviluppata utilizzando Swing. In una fase embrionale si era pensato (ed iniziato) a sviluppare da zero un JComponent custom per visualizzare il grafico in modo da avere maggiori libertà ma, per ragioni pratiche, si è alla fine optato per l'utilizzo di librerie già pronte come JFreeChart (vedi dipendenze maven).
## Requisiti
* Java 21
## Esecuzione
L'applicazione potrà essere eseguita tramite riga di comando con la seguente sintassi:
```
java -jar ./gui.jar
```
E' possibile sovrascrivere i parametri di default passando coppie chiave-valore direttamente tramite riga di comando:
```
java -jar ./gui.jar tabaqui.port=9998
```
## Note
Scegliendo di utilizzare **remote-jvm** come JVM da monitorare, sarà possibile collegarsi anche tramite **jconsole** e modificare a piacimento l'ammontare di memoria heap utilizzata tramite l'MBean **it.tabaqui:type=Memory,name=HeapManager**.
