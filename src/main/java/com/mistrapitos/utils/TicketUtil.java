package com.mistrapitos.utils;

import com.mistrapitos.models.Cliente;
import com.mistrapitos.models.DetalleVenta;
import com.mistrapitos.models.Venta;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.print.*;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.print.attribute.standard.Copies;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

/**
 * Utilidad para la impresión de tickets de venta.
 */
public class TicketUtil {
    
    private static final Logger logger = LoggerFactory.getLogger(TicketUtil.class);
    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("#,##0.00");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
    private static final int ANCHO_TICKET = 40; // Caracteres
    
    /**
     * Imprime un ticket de venta en la impresora predeterminada.
     * @param venta Venta para la que se imprimirá el ticket
     * @param cliente Cliente de la venta (opcional)
     * @return true si se imprimió correctamente, false en caso contrario
     */
    public static boolean imprimirTicket(Venta venta, Optional<Cliente> cliente) {
        try {
            String contenido = generarContenidoTicket(venta, cliente);
            return enviarAImpresora(contenido);
        } catch (Exception e) {
            logger.error("Error al imprimir ticket para la venta ID: " + venta.getIdVenta(), e);
            return false;
        }
    }
    
    /**
     * Genera el contenido del ticket de venta.
     * @param venta Venta para la que se generará el ticket
     * @param cliente Cliente de la venta (opcional)
     * @return Contenido del ticket
     */
    public static String generarContenidoTicket(Venta venta, Optional<Cliente> cliente) {
        StringBuilder sb = new StringBuilder();
        
        // Encabezado
        sb.append(centrarTexto("MIS TRAPITOS", ANCHO_TICKET)).append("\n");
        sb.append(centrarTexto("TIENDA DE ROPA", ANCHO_TICKET)).append("\n");
        sb.append(repetirCaracter('-', ANCHO_TICKET)).append("\n");
        sb.append("TICKET DE VENTA #").append(venta.getIdVenta()).append("\n");
        sb.append("FECHA: ").append(venta.getFecha().format(DATE_FORMATTER)).append("\n");
        
        // Datos del cliente
        if (cliente.isPresent()) {
            sb.append("CLIENTE: ").append(cliente.get().getNombre()).append("\n");
            if (cliente.get().getTelefono() != null && !cliente.get().getTelefono().isEmpty()) {
                sb.append("TELÉFONO: ").append(cliente.get().getTelefono()).append("\n");
            }
        }
        
        sb.append(repetirCaracter('-', ANCHO_TICKET)).append("\n");
        
        // Encabezado de detalles
        sb.append(String.format("%-20s %5s %7s %7s", "PRODUCTO", "CANT", "PRECIO", "TOTAL")).append("\n");
        sb.append(repetirCaracter('-', ANCHO_TICKET)).append("\n");
        
        // Detalles de la venta
        for (DetalleVenta detalle : venta.getDetalles()) {
            String nombreProducto = detalle.getProductoNombre();
            if (nombreProducto.length() > 20) {
                nombreProducto = nombreProducto.substring(0, 17) + "...";
            }
            
            double precioUnitario = detalle.getPrecioConDescuento();
            double subtotal = detalle.getSubtotal();
            
            sb.append(String.format("%-20s %5d %7s %7s", 
                    nombreProducto, 
                    detalle.getCantidad(), 
                    "$" + DECIMAL_FORMAT.format(precioUnitario),
                    "$" + DECIMAL_FORMAT.format(subtotal))).append("\n");
            
            // Si hay descuento, mostrarlo
            if (detalle.getDescuentoAplicado() > 0) {
                sb.append(String.format("  Desc: %.0f%% aplicado", detalle.getDescuentoAplicado())).append("\n");
            }
        }
        
        sb.append(repetirCaracter('-', ANCHO_TICKET)).append("\n");
        
        // Total
        sb.append(String.format("%32s %7s", "TOTAL:", "$" + DECIMAL_FORMAT.format(venta.getTotal()))).append("\n");
        sb.append(repetirCaracter('-', ANCHO_TICKET)).append("\n");
        
        // Método de pago
        sb.append("MÉTODO DE PAGO: ").append(venta.getMetodoPago()).append("\n");
        sb.append(repetirCaracter('-', ANCHO_TICKET)).append("\n");
        
        // Pie de página
        sb.append(centrarTexto("¡GRACIAS POR SU COMPRA!", ANCHO_TICKET)).append("\n");
        sb.append(centrarTexto("VUELVA PRONTO", ANCHO_TICKET)).append("\n");
        
        // Agregar saltos de línea al final para que el papel avance
        sb.append("\n\n\n\n");
        
        return sb.toString();
    }
    
    /**
     * Envía el contenido a la impresora predeterminada.
     * @param contenido Contenido a imprimir
     * @return true si se envió correctamente, false en caso contrario
     */
    private static boolean enviarAImpresora(String contenido) {
        try {
            // Obtener la impresora predeterminada
            PrintService impresora = PrintServiceLookup.lookupDefaultPrintService();
            if (impresora == null) {
                logger.error("No se encontró ninguna impresora predeterminada");
                return false;
            }
            
            // Convertir el contenido a bytes
            byte[] bytes = contenido.getBytes();
            InputStream is = new ByteArrayInputStream(bytes);
            
            // Crear el documento a imprimir
            DocFlavor flavor = DocFlavor.INPUT_STREAM.AUTOSENSE;
            Doc doc = new SimpleDoc(is, flavor, null);
            
            // Configurar atributos de impresión
            PrintRequestAttributeSet atributos = new HashPrintRequestAttributeSet();
            atributos.add(new Copies(1));
            
            // Crear el trabajo de impresión
            DocPrintJob job = impresora.createPrintJob();
            
            // Imprimir
            job.print(doc, atributos);
            
            // Cerrar el stream
            is.close();
            
            logger.info("Ticket enviado a la impresora: " + impresora.getName());
            return true;
        } catch (Exception e) {
            logger.error("Error al enviar el ticket a la impresora", e);
            return false;
        }
    }
    
    /**
     * Centra un texto en un ancho determinado.
     * @param texto Texto a centrar
     * @param ancho Ancho total
     * @return Texto centrado
     */
    private static String centrarTexto(String texto, int ancho) {
        if (texto.length() >= ancho) {
            return texto;
        }
        
        int espaciosIzquierda = (ancho - texto.length()) / 2;
        int espaciosDerecha = ancho - texto.length() - espaciosIzquierda;
        
        return repetirCaracter(' ', espaciosIzquierda) + texto + repetirCaracter(' ', espaciosDerecha);
    }
    
    /**
     * Repite un carácter n veces.
     * @param caracter Carácter a repetir
     * @param n Número de repeticiones
     * @return Cadena con el carácter repetido
     */
    private static String repetirCaracter(char caracter, int n) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; i++) {
            sb.append(caracter);
        }
        return sb.toString();
    }
}