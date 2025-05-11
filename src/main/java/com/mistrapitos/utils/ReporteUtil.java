package com.mistrapitos.utils;

import com.mistrapitos.controllers.ReporteController;
import com.mistrapitos.models.*;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import com.itextpdf.text.Element;
import com.itextpdf.text.Rectangle;
import java.time.LocalDateTime;
import com.itextpdf.text.Document;
import com.itextpdf.text.Font;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import java.sql.Timestamp;
import javax.sql.DataSource;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utilidad para la generación de reportes en Excel y PDF.
 */
public class ReporteUtil {

    private static final Logger logger = LoggerFactory.getLogger(ReporteUtil.class);
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy - hh:mm a");

    public static boolean generarReporteVentasExcel(List<Venta> ventas, String ruta) {
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Ventas");
            int fila = 0;

            // 1) Si todas las ventas son del mismo cliente, ponemos una fila "Cliente: X"
            if (!ventas.isEmpty()) {
                String cli = ventas.get(0).getClienteNombre();
                boolean mismo = ventas.stream()
                        .allMatch(v -> v.getClienteNombre().equals(cli));
                if (mismo) {
                    Row info = sheet.createRow(fila++);
                    info.createCell(0).setCellValue("Cliente: " + cli);
                }
            }

            // 2) Cabecera
            Row header = sheet.createRow(fila++);
            String[] cols = {"ID Venta","Cliente","Fecha","Método Pago","Total"};
            for (int i = 0; i < cols.length; i++) {
                header.createCell(i).setCellValue(cols[i]);
            }

            // 3) Filas de datos
            for (Venta v : ventas) {
                Row r = sheet.createRow(fila++);
                r.createCell(0).setCellValue(v.getIdVenta());
                r.createCell(1).setCellValue(v.getClienteNombre());
                r.createCell(2).setCellValue(v.getFecha().format(DATE_TIME));
                r.createCell(3).setCellValue(v.getMetodoPago());
                r.createCell(4).setCellValue(v.getTotal());
            }

            // 4) Auto‑ancho (n columnas = cols.length)
            for (int i = 0; i < cols.length; i++) {
                sheet.autoSizeColumn(i);
            }

            // 5) Guardar
            try (OutputStream out = new FileOutputStream(ruta)) {
                wb.write(out);
            }
            logger.info("Excel Ventas generado en {}", ruta);
            return true;
        } catch (Exception e) {
            logger.error("Error generando Excel de Ventas", e);
            return false;
        }
    }


    public static boolean generarReporteVentasPDF(List<Venta> ventas, String ruta) {
        Document doc = new Document();
        try {
            PdfWriter.getInstance(doc, new FileOutputStream(ruta));
            doc.open();

            Font titleFont  = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
            Font cellFont   = FontFactory.getFont(FontFactory.HELVETICA, 10);

            doc.add(new Paragraph("Reporte de Ventas", titleFont));
            doc.add(new Paragraph(" "));

            // 1) Encabezado "Cliente: X" opcional
            if (!ventas.isEmpty()) {
                String cli = ventas.get(0).getClienteNombre();
                boolean mismo = ventas.stream()
                        .allMatch(v -> v.getClienteNombre().equals(cli));
                if (mismo) {
                    doc.add(new Paragraph("Cliente: " + cli, cellFont));
                    doc.add(new Paragraph(" "));
                }
            }

            // 2) Tabla de 5 columnas
            PdfPTable table = new PdfPTable(5);
            table.setWidthPercentage(100);
            String[] cols = {"ID Venta","Cliente","Fecha","Método Pago","Total"};
            for (String col : cols) {
                PdfPCell h = new PdfPCell(new Phrase(col, headerFont));
                h.setHorizontalAlignment(PdfPCell.ALIGN_CENTER);
                table.addCell(h);
            }

            // 3) Filas
            for (Venta v : ventas) {
                table.addCell(new PdfPCell(new Phrase(String.valueOf(v.getIdVenta()), cellFont)));
                table.addCell(new PdfPCell(new Phrase(v.getClienteNombre(),    cellFont)));
                table.addCell(new PdfPCell(new Phrase(v.getFecha().format(DATE_TIME), cellFont)));
                table.addCell(new PdfPCell(new Phrase(v.getMetodoPago(),       cellFont)));
                table.addCell(new PdfPCell(new Phrase(String.format("$%.2f", v.getTotal()), cellFont)));
            }

            doc.add(table);
            doc.close();
            logger.info("PDF Ventas generado en {}", ruta);
            return true;
        } catch (Exception e) {
            logger.error("Error generando PDF de Ventas", e);
            if (doc.isOpen()) doc.close();
            return false;
        }
    }


    public static boolean generarReporteInventarioExcel(List<Producto> productos, String ruta) {
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Inventario");
            Row header = sheet.createRow(0);
            String[] cols = {"ID Producto","Nombre","Categoría","Precio","Stock","Tallas","Colores","Descuento"};
            for (int i = 0; i < cols.length; i++) header.createCell(i).setCellValue(cols[i]);
            int row = 1;
            for (Producto p : productos) {
                Row r = sheet.createRow(row++);
                r.createCell(0).setCellValue(p.getIdProducto());
                r.createCell(1).setCellValue(p.getNombre());
                r.createCell(2).setCellValue(p.getCategoriaNombre());
                r.createCell(3).setCellValue(p.getPrecio());
                r.createCell(4).setCellValue(p.getStock());
                r.createCell(5).setCellValue(p.getSizes());
                r.createCell(6).setCellValue(p.getColors());
                r.createCell(7).setCellValue(p.getDescuento());
            }
            for (int i = 0; i < cols.length; i++) sheet.autoSizeColumn(i);
            try (OutputStream out = new FileOutputStream(ruta)) { wb.write(out); }
            logger.info("Excel Inventario generado en {}", ruta);
            return true;
        } catch (Exception e) {
            logger.error("Error generando Excel de Inventario", e);
            return false;
        }
    }

    public static boolean generarReporteInventarioPDF(List<Producto> productos, String ruta) {
        Document doc = new Document();
        try {
            PdfWriter.getInstance(doc, new FileOutputStream(ruta));
            doc.open();
            Font title = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
            Font cellFont = FontFactory.getFont(FontFactory.HELVETICA, 10);
            doc.add(new Paragraph("Reporte de Inventario", title));
            doc.add(new Paragraph(" "));
            PdfPTable table = new PdfPTable(8);
            table.setWidthPercentage(100);
            String[] cols = {"ID","Nombre","Categoría","Precio","Stock","Tallas","Colores","Descuento"};
            for (String c : cols) {
                PdfPCell cell = new PdfPCell(new Phrase(c, headerFont));
                cell.setHorizontalAlignment(PdfPCell.ALIGN_CENTER);
                table.addCell(cell);
            }
            for (Producto p : productos) {
                table.addCell(new PdfPCell(new Phrase(String.valueOf(p.getIdProducto()), cellFont)));
                table.addCell(new PdfPCell(new Phrase(p.getNombre(), cellFont)));
                table.addCell(new PdfPCell(new Phrase(p.getCategoriaNombre(), cellFont)));
                table.addCell(new PdfPCell(new Phrase(String.format("$%.2f", p.getPrecio()), cellFont)));
                table.addCell(new PdfPCell(new Phrase(String.valueOf(p.getStock()), cellFont)));
                table.addCell(new PdfPCell(new Phrase(p.getSizes(), cellFont)));
                table.addCell(new PdfPCell(new Phrase(p.getColors(), cellFont)));
                table.addCell(new PdfPCell(new Phrase(String.format("%.2f%%", p.getDescuento()), cellFont)));
            }
            doc.add(table);
            doc.close();
            logger.info("PDF Inventario generado en {}", ruta);
            return true;
        } catch (Exception e) {
            logger.error("Error generando PDF de Inventario", e);
            if (doc.isOpen()) doc.close();
            return false;
        }
    }

    /**
     * Genera un ticket de venta en PDF con formato de ticket profesional.
     * @param carrito Lista de productos vendidos
     * @param cliente Nombre o ID del cliente
     * @param usuario Usuario que realiza la venta
     * @param ruta Ruta donde se guardará el PDF
     * @return true si se generó correctamente, false en caso contrario
     */
    public static boolean generarTicketVentaPDF(List<ProductoEnCarrito> carrito, String cliente, Usuario usuario, String ruta) {
        Document document = new Document(new Rectangle(230, 600), 10, 10, 10, 10);
        try {
            PdfWriter.getInstance(document, new FileOutputStream(ruta));
            document.open();

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14);
            Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 9);
            Font boldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9);

            Paragraph title = new Paragraph("Mis Trapitos", titleFont);
                    title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);

            document.add(new Paragraph("Fecha: " + LocalDateTime.now().format(DATE_TIME), normalFont));
            document.add(new Paragraph("Atiende: " + usuario.getNombreUsuario(), normalFont));
            document.add(new Paragraph("Cliente: " + cliente, normalFont));
            document.add(new Paragraph(" "));

            PdfPTable table = new PdfPTable(4);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{2, 1, 1, 1});

            table.addCell(new PdfPCell(new Phrase("Producto", boldFont)));
            table.addCell(new PdfPCell(new Phrase("Cant.", boldFont)));
            table.addCell(new PdfPCell(new Phrase("P.Unit", boldFont)));
            table.addCell(new PdfPCell(new Phrase("Subtotal", boldFont)));

            double total = 0;
            for (ProductoEnCarrito p : carrito) {
                table.addCell(new PdfPCell(new Phrase(p.getNombre(), normalFont)));
                table.addCell(new PdfPCell(new Phrase(String.valueOf(p.getCantidad()), normalFont)));
                table.addCell(new PdfPCell(new Phrase(String.format("$%.2f", p.getPrecioUnitario()), normalFont)));
                table.addCell(new PdfPCell(new Phrase(String.format("$%.2f", p.getSubtotal()), normalFont)));
                total += p.getSubtotal();
            }

            document.add(table);
            document.add(new Paragraph(" "));
            document.add(new Paragraph("TOTAL: $" + String.format("%.2f", total), titleFont));
            document.add(new Paragraph(" "));
            document.add(new Paragraph("¡Gracias por su compra!", boldFont));

            document.close();
            logger.info("Ticket PDF generado en {}", ruta);
            return true;
        } catch (Exception e) {
            logger.error("Error generando Ticket PDF", e);
            if (document.isOpen()) document.close();
            return false;
        }
    }
    /**
     * Genera un reporte de ventas por ciudad en formato Excel,
     * incluyendo la lista de productos vendidos.
     */
    public static boolean generarReporteVentasPorCiudadExcel(
            List<ReporteController.VentaPorCiudad> ventas, String ruta) {
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("VentasPorCiudad");
            // Cabecera con 4 columnas
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("Ciudad");
            header.createCell(1).setCellValue("Cantidad de Ventas");
            header.createCell(2).setCellValue("Total Vendido");
            header.createCell(3).setCellValue("Productos");  // Nueva columna

            int row = 1;
            for (ReporteController.VentaPorCiudad v : ventas) {
                Row r = sheet.createRow(row++);
                r.createCell(0).setCellValue(v.getCiudad());
                r.createCell(1).setCellValue(v.getCantidadVentas());
                r.createCell(2).setCellValue(v.getTotalVendido());
                r.createCell(3).setCellValue(v.getProductos());  // Listado de productos
            }
            // Ajustar ancho de 4 columnas
            for (int i = 0; i < 4; i++) sheet.autoSizeColumn(i);
            try (OutputStream out = new FileOutputStream(ruta)) { wb.write(out); }
            logger.info("Excel Ventas por Ciudad generado en {}", ruta);
            return true;
        } catch (Exception e) {
            logger.error("Error generando Excel de Ventas por Ciudad", e);
            return false;
        }
    }

    /**
     * Genera un reporte de ventas por ciudad en formato PDF,
     * incluyendo la lista de productos vendidos.
     */
    public static boolean generarReporteVentasPorCiudadPDF(
            List<ReporteController.VentaPorCiudad> ventas, String ruta) {
        Document doc = new Document();
        try {
            PdfWriter.getInstance(doc, new FileOutputStream(ruta));
            doc.open();
            Font titleFont  = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
            Font cellFont   = FontFactory.getFont(FontFactory.HELVETICA, 10);

            doc.add(new Paragraph("Reporte de Ventas por Ciudad", titleFont));
            doc.add(new Paragraph(" "));

            // Tabla de 4 columnas
            PdfPTable table = new PdfPTable(4);
            table.setWidthPercentage(100);
            String[] cols = {"Ciudad", "Cantidad de Ventas", "Total Vendido", "Productos"};
            for (String col : cols) {
                PdfPCell cell = new PdfPCell(new Phrase(col, headerFont));
                cell.setHorizontalAlignment(PdfPCell.ALIGN_CENTER);
                table.addCell(cell);
            }

            for (ReporteController.VentaPorCiudad v : ventas) {
                table.addCell(new PdfPCell(new Phrase(v.getCiudad(), cellFont)));
                table.addCell(new PdfPCell(new Phrase(String.valueOf(v.getCantidadVentas()), cellFont)));
                table.addCell(new PdfPCell(new Phrase(String.format("$%.2f", v.getTotalVendido()), cellFont)));
                table.addCell(new PdfPCell(new Phrase(v.getProductos(), cellFont)));  // Listado de productos
            }

            doc.add(table);
            doc.close();
            logger.info("PDF Ventas por Ciudad generado en {}", ruta);
            return true;
        } catch (Exception e) {
            logger.error("Error generando PDF de Ventas por Ciudad", e);
            if (doc.isOpen()) doc.close();
            return false;
        }
    }


    public List<CategoriaVentaResumen> obtenerVentasPorCategoria(LocalDate inicio, LocalDate fin) {
        List<CategoriaVentaResumen> lista = new ArrayList<>();
        String sql =
                "SELECT c.nombre AS categoria, " +
                        "       SUM(dv.cantidad * dv.precio_unitario) AS total_vendido " +
                        "FROM detalle_venta dv " +
                        "JOIN productos p ON dv.id_producto = p.id_producto " +
                        "JOIN categorias c ON p.id_categoria = c.id_categoria " +
                        "JOIN ventas v ON dv.id_venta = v.id_venta " +
                        "WHERE v.fecha BETWEEN ? AND ? " +
                        "GROUP BY c.nombre " +
                        "ORDER BY c.nombre";
        try (
                Connection conn = DatabaseUtil.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            // aquí atrapamos el SQLException del setTimestamp
            ps.setTimestamp(1, Timestamp.valueOf(inicio.atStartOfDay()));
            ps.setTimestamp(2, Timestamp.valueOf(fin.atTime(23, 59, 59)));

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(new CategoriaVentaResumen(
                            rs.getString("categoria"),
                            rs.getDouble("total_vendido")
                    ));
                }
            }
        } catch (SQLException e) {
            logger.error("Error obteniendo ventas por categoría", e);
        }
        return lista;
    }

    public List<ProductoVentaResumen> obtenerProductosMasVendidos(LocalDate desde, LocalDate hasta) {
        List<ProductoVentaResumen> lista = new ArrayList<>();
        String sql = "SELECT p.nombre, c.nombre AS categoria, SUM(dv.cantidad) AS cantidad_vendida, " +
                "SUM(dv.cantidad * dv.precio_unitario) AS total_vendido " +
                "FROM detalle_venta dv " +
                "JOIN productos p ON dv.id_producto = p.id_producto " +
                "JOIN categorias c ON p.id_categoria = c.id_categoria " +
                "JOIN ventas v ON dv.id_venta = v.id_venta " +
                "WHERE DATE(v.fecha) BETWEEN ? AND ? " +
                "GROUP BY p.nombre, c.nombre " +
                "ORDER BY cantidad_vendida DESC";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setDate(1, java.sql.Date.valueOf(desde));
            stmt.setDate(2, java.sql.Date.valueOf(hasta));
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    lista.add(new ProductoVentaResumen(
                            rs.getString("nombre"),
                            rs.getString("categoria"),
                            rs.getInt("cantidad_vendida"),
                            rs.getDouble("total_vendido")
                    ));
                }
            }
        } catch (Exception e) {
            logger.error("Error obteniendo productos más vendidos", e);
        }
        return lista;
    }

    public static boolean generarReporteProductosMasVendidosExcel(List<ProductoVentaResumen> items,
                                                                  String ruta) {
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Más Vendidos");
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("Nombre");
            header.createCell(1).setCellValue("Categoría");
            header.createCell(2).setCellValue("Cantidad Vendida");
            header.createCell(3).setCellValue("Total Vendido");
            int row = 1;
            for (ProductoVentaResumen p : items) {
                Row r = sheet.createRow(row++);
                r.createCell(0).setCellValue(p.getNombre());
                r.createCell(1).setCellValue(p.getCategoria());
                r.createCell(2).setCellValue(p.getCantidadVendida());
                r.createCell(3).setCellValue(p.getTotalVendido());
            }
            for (int i = 0; i < 4; i++) sheet.autoSizeColumn(i);
            try (OutputStream out = new FileOutputStream(ruta)) { wb.write(out); }
            logger.info("Excel Más Vendidos generado en {}", ruta);
            return true;
        } catch (Exception e) {
            logger.error("Error generando Excel de Más Vendidos", e);
            return false;
        }
    }

    public static boolean generarReporteProductosMasVendidosPDF(List<ProductoVentaResumen> items,
                                                                String ruta) {
        Document doc = new Document();
        try {
            PdfWriter.getInstance(doc, new FileOutputStream(ruta));
            doc.open();
            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
            Font cellFont = FontFactory.getFont(FontFactory.HELVETICA, 10);
            PdfPTable table = new PdfPTable(4);
            table.setWidthPercentage(100);
            String[] cols = {"Nombre","Categoría","Cantidad Vendida","Total Vendido"};
            for (String c : cols) {
                PdfPCell cell = new PdfPCell(new Phrase(c, headerFont));
                cell.setHorizontalAlignment(PdfPCell.ALIGN_CENTER);
                table.addCell(cell);
            }
            for (ProductoVentaResumen p : items) {
                table.addCell(new PdfPCell(new Phrase(p.getNombre(), cellFont)));
                table.addCell(new PdfPCell(new Phrase(p.getCategoria(), cellFont)));
                table.addCell(new PdfPCell(new Phrase(String.valueOf(p.getCantidadVendida()), cellFont)));
                table.addCell(new PdfPCell(new Phrase(String.format("$%.2f", p.getTotalVendido()), cellFont)));
            }
            doc.add(table);
            doc.close();
            logger.info("PDF Más Vendidos generado en {}", ruta);
            return true;
        } catch (Exception e) {
            logger.error("Error generando PDF de Más Vendidos", e);
            if (doc.isOpen()) doc.close();
            return false;
        }
    }

    /**
     * Genera un reporte de ventas por categoría en formato Excel.
     */
    public static boolean generarReporteVentasPorCategoriaExcel(
            List<CategoriaVentaResumen> items, String ruta) {
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("VentasPorCategoria");
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("Categoría");
            header.createCell(1).setCellValue("Total Vendido");

            int row = 1;
            for (CategoriaVentaResumen c : items) {
                Row r = sheet.createRow(row++);
                r.createCell(0).setCellValue(c.getCategoria());
                r.createCell(1).setCellValue(c.getTotal());
            }
            sheet.autoSizeColumn(0);
            sheet.autoSizeColumn(1);

            try (OutputStream out = new FileOutputStream(ruta)) {
                wb.write(out);
            }
            logger.info("Excel Ventas por Categoría generado en {}", ruta);
            return true;
        } catch (Exception e) {
            logger.error("Error generando Excel de Ventas por Categoría", e);
            return false;
        }
    }

    /**
     * Genera un reporte de ventas por categoría en formato PDF.
     */
    public static boolean generarReporteVentasPorCategoriaPDF(
            List<CategoriaVentaResumen> items, String ruta) {
        Document doc = new Document();
        try {
            PdfWriter.getInstance(doc, new FileOutputStream(ruta));
            doc.open();
            Font titleFont  = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
            Font cellFont   = FontFactory.getFont(FontFactory.HELVETICA, 10);

            doc.add(new Paragraph("Reporte de Ventas por Categoría", titleFont));
            doc.add(new Paragraph(" "));

            PdfPTable table = new PdfPTable(2);
            table.setWidthPercentage(100);
            // Encabezados
            String[] cols = {"Categoría", "Total Vendido"};
            for (String col : cols) {
                PdfPCell cell = new PdfPCell(new Phrase(col, headerFont));
                cell.setHorizontalAlignment(PdfPCell.ALIGN_CENTER);
                table.addCell(cell);
            }
            // Filas
            for (CategoriaVentaResumen c : items) {
                table.addCell(new PdfPCell(new Phrase(c.getCategoria(), cellFont)));
                table.addCell(new PdfPCell(new Phrase(
                        String.format("$%.2f", c.getTotal()), cellFont)));
            }

            doc.add(table);
            doc.close();
            logger.info("PDF Ventas por Categoría generado en {}", ruta);
            return true;
        } catch (Exception e) {
            logger.error("Error generando PDF de Ventas por Categoría", e);
            if (doc.isOpen()) doc.close();
            return false;
        }
    }
    public List<MetodoPagoResumen> obtenerMetodosPagoMasUtilizados(LocalDate inicio, LocalDate fin) {
        List<MetodoPagoResumen> lista = new ArrayList<>();
        String sql =
                "SELECT v.metodo_pago AS metodo, " +
                        "       COUNT(*)       AS veces " +
                        "FROM ventas v " +
                        "WHERE v.fecha BETWEEN ? AND ? " +
                        "GROUP BY v.metodo_pago " +
                        "ORDER BY veces DESC";
        try (
                Connection conn = DatabaseUtil.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            ps.setTimestamp(1, Timestamp.valueOf(inicio.atStartOfDay()));
            ps.setTimestamp(2, Timestamp.valueOf(fin.atTime(23,59,59)));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(new MetodoPagoResumen(
                            rs.getString("metodo"),
                            rs.getInt("veces")
                    ));
                }
            }
        } catch (SQLException e) {
            logger.error("Error obteniendo métodos de pago", e);
        }
        return lista;
    }
    public static boolean generarReporteMetodosPagoExcel(List<MetodoPagoResumen> items, String ruta) {
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("MétodosPago");
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("Método");
            header.createCell(1).setCellValue("Veces");
            int row = 1;
            for (MetodoPagoResumen m : items) {
                Row r = sheet.createRow(row++);
                r.createCell(0).setCellValue(m.getMetodoPago());
                r.createCell(1).setCellValue(m.getVeces());
            }
            sheet.autoSizeColumn(0);
            sheet.autoSizeColumn(1);
            try (OutputStream out = new FileOutputStream(ruta)) { wb.write(out); }
            logger.info("Excel MétodosPago generado en {}", ruta);
            return true;
        } catch (Exception e) {
            logger.error("Error generando Excel de MétodosPago", e);
            return false;
        }
    }

    public static boolean generarReporteMetodosPagoPDF(List<MetodoPagoResumen> items, String ruta) {
        Document doc = new Document();
        try {
            PdfWriter.getInstance(doc, new FileOutputStream(ruta));
            doc.open();
            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
            Font cellFont = FontFactory.getFont(FontFactory.HELVETICA, 10);
            PdfPTable table = new PdfPTable(2); table.setWidthPercentage(100);
            table.addCell(new PdfPCell(new Phrase("Método", headerFont)));
            table.addCell(new PdfPCell(new Phrase("Veces", headerFont)));
            for (MetodoPagoResumen m : items) {
                table.addCell(new PdfPCell(new Phrase(m.getMetodoPago(), cellFont)));
                table.addCell(new PdfPCell(new Phrase(String.valueOf(m.getVeces()), cellFont)));
            }
            doc.add(table);
            doc.close();
            logger.info("PDF MétodosPago generado en {}", ruta);
            return true;
        } catch (Exception e) {
            logger.error("Error generando PDF de MétodosPago", e);
            if (doc.isOpen()) doc.close();
            return false;
        }
    }

    public static boolean generarReporteProductoMayorStockExcel(List<Producto> items, String ruta) {
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("MayorStock");
            Row h = sheet.createRow(0);
            h.createCell(0).setCellValue("Nombre");
            h.createCell(1).setCellValue("Stock");
            int r = 1;
            for (Producto p : items) {
                Row row = sheet.createRow(r++);
                row.createCell(0).setCellValue(p.getNombre());
                row.createCell(1).setCellValue(p.getStock());
            }
            sheet.autoSizeColumn(0);
            sheet.autoSizeColumn(1);
            try (OutputStream out = new FileOutputStream(ruta)) { wb.write(out); }
            logger.info("Excel MayorStock generado en {}", ruta);
            return true;
        } catch (Exception e) {
            logger.error("Error generando Excel de MayorStock", e);
            return false;
        }
    }

    public static boolean generarReporteProductoMayorStockPDF(List<Producto> items, String ruta) {
        Document doc = new Document();
        try {
            PdfWriter.getInstance(doc, new FileOutputStream(ruta));
            doc.open();
            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
            Font cellFont = FontFactory.getFont(FontFactory.HELVETICA, 10);
            PdfPTable table = new PdfPTable(2); table.setWidthPercentage(100);
            table.addCell(new PdfPCell(new Phrase("Nombre", headerFont)));
            table.addCell(new PdfPCell(new Phrase("Stock", headerFont)));
            for (Producto p : items) {
                table.addCell(new PdfPCell(new Phrase(p.getNombre(), cellFont)));
                table.addCell(new PdfPCell(new Phrase(String.valueOf(p.getStock()), cellFont)));
            }
            doc.add(table);
            doc.close();
            logger.info("PDF MayorStock generado en {}", ruta);
            return true;
        } catch (Exception e) {
            logger.error("Error generando PDF de MayorStock", e);
            if (doc.isOpen()) doc.close();
            return false;
        }
    }

    /**
     * Genera un reporte de productos de proveedor en formato Excel,
     * incluyendo el nombre del proveedor.
     */
    public static boolean generarReporteProductosPorProveedorExcel(
            String proveedor,
            List<Producto> items,
            String ruta) {
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("ProductosProveedor");
            // Fila de proveedor
            Row infoRow = sheet.createRow(0);
            infoRow.createCell(0).setCellValue("Proveedor: " + proveedor);
            // Encabezado
            Row header = sheet.createRow(1);
            header.createCell(0).setCellValue("Producto");
            header.createCell(1).setCellValue("Stock");
            // Datos
            int row = 2;
            for (Producto p : items) {
                Row r = sheet.createRow(row++);
                r.createCell(0).setCellValue(p.getNombre());
                r.createCell(1).setCellValue(p.getStock());
            }
            sheet.autoSizeColumn(0);
            sheet.autoSizeColumn(1);
            try (OutputStream out = new FileOutputStream(ruta)) {
                wb.write(out);
            }
            logger.info("Excel ProductosPorProveedor generado en {}", ruta);
            return true;
        } catch (Exception e) {
            logger.error("Error generando Excel de ProductosPorProveedor", e);
            return false;
        }
    }

    /**
     * Genera un reporte de productos de proveedor en formato PDF,
     * incluyendo el nombre del proveedor.
     */
    public static boolean generarReporteProductosPorProveedorPDF(
            String proveedor,
            List<Producto> items,
            String ruta) {
        Document doc = new Document();
        try {
            PdfWriter.getInstance(doc, new FileOutputStream(ruta));
            doc.open();
            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
            Font cellFont = FontFactory.getFont(FontFactory.HELVETICA, 10);
            // Título con proveedor
            Paragraph title = new Paragraph("Productos del Proveedor: " + proveedor,
                    FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14));
            title.setAlignment(Element.ALIGN_CENTER);
            doc.add(title);
            doc.add(new Paragraph(" "));
            PdfPTable table = new PdfPTable(2);
            table.setWidthPercentage(100);
            table.addCell(new PdfPCell(new Phrase("Producto", headerFont)));
            table.addCell(new PdfPCell(new Phrase("Stock", headerFont)));
            for (Producto p : items) {
                table.addCell(new PdfPCell(new Phrase(p.getNombre(), cellFont)));
                table.addCell(new PdfPCell(new Phrase(String.valueOf(p.getStock()), cellFont)));
            }
            doc.add(table);
            doc.close();
            logger.info("PDF ProductosPorProveedor generado en {}", ruta);
            return true;
        } catch (Exception e) {
            logger.error("Error generando PDF de ProductosPorProveedor", e);
            if (doc.isOpen()) doc.close();
            return false;
        }
    }

    /**
     * Genera un reporte de productos comprados más de una vez por cliente en formato Excel,
     * incluyendo el nombre del cliente y el total de veces.
     */
    public static boolean generarReporteCompradosMasDeUnaVezExcel(
            String cliente,
            List<ReporteController.ProductoRepetido> items,
            String ruta) {
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("CompradosMasDeUnaVez");
            // Fila de cliente
            Row infoRow = sheet.createRow(0);
            infoRow.createCell(0).setCellValue("Cliente: " + cliente);
            // Encabezado
            Row header = sheet.createRow(1);
            header.createCell(0).setCellValue("Producto");
            header.createCell(1).setCellValue("Veces");
            // Datos
            int r = 2;
            for (ReporteController.ProductoRepetido pr : items) {
                Row row = sheet.createRow(r++);
                row.createCell(0).setCellValue(pr.getNombre());
                row.createCell(1).setCellValue(pr.getVeces());
            }
            sheet.autoSizeColumn(0);
            sheet.autoSizeColumn(1);
            try (OutputStream out = new FileOutputStream(ruta)) {
                wb.write(out);
            }
            logger.info("Excel CompradosMasDeUnaVez generado en {}", ruta);
            return true;
        } catch (Exception e) {
            logger.error("Error generando Excel de CompradosMasDeUnaVez", e);
            return false;
        }
    }

    /**
     * Genera un reporte de productos comprados más de una vez por cliente en formato PDF,
     * incluyendo el nombre del cliente.
     */
    public static boolean generarReporteCompradosMasDeUnaVezPDF(
            String cliente,
            List<ReporteController.ProductoRepetido> items,
            String ruta) {
        Document doc = new Document();
        try {
            PdfWriter.getInstance(doc, new FileOutputStream(ruta));
            doc.open();
            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
            Font cellFont = FontFactory.getFont(FontFactory.HELVETICA, 10);
            // Título con cliente
            Paragraph title = new Paragraph("Comprados Más de Una Vez - Cliente: " + cliente,
                    FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14));
            title.setAlignment(Element.ALIGN_CENTER);
            doc.add(title);
            doc.add(new Paragraph(" "));
            PdfPTable table = new PdfPTable(2);
            table.setWidthPercentage(100);
            table.addCell(new PdfPCell(new Phrase("Producto", headerFont)));
            table.addCell(new PdfPCell(new Phrase("Veces", headerFont)));
            for (ReporteController.ProductoRepetido pr : items) {
                table.addCell(new PdfPCell(new Phrase(pr.getNombre(), cellFont)));
                table.addCell(new PdfPCell(new Phrase(String.valueOf(pr.getVeces()), cellFont)));
            }
            doc.add(table);
            doc.close();
            logger.info("PDF CompradosMasDeUnaVez generado en {}", ruta);
            return true;
        } catch (Exception e) {
            logger.error("Error generando PDF de CompradosMasDeUnaVez", e);
            if (doc.isOpen()) doc.close();
            return false;
        }
    }

    public static boolean generarReporteProductosNoVendidos3MesesExcel(List<Producto> items, String ruta) {
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("NoVendidos3Meses");
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("Producto");
            int row = 1;
            for (Producto p : items) {
                Row r = sheet.createRow(row++);
                r.createCell(0).setCellValue(p.getNombre());
            }
            sheet.autoSizeColumn(0);
            try (OutputStream out = new FileOutputStream(ruta)) { wb.write(out); }
            logger.info("Excel NoVendidos3Meses generado en {}", ruta);
            return true;
        } catch (Exception e) {
            logger.error("Error generando Excel de NoVendidos3Meses", e);
            return false;
        }
    }

    public static boolean generarReporteProductosNoVendidos3MesesPDF(List<Producto> items, String ruta) {
        Document doc = new Document();
        try {
            PdfWriter.getInstance(doc, new FileOutputStream(ruta));
            doc.open();
            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
            Font cellFont = FontFactory.getFont(FontFactory.HELVETICA, 10);
            PdfPTable table = new PdfPTable(1);
            table.setWidthPercentage(100);
            table.addCell(new PdfPCell(new Phrase("Producto", headerFont)));
            for (Producto p : items) {
                table.addCell(new PdfPCell(new Phrase(p.getNombre(), cellFont)));
            }
            doc.add(table);
            doc.close();
            logger.info("PDF NoVendidos3Meses generado en {}", ruta);
            return true;
        } catch (Exception e) {
            logger.error("Error generando PDF de NoVendidos3Meses", e);
            if (doc.isOpen()) doc.close();
            return false;
        }
    }
}
