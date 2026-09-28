package controladores;

import com.stripe.Stripe;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;

import org.json.JSONObject;
import utilidades.EscritorDeLogsUtilidad;

@WebServlet("/stripe")
public class StripeControlador extends HttpServlet {

    // ⚠️ En producción esto NUNCA va hardcodeado
    private final String CLAVE_SECRETA = "sk_test_51SqJajFTwqfdh4EMPSledWlTSmumO1llV7VirK5mEw23GOVQw2Sfhldp02nSPkJW45BhQA2A9AIB3dmvtAD9JB6B00Dp4WSEf6";

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        EscritorDeLogsUtilidad.registrar("StripeControlador - doPost() - Inicio de petición");
        response.setContentType("application/json;charset=UTF-8");

        try {
            Stripe.apiKey = CLAVE_SECRETA;
            EscritorDeLogsUtilidad.registrar("StripeControlador - Clave API de Stripe configurada");

            HttpSession sesion = request.getSession(false);
            if (sesion == null) {
                String error = "Sesión no válida";
                EscritorDeLogsUtilidad.registrar("StripeControlador - ERROR: " + error);
                throw new Exception(error);
            }

            // 🔹 Obtener el total REAL calculado en CarritoVistaControlador
            BigDecimal totalCarrito = (BigDecimal) sesion.getAttribute("totalCarrito");
            EscritorDeLogsUtilidad.registrar("StripeControlador - Total carrito obtenido: " + totalCarrito);

            if (totalCarrito == null || totalCarrito.compareTo(BigDecimal.ZERO) <= 0) {
                String error = "Total del carrito inválido";
                EscritorDeLogsUtilidad.registrar("StripeControlador - ERROR: " + error);
                throw new Exception(error);
            }

            // 🔹 Stripe trabaja en CÉNTIMOS
            long totalCentimos = totalCarrito.multiply(BigDecimal.valueOf(100)).longValueExact();
            EscritorDeLogsUtilidad.registrar("StripeControlador - Total en céntimos para Stripe: " + totalCentimos);

            // 🔹 Crear sesión de checkout con el TOTAL REAL
            SessionCreateParams params = SessionCreateParams.builder()
                    .setMode(SessionCreateParams.Mode.PAYMENT)
                    .setSuccessUrl("http://pablorosadev.com/vistaPastrendleria/carrito?accion=exito")
                    .setCancelUrl("http://pablorosadev.com/vistaPastrendleria/carrito?accion=cancelado")
                    .addLineItem(
                            SessionCreateParams.LineItem.builder()
                                    .setQuantity(1L)
                                    .setPriceData(
                                            SessionCreateParams.LineItem.PriceData.builder()
                                                    .setCurrency("eur")
                                                    .setUnitAmount(totalCentimos) // ✅ TOTAL REAL
                                                    .setProductData(
                                                            SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                                    .setName("Compra Dulce Sabor")
                                                                    .build()
                                                    )
                                                    .build()
                                    )
                                    .build()
                    )
                    .build();

            Session sessionStripe = Session.create(params);
            EscritorDeLogsUtilidad.registrar("StripeControlador - Sesión de Stripe creada con ID: " + sessionStripe.getId());

            // 🔹 Devolver ID de sesión al frontend
            JSONObject json = new JSONObject();
            json.put("sessionId", sessionStripe.getId());

            PrintWriter out = response.getWriter();
            out.print(json.toString());
            out.flush();

            EscritorDeLogsUtilidad.registrar("StripeControlador - Respuesta enviada al frontend con sessionId");

        } catch (Exception e) {
            EscritorDeLogsUtilidad.registrar("StripeControlador - ERROR: " + e.getMessage());
            e.printStackTrace();

            JSONObject json = new JSONObject();
            json.put("error", e.getMessage());

            PrintWriter out = response.getWriter();
            out.print(json.toString());
            out.flush();
        }
    }
}
