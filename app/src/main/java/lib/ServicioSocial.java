package lib;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import java.io.IOException;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
//import javax.swing.ImageIcon;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/**
 * 
 * @author mar
 * 13/11/2017
 *
 */
public class ServicioSocial {
    // estados
    public final static String APROBADO = "aprobado";
    public final static String VACIO = "vacio";
    public final static String ERROR = "error";
    final static String URL_RAIZ = "http://192.168.1.74/ssocial/";//la de mi casa
//        final static String URL_RAIZ = "http://192.168.43.143/ssocial/";// cuando uso el telefono como modem
    final static String URL_BASE = URL_RAIZ + "index.php?modulo=";
//    final static String URL_BASE = "http://192.168.43.143/ssocial/index.php?modulo="; // cuando uso el telefono?
    //    final static String URL_BASE = "http://192.168.42.190/index.php?modulo="; // o es esta otra cuando uso el telefono?
    final static String LOGUEO = "logeo";
    final static String SALIR = "salir";
    final static String MI_CUENTA = "miCuenta";
    final static String CARTA_PRESENTACION = "admin&avance=v_cartaPresentacion";

    public final static String COOKIE_PHP = "PHPSESSID";

    public final static String TERCER_A = "Tercer Avance";
    public final static String CURSO = "Asistencia en curso de Inducción";
    public final static String SOLICITUD_RE  = "Solicitud de Registro Servicio Social";
    public final static String  INFORME_G = "Informe Global";
    public final static String CARTA_EV =  "Carta de evaluación receptora";
    public final static String CARTA_PRE = "Solicitar carta de presentación";
    public final static String SEGUNDO_A ="Segundo Avance";
    public final static String OFICIO_T = "Oficio de Terminación";
    public final static String PRIMER_A = "Primer avance";

    private String mensaje; //guarda el mesaje de respuesta en algunos metodos
    private String noControl;
    private String pass;
    private String cookie;
    public String mensajes;
    public boolean sesionIniciada;
    public HashMap<String, String> actividades; // es que es molesto tener que buscar formas
    // de pasar las cosas cuando  usas clases anonimas

    /**
     * Crea una instancia de esta madre a la vez que inicia sesión con las credenciales provistas. Lanza excepcion si sale un error.
     * @param noControl Numero de control del alumno, debe tner 8 dígitos
     * @param pass Contraseña del alumno
     */
    public ServicioSocial(String noControl, String pass) {
        this.noControl = noControl;
        this.pass = pass;
        sesionIniciada = false;
    }
    public String iniciarSesion()  {
        if(noControl.length() != 8) return ("Error, el número de contorl sólo puede tener 8 dígitos.");
        // obtener cookie
        //obtener cookie de phpsesion
        Document doc;
        try {
            Connection.Response respuesta = Jsoup.connect(URL_BASE)
                    .method(Connection.Method.HEAD)
                    .execute();
            cookie = respuesta.cookie(COOKIE_PHP);
            // mandamos nuestra solicitud
            doc = Jsoup.connect(URL_BASE + LOGUEO)
                    .data("noControl", noControl)
                    .data("clave", pass)
                    .data("sesion", "Iniciar Sesion")
                    .followRedirects(false)
                    .cookie(COOKIE_PHP, cookie)
                    .timeout(2000)
                    .post();
        }catch (Exception ex){
            String msg ="Error al acceder al servidor" + "\n" + ex.getMessage();
            Log.e("ServicioSocial", msg);
            ex.printStackTrace();
            return msg;
        }
        // comprobamos que nos hayamos podido 
//        String resultado = doc.getElementById("respuesta").outerHtml();
        String resultado = doc.outerHtml();
        
        if(resultado.contains("correctamente")) {
            sesionIniciada = true;
            return "Sesion iniciada correctamente";

        }
        else if(resultado.contains("Error")){
            sesionIniciada = false;
            return "Error con los datos de acceso";

//            throw new Exception("Datos de acceso incorrectos");
        }
        else {
            sesionIniciada = false;
            return "Error desconocido";
        }
    }
    
    /**
     * Envia una solicitud de cerrar sesion, lanza excepción si no lo puede comprobar.
     */
    public void cerrarSesion() throws Exception{
        Document doc = Jsoup.connect(URL_BASE + SALIR).get();
        System.out.println(doc.getElementsByTag("h2").text());
    }
    
    /**
     * Regresa el progreso del estado de liberacion actual. En cao de haber error regresa -1
     * @return entero del 0 al 100
     */
    public int progresoDeLiberacion() {
        int progreso = 0;
        try {
            Document doc = conectar(URL_BASE + MI_CUENTA).get();
            String porcentaje = doc.getElementsByClass("barra").first().child(0).text();
            porcentaje = porcentaje.substring(0, porcentaje.length()-1);
            progreso = Integer.valueOf(porcentaje);
        } catch (IOException ex) {
            System.err.println("Error al obtener progreso");
            progreso = -1;
        }
        return progreso;
    }
    
    public Bitmap descargarImagen() {
//        String URL = "http://localhost/ssocial/usuarios/" + noControl + "/avatar.jpg";
        String URL = "http://tecuruapan.edu.mx/ssocial/usuarios/" + noControl  + "/avatar.jpg";
        try {
            Connection.Response respuesta = Jsoup.connect(URL)
                    .ignoreContentType(true).
                    execute();
            Bitmap img = BitmapFactory.decodeByteArray(respuesta.bodyAsBytes(),0,respuesta.bodyAsBytes().length);
//            ImageIcon img = new ImageIcon(respuesta.bodyAsBytes());
            return img;
        } catch (IOException ex) {
            Logger.getLogger(ServicioSocial.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
        /* Guardando al disco duro            
        ImageIcon img = ServicioSocial.descargarImagen();
        Image i = img.getImage();
        BufferedImage bi = new BufferedImage(i.getWidth(null), i.getHeight(null), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = bi.createGraphics();
        g.drawImage(i, 0,0, null);
        g.dispose();
        ImageIO.write(bi, "jpg",  new File("img.jpg"));
        */
    }
    /*
    posibles estados:
    espera
    alerta
    aprobado
    vacio 
    error / reprobado
    */
    public HashMap<String, String> recuperarActividades () {
        HashMap<String, String> actividades = new HashMap<>();
        try {
            Document doc = conectar(URL_BASE + MI_CUENTA).get();
            Elements tabla = doc.getElementsByTag("tr");
            String nombre = "";
            String estado = "";
            for(int i =1; i< tabla.size() -9; i ++) {
                nombre = tabla.get(i).text();
                nombre = nombre.substring(2, nombre.length() - 1);
                estado = tabla.get(i).getElementsByTag("img").first().attr("src");
                estado = interpretarEstado(estado);
                actividades.put(nombre, estado);
            }
            
        } catch (IOException ex) {
            Logger.getLogger(ServicioSocial.class.getName()).log(Level.SEVERE, null, ex);
        }

        return actividades;
    }
    
    
   
    /**
     * Devuelve todos los datos del estudiante registrado: nombre, dirección, teléfono, celular, correo, matrícula, idUnico, carrera, semestre, 
     * @return Un HashMap que contiene los datos, cada uno con su nombre como llave. por ejemplo: "nombre", "Rico Aguilar Omar"
     */    
    public HashMap<String, String> recuperarMisDatos () {
        HashMap<String, String> datos = new HashMap<>();
        try{
            Document doc = conectar(URL_BASE + MI_CUENTA).get();
            String nombre = doc.getElementById("nombre").val();
            String direccion = doc.getElementById("direccion").val();
            String telefono = doc.getElementById("telefono").val();
            String celular = doc.getElementById("celular").val();
            String correo = doc.getElementById("correo").val();

            String periodo = "";
            Element periodos = doc.getElementById("periodo");
            for(int i = 0; i<periodos.childNodeSize(); i++) {
                if(periodos.child(i).hasAttr("selected")){
                    periodo = periodos.child(i).text();
                    break;
                }
            }

            String cuerpo = doc.text();
            
            String matricula = "" ;
            String idUnico = "" ;
            String semestre = "";
            String carrera = "";
            
            Matcher m = Pattern.compile("Matr.cula: (\\d{8})|Semestre: (\\w*)|Carrera: (\\w*\\. +\\w*)|ID .nico: (\\d*)").matcher(cuerpo);
            if(m.find()){
                matricula = m.group(1);
                m.find();
                semestre = m.group(2);
                m.find();
                carrera = m.group(3);
                m.find();
                idUnico = m.group(4);
            }
            
            datos.put("nombre", nombre);
            datos.put("direccion", direccion);
            datos.put("telefono", telefono);
            datos.put("celular", celular);
            datos.put("periodo", periodo);
            datos.put("correo", correo);
            datos.put("matricula", matricula);
            datos.put("idUnico", idUnico);
            datos.put("semestre", semestre);
            datos.put("carrera", carrera);
            
            
            
        }catch(final Exception e) {
            System.err.println("Error al recuperar mis datos:" + e.getMessage());
        }
        
        return datos;
    }

    
  
    
    /**
     * Devuelve los títulos de las noticias en la página principal seguidos de una coma y luego la fecha.
     * No es necesario haber iniciado sesión antes.
     * @return Un arrego de String donde cada posición tiene un titulo, null si no hay noticias o no se pudo conectar.
     */
    public static String[] recuperarNoticias() {
        String [] noticias;
        try {  
            Document doc = Jsoup.connect(URL_BASE ).get();
            Elements links = doc.getElementById("noticias").getElementsByTag("a");
            noticias = new String[links.size()];
            int indice = 0;
            for(Element link : links){
                noticias[indice] = link.text();
                System.out.println(link.text());
            }
        } catch (IOException ex) {
            Logger.getLogger(ServicioSocial.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
        
        return null;
    }

    /**
     * Actualiza los datos que salen en la sección de mi cuenta. Si no se registran se devuelve false entonces hay que checar el mensaje de error en
     * en la variable con ultimoMensaje()
     * @param nombre Se recomienda que empiece por apellidos
     * @param direccion Direccion completa
     * @param telefono Teléfono de casa
     * @param celular Teléfono celular
     * @param correo Dirección de correo electrónico
     * @param periodo En realidad hay que mandar 3, 2, 1, según los datos del spiner, temporalmente desailitado
     * @return Si los datos fueron actualizados con éxito
     */
    public boolean actualizarMisDatos(String nombre, String direccion, String telefono, String celular, String correo, String periodo) {
        boolean actualizadosConExito = false;
        try {
//            Document respuesta = conectar(URL_BASE + MI_CUENTA).get();
            Document respuesta = conectar(URL_BASE + MI_CUENTA)
                    .data("nombre", nombre)
                    .data("direccion", direccion)
                    .data("telefono", telefono)
                    .data("celular", celular)
                    .data("correo", correo)
                    .data("periodo", "3")
                    .data("guardar", "Guardar Cambios")
                    .post();
            Element aviso = respuesta.getElementById("aviso");
            Element error = respuesta.getElementById("color");
            if(aviso != null){
                actualizadosConExito = aviso.text().contains("DATOS ACTUALIZADOS CORRECTAMENTE");
                mensaje = aviso.text();
            }
            if(error != null){
                actualizadosConExito = false;
                mensaje = error.text();
            }


        } catch (IOException ex) {
            Logger.getLogger(ServicioSocial.class.getName()).log(Level.SEVERE, null, ex);
            this.mensaje = "Se disparó una excepción: " + ex.getMessage();
            return false;
        }

        return actualizadosConExito;
    }

    /**
     * Devuelve el estado segun el nombre de la imagen.
     * Como que tiene un bug la cosa parseadora de jsoup, en una devuelve acio
     * en lugar de vacio
     * @param estado el estado, por ejemplo: "error", "espera", etc.
     * @return 
     */
    private String interpretarEstado(String estado) {
        estado = estado.substring(7,estado.length() - 4);
        if(estado.equals("acio")) return "vacio";
        return estado;
    }
    
      /**
     * Devuelve una conexion con a cookie puesta para mantener la sesion.
     * @param url la url a la que nos vamos a conectar
     * @return objeto Connection para que quien llame este metodo escoja otros detalles
     */
    private Connection conectar(String url) {
        return Jsoup.connect(url).cookie(COOKIE_PHP, cookie);
    }

    /**
     * Devuelve el link de descarga del formato para que se encargue de bajarlo el sistema.
     * @return
     */
    public String linkFormatoEvaluacionR() {
        return URL_RAIZ + "documentos.php?cual=" + "Evaluacion_Receptora.docx";
    }

    public String linkFormatoSolicitudRe () {
        return URL_RAIZ + "documentos.php?cual=" + "Solicitud_De_Registro.docx";
    }

    public String linkFormatoInformeBimestral () {
        //http://localhost/ssocial/documentos.php?cual=Informe_Bimestral.docx
        return URL_RAIZ + "documentos.php?cual=" + "Informe_Bimestral.docx";
    }

    public String linkFormatoInformeGlobal () {
        //http://localhost/ssocial/documentos.php?cual=Informe_Global.docx
        return URL_RAIZ + "documentos.php?cual=" + "Informe_Global.docx";
    }
    public String linkEjemploCartaAceptacion() {
        //http://tecuruapan.edu.mx/ssocial/documentos.php?cual=encuestaservicio.pdf
        return URL_RAIZ + "documentos.php?cual=" + "encuestaservicio.pdf";
    }

    public String linkSubirEvaluacion() {
        return URL_BASE + "admin&avance=v_evaluacionReceptora";
    }
     public String getCookie() {
        return cookie;
     }
    /**
     * Devuelve el mensaje que haya sio almacenado y luego lo vacia.
     * @return Algún posible mensaje de respuesta.
     */
    public String ultimoMensaje() {
        String salida = mensaje;
        this.mensaje = "";
        return salida;
    }

    public HashMap<String, String> recuperarDatosCartaPresentacion() {
        HashMap<String, String> datos = new HashMap<>();
        try{
            //http://localhost/ssocial/index.php?modulo=admin&avance=v_cartaPresentacion
            Document doc = conectar(URL_BASE + CARTA_PRESENTACION).get();
            String dependencia = doc.getElementById("dependencia").val();
            String ambito = parsearSelect(doc.getElementById("ambito"));
            String organismo = parsearSelect(doc.getElementById("organismo"));
            String encargado = doc.getElementById("encargado").val();
            String puesto = doc.getElementById("puesto").val();
            String direccion = doc.getElementById("direccion").val();
            String telefono = doc.getElementById("telefono").val();
            String programa = doc.getElementById("programa").val();
            String subprograma = doc.getElementById("subprograma").val();

            String idia = parsearSelect(doc.getElementById("idia"));
            String imes = parsearSelect(doc.getElementById("imes"));
            String anyo = parsearSelect(doc.getElementById("anyo"));
            String tdia = parsearSelect(doc.getElementById("tdia"));
            String tmes = parsearSelect(doc.getElementById("tmes"));
            String eanyo = parsearSelect(doc.getElementById("eanyo"));

            datos.put("dependencia", dependencia);
            datos.put("ambito", ambito);
            datos.put("organismo", organismo);
            datos.put("encargado", encargado);
            datos.put("puesto", puesto);
            datos.put("direccion", direccion);
            datos.put("telefono", telefono);
            datos.put("programa", programa);
            datos.put("subprograma", subprograma);
            datos.put("fechaInicio", idia + "/" + imes + "/" + anyo) ;
            datos.put("fechaFinal", tdia + "/" + tmes + "/" + eanyo) ;

        }catch(final Exception e) {
            System.err.println("Error al recuperar mis datos:" + e.getMessage());
        }

        return datos;
    }
    /**
     * Encuentra el valor seleccionado dentro del select (HTML).
     * @param select Elemento HTML sobre el cual se busca.
     * @return Valor que tenga el atributo de seleccionado en el spinner.
     */
    private String parsearSelect(Element select){
        String valorSeleccionado = "";
        for(int i = 0; i<select.childNodeSize(); i++) {
            if(select.child(i).hasAttr("selected")){
                valorSeleccionado = select.child(i).val();
                break;
            }
        }
        return valorSeleccionado;
    }

}
