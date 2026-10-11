/* Doctor dashboard: appointments, patient details and medical records. */

const API = "http://localhost:8080/api";


/* ---------- Date ---------- */

function todayString() {

    const now = new Date();

    return `${now.getFullYear()}-${
        String(now.getMonth() + 1).padStart(2, "0")
    }-${
        String(now.getDate()).padStart(2, "0")
    }`;
}


/* ---------- Safe display of text ---------- */

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


/* ---------- Check doctor login ---------- */

function requireDoctorLogin() {

    const doctorId = localStorage.getItem("doctorId");

    if (!doctorId) {
        window.location.href = "index.html";
        return null;
    }

    document.getElementById("doctorIdDisplay").textContent =
        doctorId;

    document.getElementById("doctorName").textContent =
        localStorage.getItem("doctorName") || "Doctor";

    return doctorId;
}


/* ---------- Logout ---------- */

function logoutDoctor() {

    localStorage.removeItem("doctorId");
    localStorage.removeItem("doctorName");

    window.location.href = "index.html";
}


/* ---------- Start dashboard ---------- */

document.addEventListener("DOMContentLoaded", () => {

    document.getElementById("recordDate").value =
        todayString();

    document.getElementById("recordForm")
        .addEventListener("submit", saveMedicalRecord);

    loadDoctorDashboard();
});


/* ---------- Load appointments ---------- */

async function loadDoctorDashboard() {

    const doctorId = requireDoctorLogin();

    if (!doctorId) return;

    try {

        const appointments = await fetchJson(
            `${API}/appointments/doctor?doctorId=${
                encodeURIComponent(doctorId)
            }`
        );

        const todayAppointments = appointments.filter(
            item => item.date === todayString()
        );

        const patientIds = new Set(
            appointments.map(item => item.patientId)
        );

        document.getElementById("todayCount").textContent =
            todayAppointments.length;

        document.getElementById("totalCount").textContent =
            appointments.length;

        document.getElementById("patientCount").textContent =
            patientIds.size;

        renderAppointments(
            todayAppointments,
            "todayAppointments"
        );

        renderAppointments(
            appointments,
            "allAppointments"
        );

    } catch (error) {

        showError("todayAppointments", error.message);
        showError("allAppointments", error.message);
    }
}


/* ---------- Render appointment table ---------- */

function renderAppointments(appointments, containerId) {

    const container = document.getElementById(containerId);

    if (!appointments.length) {
        container.innerHTML =
            '<p class="empty-state">No appointments found for this view.</p>';
        return;
    }

    let html = `
        <div class="table-wrap">
            <table>
                <thead>
                    <tr>
                        <th>ID</th>
                        <th>Patient</th>
                        <th>Patient ID</th>
                        <th>Date</th>
                        <th>Time</th>
                        <th>Status</th>
                        <th>Actions</th>
                    </tr>
                </thead>
                <tbody>
    `;

    appointments.forEach(item => {

        html += `
            <tr>
                <td>${item.id}</td>
                <td>${escapeHtml(item.patient)}</td>
                <td>${item.patientId}</td>
                <td>${escapeHtml(item.date)}</td>
                <td>${escapeHtml(item.time)}</td>
                <td>
                    <span class="status">
                        ${escapeHtml(item.status)}
                    </span>
                </td>
                <td class="actions">

                    <button class="action-btn"
                            type="button"
                            onclick="viewPatient(${item.patientId})">
                        Patient Details
                    </button>

                    <button class="action-btn secondary"
                            type="button"
                            onclick="viewRecord(${item.patientId})">
                        View Record
                    </button>

                    <button class="action-btn light"
                            type="button"
                            onclick="selectPatient(${item.patientId})">
                        Edit Record
                    </button>

                </td>
            </tr>
        `;
    });

    container.innerHTML =
        html + "</tbody></table></div>";
}


/* ---------- View patient details ---------- */

async function viewPatient(patientId) {

    try {

        const patient = await fetchJson(
            `${API}/doctor/patient?patientId=${
                encodeURIComponent(patientId)
            }`
        );

        document.getElementById("patientInformation").innerHTML = `
            <div class="info-grid">

                <p><strong>Patient ID:</strong> ${patient.patientId}</p>
                <p><strong>Name:</strong> ${escapeHtml(patient.name)}</p>
                <p><strong>Phone:</strong> ${escapeHtml(patient.phone)}</p>
                <p><strong>Email:</strong> ${escapeHtml(patient.email)}</p>

                <p>
                    <strong>Health Problem:</strong>
                    ${escapeHtml(patient.disease)}
                </p>

                <p>
                    <strong>Blood Group:</strong>
                    ${escapeHtml(patient.bloodGroup)}
                </p>

                <p>
                    <strong>Address:</strong>
                    ${escapeHtml(patient.address)}
                </p>

                <p>
                    <strong>Emergency Contact:</strong>
                    ${escapeHtml(patient.emergencyContact)}
                </p>

            </div>
        `;

    } catch (error) {

        showError("patientInformation", error.message);
    }
}


/* ---------- View medical record ---------- */

async function viewRecord(patientId) {

    try {

        const record = await fetchJson(
            `${API}/record?patientId=${
                encodeURIComponent(patientId)
            }`
        );

        document.getElementById("medicalRecord").innerHTML = `
            <div class="info-grid">

                <p><strong>Record ID:</strong> ${record.recordId}</p>
                <p><strong>Patient ID:</strong> ${patientId}</p>

                <p>
                    <strong>Diagnosis:</strong>
                    ${escapeHtml(record.diagnosis)}
                </p>

                <p>
                    <strong>Prescription:</strong>
                    ${escapeHtml(record.prescription)}
                </p>

                <p>
                    <strong>Medical History:</strong>
                    ${escapeHtml(record.history)}
                </p>

                <p>
                    <strong>Date:</strong>
                    ${escapeHtml(record.date)}
                </p>

            </div>
        `;

    } catch (error) {

        showError("medicalRecord", error.message);
    }
}


/* ---------- Select a patient for editing ---------- */

async function selectPatient(patientId) {

    document.getElementById("recordPatientId").value =
        patientId;

    document.getElementById("recordDate").value =
        todayString();

    try {

        const record = await fetchJson(
            `${API}/record?patientId=${
                encodeURIComponent(patientId)
            }`
        );

        document.getElementById("diagnosis").value =
            record.diagnosis || "";

        document.getElementById("prescription").value =
            record.prescription || "";

        document.getElementById("history").value =
            record.history || "";

        document.getElementById("recordDate").value =
            record.date || todayString();

    } catch (_) {

        document.getElementById("diagnosis").value = "";
        document.getElementById("prescription").value = "";
        document.getElementById("history").value = "";
    }

    document.getElementById("recordForm").scrollIntoView({
        behavior: "smooth",
        block: "start"
    });
}


/* ---------- Save or update medical record ---------- */

async function saveMedicalRecord(event) {

    event.preventDefault();

    const form =
        document.getElementById("recordForm");

    const body =
        new URLSearchParams(new FormData(form));

    const feedback =
        document.getElementById("recordMessage");

    try {

        const result = await fetchJson(
            `${API}/record`,
            {
                method: "POST",

                headers: {
                    "Content-Type":
                        "application/x-www-form-urlencoded"
                },

                body
            }
        );

        feedback.textContent = result.message;
        feedback.className = "feedback success";

        await viewRecord(
            document.getElementById("recordPatientId").value
        );

    } catch (error) {

        feedback.textContent = error.message;
        feedback.className = "feedback error";
    }
}


/* ---------- Show errors ---------- */

function showError(containerId, message) {

    document.getElementById(containerId).innerHTML =
        `<p class="error-state">${escapeHtml(message)}</p>`;
}