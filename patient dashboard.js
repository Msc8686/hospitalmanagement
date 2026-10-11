/* =====================================================
   PATIENT DASHBOARD
   ===================================================== */

const API = "http://localhost:8080/api";


/* ---------- Safe HTML text ---------- */

function escapeHtml(value) {

    return String(value ?? "")
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;")
        .replaceAll("'", "&#039;");
}


/* ---------- API helper ---------- */

async function fetchJson(url, options = {}) {

    const response = await fetch(url, options);
    const data = await response.json();

    if (!response.ok) {
        throw new Error(data.message || "Request failed.");
    }

    return data;
}


/* ---------- Patient identity ---------- */

function getPatientId() {
    return localStorage.getItem("patientId");
}

function ensurePatientLogin() {

    const id = getPatientId();

    if (!id) {
        window.location.href = "index.html";
        return false;
    }

    document.getElementById("patientIdHeader").textContent = id;

    document.getElementById("patientName").textContent =
        localStorage.getItem("patientName") || "Patient";

    return true;
}


/* ---------- Logout ---------- */

function logoutPatient() {

    localStorage.removeItem("patientId");
    localStorage.removeItem("patientName");

    window.location.href = "index.html";
}


/* ---------- Status message ---------- */

function setFeedback(id, message, success) {

    const element = document.getElementById(id);

    element.textContent = message;

    element.className = success
        ? "feedback success"
        : "feedback error";
}


/* ---------- Startup ---------- */

document.addEventListener("DOMContentLoaded", () => {

    if (!ensurePatientLogin()) return;

    const today = new Date();

    const minDate =
        `${today.getFullYear()}-${
            String(today.getMonth() + 1).padStart(2, "0")
        }-${
            String(today.getDate()).padStart(2, "0")
        }`;

    document.getElementById("appointmentDate").min = minDate;
    document.getElementById("rescheduleDate").min = minDate;

    document.getElementById("appointmentForm")
        .addEventListener("submit", bookAppointment);

    document.getElementById("rescheduleForm")
        .addEventListener("submit", rescheduleAppointment);

    document.getElementById("cancelForm")
        .addEventListener("submit", cancelAppointment);

    loadDoctors();
    loadPatientAppointments();
    loadMedicalRecord();
});


/* =====================================================
   LOAD DOCTORS
   ===================================================== */

async function loadDoctors() {

    const select =
        document.getElementById("doctorSelect");

    try {

        const doctors = await fetchJson(`${API}/doctors`);

        select.innerHTML =
            '<option value="">Select a doctor</option>' +
            doctors.map(doctor => `
                <option value="${doctor.id}">
                    ${escapeHtml(doctor.name)}
                    — ${escapeHtml(doctor.specialization)}
                </option>
            `).join("");

    } catch (error) {

        select.innerHTML =
            '<option value="">Unable to load doctors</option>';

        setFeedback(
            "bookingFeedback",
            error.message,
            false
        );
    }
}


/* =====================================================
   BOOK APPOINTMENT
   ===================================================== */

async function bookAppointment(event) {

    event.preventDefault();

    const form =
        document.getElementById("appointmentForm");

    const data =
        new URLSearchParams(new FormData(form));

    data.set("patientId", getPatientId());

    try {

        const result = await fetchJson(
            `${API}/appointment/book`,
            {
                method: "POST",

                headers: {
                    "Content-Type":
                        "application/x-www-form-urlencoded"
                },

                body: data
            }
        );

        setFeedback(
            "bookingFeedback",
            `${result.message} Appointment ID: ${result.appointmentId}`,
            true
        );

        form.reset();

        await loadPatientAppointments();

    } catch (error) {

        setFeedback(
            "bookingFeedback",
            error.message,
            false
        );
    }
}


/* =====================================================
   LOAD PATIENT APPOINTMENTS
   ===================================================== */

async function loadPatientAppointments() {

    if (!ensurePatientLogin()) return;

    try {

        const appointments = await fetchJson(
            `${API}/appointments/patient?patientId=${
                encodeURIComponent(getPatientId())
            }`
        );

        renderAppointments(appointments);

    } catch (error) {

        document.getElementById("appointments").innerHTML =
            `<p class="error-state">${escapeHtml(error.message)}</p>`;
    }
}


/* ---------- Appointment table ---------- */

function renderAppointments(appointments) {

    const container =
        document.getElementById("appointments");

    if (!appointments.length) {

        container.innerHTML =
            '<p class="empty-state">You have no appointments yet.</p>';

        return;
    }

    let html = `
        <div class="table-wrap">
            <table>
                <thead>
                    <tr>
                        <th>Appointment ID</th>
                        <th>Doctor</th>
                        <th>Date</th>
                        <th>Time</th>
                        <th>Status</th>
                        <th>Action</th>
                    </tr>
                </thead>
                <tbody>
    `;

    appointments.forEach(appointment => {

        const date =
            escapeHtml(appointment.date);

        const time =
            escapeHtml(appointment.time);

        html += `
            <tr>
                <td>${appointment.id}</td>
                <td>${escapeHtml(appointment.doctor)}</td>
                <td>${date}</td>
                <td>${time}</td>
                <td>${escapeHtml(appointment.status)}</td>

                <td>
                    <button class="action-btn"
                            type="button"
                            onclick="prefillAppointment(
                                ${appointment.id},
                                '${date}',
                                '${time}'
                            )">
                        Reschedule
                    </button>

                    <button class="action-btn danger"
                            type="button"
                            onclick="quickCancel(${appointment.id})">
                        Cancel
                    </button>
                </td>
            </tr>
        `;
    });

    container.innerHTML =
        html + "</tbody></table></div>";
}


/* ---------- Fill the reschedule form ---------- */

function prefillAppointment(id, date, time) {

    document.getElementById("rescheduleId").value = id;
    document.getElementById("rescheduleDate").value = date;
    document.getElementById("rescheduleTime").value = time;

    document.getElementById("rescheduleForm").scrollIntoView({
        behavior: "smooth",
        block: "center"
    });
}


/* =====================================================
   RESCHEDULE APPOINTMENT
   ===================================================== */

async function rescheduleAppointment(event) {

    event.preventDefault();

    const form =
        document.getElementById("rescheduleForm");

    try {

        const result = await fetchJson(
            `${API}/appointment/reschedule`,
            {
                method: "POST",

                headers: {
                    "Content-Type":
                        "application/x-www-form-urlencoded"
                },

                body: new URLSearchParams(
                    new FormData(form)
                )
            }
        );

        setFeedback(
            "rescheduleFeedback",
            result.message,
            true
        );

        await loadPatientAppointments();

    } catch (error) {

        setFeedback(
            "rescheduleFeedback",
            error.message,
            false
        );
    }
}


/* =====================================================
   CANCEL APPOINTMENT
   ===================================================== */

async function quickCancel(appointmentId) {

    if (!window.confirm(
            `Cancel appointment ${appointmentId}?`
    )) {
        return;
    }

    await cancelAppointmentRequest(appointmentId);
}

async function cancelAppointment(event) {

    event.preventDefault();

    const appointmentId =
        new FormData(
            document.getElementById("cancelForm")
        ).get("appointmentId");

    await cancelAppointmentRequest(appointmentId);
}

async function cancelAppointmentRequest(appointmentId) {

    try {

        const body = new URLSearchParams({
            appointmentId: String(appointmentId)
        });

        const result = await fetchJson(
            `${API}/appointment/cancel`,
            {
                method: "POST",

                headers: {
                    "Content-Type":
                        "application/x-www-form-urlencoded"
                },

                body
            }
        );

        setFeedback(
            "cancelFeedback",
            result.message,
            true
        );

        document.getElementById("cancelForm").reset();

        await loadPatientAppointments();

    } catch (error) {

        setFeedback(
            "cancelFeedback",
            error.message,
            false
        );
    }
}


/* =====================================================
   LOAD MEDICAL RECORD
   ===================================================== */

async function loadMedicalRecord() {

    if (!ensurePatientLogin()) return;

    try {

        const record = await fetchJson(
            `${API}/record?patientId=${
                encodeURIComponent(getPatientId())
            }`
        );

        document.getElementById("medicalRecord").innerHTML = `
            <div class="info-grid">

                <p><strong>Record ID:</strong> ${record.recordId}</p>

                <p><strong>Date:</strong>
                    ${escapeHtml(record.date)}
                </p>

                <p><strong>Diagnosis:</strong>
                    ${escapeHtml(record.diagnosis)}
                </p>

                <p><strong>Prescription:</strong>
                    ${escapeHtml(record.prescription)}
                </p>

                <p class="full">
                    <strong>Medical History:</strong>
                    ${escapeHtml(record.history)}
                </p>

            </div>
        `;

    } catch (error) {

        document.getElementById("medicalRecord").innerHTML =
            `<p class="empty-state">${escapeHtml(error.message)}</p>`;
    }
}