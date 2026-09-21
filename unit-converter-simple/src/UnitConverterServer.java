import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public class UnitConverterServer {

    private static final int PORT = 8080;
    private static final Path WEB_DIR = Paths.get("web").toAbsolutePath().normalize();

    private static final Map<String, LinkedHashMap<String, Unit>> UNITS =
            new LinkedHashMap<>();

    static {
        add("length", "meter", "Meters", 1.0);
        add("length", "kilometer", "Kilometers", 1000.0);
        add("length", "centimeter", "Centimeters", 0.01);
        add("length", "millimeter", "Millimeters", 0.001);
        add("length", "mile", "Miles", 1609.344);
        add("length", "yard", "Yards", 0.9144);
        add("length", "foot", "Feet", 0.3048);
        add("length", "inch", "Inches", 0.0254);

        add("weight", "kilogram", "Kilograms", 1.0);
        add("weight", "gram", "Grams", 0.001);
        add("weight", "milligram", "Milligrams", 0.000001);
        add("weight", "metric-ton", "Metric Tons", 1000.0);
        add("weight", "pound", "Pounds", 0.45359237);
        add("weight", "ounce", "Ounces", 0.028349523125);

        add("temperature", "celsius", "Celsius", 1.0);
        add("temperature", "fahrenheit", "Fahrenheit", 1.0);
        add("temperature", "kelvin", "Kelvin", 1.0);

        add("area", "square-meter", "Square Meters", 1.0);
        add("area", "square-kilometer", "Square Kilometers", 1000000.0);
        add("area", "square-centimeter", "Square Centimeters", 0.0001);
        add("area", "square-foot", "Square Feet", 0.09290304);
        add("area", "square-yard", "Square Yards", 0.83612736);
        add("area", "acre", "Acres", 4046.8564224);
        add("area", "hectare", "Hectares", 10000.0);

        add("volume", "liter", "Liters", 1.0);
        add("volume", "milliliter", "Milliliters", 0.001);
        add("volume", "cubic-meter", "Cubic Meters", 1000.0);
        add("volume", "cubic-centimeter", "Cubic Centimeters", 0.001);
        add("volume", "gallon", "US Gallons", 3.785411784);
        add("volume", "quart", "US Quarts", 0.946352946);
        add("volume", "pint", "US Pints", 0.473176473);
        add("volume", "cup", "US Cups", 0.2365882365);

        add("speed", "meter-per-second", "Meters / Second", 1.0);
        add("speed", "kilometer-per-hour", "Kilometers / Hour", 0.2777777777777778);
        add("speed", "mile-per-hour", "Miles / Hour", 0.44704);
        add("speed", "foot-per-second", "Feet / Second", 0.3048);
        add("speed", "knot", "Knots", 0.5144444444444445);

        add("time", "second", "Seconds", 1.0);
        add("time", "millisecond", "Milliseconds", 0.001);
        add("time", "minute", "Minutes", 60.0);
        add("time", "hour", "Hours", 3600.0);
        add("time", "day", "Days", 86400.0);
        add("time", "week", "Weeks", 604800.0);
        add("time", "year", "Years", 31536000.0);
    }

    private record Unit(String key, String label, double factor) {}

    private static void add(
            String category,
            String key,
            String label,
            double factor
    ) {
        UNITS
                .computeIfAbsent(category, ignored -> new LinkedHashMap<>())
                .put(key, new Unit(key, label, factor));
    }

    public static void main(String[] args) throws Exception {

        HttpServer server =
                HttpServer.create(
                        new InetSocketAddress(PORT),
                        0
                );

        server.createContext(
                "/api/categories",
                UnitConverterServer::categories
        );

        server.createContext(
                "/api/convert",
                UnitConverterServer::convert
        );

        server.createContext(
                "/",
                UnitConverterServer::staticFiles
        );

        server.setExecutor(null);
        server.start();

        System.out.println();
        System.out.println("========================================");
        System.out.println("       UNIT CONVERTER SERVER");
        System.out.println("========================================");
        System.out.println("Server running at:");
        System.out.println("http://localhost:" + PORT);
        System.out.println();
        System.out.println("Press Ctrl+C to stop.");
    }

    private static void categories(HttpExchange exchange)
            throws IOException {

        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendJson(
                    exchange,
                    405,
                    "{\"error\":\"Method not allowed\"}"
            );
            return;
        }

        StringBuilder json =
                new StringBuilder("{");

        int categoryIndex = 0;

        for (Map.Entry<String, LinkedHashMap<String, Unit>> category :
                UNITS.entrySet()) {

            if (categoryIndex++ > 0) {
                json.append(",");
            }

            json.append("\"")
                    .append(escape(category.getKey()))
                    .append("\":[");

            int unitIndex = 0;

            for (Unit unit : category.getValue().values()) {

                if (unitIndex++ > 0) {
                    json.append(",");
                }

                json.append("{")
                        .append("\"key\":\"")
                        .append(escape(unit.key()))
                        .append("\",")
                        .append("\"label\":\"")
                        .append(escape(unit.label()))
                        .append("\"")
                        .append("}");
            }

            json.append("]");
        }

        json.append("}");

        sendJson(
                exchange,
                200,
                json.toString()
        );
    }

    private static void convert(HttpExchange exchange)
            throws IOException {

        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendJson(
                    exchange,
                    405,
                    "{\"error\":\"Method not allowed\"}"
            );
            return;
        }

        try {

            String body =
                    new String(
                            exchange.getRequestBody().readAllBytes(),
                            StandardCharsets.UTF_8
                    );

            String category =
                    readJsonString(body, "category");

            String from =
                    readJsonString(body, "from");

            String to =
                    readJsonString(body, "to");

            double value =
                    readJsonNumber(body, "value");

            ConversionResult result =
                    performConversion(
                            category,
                            from,
                            to,
                            value
                    );

            String json =
                    "{"
                            + "\"category\":\""
                            + escape(category)
                            + "\","
                            + "\"from\":\""
                            + escape(from)
                            + "\","
                            + "\"to\":\""
                            + escape(to)
                            + "\","
                            + "\"input\":"
                            + number(value)
                            + ","
                            + "\"result\":"
                            + number(result.result)
                            + ","
                            + "\"formattedResult\":\""
                            + escape(result.formatted)
                            + "\","
                            + "\"fromLabel\":\""
                            + escape(result.fromLabel)
                            + "\","
                            + "\"toLabel\":\""
                            + escape(result.toLabel)
                            + "\""
                            + "}";

            System.out.println(
                    "Conversion: "
                            + value
                            + " "
                            + result.fromLabel
                            + " -> "
                            + result.formatted
                            + " "
                            + result.toLabel
            );

            sendJson(
                    exchange,
                    200,
                    json
            );

        } catch (IllegalArgumentException ex) {

            sendJson(
                    exchange,
                    400,
                    "{\"error\":\""
                            + escape(ex.getMessage())
                            + "\"}"
            );

        } catch (Exception ex) {

            sendJson(
                    exchange,
                    500,
                    "{\"error\":\"Server error\"}"
            );
        }
    }

    private record ConversionResult(
            double result,
            String formatted,
            String fromLabel,
            String toLabel
    ) {}

    private static ConversionResult performConversion(
            String category,
            String from,
            String to,
            double value
    ) {

        category =
                normalize(category);

        from =
                normalize(from);

        to =
                normalize(to);

        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException(
                    "Value must be a valid number."
            );
        }

        if (!UNITS.containsKey(category)) {
            throw new IllegalArgumentException(
                    "Unsupported category: " + category
            );
        }

        if (!UNITS.get(category).containsKey(from)) {
            throw new IllegalArgumentException(
                    "Unsupported source unit: " + from
            );
        }

        if (!UNITS.get(category).containsKey(to)) {
            throw new IllegalArgumentException(
                    "Unsupported target unit: " + to
            );
        }

        double result;

        if ("temperature".equals(category)) {

            result =
                    convertTemperature(
                            value,
                            from,
                            to
                    );

        } else {

            double baseValue =
                    value
                            * UNITS
                            .get(category)
                            .get(from)
                            .factor();

            result =
                    baseValue
                            / UNITS
                            .get(category)
                            .get(to)
                            .factor();
        }

        if (!Double.isFinite(result)) {
            throw new IllegalArgumentException(
                    "Conversion result is invalid."
            );
        }

        result =
                clean(result);

        String formatted =
                format(result);

        return new ConversionResult(
                result,
                formatted,
                UNITS.get(category).get(from).label(),
                UNITS.get(category).get(to).label()
        );
    }

    private static double convertTemperature(
            double value,
            String from,
            String to
    ) {

        if (from.equals(to)) {
            return value;
        }

        double celsius;

        switch (from) {

            case "celsius":
                celsius = value;
                break;

            case "fahrenheit":
                celsius =
                        (value - 32.0)
                                * 5.0
                                / 9.0;
                break;

            case "kelvin":
                celsius =
                        value - 273.15;
                break;

            default:
                throw new IllegalArgumentException(
                        "Unsupported temperature unit."
                );
        }

        return switch (to) {

            case "celsius" ->
                    celsius;

            case "fahrenheit" ->
                    celsius * 9.0 / 5.0 + 32.0;

            case "kelvin" ->
                    celsius + 273.15;

            default ->
                    throw new IllegalArgumentException(
                            "Unsupported temperature unit."
                    );
        };
    }

    private static void staticFiles(
            HttpExchange exchange
    ) throws IOException {

        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {

            sendText(
                    exchange,
                    405,
                    "Method not allowed",
                    "text/plain; charset=UTF-8"
            );

            return;
        }

        URI uri =
                exchange.getRequestURI();

        String requestPath =
                uri.getPath();

        if (requestPath.equals("/")) {
            requestPath = "/index.html";
        }

        if (requestPath.contains("..")) {

            sendText(
                    exchange,
                    400,
                    "Invalid path",
                    "text/plain; charset=UTF-8"
            );

            return;
        }

        Path file =
                WEB_DIR.resolve(
                        requestPath.substring(1)
                ).normalize();

        if (!file.startsWith(WEB_DIR)
                || !Files.exists(file)
                || Files.isDirectory(file)) {

            sendText(
                    exchange,
                    404,
                    "File not found",
                    "text/plain; charset=UTF-8"
            );

            return;
        }

        byte[] data =
                Files.readAllBytes(file);

        String contentType =
                contentType(file);

        exchange.getResponseHeaders()
                .set(
                        "Content-Type",
                        contentType
                );

        exchange.sendResponseHeaders(
                200,
                data.length
        );

        try (OutputStream output =
                     exchange.getResponseBody()) {

            output.write(data);
        }
    }

    private static String contentType(
            Path file
    ) {

        String name =
                file.getFileName()
                        .toString()
                        .toLowerCase(Locale.ROOT);

        if (name.endsWith(".html")) {
            return "text/html; charset=UTF-8";
        }

        if (name.endsWith(".css")) {
            return "text/css; charset=UTF-8";
        }

        if (name.endsWith(".js")) {
            return "application/javascript; charset=UTF-8";
        }

        return "application/octet-stream";
    }

    private static String readJsonString(
            String json,
            String key
    ) {

        String pattern =
                "\"" + key + "\"";

        int keyIndex =
                json.indexOf(pattern);

        if (keyIndex < 0) {
            throw new IllegalArgumentException(
                    "Missing field: " + key
            );
        }

        int colon =
                json.indexOf(
                        ":",
                        keyIndex + pattern.length()
                );

        int firstQuote =
                json.indexOf(
                        "\"",
                        colon + 1
                );

        int secondQuote =
                json.indexOf(
                        "\"",
                        firstQuote + 1
                );

        if (colon < 0
                || firstQuote < 0
                || secondQuote < 0) {

            throw new IllegalArgumentException(
                    "Invalid field: " + key
            );
        }

        return json.substring(
                firstQuote + 1,
                secondQuote
        );
    }

    private static double readJsonNumber(
            String json,
            String key
    ) {

        String pattern =
                "\"" + key + "\"";

        int keyIndex =
                json.indexOf(pattern);

        if (keyIndex < 0) {
            throw new IllegalArgumentException(
                    "Missing field: " + key
            );
        }

        int colon =
                json.indexOf(
                        ":",
                        keyIndex + pattern.length()
                );

        if (colon < 0) {
            throw new IllegalArgumentException(
                    "Invalid field: " + key
            );
        }

        StringBuilder number =
                new StringBuilder();

        for (int i = colon + 1;
             i < json.length();
             i++) {

            char c =
                    json.charAt(i);

            if (
                    Character.isDigit(c)
                            || c == '-'
                            || c == '+'
                            || c == '.'
                            || c == 'e'
                            || c == 'E'
            ) {

                number.append(c);

            } else if (number.length() > 0) {

                break;
            }
        }

        if (number.length() == 0) {
            throw new IllegalArgumentException(
                    "Invalid number: " + key
            );
        }

        try {

            return Double.parseDouble(
                    number.toString()
            );

        } catch (NumberFormatException ex) {

            throw new IllegalArgumentException(
                    "Invalid number: " + key
            );
        }
    }

    private static String normalize(
            String value
    ) {

        if (value == null) {
            return "";
        }

        return value
                .trim()
                .toLowerCase(Locale.ROOT);
    }

    private static double clean(
            double value
    ) {

        if (Math.abs(value) < 1e-12) {
            return 0.0;
        }

        return Math.round(
                value * 1_000_000_000_000d
        ) / 1_000_000_000_000d;
    }

    private static String format(
            double value
    ) {

        if (value == 0) {
            return "0";
        }

        if (
                Math.abs(value) >= 1e12
                        || Math.abs(value) < 1e-9
        ) {

            return String.format(
                    Locale.US,
                    "%.10g",
                    value
            );
        }

        return String.format(
                Locale.US,
                "%.12f",
                value
        )
                .replaceAll(
                        "0+$",
                        ""
                )
                .replaceAll(
                        "\\.$",
                        ""
                );
    }

    private static String number(
            double value
    ) {

        if (
                Double.isFinite(value)
                && value == Math.rint(value)
                && Math.abs(value) < 9_000_000_000_000_000L
        ) {

            return Long.toString(
                    (long) value
            );
        }

        return Double.toString(value);
    }

    private static String escape(
            String value
    ) {

        if (value == null) {
            return "";
        }

        return value
                .replace(
                        "\\",
                        "\\\\"
                )
                .replace(
                        "\"",
                        "\\\""
                )
                .replace(
                        "\n",
                        "\\n"
                )
                .replace(
                        "\r",
                        "\\r"
                );
    }

    private static void sendJson(
            HttpExchange exchange,
            int status,
            String json
    ) throws IOException {

        sendText(
                exchange,
                status,
                json,
                "application/json; charset=UTF-8"
        );
    }

    private static void sendText(
            HttpExchange exchange,
            int status,
            String text,
            String contentType
    ) throws IOException {

        byte[] data =
                text.getBytes(
                        StandardCharsets.UTF_8
                );

        exchange.getResponseHeaders()
                .set(
                        "Content-Type",
                        contentType
                );

        exchange.sendResponseHeaders(
                status,
                data.length
        );

        try (OutputStream output =
                     exchange.getResponseBody()) {

            output.write(data);
        }
    }
}
