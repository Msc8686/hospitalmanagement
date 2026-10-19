import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.sql.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.nio.file.Files;
import java.nio.file.Path;

public class HospitalServer {

    static Hospital hospital = new Hospital("City Care Hospital");

    static Administrator administrator =
            new Administrator(1, "admin", "admin123");

    static int nextPatientId = 1001;
    static int nextDoctorId = 501;
    static int nextAppointmentId = 10001;
    static int nextRecordId = 20001;

    public static void main(String[] args) throws Exception {

        addSampleDoctors();

        HttpServer server =
                HttpServer.create(new InetSocketAddress(8080), 0);

                
        server.createContext("/api/doctors",
                HospitalServer::getDoctors);

        server.createContext("/api/register",
                HospitalServer::registerPatient);

        server.createContext("/api/login/patient",
                HospitalServer::patientLogin);

        server.createContext("/api/login/doctor",
                HospitalServer::doctorLogin);

        server.createContext("/api/login/admin",
                HospitalServer::adminLogin);

        server.createContext("/api/appointment/book",
                HospitalServer::bookAppointment);

        server.createContext("/api/appointment/reschedule",
                HospitalServer::rescheduleAppointment);

        server.createContext("/api/appointment/cancel",
                HospitalServer::cancelAppointment);

        server.createContext("/api/appointments/patient",
                HospitalServer::patientAppointments);

        server.createContext("/api/appointments/doctor",
                HospitalServer::doctorAppointments);

        server.createContext("/api/record",
                HospitalServer::medicalRecord);

        server.createContext("/api/admin/patients",
                HospitalServer::adminPatients);

        server.createContext("/api/admin/doctors",
                HospitalServer::adminDoctors);

        server.createContext("/api/admin/add-doctor",
                HospitalServer::addDoctor);

        server.createContext("/api/admin/remove-doctor",
                HospitalServer::removeDoctor);

        server.createContext("/api/admin/report",
                HospitalServer::report);
       server.createContext("/", exchange -> {
    try {
        // Serve files only for GET requests.
        if (!exchange.getRequestMethod().equalsIgnoreCase("GET")) {
            exchange.getResponseHeaders().set("Allow", "GET");
            exchange.sendResponseHeaders(405, -1);
            return;
        }

        // Main project folder (the folder containing index.html).
        Path root = Path.of(".").toAbsolutePath().normalize();

        // Get the requested URL path, ignoring query parameters.
        String requestPath = exchange.getRequestURI().getPath();

        // Open index.html when the homepage is requested.
        if (requestPath.equals("/")) {
            requestPath = "/index.html";
        }

        // Convert the URL path to a file path.
        Path file = root.resolve(requestPath.substring(1)).normalize();

        // Prevent requests from accessing files outside the project folder.
        if (!file.startsWith(root) || !Files.isRegularFile(file)) {
            byte[] message = "404 Not Found".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set(
                "Content-Type", "text/plain; charset=UTF-8"
            );
            exchange.sendResponseHeaders(404, message.length);
            exchange.getResponseBody().write(message);
            return;
        }

        // Read the requested HTML, CSS, JavaScript, or other asset.
        byte[] content = Files.readAllBytes(file);

        // Set the correct content type for common static files.
        String name = file.getFileName().toString().toLowerCase();
        String contentType;

        if (name.endsWith(".html")) {
            contentType = "text/html; charset=UTF-8";
        } else if (name.endsWith(".css")) {
            contentType = "text/css; charset=UTF-8";
        } else if (name.endsWith(".js")) {
            contentType = "text/javascript; charset=UTF-8";
        } else if (name.endsWith(".json")) {
            contentType = "application/json; charset=UTF-8";
        } else if (name.endsWith(".svg")) {
            contentType = "image/svg+xml";
        } else if (name.endsWith(".png")) {
            contentType = "image/png";
        } else if (name.endsWith(".jpg") || name.endsWith(".jpeg")) {
            contentType = "image/jpeg";
        } else if (name.endsWith(".gif")) {
            contentType = "image/gif";
        } else if (name.endsWith(".ico")) {
            contentType = "image/x-icon";
        } else if (name.endsWith(".woff2")) {
            contentType = "font/woff2";
        } else if (name.endsWith(".woff")) {
            contentType = "font/woff";
        } else if (name.endsWith(".ttf")) {
            contentType = "font/ttf";
        } else {
            contentType = "application/octet-stream";
        }

        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
        exchange.sendResponseHeaders(200, content.length);
        exchange.getResponseBody().write(content);

    } catch (IOException e) {
        e.printStackTrace();

        try {
            exchange.sendResponseHeaders(500, -1);
        } catch (IOException ignored) {
            // The response may already have been sent.
        }
    } finally {
        exchange.close();
    }
});
    
        server.setExecutor(null);
        server.start();

        System.out.println("====================================");
        System.out.println(" Hospital Management Server Started");
        System.out.println(" Server: http://localhost:8080");
        System.out.println("====================================");
    }


    // =====================================================
    // SAMPLE DOCTORS
    // =====================================================

    static void addSampleDoctors() {

        Doctor d1 = new Doctor(
                nextDoctorId++,
                "Dr. Arun Kumar",
                "9876543210",
                "arun@hospital.com",
                "Cardiologist",
                "Cardiology",
                10
        );

        Doctor d2 = new Doctor(
                nextDoctorId++,
                "Dr. Meera Nair",
                "9876501234",
                "meera@hospital.com",
                "Dermatologist",
                "Dermatology",
                7
        );

        Doctor d3 = new Doctor(
                nextDoctorId++,
                "Dr. Rahul Menon",
                "9876512345",
                "rahul@hospital.com",
                "General Physician",
                "General Medicine",
                8
        );

        hospital.addDoctor(d1);
        hospital.addDoctor(d2);
        hospital.addDoctor(d3);
    }


    // =====================================================
    // GET DOCTORS
    // =====================================================

    static void getDoctors(HttpExchange exchange) throws IOException {

        StringBuilder json = new StringBuilder("[");
        boolean first = true;

        for (Doctor d : hospital.getDoctors()) {

            if (!first) {
                json.append(",");
            }

            json.append("{")
                    .append("\"id\":").append(d.getDoctorId()).append(",")
                    .append("\"name\":\"").append(escape(d.getName())).append("\",")
                    .append("\"specialization\":\"")
                    .append(escape(d.getSpecialization())).append("\",")
                    .append("\"department\":\"")
                    .append(escape(d.getDepartment())).append("\",")
                    .append("\"experience\":").append(d.getExperience())
                    .append("}");

            first = false;
        }

        json.append("]");

        sendResponse(exchange, 200, json.toString());
    }


    // =====================================================
    // PATIENT REGISTRATION
    // =====================================================


static void registerPatient(HttpExchange exchange)
        throws IOException {

    if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
        sendResponse(exchange, 405, "Method Not Allowed");
        return;
    }

    String contentType = exchange.getRequestHeaders().getFirst("Content-Type");
    if (contentType == null
            || !contentType.toLowerCase().startsWith("application/json")) {
        sendResponse(exchange, 415,
                "{\"success\":false,\"message\":\"Content-Type must be application/json\"}");
        return;
    }

    Map<String, String> data;
    try {
        data = readJsonData(exchange);
    } catch (IllegalArgumentException e) {
        sendResponse(exchange, 400,
                "{\"success\":false,\"message\":\"Invalid JSON request body\"}");
        return;
    }

    String name = data.get("fullName");
    String phone = data.get("phoneNumber");
    String email = data.get("email");
    String disease = data.get("disease");
    String address = data.get("address");
    String bloodGroup = data.get("bloodGroup");
    String emergencyContact = data.get("emergencyContact");

    try (Connection conn = DatabaseConnection.getConnection()) {

        int patientId;

        // Find the next available patient ID.
        String idSql =
                "SELECT COALESCE(MAX(patient_id), 1000) + 1 AS next_id "
                + "FROM patients";

        try (PreparedStatement idStmt =
                     conn.prepareStatement(idSql);
             ResultSet rs = idStmt.executeQuery()) {

            rs.next();
            patientId = rs.getInt("next_id");
        }

        // Insert the patient into MySQL.
        String sql =
                "INSERT INTO patients "
                + "(patient_id, name, phone, email, disease, address, "
                + "blood_group, emergency_contact) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, patientId);
            stmt.setString(2, name);
            stmt.setString(3, phone);
            stmt.setString(4, email);
            stmt.setString(5, disease);
            stmt.setString(6, address);
            stmt.setString(7, bloodGroup);
            stmt.setString(8, emergencyContact);

            stmt.executeUpdate();
        }

        // Keep the current in-memory application working.
        Patient patient = new Patient(
                patientId,
                name,
                phone,
                email,
                disease,
                address,
                bloodGroup,
                emergencyContact
        );

        hospital.addPatient(patient);
        nextPatientId = Math.max(nextPatientId, patientId + 1);

        String response =
                "{"
                + "\"success\":true,"
                + "\"message\":\"Patient registered successfully\","
                + "\"patientId\":" + patient.getPatientId()
                + "}";

        sendResponse(exchange, 200, response);

    } catch (SQLException e) {
        e.printStackTrace();

        sendResponse(exchange, 500,
                "{\"success\":false,\"message\":\"Database error while registering patient\"}");
    }
}

       


    // =====================================================
    // PATIENT LOGIN
    // =====================================================

    static void patientLogin(HttpExchange exchange)
            throws IOException {

        if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
            sendResponse(exchange, 405, "Method Not Allowed");
            return;
        }

        Map<String, String> data = readFormData(exchange);

        int patientId;

        try {
            patientId = Integer.parseInt(data.get("patientId"));
        } catch (Exception e) {
            sendResponse(exchange, 400,
                    "{\"success\":false,\"message\":\"Invalid patient ID\"}");
            return;
        }

        Patient patient = hospital.findPatient(patientId);

        if (patient == null) {
            sendResponse(exchange, 401,
                    "{\"success\":false,\"message\":\"Patient not found\"}");
            return;
        }

        String response =
                "{"
                        + "\"success\":true,"
                        + "\"message\":\"Login successful\","
                        + "\"patientId\":" + patient.getPatientId() + ","
                        + "\"name\":\"" + escape(patient.getName()) + "\""
                        + "}";

        sendResponse(exchange, 200, response);
    }


    // =====================================================
    // DOCTOR LOGIN
    // =====================================================

    static void doctorLogin(HttpExchange exchange)
            throws IOException {

        if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
            sendResponse(exchange, 405, "Method Not Allowed");
            return;
        }

        Map<String, String> data = readFormData(exchange);

        int doctorId;

        try {
            doctorId = Integer.parseInt(data.get("doctorId"));
        } catch (Exception e) {
            sendResponse(exchange, 400,
                    "{\"success\":false,\"message\":\"Invalid doctor ID\"}");
            return;
        }

        Doctor doctor = hospital.findDoctor(doctorId);

        if (doctor == null) {
            sendResponse(exchange, 401,
                    "{\"success\":false,\"message\":\"Doctor not found\"}");
            return;
        }

        String response =
                "{"
                        + "\"success\":true,"
                        + "\"doctorId\":" + doctor.getDoctorId() + ","
                        + "\"name\":\"" + escape(doctor.getName()) + "\""
                        + "}";

        sendResponse(exchange, 200, response);
    }


    // =====================================================
    // ADMIN LOGIN
    // =====================================================

    static void adminLogin(HttpExchange exchange)
            throws IOException {

        if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
            sendResponse(exchange, 405, "Method Not Allowed");
            return;
        }

        Map<String, String> data = readFormData(exchange);

        String username = data.get("username");
        String password = data.get("password");

        if (administrator.login(username, password)) {

            sendResponse(exchange, 200,
                    "{\"success\":true,\"message\":\"Admin login successful\"}");

        } else {

            sendResponse(exchange, 401,
                    "{\"success\":false,\"message\":\"Invalid username or password\"}");
        }
    }


    // =====================================================
    // BOOK APPOINTMENT
    // =====================================================

    static void bookAppointment(HttpExchange exchange)
            throws IOException {

        if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
            sendResponse(exchange, 405, "Method Not Allowed");
            return;
        }

        Map<String, String> data = readFormData(exchange);

        int patientId;
        int doctorId;

        try {
            patientId = Integer.parseInt(data.get("patientId"));
            doctorId = Integer.parseInt(data.get("doctorId"));
        } catch (Exception e) {
            sendResponse(exchange, 400,
                    "{\"success\":false,\"message\":\"Invalid ID\"}");
            return;
        }

        String date = data.get("date");
        String time = data.get("time");

        Patient patient = hospital.findPatient(patientId);
        Doctor doctor = hospital.findDoctor(doctorId);

        if (patient == null) {
            sendResponse(exchange, 404,
                    "{\"success\":false,\"message\":\"Patient not found\"}");
            return;
        }

        if (doctor == null) {
            sendResponse(exchange, 404,
                    "{\"success\":false,\"message\":\"Doctor not found\"}");
            return;
        }

        if (!hospital.isDoctorAvailable(doctorId, date, time)) {

            sendResponse(exchange, 409,
                    "{\"success\":false,\"message\":\"Doctor is not available\"}");

            return;
        }

        Appointment appointment =
                new Appointment(
                        nextAppointmentId++,
                        patient,
                        doctor,
                        date,
                        time
                );

        appointment.bookAppointment();

        hospital.addAppointment(appointment);

        String response =
                "{"
                        + "\"success\":true,"
                        + "\"message\":\"Appointment booked successfully\","
                        + "\"appointmentId\":"
                        + appointment.getAppointmentId()
                        + "}";

        sendResponse(exchange, 200, response);
    }


    // =====================================================
    // RESCHEDULE
    // =====================================================

    static void rescheduleAppointment(HttpExchange exchange)
            throws IOException {

        if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
            sendResponse(exchange, 405, "Method Not Allowed");
            return;
        }

        Map<String, String> data = readFormData(exchange);

        int appointmentId;

        try {
            appointmentId =
                    Integer.parseInt(data.get("appointmentId"));
        } catch (Exception e) {
            sendResponse(exchange, 400,
                    "{\"success\":false,\"message\":\"Invalid appointment ID\"}");
            return;
        }

        String newDate = data.get("date");
        String newTime = data.get("time");

        Appointment appointment =
                hospital.findAppointment(appointmentId);

        if (appointment == null) {
            sendResponse(exchange, 404,
                    "{\"success\":false,\"message\":\"Appointment not found\"}");
            return;
        }

        if (appointment.getStatus().equalsIgnoreCase("Cancelled")) {
            sendResponse(exchange, 400,
                    "{\"success\":false,\"message\":\"Cancelled appointment cannot be rescheduled\"}");
            return;
        }

        if (!hospital.isDoctorAvailable(
                appointment.getDoctor().getDoctorId(),
                newDate,
                newTime)) {

            sendResponse(exchange, 409,
                    "{\"success\":false,\"message\":\"Doctor is not available at the new time\"}");

            return;
        }

        appointment.rescheduleAppointment(newDate, newTime);

        sendResponse(exchange, 200,
                "{\"success\":true,\"message\":\"Appointment rescheduled successfully\"}");
    }


    // =====================================================
    // CANCEL APPOINTMENT
    // =====================================================

    static void cancelAppointment(HttpExchange exchange)
            throws IOException {

        if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
            sendResponse(exchange, 405, "Method Not Allowed");
            return;
        }

        Map<String, String> data = readFormData(exchange);

        int appointmentId;

        try {
            appointmentId =
                    Integer.parseInt(data.get("appointmentId"));
        } catch (Exception e) {
            sendResponse(exchange, 400,
                    "{\"success\":false,\"message\":\"Invalid appointment ID\"}");
            return;
        }

        Appointment appointment =
                hospital.findAppointment(appointmentId);

        if (appointment == null) {
            sendResponse(exchange, 404,
                    "{\"success\":false,\"message\":\"Appointment not found\"}");
            return;
        }

        appointment.cancelAppointment();

        sendResponse(exchange, 200,
                "{\"success\":true,\"message\":\"Appointment cancelled successfully\"}");
    }


    // =====================================================
    // PATIENT APPOINTMENTS
    // =====================================================

    static void patientAppointments(HttpExchange exchange)
            throws IOException {

        String query = exchange.getRequestURI().getQuery();

        Map<String, String> data =
                parseForm(query);

        int patientId;

        try {
            patientId =
                    Integer.parseInt(data.get("patientId"));
        } catch (Exception e) {
            sendResponse(exchange, 400, "Invalid patient ID");
            return;
        }

        StringBuilder json = new StringBuilder("[");
        boolean first = true;

        for (Appointment a : hospital.getAppointments()) {

            if (a.getPatient().getPatientId() == patientId) {

                if (!first) {
                    json.append(",");
                }

                json.append(appointmentJSON(a));

                first = false;
            }
        }

        json.append("]");

        sendResponse(exchange, 200, json.toString());
    }


    // =====================================================
    // DOCTOR APPOINTMENTS
    // =====================================================

    static void doctorAppointments(HttpExchange exchange)
            throws IOException {

        String query = exchange.getRequestURI().getQuery();

        Map<String, String> data =
                parseForm(query);

        int doctorId;

        try {
            doctorId =
                    Integer.parseInt(data.get("doctorId"));
        } catch (Exception e) {
            sendResponse(exchange, 400, "Invalid doctor ID");
            return;
        }

        StringBuilder json = new StringBuilder("[");
        boolean first = true;

        for (Appointment a : hospital.getAppointments()) {

            if (a.getDoctor().getDoctorId() == doctorId) {

                if (!first) {
                    json.append(",");
                }

                json.append(appointmentJSON(a));

                first = false;
            }
        }

        json.append("]");

        sendResponse(exchange, 200, json.toString());
    }


    // =====================================================
    // MEDICAL RECORD
    // =====================================================

    static void medicalRecord(HttpExchange exchange)
            throws IOException {

        if (exchange.getRequestMethod().equalsIgnoreCase("GET")) {

            String query =
                    exchange.getRequestURI().getQuery();

            Map<String, String> data =
                    parseForm(query);

            int patientId;

            try {
                patientId =
                        Integer.parseInt(data.get("patientId"));
            } catch (Exception e) {
                sendResponse(exchange, 400,
                        "{\"success\":false,\"message\":\"Invalid patient ID\"}");
                return;
            }

            MedicalRecord record =
                    hospital.findMedicalRecord(patientId);

            if (record == null) {

                sendResponse(exchange, 404,
                        "{\"success\":false,\"message\":\"No medical record found\"}");

                return;
            }

            String json =
                    "{"
                            + "\"recordId\":" + record.getRecordId() + ","
                            + "\"patientId\":" + record.getPatientId() + ","
                            + "\"diagnosis\":\""
                            + escape(record.getDiagnosis()) + "\","
                            + "\"prescription\":\""
                            + escape(record.getPrescription()) + "\","
                            + "\"history\":\""
                            + escape(record.getMedicalHistory()) + "\","
                            + "\"date\":\""
                            + escape(record.getRecordDate()) + "\""
                            + "}";

            sendResponse(exchange, 200, json);

        } else if (exchange.getRequestMethod().equalsIgnoreCase("POST")) {

            Map<String, String> data =
                    readFormData(exchange);

            int patientId;

            try {
                patientId =
                        Integer.parseInt(data.get("patientId"));
            } catch (Exception e) {
                sendResponse(exchange, 400,
                        "{\"success\":false,\"message\":\"Invalid patient ID\"}");
                return;
            }

            String diagnosis = data.get("diagnosis");
            String prescription = data.get("prescription");
            String history = data.get("history");
            String date = data.get("date");

            MedicalRecord record =
                    hospital.findMedicalRecord(patientId);

            if (record == null) {

                record =
                        new MedicalRecord(
                                nextRecordId++,
                                patientId,
                                diagnosis,
                                prescription,
                                history,
                                date
                        );

                hospital.addMedicalRecord(record);

            } else {

                record.updateRecord(
                        diagnosis,
                        prescription,
                        history,
                        date
                );
            }

            sendResponse(exchange, 200,
                    "{\"success\":true,\"message\":\"Medical record saved successfully\"}");
        }
    }


    // =====================================================
    // ADMIN - PATIENTS
    // =====================================================

    static void adminPatients(HttpExchange exchange)
            throws IOException {

        StringBuilder json = new StringBuilder("[");
        boolean first = true;

        for (Patient p : hospital.getPatients()) {

            if (!first) {
                json.append(",");
            }

            json.append("{")
                    .append("\"id\":").append(p.getPatientId()).append(",")
                    .append("\"name\":\"").append(escape(p.getName())).append("\",")
                    .append("\"phone\":\"").append(escape(p.getPhone())).append("\",")
                    .append("\"email\":\"").append(escape(p.getEmail())).append("\",")
                    .append("\"disease\":\"").append(escape(p.getDisease())).append("\"")
                    .append("}");

            first = false;
        }

        json.append("]");

        sendResponse(exchange, 200, json.toString());
    }


    // =====================================================
    // ADMIN - DOCTORS
    // =====================================================

    static void adminDoctors(HttpExchange exchange)
            throws IOException {

        getDoctors(exchange);
    }


    // =====================================================
    // ADMIN - ADD DOCTOR
    // =====================================================

    static void addDoctor(HttpExchange exchange)
            throws IOException {

        if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
            sendResponse(exchange, 405, "Method Not Allowed");
            return;
        }

        Map<String, String> data =
                readFormData(exchange);

        String name = data.get("name");
        String phone = data.get("phone");
        String email = data.get("email");
        String specialization = data.get("specialization");
        String department = data.get("department");

        int experience;

        try {
            experience =
                    Integer.parseInt(data.get("experience"));
        } catch (Exception e) {
            experience = 0;
        }

        Doctor doctor =
                new Doctor(
                        nextDoctorId++,
                        name,
                        phone,
                        email,
                        specialization,
                        department,
                        experience
                );

        hospital.addDoctor(doctor);

        sendResponse(exchange, 200,
                "{"
                        + "\"success\":true,"
                        + "\"doctorId\":" + doctor.getDoctorId()
                        + "}");
    }


    // =====================================================
    // ADMIN - REMOVE DOCTOR
    // =====================================================

    static void removeDoctor(HttpExchange exchange)
            throws IOException {

        if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
            sendResponse(exchange, 405, "Method Not Allowed");
            return;
        }

        Map<String, String> data =
                readFormData(exchange);

        int doctorId;

        try {
            doctorId =
                    Integer.parseInt(data.get("doctorId"));
        } catch (Exception e) {
            sendResponse(exchange, 400,
                    "{\"success\":false,\"message\":\"Invalid doctor ID\"}");
            return;
        }

        Doctor doctor =
                hospital.findDoctor(doctorId);

        if (doctor == null) {

            sendResponse(exchange, 404,
                    "{\"success\":false,\"message\":\"Doctor not found\"}");

            return;
        }

        hospital.removeDoctor(doctorId);

        sendResponse(exchange, 200,
                "{\"success\":true,\"message\":\"Doctor removed successfully\"}");
    }


    // =====================================================
    // ADMIN REPORT
    // =====================================================

    static void report(HttpExchange exchange)
            throws IOException {

        String json =
                "{"
                        + "\"hospital\":\"City Care Hospital\","
                        + "\"patients\":" + hospital.getPatients().size() + ","
                        + "\"doctors\":" + hospital.getDoctors().size() + ","
                        + "\"appointments\":" + hospital.getAppointments().size() + ","
                        + "\"records\":" + hospital.getMedicalRecords().size()
                        + "}";

        sendResponse(exchange, 200, json);
    }


    // =====================================================
    // APPOINTMENT JSON
    // =====================================================

    static String appointmentJSON(Appointment a) {

        return "{"
                + "\"id\":" + a.getAppointmentId() + ","
                + "\"patient\":\""
                + escape(a.getPatient().getName()) + "\","
                + "\"doctor\":\""
                + escape(a.getDoctor().getName()) + "\","
                + "\"date\":\""
                + escape(a.getDate()) + "\","
                + "\"time\":\""
                + escape(a.getTime()) + "\","
                + "\"status\":\""
                + escape(a.getStatus()) + "\""
                + "}";
    }


    // =====================================================
    // READ FORM DATA
    // =====================================================

    static Map<String, String> readFormData(
            HttpExchange exchange)
            throws IOException {

        InputStream input =
                exchange.getRequestBody();

        String body =
                new String(
                        input.readAllBytes(),
                        StandardCharsets.UTF_8
                );

        return parseForm(body);
    }


    // =====================================================
    // PARSE FORM
    // =====================================================

    static Map<String, String> parseForm(String data) {

        Map<String, String> map =
                new HashMap<>();

        if (data == null || data.isEmpty()) {
            return map;
        }

        String[] pairs =
                data.split("&");

        for (String pair : pairs) {

            String[] parts =
                    pair.split("=", 2);

            if (parts.length == 2) {

                String key =
                        URLDecoder.decode(
                                parts[0],
                                StandardCharsets.UTF_8
                        );

                String value =
                        URLDecoder.decode(
                                parts[1],
                                StandardCharsets.UTF_8
                        );

                map.put(key, value);
            }
        }

        return map;
    }

    static Map<String, String> readJsonData(
            HttpExchange exchange)
            throws IOException {

        String body =
                new String(
                        exchange.getRequestBody().readAllBytes(),
                        StandardCharsets.UTF_8
                );

        return parseJsonObject(body);
    }

    static Map<String, String> parseJsonObject(String json) {

        Map<String, String> map = new HashMap<>();
        int[] index = {0};

        skipJsonWhitespace(json, index);
        expectJsonCharacter(json, index, '{');
        skipJsonWhitespace(json, index);

        if (consumeJsonCharacter(json, index, '}')) {
            skipJsonWhitespace(json, index);
            if (index[0] != json.length()) {
                throw new IllegalArgumentException("Unexpected content after JSON object");
            }
            return map;
        }

        while (true) {
            skipJsonWhitespace(json, index);
            String key = parseJsonString(json, index);
            skipJsonWhitespace(json, index);
            expectJsonCharacter(json, index, ':');
            skipJsonWhitespace(json, index);
            String value = parseJsonString(json, index);
            map.put(key, value);
            skipJsonWhitespace(json, index);

            if (consumeJsonCharacter(json, index, '}')) {
                break;
            }

            expectJsonCharacter(json, index, ',');
        }

        skipJsonWhitespace(json, index);
        if (index[0] != json.length()) {
            throw new IllegalArgumentException("Unexpected content after JSON object");
        }

        return map;
    }

    private static String parseJsonString(String json, int[] index) {

        expectJsonCharacter(json, index, '"');
        StringBuilder value = new StringBuilder();

        while (index[0] < json.length()) {
            char character = json.charAt(index[0]++);

            if (character == '"') {
                return value.toString();
            }

            if (character < 0x20) {
                throw new IllegalArgumentException("Unescaped control character in JSON string");
            }

            if (character != '\\') {
                value.append(character);
                continue;
            }

            if (index[0] >= json.length()) {
                throw new IllegalArgumentException("Incomplete JSON escape");
            }

            char escaped = json.charAt(index[0]++);
            switch (escaped) {
                case '"':
                case '\\':
                case '/':
                    value.append(escaped);
                    break;
                case 'b':
                    value.append('\b');
                    break;
                case 'f':
                    value.append('\f');
                    break;
                case 'n':
                    value.append('\n');
                    break;
                case 'r':
                    value.append('\r');
                    break;
                case 't':
                    value.append('\t');
                    break;
                case 'u':
                    if (index[0] + 4 > json.length()) {
                        throw new IllegalArgumentException("Incomplete Unicode escape");
                    }
                    try {
                        value.append((char) Integer.parseInt(
                                json.substring(index[0], index[0] + 4), 16));
                    } catch (NumberFormatException e) {
                        throw new IllegalArgumentException("Invalid Unicode escape", e);
                    }
                    index[0] += 4;
                    break;
                default:
                    throw new IllegalArgumentException("Invalid JSON escape");
            }
        }

        throw new IllegalArgumentException("Unterminated JSON string");
    }

    private static void skipJsonWhitespace(String json, int[] index) {
        while (index[0] < json.length()
                && (json.charAt(index[0]) == ' '
                || json.charAt(index[0]) == '\n'
                || json.charAt(index[0]) == '\r'
                || json.charAt(index[0]) == '\t')) {
            index[0]++;
        }
    }

    private static boolean consumeJsonCharacter(
            String json, int[] index, char expected) {
        if (index[0] < json.length() && json.charAt(index[0]) == expected) {
            index[0]++;
            return true;
        }
        return false;
    }

    private static void expectJsonCharacter(
            String json, int[] index, char expected) {
        if (!consumeJsonCharacter(json, index, expected)) {
            throw new IllegalArgumentException("Invalid JSON object structure");
        }
    }


    // =====================================================
    // SEND RESPONSE
    // =====================================================

    static void sendResponse(
            HttpExchange exchange,
            int status,
            String response)
            throws IOException {

        exchange.getResponseHeaders()
                .set("Access-Control-Allow-Origin", "*");

        exchange.getResponseHeaders()
                .set("Content-Type", "application/json");

        byte[] bytes =
                response.getBytes(StandardCharsets.UTF_8);

        exchange.sendResponseHeaders(
                status,
                bytes.length
        );

        OutputStream output =
                exchange.getResponseBody();

        output.write(bytes);
        output.close();
    }


    // =====================================================
    // ESCAPE JSON
    // =====================================================

    http://localhost:8080 static String escape(String text) {

        if (text == null) {
            return "";
        }

        return text
                .replace("\\", "\\\\")
                .replace("\"", "\\\"");
    }
}